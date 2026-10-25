package main

import (
	"bytes"
	"io"
	"mime"
	"net/http"
	"regexp"
	"strconv"
	"strings"
)

const maxCompactHTMLBytes = 4 << 20

var (
	compactDropBlocks = []*regexp.Regexp{
		regexp.MustCompile(`(?is)<!--.*?-->`),
		regexp.MustCompile(`(?is)<script\b[^>]*>.*?</script\s*>`),
		regexp.MustCompile(`(?is)<style\b[^>]*>.*?</style\s*>`),
		regexp.MustCompile(`(?is)<noscript\b[^>]*>.*?</noscript\s*>`),
		regexp.MustCompile(`(?is)<iframe\b[^>]*>.*?</iframe\s*>`),
		regexp.MustCompile(`(?is)<svg\b[^>]*>.*?</svg\s*>`),
		regexp.MustCompile(`(?is)<canvas\b[^>]*>.*?</canvas\s*>`),
		regexp.MustCompile(`(?is)<video\b[^>]*>.*?</video\s*>`),
		regexp.MustCompile(`(?is)<audio\b[^>]*>.*?</audio\s*>`),
		regexp.MustCompile(`(?is)<form\b[^>]*>.*?</form\s*>`),
		regexp.MustCompile(`(?is)<nav\b[^>]*>.*?</nav\s*>`),
		regexp.MustCompile(`(?is)<footer\b[^>]*>.*?</footer\s*>`),
		regexp.MustCompile(`(?is)<header\b[^>]*>.*?</header\s*>`),
	}
	compactDropVoid   = regexp.MustCompile(`(?is)<(link|input|source|meta|button)\b[^>]*?/?>`)
	compactTags       = regexp.MustCompile(`(?is)<[^>]*?>`)
	compactAttrs      = regexp.MustCompile(`(?is)\s+(href|src|alt|title|width|height)\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)`)
	compactWhitespace = regexp.MustCompile(`[\t\r\n ]+`)
)

var compactAllowedTags = map[string]bool{
	"a": true, "abbr": true, "b": true, "blockquote": true, "br": true,
	"caption": true, "code": true, "dd": true, "div": true, "dl": true,
	"dt": true, "em": true, "h1": true, "h2": true, "h3": true,
	"h4": true, "h5": true, "h6": true, "hr": true, "i": true,
	"img": true, "li": true, "ol": true, "p": true, "pre": true,
	"small": true, "span": true, "strong": true, "table": true,
	"tbody": true, "td": true, "th": true, "thead": true, "title": true,
	"tr": true, "u": true, "ul": true, "html": true, "head": true, "body": true,
}

// mobileResponse prefers a real mobile-viewport render for J2ME. If Chromium
// is unavailable or rendering fails, it falls back to the semantic TBO1 page.
func mobileResponse(resp *http.Response) error {
	return mobileResponseWithChrome(resp, findChromeBinary())
}

