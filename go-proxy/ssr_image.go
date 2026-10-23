package main

import (
	"bytes"
	"context"
	"image/jpeg"
	"io"
	"net/http"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/chromedp/chromedp"
)

const ssrImageMode = "SSRIMG"

// Common Chrome/Chromium binary names and paths to try.
var chromeCandidates = []string{
	"google-chrome",
	"google-chrome-stable",
	"google-chrome-beta",
	"google-chrome-unstable",
	"chromium",
	"chromium-browser",
	"chrome",
	"chrome.exe",
	"chromium.exe",
	"/usr/bin/google-chrome",
	"/usr/bin/google-chrome-stable",
	"/usr/bin/chromium",
	"/usr/bin/chromium-browser",
	"/usr/local/bin/chromium",
	"/snap/bin/chromium",
	"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
	"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
	"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
}

func findChromeBinary() string {
	// Prefer explicit env override
	if env := os.Getenv("CHROME_PATH"); env != "" {
		if _, err := os.Stat(env); err == nil {
			return env
		}
	}
	for _, name := range chromeCandidates {
		if path, err := exec.LookPath(name); err == nil {
			return path
		}
		if _, err := os.Stat(name); err == nil {
			return name
		}
	}
	return ""
}

// ssrImageResponse renders the page with headless Chrome and returns a JPEG.
// Tries multiple common Chrome/Chromium binary names and paths.
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

	pageURL := resp.Request.URL.String()
	_ = resp.Body.Close()

	chromePath := findChromeBinary()
	if chromePath == "" {
		return io.EOF // or a clearer error; caller will surface it
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	opts := append(chromedp.DefaultExecAllocatorOptions[:],
		chromedp.ExecPath(chromePath),
		chromedp.WindowSize(240, 400),
		chromedp.DisableGPU,
		chromedp.NoSandbox,
		chromedp.Headless,
	)
	allocCtx, allocCancel := chromedp.NewExecAllocator(ctx, opts...)
	defer allocCancel()
	chromeCtx, chromeCancel := chromedp.NewContext(allocCtx)
	defer chromeCancel()

	var buf []byte
	err := chromedp.Run(chromeCtx,
		chromedp.Navigate(pageURL),
		chromedp.Sleep(2500*time.Millisecond),
		chromedp.CaptureScreenshot(&buf),
	)
	if err != nil {
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
	_ = jpeg.Encode(&out, img, &jpeg.Options{Quality: 45})

	resp.Body = io.NopCloser(bytes.NewReader(out.Bytes()))
	resp.ContentLength = int64(out.Len())
	resp.Header.Set("Content-Type", "image/jpeg")
	resp.Header.Set("Content-Length", itoa(out.Len()))
	resp.Header.Del("Content-Encoding")
	return nil
}
