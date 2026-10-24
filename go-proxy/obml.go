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
const maxOBMLBlocks = 1100
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
		if kind != 7 {
			text = strings.TrimSpace(strings.Join(strings.Fields(text), " "))
		}
		if len(blocks) >= maxOBMLBlocks || (text == "" && kind != 6 && kind != 5 && kind != 7) {
			return
		}
		if len(text) > maxOBMLString {
			text = text[:maxOBMLString]
			for !utf8.ValidString(text) { text = text[:len(text)-1] }
		}
		if kind == 5 && link != "" { link = rewriteImageURL(link) }
		blocks = append(blocks, obmlBlock{kind: kind, text: text, url: link})
	}
	attr := func(n *xhtml.Node, key string) string {
		for _, a := range n.Attr { if strings.EqualFold(a.Key, key) { return strings.TrimSpace(a.Val) } }
		return ""
	}
	hidden := func(n *xhtml.Node) bool {
		if attr(n, "hidden") != "" { return true }
		style := strings.ToLower(strings.ReplaceAll(attr(n, "style"), " ", ""))
		return strings.Contains(style, "display:none") || strings.Contains(style, "visibility:hidden")
	}
	textContent := func(root *xhtml.Node) string {
		var b strings.Builder
		var visit func(*xhtml.Node)
		visit = func(n *xhtml.Node) {
			if n.Type == xhtml.TextNode { b.WriteString(htmlpkg.UnescapeString(n.Data)); b.WriteByte(' ') }
			for c := n.FirstChild; c != nil; c = c.NextSibling { visit(c) }
		}
		visit(root)
		return strings.TrimSpace(strings.Join(strings.Fields(b.String()), " "))
	}
	var walk func(*xhtml.Node, byte, bool)
	walk = func(n *xhtml.Node, kind byte, inLink bool) {
		if n.Type == xhtml.TextNode {
			if !inLink { add(kind, htmlpkg.UnescapeString(n.Data), "") }
			return
		}
		if n.Type != xhtml.ElementNode {
			for c := n.FirstChild; c != nil; c = c.NextSibling { walk(c, kind, inLink) }
			return
		}
		tag := strings.ToLower(n.Data)
		if hidden(n) || tag == "script" || tag == "style" || tag == "noscript" || tag == "iframe" ||
			tag == "svg" || tag == "canvas" || tag == "video" || tag == "audio" || tag == "nav" ||
			tag == "footer" || tag == "header" || tag == "template" { return }

		if tag == "title" && title == pageURL { title = textContent(n); return }
		if tag == "form" {
			action := obmlAbsolute(pageURL, attr(n, "action"))
			if action == "" { action = pageURL }
			method := strings.ToLower(attr(n, "method"))
			if method == "" { method = "get" }
			if method != "get" { method = "get" } // MIDP form submission currently supports GET safely.
			formLabel := attr(n, "aria-label")
			if formLabel == "" { formLabel = attr(n, "name") }
			if formLabel == "" { formLabel = "Form" }
			var payload strings.Builder
			payload.WriteString(formLabel); payload.WriteByte('\x1f')
			payload.WriteString(action); payload.WriteByte('\x1f')
			payload.WriteString(method)
			var collect func(*xhtml.Node)
			collect = func(c *xhtml.Node) {
				if c.Type == xhtml.ElementNode {
					ct := strings.ToLower(c.Data)
					if ct == "input" || ct == "textarea" || ct == "select" {
						typ := strings.ToLower(attr(c, "type")); if typ == "" { typ = ct }
						name := attr(c, "name")
						if name != "" && typ != "submit" && typ != "button" && typ != "reset" && typ != "file" && typ != "image" {
							value := attr(c, "value")
							if ct == "textarea" { value = textContent(c) }
							if ct == "select" {
								for o := c.FirstChild; o != nil; o = o.NextSibling {
									if o.Type == xhtml.ElementNode && strings.EqualFold(o.Data, "option") {
										value = attr(o, "value"); if value == "" { value = textContent(o) }
										if attr(o, "selected") != "" { break }
									}
								}
							}
							label := attr(c, "aria-label"); if label == "" { label = attr(c, "placeholder") }; if label == "" { label = name }
							for _, v := range []string{label, typ, name, value} { payload.WriteByte('\x1f'); payload.WriteString(strings.ReplaceAll(strings.ReplaceAll(v, "\x1f", " "), "\x00", "")) }
						}
					}
				}
				for child := c.FirstChild; child != nil; child = child.NextSibling { collect(child) }
			}
			collect(n)
			add(7, payload.String(), action)
			return
		}
		newKind := kind
		switch tag {
		case "h1": newKind = 1
		case "h2", "h3", "h4", "h5", "h6", "strong", "b": newKind = 2
		case "li": add(3, "•", "")
		case "p", "div", "br", "hr", "tr", "section", "article", "table", "ul", "ol", "fieldset":
			if !inLink { add(6, "", "") }
		case "td", "th":
			if !inLink { add(6, "", "") }
		}
		if tag == "img" {
			src, alt := attr(n, "src"), attr(n, "alt")
			if src == "" { src = attr(n, "data-src") }
			if imageURL := obmlAbsolute(pageURL, src); imageURL != "" { add(5, alt, imageURL) }
			return
		}
		if tag == "input" {
			typ := strings.ToLower(attr(n, "type"))
			if typ == "" { typ = "text" }
			if typ == "hidden" || typ == "submit" || typ == "button" || typ == "reset" || typ == "file" { return }
			name := attr(n, "name"); label := attr(n, "aria-label")
			if label == "" { label = attr(n, "placeholder") }; if label == "" { label = attr(n, "value") }; if label == "" { label = name }; if label == "" { label = typ }
			add(8, "["+label+"] ("+typ+")", "")
			return
		}
		if tag == "textarea" || tag == "select" {
			label := attr(n, "aria-label"); if label == "" { label = attr(n, "name") }; if label == "" { label = tag }
			add(8, "["+label+"] ("+tag+")", "")
			return
		}
		if tag == "button" {
			label := textContent(n); if label == "" { label = attr(n, "value") }; if label == "" { label = "Submit" }
			add(8, "["+label+"]", "")
			return
		}
		if tag == "a" {
			linkURL := obmlAbsolute(pageURL, attr(n, "href"))
			label := textContent(n)
			if linkURL != "" { add(4, label, linkURL) } else if label != "" { add(kind, label, "") }
			return
		}
		for c := n.FirstChild; c != nil; c = c.NextSibling { walk(c, newKind, inLink) }
	}
	walk(doc, 0, false)
	var out bytes.Buffer
	out.WriteString(obmlMagic)
	writeOBMLString(&out, title)
	_ = binary.Write(&out, binary.BigEndian, uint16(len(blocks)))
	for _, b := range blocks { out.WriteByte(b.kind); writeOBMLString(&out, b.text); writeOBMLString(&out, b.url) }
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
