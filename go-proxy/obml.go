package main

import (
	"bytes"
	"encoding/binary"
	htmlpkg "html"
	"io"
	"net/http"
	"net/url"
	"strings"
	"unicode/utf8"

	xhtml "golang.org/x/net/html"
)

const obmlMagic = "TBO1"
const maxOBMLBlocks = 2500
const maxOBMLString = 65535

type obmlBlock struct {
	kind byte
	text string
	url  string
}

func obmlAbsolute(base, ref string) string {
	if ref == "" {
		return ""
	}
	b, err := url.Parse(base)
	if err != nil {
		return ""
	}
	r, err := url.Parse(strings.TrimSpace(ref))
	if err != nil {
		return ""
	}
	u := b.ResolveReference(r)
	if (u.Scheme != "http" && u.Scheme != "https") || u.User != nil {
		return ""
	}
	u.Fragment = ""
	return u.String()
}

func buildOBMLPage(pageURL, source string) ([]byte, error) {
	doc, err := xhtml.Parse(strings.NewReader(source))
	if err != nil {
		return nil, err
	}

	title := pageURL
	var blocks []obmlBlock
	add := func(kind byte, text, link string) {
		text = strings.TrimSpace(strings.Join(strings.Fields(text), " "))
		if len(blocks) >= maxOBMLBlocks || (text == "" && kind != 6 && kind != 5) {
			return
		}
		if len(text) > maxOBMLString {
			text = text[:maxOBMLString]
		}
		// For images, rewrite URL through the compression proxy
		if kind == 5 && link != "" {
			link = rewriteImageURL(link)
		}
		blocks = append(blocks, obmlBlock{kind: kind, text: text, url: link})
	}

	var walk func(*xhtml.Node, byte, bool, string)
	walk = func(n *xhtml.Node, kind byte, inLink bool, linkURL string) {
		if n.Type == xhtml.TextNode {
			txt := htmlpkg.UnescapeString(n.Data)
			if !inLink {
				add(kind, txt, "")
			}
			return
		}
		if n.Type != xhtml.ElementNode {
			for c := n.FirstChild; c != nil; c = c.NextSibling {
				walk(c, kind, inLink, linkURL)
			}
			return
		}

		tag := strings.ToLower(n.Data)
		if tag == "script" || tag == "style" || tag == "noscript" || tag == "iframe" || tag == "svg" || tag == "canvas" || tag == "video" || tag == "audio" || tag == "nav" || tag == "footer" || tag == "header" || tag == "form" {
			return
		}

		newKind := kind
		switch tag {
		case "h1":
			newKind = 1
		case "h2", "h3", "h4", "h5", "h6", "strong", "b":
			newKind = 2
		case "li":
			add(3, "•", "")
		case "p", "div", "br", "hr", "tr", "section", "article":
			if !inLink {
				add(6, "", "")
			}
		}

		if tag == "img" {
			var src, alt string
			for _, a := range n.Attr {
				if strings.EqualFold(a.Key, "src") {
					src = a.Val
				}
				if strings.EqualFold(a.Key, "alt") {
					alt = a.Val
				}
			}
			imageURL := obmlAbsolute(pageURL, src)
			if imageURL != "" {
				add(5, alt, imageURL)
			}
			return
		}

		if tag == "a" {
			var href string
			for _, a := range n.Attr {
				if strings.EqualFold(a.Key, "href") {
					href = a.Val
				}
			}
			linkURL = obmlAbsolute(pageURL, href)
			inLink = linkURL != ""
			var linkText strings.Builder
			var collect func(*xhtml.Node)
			collect = func(nn *xhtml.Node) {
				if nn.Type == xhtml.TextNode {
					linkText.WriteString(htmlpkg.UnescapeString(nn.Data))
				}
				for c := nn.FirstChild; c != nil; c = c.NextSibling {
					collect(c)
				}
			}
			collect(n)
			if inLink {
				add(4, linkText.String(), linkURL)
			}
			return
		}

		if tag == "title" && title == pageURL {
			var tb strings.Builder
			for c := n.FirstChild; c != nil; c = c.NextSibling {
				if c.Type == xhtml.TextNode {
					tb.WriteString(c.Data)
				}
			}
			title = strings.TrimSpace(htmlpkg.UnescapeString(tb.String()))
			return
		}

		for c := n.FirstChild; c != nil; c = c.NextSibling {
			walk(c, newKind, inLink, linkURL)
		}
	}

	walk(doc, 0, false, "")

	var out bytes.Buffer
	out.WriteString(obmlMagic)
	writeOBMLString(&out, title)
	if len(blocks) > 65535 {
		blocks = blocks[:65535]
	}
	_ = binary.Write(&out, binary.BigEndian, uint16(len(blocks)))
	for _, b := range blocks {
		out.WriteByte(b.kind)
		writeOBMLString(&out, b.text)
		writeOBMLString(&out, b.url)
	}
	return out.Bytes(), nil
}

func writeOBMLString(out *bytes.Buffer, value string) {
	if !utf8.ValidString(value) {
		value = strings.ToValidUTF8(value, "\ufffd")
	}
	if len(value) > maxOBMLString {
		value = value[:maxOBMLString]
		for !utf8.ValidString(value) {
			value = value[:len(value)-1]
		}
	}
	_ = binary.Write(out, binary.BigEndian, uint16(len(value)))
	out.WriteString(value)
}

func obmlResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil || resp.Request.Header.Get("X-TGFN-Format") != "TBO1" ||
		resp.StatusCode < 200 || resp.StatusCode >= 300 || resp.Body == nil {
		return nil
	}
	mediaType := strings.ToLower(strings.TrimSpace(strings.Split(resp.Header.Get("Content-Type"), ";")[0]))
	if mediaType != "text/html" && mediaType != "application/xhtml+xml" {
		return nil
	}
	data, err := io.ReadAll(io.LimitReader(resp.Body, maxCompactHTMLBytes+1))
	if err != nil {
		return err
	}
	if len(data) > maxCompactHTMLBytes {
		resp.Body = io.NopCloser(io.MultiReader(bytes.NewReader(data), resp.Body))
		return nil
	}
	_ = resp.Body.Close()
	pageURL := resp.Request.URL.String()
	packet, err := buildOBMLPage(pageURL, string(data))
	if err != nil {
		return err
	}
	resp.Body = io.NopCloser(bytes.NewReader(packet))
	resp.ContentLength = int64(len(packet))
	resp.Header.Set("Content-Type", "application/x-tgfn-obml; version=1")
	resp.Header.Set("Content-Length", strconvItoa(len(packet)))
	resp.Header.Del("Content-Encoding")
	resp.Header.Del("Content-MD5")
	resp.Header.Del("ETag")
	return nil
}

func strconvItoa(n int) string {
	if n == 0 {
		return "0"
	}
	var b [20]byte
	i := len(b)
	for n > 0 {
		i--
		b[i] = byte('0' + n%10)
		n /= 10
	}
	return string(b[i:])
}