func mobileResponseWithChrome(resp *http.Response, chromePath string) error {
	if resp == nil || resp.Request == nil || resp.StatusCode < 200 || resp.StatusCode >= 300 || resp.Body == nil {
		return nil
	}
	ct := strings.ToLower(resp.Header.Get("Content-Type"))
	if !strings.Contains(ct, "text/html") && !strings.Contains(ct, "xhtml") {
		return nil
	}

	originalRequestHeader := resp.Request.Header.Clone()
	if chromePath == "" {
		resp.Request.Header.Set("X-TGFN-Format", "TBO1")
		err := obmlResponse(resp)
		resp.Request.Header = originalRequestHeader
		return err
	}

	originalHeaders := resp.Header.Clone()
	data, err := io.ReadAll(io.LimitReader(resp.Body, maxCompactHTMLBytes+1))
	if err != nil {
		return err
	}
	if len(data) > maxCompactHTMLBytes {
		// Preserve the bytes already read and the still-open upstream stream.
		resp.Body = io.NopCloser(io.MultiReader(bytes.NewReader(data), resp.Body))
		resp.Request.Header.Set("X-TGFN-Format", "TBO1")
		err = obmlResponse(resp)
		resp.Request.Header = originalRequestHeader
		return err
	}
	_ = resp.Body.Close()
	resp.Body = io.NopCloser(bytes.NewReader(data))
	resp.ContentLength = int64(len(data))

	fallback := func() error {
		resp.Body = io.NopCloser(bytes.NewReader(data))
		resp.ContentLength = int64(len(data))
		resp.Header = originalHeaders.Clone()
		resp.Request.Header = originalRequestHeader.Clone()
		resp.Request.Header.Set("X-TGFN-Format", "TBO1")
		fallbackErr := obmlResponse(resp)
		resp.Request.Header = originalRequestHeader
		return fallbackErr
	}

	resp.Request.Header.Set("X-TGFN-Format", ssrImageMode)
	err = ssrImageResponse(resp)
	resp.Request.Header = originalRequestHeader
	if err != nil {
		return fallback()
	}
	return nil
}

// compactHTMLResponse transforms only requests explicitly made by the J2ME client.
func compactHTMLResponse(resp *http.Response) error {
	if resp != nil && resp.Request != nil {
		switch resp.Request.Header.Get("X-TGFN-Format") {
		case "TBO1":
			// TBO1 is the compact binary protocol decoded by the MIDP client.
			// Do not send minified HTML under the TBO1 format header.
			return obmlResponse(resp)
		case "FULL":
			return fullCompressResponse(resp)
		case "SSR":
			return ssrResponse(resp)
		case "SSRIMG":
			return ssrImageResponse(resp)
		case "MOB1":
			return mobileResponse(resp)
		}
	}
	if resp == nil || resp.Request == nil || resp.Request.Header.Get("X-TGFN-Compact") != "1" {
		return nil
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 || resp.Body == nil {
		return nil
	}
	mediaType, _, err := mime.ParseMediaType(resp.Header.Get("Content-Type"))
	if err != nil || (!strings.EqualFold(mediaType, "text/html") && !strings.EqualFold(mediaType, "application/xhtml+xml")) {
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
	compact := []byte(compactHTML(string(data)))
	resp.Body = io.NopCloser(bytes.NewReader(compact))
	resp.ContentLength = int64(len(compact))
	resp.Header.Set("Content-Length", strconv.Itoa(len(compact)))
	resp.Header.Del("Content-Encoding")
	resp.Header.Del("Content-MD5")
	resp.Header.Del("ETag")
	return nil
}

func compactHTML(input string) string {
	for _, pattern := range compactDropBlocks {
		input = pattern.ReplaceAllString(input, " ")
	}
	input = compactDropVoid.ReplaceAllString(input, "")
	input = compactTags.ReplaceAllStringFunc(input, func(tag string) string {
		if len(tag) < 3 || tag[1] == '!' || tag[1] == '?' {
			return ""
		}
		closing := tag[1] == '/'
		start := 1
		if closing {
			start++
		}
		end := start
		for end < len(tag) && ((tag[end] >= 'a' && tag[end] <= 'z') ||
			(tag[end] >= 'A' && tag[end] <= 'Z') || (tag[end] >= '0' && tag[end] <= '9')) {
			end++
		}
		if end == start {
			return ""
		}
		name := strings.ToLower(tag[start:end])
		if !compactAllowedTags[name] {
			return ""
		}
		if closing {
			return "</" + name + ">"
		}
		matches := compactAttrs.FindAllStringSubmatch(tag[end:], -1)
		attrs := ""
		for _, match := range matches {
			attrs += " " + strings.ToLower(match[1]) + "=" + match[2]
		}
		return "<" + name + attrs + ">"
	})
	input = compactWhitespace.ReplaceAllString(input, " ")
	input = strings.ReplaceAll(input, "> <", "><")
	return strings.TrimSpace(input)
}
