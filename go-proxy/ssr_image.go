package main

import (
	"bytes"
	"context"
	"image/jpeg"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/chromedp/chromedp"
)

const ssrImageMode = "SSRIMG"

// ssrImageResponse renders the page with headless Chrome and returns a JPEG.
// This is the closest practical way to deliver full HTML/CSS look to J2ME.
// Requires Chrome/Chromium installed on the machine running the proxy.
func ssrImageResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil || resp.Request.Header.Get("X-TGFN-Format") != ssrImageMode {
		return nil
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 || resp.Body == nil {
		return nil
	}
	ct := strings.ToLower(resp.Header.Get("Content-Type"))
	if !strings.Contains(ct, "text/html") && !strings.Contains(ct, "xhtml") {
		return nil
	}

	// We already have the HTML; for a real screenshot we re-navigate in Chrome.
	pageURL := resp.Request.URL.String()
	_ = resp.Body.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 25*time.Second)
	defer cancel()

	opts := append(chromedp.DefaultExecAllocatorOptions[:],
		chromedp.WindowSize(240, 400),
		chromedp.DisableGPU,
		chromedp.NoSandbox,
	)
	allocCtx, allocCancel := chromedp.NewExecAllocator(ctx, opts...)
	defer allocCancel()
	chromeCtx, chromeCancel := chromedp.NewContext(allocCtx)
	defer chromeCancel()

	var buf []byte
	err := chromedp.Run(chromeCtx,
		chromedp.Navigate(pageURL),
		chromedp.Sleep(2*time.Second),
		chromedp.CaptureScreenshot(&buf),
	)
	if err != nil {
		// Fallback: return a simple error image or original
		return err
	}

	// Re-encode at lower quality for J2ME
	img, err := jpeg.Decode(bytes.NewReader(buf))
	if err != nil {
		resp.Body = io.NopCloser(bytes.NewReader(buf))
		resp.ContentLength = int64(len(buf))
		resp.Header.Set("Content-Type", "image/jpeg")
		return nil
	}
	var out bytes.Buffer
	_ = jpeg.Encode(&out, img, &jpeg.Options{Quality: 50})

	resp.Body = io.NopCloser(bytes.NewReader(out.Bytes()))
	resp.ContentLength = int64(out.Len())
	resp.Header.Set("Content-Type", "image/jpeg")
	resp.Header.Set("Content-Length", itoa(out.Len()))
	resp.Header.Del("Content-Encoding")
	return nil
}
