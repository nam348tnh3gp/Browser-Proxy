package main

import (
	"bytes"
	"compress/gzip"
	"io"
	"net/http"
	"regexp"
	"strings"
)

const fullModeHeader = "X-TGFN-Format"
const fullModeValue = "FULL"

var (
	fullDropScript = regexp.MustCompile(`(?is)<script\b[^>]*>.*?</script\s*>`)
	fullDropStyle  = regexp.MustCompile(`(?is)<style\b[^>]*>.*?</style\s*>`)
	fullDropHeavy  = regexp.MustCompile(`(?is)<(iframe|svg|canvas|video|audio|noscript)\b[^>]*>.*?</\1\s*>`)
	fullImgSrc     = regexp.MustCompile(`(?is)(<img\b[^>]*?\s+src\s*=\s*["'])([^"']+)(["'])`)
	fullWhitespace = regexp.MustCompile(`[\t\r\n ]+`)
)

// fullCompressResponse keeps most HTML structure, links and basic CSS
// while removing scripts and compressing images via the proxy.
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

	html := string(data)
	// Remove heavy non-content
	html = fullDropScript.ReplaceAllString(html, "")
	html = fullDropStyle.ReplaceAllString(html, "")
	html = fullDropHeavy.ReplaceAllString(html, "")

	// Rewrite image src to go through compression endpoint
	html = fullImgSrc.ReplaceAllStringFunc(html, func(m string) string {
		sub := fullImgSrc.FindStringSubmatch(m)
		if len(sub) < 4 {
			return m
		}
		orig := sub[2]
		// Only rewrite absolute or protocol-relative http(s)
		if strings.HasPrefix(orig, "http://") || strings.HasPrefix(orig, "https://") || strings.HasPrefix(orig, "//") {
			if strings.HasPrefix(orig, "//") {
				orig = "https:" + orig
			}
			return sub[1] + rewriteImageURL(orig) + sub[3]
		}
		return m
	})

	// Light whitespace cleanup (keep structure)
	html = fullWhitespace.ReplaceAllString(html, " ")
	html = strings.ReplaceAll(html, "> <", "><")

	compressed := []byte(html)

	// Gzip the result
	var gz bytes.Buffer
	w := gzip.NewWriter(&gz)
	_, _ = w.Write(compressed)
	_ = w.Close()

	resp.Body = io.NopCloser(bytes.NewReader(gz.Bytes()))
	resp.ContentLength = int64(gz.Len())
	resp.Header.Set("Content-Encoding", "gzip")
	resp.Header.Set("Content-Length", itoa(gz.Len()))
	resp.Header.Set("Content-Type", "text/html; charset=utf-8")
	resp.Header.Del("Content-MD5")
	resp.Header.Del("ETag")
	return nil
}

func itoa(n int) string {
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
