package main

import (
	"regexp"
	"strings"
	"time"

	"github.com/dop251/goja"
)

const (
	maxInlineJSScripts = 12
	maxInlineJSBytes   = 128 << 10
	inlineJSTimeout    = 100 * time.Millisecond
)

var inlineScriptPattern = regexp.MustCompile("(?is)<script\\b([^>]*)>(.*?)</script\\s*>")
var scriptSourcePattern = regexp.MustCompile("(?i)\\bsrc\\s*=")

// renderInlineJavaScript executes a deliberately small, server-side JavaScript
// subset for static rendering. It has no network, filesystem, browser, or OS APIs.
// document.write/writeln output is inserted where each inline script appeared.
// Scripts are time- and size-limited; external scripts are left for the proxy's
// HTML parser to ignore because loading arbitrary script URLs is unsafe here.
func renderInlineJavaScript(pageURL, source string) string {
	matches := inlineScriptPattern.FindAllStringSubmatchIndex(source, maxInlineJSScripts+1)
	if len(matches) == 0 {
		return source
	}
	rt := goja.New()
	var written strings.Builder
	document := rt.NewObject()
	_ = document.Set("write", func(call goja.FunctionCall) goja.Value {
		for _, arg := range call.Arguments {
			written.WriteString(arg.String())
			if written.Len() > maxCompactHTMLBytes {
				break
			}
		}
		return goja.Undefined()
	})
	_ = document.Set("writeln", func(call goja.FunctionCall) goja.Value {
		for _, arg := range call.Arguments {
			written.WriteString(arg.String())
			if written.Len() > maxCompactHTMLBytes {
				break
			}
		}
		written.WriteByte(' ')
		return goja.Undefined()
	})
	_ = document.Set("title", "")
	_ = rt.Set("document", document)
	window := rt.NewObject()
	_ = window.Set("document", document)
	_ = rt.Set("window", window)
	location := rt.NewObject()
	_ = location.Set("href", pageURL)
	_ = location.Set("protocol", func() string {
		if strings.HasPrefix(pageURL, "https:") {
			return "https:"
		}
		return "http:"
	}())
	_ = rt.Set("location", location)
	navigator := rt.NewObject()
	_ = navigator.Set("userAgent", "TGFN BrowserMini mobile renderer")
	_ = rt.Set("navigator", navigator)
	console := rt.NewObject()
	noop := func(goja.FunctionCall) goja.Value { return goja.Undefined() }
	_ = console.Set("log", noop)
	_ = console.Set("warn", noop)
	_ = console.Set("error", noop)
	_ = rt.Set("console", console)

	var output strings.Builder
	cursor := 0
	processed := 0
	for _, match := range matches {
		start, end := match[0], match[1]
		attrStart, attrEnd := match[2], match[3]
		codeStart, codeEnd := match[4], match[5]
		output.WriteString(source[cursor:start])
		attrs := source[attrStart:attrEnd]
		code := source[codeStart:codeEnd]
		if !scriptSourcePattern.MatchString(attrs) && strings.TrimSpace(code) != "" &&
			processed < maxInlineJSScripts && len(code) <= maxInlineJSBytes {
			written.Reset()
			timer := time.AfterFunc(inlineJSTimeout, func() { rt.Interrupt("JavaScript time limit exceeded") })
			_, _ = rt.RunString(code)
			timer.Stop()
			rt.ClearInterrupt()
			if written.Len() > maxCompactHTMLBytes {
				rendered := written.String()[:maxCompactHTMLBytes]
				output.WriteString(rendered)
			} else {
				output.WriteString(written.String())
			}
			processed++
		}
		// Remove script tags from the resulting static document.
		cursor = end
	}
	output.WriteString(source[cursor:])
	if len(matches) > maxInlineJSScripts {
		// Remove any remaining script blocks without evaluating them.
		output.WriteString(inlineScriptPattern.ReplaceAllString(source[cursor:], ""))
	}
	return output.String()
}
