package main

import (
	"bytes"
	"encoding/binary"
	htmlpkg "html"
	"io"
	"net/http"
	"strings"

	xhtml "golang.org/x/net/html"
)

const ssrMagic = "SSR1"
const ssrModeValue = "SSR"

type ssrBlock struct {
	kind byte // 0=text, 1=h1, 2=h2, 3=list, 4=link, 5=image, 6=break
	text string
	url  string
}

// buildSSRPage does lightweight server-side layout extraction.
// It preserves more structure and produces blocks the client can paint
// with clearer visual hierarchy (closer to a rendered page than plain text).
func buildSSRPage(pageURL, source string) ([]byte, error) {
	doc, err := xhtml.Parse(strings.NewReader(source))
	if err != nil {
		return nil, err
	}

	title := pageURL
	var blocks []ssrBlock
	add := func(kind byte, text, link string) {
		text = strings.TrimSpace(strings.Join(strings.Fields(text), " "))
		if len(blocks) >= 3000 || (text == "" && kind != 6 && kind != 5) {
			return
		}
		if len(text) > 4000 {
			text = text[:4000]
		}
		if kind == 5 && link != "" {
			link = rewriteImageURL(link)
		}
		blocks = append(blocks, ssrBlock{kind: kind, text: text, url: link})
	}

	var walk func(*xhtml.Node, byte)
	walk = func(n *xhtml.Node, kind byte) {
		if n.Type == xhtml.TextNode {
			txt := htmlpkg.UnescapeString(n.Data)
			if strings.TrimSpace(txt) != "" {
				add(kind, txt, "")
			}
			return
		}
		if n.Type != xhtml.ElementNode {
			for c := n.FirstChild; c != nil; c = c.NextSibling {
				walk(c, kind)
			}
			return
		}

		tag := strings.ToLower(n.Data)
		if tag == "script" || tag == "style" || tag == "noscript" || tag == "iframe" ||
			tag == "svg" || tag == "canvas" || tag == "video" || tag == "audio" {
			return
		}

		newKind := kind
		switch tag {
		case "h1":
			newKind = 1
			add(6, "", "")
		case "h2", "h3", "h4":
			newKind = 2
			add(6, "", "")
		case "li":
			add(3, "•", "")
		case "p", "div", "br", "hr", "section", "article":
			add(6, "", "")
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
				add(6, "", "")
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
			linkURL := obmlAbsolute(pageURL, href)
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
			if linkURL != "" {
				add(4, linkText.String(), linkURL)
			} else if strings.TrimSpace(linkText.String()) != "" {
				add(kind, linkText.String(), "")
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
			walk(c, newKind)
		}
	}

	walk(doc, 0)

	var out bytes.Buffer
	out.WriteString(ssrMagic)
	writeSSRString(&out, title)
	if len(blocks) > 65535 {
		blocks = blocks[:65535]
	}
	_ = binary.Write(&out, binary.BigEndian, uint16(len(blocks)))
	for _, b := range blocks {
		out.WriteByte(b.kind)
		writeSSRString(&out, b.text)
		writeSSRString(&out, b.url)
	}
	return out.Bytes(), nil
}

func writeSSRString(out *bytes.Buffer, value string) {
	if len(value) > 65535 {
		value = value[:65535]
	}
	_ = binary.Write(out, binary.BigEndian, uint16(len(value)))
	out.WriteString(value)
}

func ssrResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil || resp.Request.Header.Get("X-TGFN-Format") != ssrModeValue {
		return nil
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 || resp.Body == nil {
		return nil
	}
	ct := strings.ToLower(resp.Header.Get("Content-Type"))
	if !strings.Contains(ct, "text/html") && !strings.Contains(ct, "xhtml") {
		return nil
	}
	data, err := io.ReadAll(io.LimitReader(resp.Body, maxCompactHTMLBytes+1))
	if err != nil || len(data) > maxCompactHTMLBytes {
		return err
	}
	_ = resp.Body.Close()
	packet, err := buildSSRPage(resp.Request.URL.String(), string(data))
	if err != nil {
		return err
	}
	resp.Body = io.NopCloser(bytes.NewReader(packet))
	resp.ContentLength = int64(len(packet))
	resp.Header.Set("Content-Type", "application/x-tgfn-ssr; version=1")
	resp.Header.Set("Content-Length", itoa(len(packet)))
	resp.Header.Del("Content-Encoding")
	return nil
}
