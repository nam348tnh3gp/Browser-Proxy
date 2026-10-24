package main

import (
	"bytes"
	"encoding/binary"
	htmlpkg "html"
	"io"
	"net/http"
	"net/url"
	"regexp"
	"strings"
	"unicode/utf8"

	xhtml "golang.org/x/net/html"
)

const obmlMagic = "TBO1"
const maxOBMLBlocks = 4000
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
	// Execute a tightly sandboxed subset of inline JavaScript before parsing so
	// document.write()/writeln() generated markup becomes part of the TBO1 page.
	// External scripts and browser APIs are intentionally unavailable.
	source = renderInlineJavaScript(pageURL, source)
	doc, err := xhtml.Parse(strings.NewReader(source))
	if err != nil {
		return nil, err
	}

	title := pageURL
	var blocks []obmlBlock
	tableRowCells := 0
	add := func(kind byte, text, link string) {
		if kind != 7 {
			text = strings.TrimSpace(strings.Join(strings.Fields(text), " "))
		}
		if len(blocks) >= maxOBMLBlocks || (text == "" && kind != 6 && kind != 5 && kind != 7) {
			return
		}
		// Coalesce adjacent inline text fragments of the same style. HTML parsers
		// split text around entities and inline tags; emitting each fragment as a
		// separate block creates a one-word-per-line layout on small Java ME screens.
		if kind != 6 && kind != 5 && kind != 7 && kind != 9 && len(blocks) > 0 {
			last := &blocks[len(blocks)-1]
			if last.kind == kind && last.url == "" && last.text != "" && text != "" {
				last.text += " " + text
				if len(last.text) > maxOBMLString {
					last.text = strings.ToValidUTF8(last.text[:maxOBMLString], "")
				}
				return
			}
		}
		if len(text) > maxOBMLString {
			text = text[:maxOBMLString]
			for !utf8.ValidString(text) {
				text = text[:len(text)-1]
			}
		}
		if kind == 5 && link != "" {
			if separator := strings.IndexByte(link, '\x1f'); separator >= 0 {
				imageURL := rewriteImageURL(link[:separator])
				link = imageURL + "\x1f" + link[separator+1:]
			} else {
				link = rewriteImageURL(link)
			}
		}
		blocks = append(blocks, obmlBlock{kind: kind, text: text, url: link})
	}

	attr := func(n *xhtml.Node, key string) string {
		for _, a := range n.Attr {
			if strings.EqualFold(a.Key, key) {
				return strings.TrimSpace(a.Val)
			}
		}
		return ""
	}
	hasAttr := func(n *xhtml.Node, key string) bool {
		for _, a := range n.Attr {
			if strings.EqualFold(a.Key, key) {
				return true
			}
		}
		return false
	}
	hidden := func(n *xhtml.Node) bool {
		for _, a := range n.Attr {
			if strings.EqualFold(a.Key, "hidden") {
				return true
			}
		}
		style := strings.ToLower(strings.ReplaceAll(attr(n, "style"), " ", ""))
		return strings.Contains(style, "display:none") || strings.Contains(style, "visibility:hidden")
	}
	// Extract simple inline stylesheet background images so banners and logos
	// implemented with CSS are retained in the compact document flow.
	cssBackgrounds := make(map[string]string)
	cssTextStyles := make(map[string]byte)
	cssColors := make(map[string]string)
	cssRulePattern := regexp.MustCompile(`(?is)([^{}]+)\{([^{}]*)\}`)
	cssURLPattern := regexp.MustCompile(`(?is)url\(\s*['"]?([^)'"]+)['"]?\s*\)`)
	cssStyleKind := func(declarations string) byte {
		value := strings.ToLower(strings.ReplaceAll(declarations, " ", ""))
		switch {
		case strings.Contains(value, "text-decoration:line-through"):
			return 15
		case strings.Contains(value, "text-decoration:underline"):
			return 17
		case strings.Contains(value, "font-family:monospace"), strings.Contains(value, "font-family:courier"):
			return 11
		case strings.Contains(value, "font-style:italic"):
			return 10
		case strings.Contains(value, "font-weight:bold"), strings.Contains(value, "font-weight:600"), strings.Contains(value, "font-weight:700"), strings.Contains(value, "font-weight:800"):
			return 2
		case strings.Contains(value, "font-size:smaller"), strings.Contains(value, "font-size:x-small"), strings.Contains(value, "font-size:xx-small"):
			return 18
		case strings.Contains(value, "font-size:larger"), strings.Contains(value, "font-size:x-large"), strings.Contains(value, "font-size:xx-large"):
			return 19
		case strings.Contains(value, "background-color:yellow"), strings.Contains(value, "background-color:#ff0"), strings.Contains(value, "background-color:#ffff00"):
			return 16
		case strings.Contains(value, "text-align:center"):
			return 20
		case strings.Contains(value, "text-align:right"):
			return 21
		}
		return 0
	}
	var collectStyles func(*xhtml.Node)
	collectStyles = func(node *xhtml.Node) {
		if node.Type == xhtml.ElementNode && strings.EqualFold(node.Data, "style") {
			var css strings.Builder
			for child := node.FirstChild; child != nil; child = child.NextSibling {
				if child.Type == xhtml.TextNode {
					css.WriteString(child.Data)
				}
			}
			for _, rule := range cssRulePattern.FindAllStringSubmatch(css.String(), -1) {
				declarations := rule[2]
				urlMatch := cssURLPattern.FindStringSubmatch(declarations)
				styleKind := cssStyleKind(declarations)
				if len(urlMatch) < 2 && styleKind == 0 {
					continue
				}
				for _, selector := range strings.Split(rule[1], ",") {
					selector = strings.ToLower(strings.TrimSpace(selector))
					// Support simple tag, class and ID selectors; skip combinators/pseudos.
					if selector == "" || strings.ContainsAny(selector, " >+~:[") {
						continue
					}
					if len(urlMatch) > 1 && strings.Contains(strings.ToLower(declarations), "background") {
						cssBackgrounds[selector] = strings.TrimSpace(urlMatch[1])
					}
					if styleKind != 0 {
						cssTextStyles[selector] = styleKind
					}
					// Capture text color for Opera Mini-like fidelity
					if idx := strings.Index(strings.ToLower(declarations), "color:"); idx >= 0 {
						rest := strings.TrimSpace(strings.Split(declarations[idx+6:], ";")[0])
						rest = strings.TrimSpace(rest)
						if strings.HasPrefix(rest, "#") && len(rest) >= 4 {
							cssColors[selector] = rest
						}
					}
				}
			}
		}
		for child := node.FirstChild; child != nil; child = child.NextSibling {
			collectStyles(child)
		}
	}
	collectStyles(doc)
	backgroundFor := func(node *xhtml.Node) string {
		style := attr(node, "style")
		if strings.Contains(strings.ToLower(style), "background") {
			if match := cssURLPattern.FindStringSubmatch(style); len(match) > 1 {
				return strings.TrimSpace(match[1])
			}
		}
		if bg := attr(node, "background"); bg != "" {
			return bg
		}
		if bg := cssBackgrounds[strings.ToLower(node.Data)]; bg != "" {
			return bg
		}
		if id := attr(node, "id"); id != "" {
			if bg := cssBackgrounds["#"+strings.ToLower(id)]; bg != "" {
				return bg
			}
		}
		for _, className := range strings.Fields(attr(node, "class")) {
			if bg := cssBackgrounds["."+strings.ToLower(className)]; bg != "" {
				return bg
			}
		}
		return ""
	}
	colorFor := func(node *xhtml.Node) string {
		if node == nil {
			return ""
		}
		// CSS class/id/tag color
		if id := attr(node, "id"); id != "" {
			if c := cssColors["#"+strings.ToLower(id)]; c != "" {
				return c
			}
		}
		for _, className := range strings.Fields(attr(node, "class")) {
			if c := cssColors["."+strings.ToLower(className)]; c != "" {
				return c
			}
		}
		if c := cssColors[strings.ToLower(node.Data)]; c != "" {
			return c
		}
		style := strings.ToLower(attr(node, "style"))
		// color:#xxx or color:rgb
		if idx := strings.Index(style, "color:"); idx >= 0 {
			rest := style[idx+6:]
			rest = strings.TrimSpace(strings.Split(rest, ";")[0])
			if strings.HasPrefix(rest, "#") && len(rest) >= 4 {
				return rest
			}
			// named colors
			named := map[string]string{"red":"#c00","blue":"#06c","green":"#060","black":"#000","white":"#fff","gray":"#666","grey":"#666","orange":"#f60","purple":"#60c"}
			if hex, ok := named[rest]; ok {
				return hex
			}
		}
		return ""
	}
	textStyleFor := func(node *xhtml.Node) byte {
		if kind := cssTextStyles[strings.ToLower(node.Data)]; kind != 0 {
			return kind
		}
		if id := attr(node, "id"); id != "" {
			if kind := cssTextStyles["#"+strings.ToLower(id)]; kind != 0 {
				return kind
			}
		}
		for _, className := range strings.Fields(attr(node, "class")) {
			if kind := cssTextStyles["."+strings.ToLower(className)]; kind != 0 {
				return kind
			}
		}
		return 0
	}
	textContent := func(root *xhtml.Node) string {
		var b strings.Builder
		var visit func(*xhtml.Node)
		visit = func(n *xhtml.Node) {
			if n.Type == xhtml.TextNode {
				b.WriteString(htmlpkg.UnescapeString(n.Data))
				b.WriteByte(' ')
			}
			for c := n.FirstChild; c != nil; c = c.NextSibling {
				visit(c)
			}
		}
		visit(root)
		return strings.TrimSpace(strings.Join(strings.Fields(b.String()), " "))
	}

	activeAnchorURL := ""
	var walk func(*xhtml.Node, byte, bool)
	walk = func(n *xhtml.Node, kind byte, inLink bool) {
		if n.Type == xhtml.TextNode {
			if !inLink {
				col := colorFor(n.Parent)
				txt := htmlpkg.UnescapeString(n.Data)
				if col != "" && txt != "" {
					add(22, txt, col) // kind 22 = colored text, url holds #hex
				} else {
					add(kind, txt, "")
				}
			}
			return
		}
		if n.Type != xhtml.ElementNode {
			for c := n.FirstChild; c != nil; c = c.NextSibling {
				walk(c, kind, inLink)
			}
			return
		}

		tag := strings.ToLower(n.Data)
		if hidden(n) || tag == "script" || tag == "style" || tag == "noscript" || tag == "iframe" ||
			tag == "svg" || tag == "canvas" || tag == "video" || tag == "audio" || tag == "template" {
			return
		}
		if tag == "title" && title == pageURL {
			title = textContent(n)
			return
		}

		if tag == "form" {
			action := obmlAbsolute(pageURL, attr(n, "action"))
			if action == "" {
				action = pageURL
			}
			method := strings.ToLower(attr(n, "method"))
			if method == "" {
				method = "get"
			}
			if method != "get" {
				method = "get" // MIDP form submission currently supports GET safely.
			}
			formLabel := attr(n, "aria-label")
			if formLabel == "" {
				formLabel = attr(n, "name")
			}
			if formLabel == "" {
				formLabel = "Form"
			}

			var payload strings.Builder
			payload.WriteString(formLabel)
			payload.WriteByte('\x1f')
			payload.WriteString(action)
			payload.WriteByte('\x1f')
			payload.WriteString(method)

			var collect func(*xhtml.Node)
			collect = func(c *xhtml.Node) {
				if c.Type == xhtml.ElementNode {
					ct := strings.ToLower(c.Data)
					if ct == "input" || ct == "textarea" || ct == "select" {
						typ := strings.ToLower(attr(c, "type"))
						if typ == "" {
							typ = ct
						}
						name := attr(c, "name")
						if name != "" && typ != "submit" && typ != "button" && typ != "reset" &&
							typ != "file" && typ != "image" {
							value := attr(c, "value")
							if ct == "textarea" {
								value = textContent(c)
							}
							if ct == "select" {
								selectedValue := ""
								var encodedOptions []string
								for o := c.FirstChild; o != nil; o = o.NextSibling {
									if o.Type != xhtml.ElementNode || !strings.EqualFold(o.Data, "option") {
										continue
									}
									optionValue := attr(o, "value")
									optionLabel := textContent(o)
									if optionValue == "" {
										optionValue = optionLabel
									}
									cleanOption := strings.NewReplacer("\x1d", " ", "\x1e", " ")
									optionLabel = cleanOption.Replace(optionLabel)
									optionValue = cleanOption.Replace(optionValue)
									encodedOptions = append(encodedOptions, optionLabel+"\x1d"+optionValue)
									if selectedValue == "" || hasAttr(o, "selected") {
										selectedValue = optionValue
									}
								}
								value = selectedValue
								if len(encodedOptions) > 0 {
									value += "\x1e" + strings.Join(encodedOptions, "\x1e")
								}
							}
							if (typ == "checkbox" || typ == "radio") && hasAttr(c, "checked") {
								typ += "_checked"
							}
							label := attr(c, "aria-label")
							if label == "" {
								label = attr(c, "placeholder")
							}
							if label == "" {
								label = name
							}
							for _, valuePart := range []string{label, typ, name, value} {
								payload.WriteByte('\x1f')
								valuePart = strings.ReplaceAll(valuePart, "\x1f", " ")
								valuePart = strings.ReplaceAll(valuePart, "\x00", "")
								payload.WriteString(valuePart)
							}
						}
					}
				}
				for child := c.FirstChild; child != nil; child = child.NextSibling {
					collect(child)
				}
			}
			collect(n)
			add(7, payload.String(), action)
			return
		}

		newKind := kind
		switch tag {
		case "center":
			newKind = 20
		case "h1":
			newKind = 1
		case "h2", "strong", "b":
			newKind = 2
		case "h3", "h4", "h5", "h6":
			newKind = 14
		case "em", "i", "cite", "var":
			newKind = 10
		case "code", "kbd", "samp", "pre":
			newKind = 11
		case "blockquote":
			newKind = 12
		case "del", "s", "strike":
			newKind = 15
		case "mark":
			newKind = 16
		case "u", "ins":
			newKind = 17
		case "small":
			newKind = 18
		case "big":
			newKind = 19
		case "hr":
			add(13, "────────", "")
		case "li":
			add(3, "•", "")
		case "p", "div", "br", "section", "article", "table", "ul", "ol", "fieldset":
			if !inLink {
				add(6, "", "")
			}
		case "tr":
			tableRowCells = 0
			if !inLink {
				add(6, "", "")
			}
		case "td", "th":
			if !inLink && tableRowCells > 0 {
				// Keep table columns distinguishable after the narrow-screen reflow.
				add(9, "|", "")
			}
			tableRowCells++
		}

		// Apply simple embedded stylesheet typography after tag defaults, then
		// allow inline styles to override it.
		if styleKind := textStyleFor(n); styleKind != 0 {
			newKind = styleKind
		}
		// Preserve a few semantic CSS text styles even when the original
		// stylesheet cannot be reproduced by the small MIDP renderer.
		inlineStyle := strings.ToLower(attr(n, "style"))
		if strings.Contains(inlineStyle, "font-style:italic") || strings.Contains(inlineStyle, "font-style: italic") {
			newKind = 10
		}
		if strings.Contains(inlineStyle, "font-weight:bold") || strings.Contains(inlineStyle, "font-weight: bold") {
			newKind = 2
		}
		if strings.Contains(inlineStyle, "text-decoration:line-through") || strings.Contains(inlineStyle, "text-decoration: line-through") {
			newKind = 15
		}
		if strings.Contains(inlineStyle, "text-decoration:underline") || strings.Contains(inlineStyle, "text-decoration: underline") {
			newKind = 17
		}
		if strings.Contains(inlineStyle, "font-size:smaller") || strings.Contains(inlineStyle, "font-size: smaller") ||
			strings.Contains(inlineStyle, "font-size:x-small") || strings.Contains(inlineStyle, "font-size: x-small") {
			newKind = 18
		}
		if strings.Contains(inlineStyle, "font-size:larger") || strings.Contains(inlineStyle, "font-size: larger") ||
			strings.Contains(inlineStyle, "font-size:x-large") || strings.Contains(inlineStyle, "font-size: x-large") {
			newKind = 19
		}
		if strings.Contains(inlineStyle, "text-align:center") || strings.Contains(inlineStyle, "text-align: center") || strings.EqualFold(attr(n, "align"), "center") {
			newKind = 20
		} else if strings.Contains(inlineStyle, "text-align:right") || strings.Contains(inlineStyle, "text-align: right") || strings.EqualFold(attr(n, "align"), "right") {
			newKind = 21
		}
		if bg := backgroundFor(n); bg != "" && tag != "html" && tag != "head" && tag != "style" {
			if imageURL := obmlAbsolute(pageURL, bg); imageURL != "" {
				add(5, attr(n, "aria-label"), imageURL)
			}
		}
		if tag == "img" {
			src, alt := attr(n, "src"), attr(n, "alt")
			if src == "" || strings.HasPrefix(strings.ToLower(src), "data:") || strings.HasPrefix(src, "about:") {
				src = attr(n, "data-src")
			}
			if src == "" {
				src = attr(n, "data-original")
			}
			if src == "" {
				for _, candidate := range strings.Split(attr(n, "srcset"), ",") {
					fields := strings.Fields(strings.TrimSpace(candidate))
					if len(fields) > 0 && !strings.HasPrefix(strings.ToLower(fields[0]), "data:") {
						src = fields[0]
						break
					}
				}
			}
			if imageURL := obmlAbsolute(pageURL, src); imageURL != "" {
				imageTarget := imageURL
				if activeAnchorURL != "" {
					imageTarget += "\x1f" + activeAnchorURL
				}
				add(5, alt, imageTarget)
			}
			return
		}
		if tag == "input" {
			typ := strings.ToLower(attr(n, "type"))
			if typ == "" {
				typ = "text"
			}
			if typ == "hidden" || typ == "submit" || typ == "button" || typ == "reset" || typ == "file" {
				return
			}
			name := attr(n, "name")
			label := attr(n, "aria-label")
			if label == "" {
				label = attr(n, "placeholder")
			}
			if label == "" {
				label = attr(n, "value")
			}
			if label == "" {
				label = name
			}
			if label == "" {
				label = typ
			}
			add(8, "["+label+"] ("+typ+")", "")
			return
		}
		if tag == "textarea" || tag == "select" {
			label := attr(n, "aria-label")
			if label == "" {
				label = attr(n, "name")
			}
			if label == "" {
				label = tag
			}
			add(8, "["+label+"] ("+tag+")", "")
			return
		}
		if tag == "button" {
			label := textContent(n)
			if label == "" {
				label = attr(n, "value")
			}
			if label == "" {
				label = "Submit"
			}
			add(8, "["+label+"]", "")
			return
		}
		if tag == "a" {
			linkURL := obmlAbsolute(pageURL, attr(n, "href"))
			label := textContent(n)
			if label == "" {
				var findAlt func(*xhtml.Node) string
				findAlt = func(child *xhtml.Node) string {
					if child.Type == xhtml.ElementNode && strings.EqualFold(child.Data, "img") {
						if alt := attr(child, "alt"); alt != "" {
							return alt
						}
						if title := attr(child, "title"); title != "" {
							return title
						}
					}
					for nested := child.FirstChild; nested != nil; nested = nested.NextSibling {
						if alt := findAlt(nested); alt != "" {
							return alt
						}
					}
					return ""
				}
				label = findAlt(n)
			}
			if linkURL != "" && label != "" && textContent(n) != "" {
				add(4, label, linkURL)
			} else if label != "" && textContent(n) != "" {
				add(kind, label, "")
			}
			// Preserve image/logo children in their original position and carry
			// the enclosing anchor URL so the image itself remains clickable.
			previousAnchorURL := activeAnchorURL
			activeAnchorURL = linkURL
			for child := n.FirstChild; child != nil; child = child.NextSibling {
				walk(child, newKind, true)
			}
			activeAnchorURL = previousAnchorURL
			return
		}
		for c := n.FirstChild; c != nil; c = c.NextSibling {
			walk(c, newKind, inLink)
		}
	}

	walk(doc, 0, false)
	var out bytes.Buffer
	out.WriteString(obmlMagic)
	writeOBMLString(&out, title)
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
	// Execute bounded inline JavaScript before converting the resulting HTML
	// into TBO1. This is a static-rendering subset, not a full browser DOM.
	renderedHTML := renderInlineJavaScript(pageURL, string(data))
	packet, err := buildOBMLPage(pageURL, renderedHTML)
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
