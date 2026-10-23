package main

import (
	"bytes"
	"encoding/binary"
	"html"
	"io"
	"net/http"
	"net/url"
	"regexp"
	"strings"
	"unicode/utf8"
)

const obmlMagic = "TBO1"
const maxOBMLBlocks = 1200
const maxOBMLString = 65535

var (
	obmlTags  = regexp.MustCompile("(?is)<[^>]*>|[^<]+")
	obmlAttr  = regexp.MustCompile("(?is)\\b(href|src|alt|title)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)")
	obmlTitle = regexp.MustCompile("(?is)<title\\b[^>]*>(.*?)</title\\s*>")
	obmlSkip  = []*regexp.Regexp{
		regexp.MustCompile("(?is)<!--.*?-->"),
		regexp.MustCompile("(?is)<head\\b[^>]*>.*?</head\\s*>"),
		regexp.MustCompile("(?is)<script\\b[^>]*>.*?</script\\s*>"),
		regexp.MustCompile("(?is)<style\\b[^>]*>.*?</style\\s*>"),
		regexp.MustCompile("(?is)<noscript\\b[^>]*>.*?</noscript\\s*>"),
		regexp.MustCompile("(?is)<iframe\\b[^>]*>.*?</iframe\\s*>"),
		regexp.MustCompile("(?is)<svg\\b[^>]*>.*?</svg\\s*>"),
		regexp.MustCompile("(?is)<canvas\\b[^>]*>.*?</canvas\\s*>"),
		regexp.MustCompile("(?is)<video\\b[^>]*>.*?</video\\s*>"),
		regexp.MustCompile("(?is)<audio\\b[^>]*>.*?</audio\\s*>"),
		regexp.MustCompile("(?is)<form\\b[^>]*>.*?</form\\s*>"),
		regexp.MustCompile("(?is)<nav\\b[^>]*>.*?</nav\\s*>"),
		regexp.MustCompile("(?is)<footer\\b[^>]*>.*?</footer\\s*>"),
		regexp.MustCompile("(?is)<header\\b[^>]*>.*?</header\\s*>"),
	}
	obmlSpace = regexp.MustCompile("[\\t\\r\\n ]+")
)

type obmlBlock struct {
	kind byte
	text string
	url  string
}

func obmlAttribute(tag, name string) string {
	for _, m := range obmlAttr.FindAllStringSubmatch(tag, -1) {
		if strings.EqualFold(m[1], name) {
			v := strings.Trim(m[2], "\"'")
			return html.UnescapeString(v)
		}
	}
	return ""
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
	title := pageURL
	if m := obmlTitle.FindStringSubmatch(source); len(m) > 1 {
		title = strings.TrimSpace(obmlSpace.ReplaceAllString(html.UnescapeString(compactTags.ReplaceAllString(m[1], "")), " "))
	}
	for _, p := range obmlSkip {
		source = p.ReplaceAllString(source, " ")
	}

	blocks := make([]obmlBlock, 0, 128)
	add := func(kind byte, text, link string) {
		text = strings.TrimSpace(obmlSpace.ReplaceAllString(text, " "))
		if len(blocks) >= maxOBMLBlocks || (text == "" && kind != 6 && kind != 5) {
			return
		}
		if len(text) > maxOBMLString {
			text = text[:maxOBMLString]
		}
		blocks = append(blocks, obmlBlock{kind: kind, text: text, url: link})
	}

	kind := byte(0)
	inLink := false
	linkURL := ""
	var linkText strings.Builder
	for _, token := range obmlTags.FindAllString(source, -1) {
		if strings.HasPrefix(token, "<") {
			t := strings.ToLower(strings.TrimSpace(token[1:]))
			closing := strings.HasPrefix(t, "/")
			if closing {
				t = strings.TrimSpace(t[1:])
			}
			nameEnd := strings.IndexAny(t, " />\t\r\n")
			name := t
			if nameEnd >= 0 {
				name = t[:nameEnd]
			}
			switch name {
			case "a":
				if closing && inLink {
					add(4, linkText.String(), linkURL)
					inLink = false
					linkURL = ""
					linkText.Reset()
				} else if !closing {
					linkURL = obmlAbsolute(pageURL, obmlAttribute(token, "href"))
					inLink = linkURL != ""
					linkText.Reset()
				}
			case "h1":
				if !closing {
					kind = 1
				} else {
					kind = 0
				}
			case "h2", "h3", "strong", "b":
				if !closing {
					kind = 2
				} else {
					kind = 0
				}
			case "img":
				if !closing {
					alt := obmlAttribute(token, "alt")
					imageURL := obmlAbsolute(pageURL, obmlAttribute(token, "src"))
					if imageURL != "" {
						add(5, alt, imageURL)
					}
				}
			case "li":
				if !closing {
					add(3, "•", "")
				}
			case "p", "div", "br", "hr", "tr":
				if closing || name == "br" || name == "hr" || name == "p" || name == "div" {
					if !inLink {
						add(6, "", "")
					}
				}
			}
			continue
		}
		txt := html.UnescapeString(token)
		txt = obmlSpace.ReplaceAllString(txt, " ")
		if inLink {
			linkText.WriteString(txt)
		} else {
			add(kind, txt, "")
		}
	}
	if inLink {
		add(4, linkText.String(), linkURL)
	}

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
		value = strings.ToValidUTF8(value, "�")
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

// obmlResponse upgrades opted-in HTML responses to the compact TBO1 block protocol.
// This is an original, OBML-inspired format, not Opera's proprietary OBML wire format.
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
