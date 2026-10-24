package main

import (
	"io"
	"net/http"
	"net/url"
	"regexp"
	"strings"
)

const fullModeHeader = "X-TGFN-Format"
const fullModeValue = "FULL"

var (
	fullDropScript = regexp.MustCompile(`(?is)<script\b[^>]*>.*?</script\s*>`)
	// styles kept for fuller web
	fullDropStyle  = regexp.MustCompile(`(?is)<!--style-kept-->`)
	fullDropHeavy  = []*regexp.Regexp{
		regexp.MustCompile(`(?is)<iframe\b[^>]*>.*?</iframe\s*>`),
		regexp.MustCompile(`(?is)<svg\b[^>]*>.*?</svg\s*>`),
		regexp.MustCompile(`(?is)<canvas\b[^>]*>.*?</canvas\s*>`),
		regexp.MustCompile(`(?is)<video\b[^>]*>.*?</video\s*>`),
		regexp.MustCompile(`(?is)<audio\b[^>]*>.*?</audio\s*>`),
		regexp.MustCompile(`(?is)<noscript\b[^>]*>.*?</noscript\s*>`),
		regexp.MustCompile(`(?is)<object\b[^>]*>.*?</object\s*>`),
		regexp.MustCompile(`(?is)<embed\b[^>]*/?>`),
	}
	fullImgSrc     = regexp.MustCompile(`(?is)(<img\b[^>]*?\s+src\s*=\s*["'])([^"']+)(["'])`)
	fullHref       = regexp.MustCompile(`(?is)(<a\b[^>]*?\s+href\s*=\s*["'])([^"']+)(["'])`)
	fullWhitespace = regexp.MustCompile(`[ \t\r\n]+`)
	fullComments   = regexp.MustCompile(`(?s)<!--.*?-->`)
)

// fullCompressResponse keeps the original HTML structure (links, forms, semantic tags)
// while removing only heavy non-content. Output is minified plain HTML that J2ME can parse.
func fullCompressResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil || resp.Request.Header.Get(fullModeHeader) != fullModeValue {
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

	pageURL := resp.Request.URL
	html := string(data)

	// Drop only heavy / non-renderable parts. Keep structure, links, forms, images.
	html = fullDropScript.ReplaceAllString(html, "")
	html = // fullDropStyle.ReplaceAllString(html, "")
	html = fullComments.ReplaceAllString(html, "")
	for _, re := range fullDropHeavy {
		html = re.ReplaceAllString(html, "")
	}

	// Make image and link URLs absolute so the J2ME client can resolve them easily
	html = rewriteAttr(html, fullImgSrc, pageURL, true)
	html = rewriteAttr(html, fullHref, pageURL, false)

	// Light whitespace collapse (preserve tag structure)
	html = fullWhitespace.ReplaceAllString(html, " ")
	html = strings.TrimSpace(html)

	out := []byte(html)
	resp.Body = io.NopCloser(strings.NewReader(html))
	resp.ContentLength = int64(len(out))
	resp.Header.Set("Content-Type", "text/html; charset=utf-8")
	resp.Header.Set("Content-Length", itoa(len(out)))
	resp.Header.Del("Content-Encoding")
	resp.Header.Del("Content-MD5")
	resp.Header.Del("ETag")
	return nil
}

func rewriteAttr(html string, re *regexp.Regexp, base *url.URL, isImage bool) string {
	return re.ReplaceAllStringFunc(html, func(m string) string {
		sub := re.FindStringSubmatch(m)
		if len(sub) < 4 {
			return m
		}
		orig := strings.TrimSpace(sub[2])
		if orig == "" || strings.HasPrefix(orig, "data:") || strings.HasPrefix(orig, "javascript:") {
			return m
		}
		abs := resolveAgainst(base, orig)
		if abs == "" {
			return m
		}
		if isImage {
			abs = rewriteImageURL(abs)
		}
		return sub[1] + abs + sub[3]
	})
}

func resolveAgainst(base *url.URL, ref string) string {
	if base == nil {
		return ref
	}
	u, err := url.Parse(ref)
	if err != nil {
		return ref
	}
	return base.ResolveReference(u).String()
}
