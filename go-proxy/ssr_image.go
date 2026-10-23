package main

import (
	"bytes"
	"context"
	"fmt"
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
	// Termux (Android)
	"/data/data/com.termux/files/usr/bin/chromium-browser",
	"/data/data/com.termux/files/usr/bin/chromium",
	"/data/data/com.termux/files/usr/bin/google-chrome",
	// Common names
	"chromium-browser",
	"chromium",
	"google-chrome",
	"google-chrome-stable",
	"google-chrome-beta",
	"chrome",
	"chrome.exe",
	"chromium.exe",
	// Absolute paths
	"/usr/bin/chromium-browser",
	"/usr/bin/chromium",
	"/usr/bin/google-chrome",
	"/usr/bin/google-chrome-stable",
	"/usr/local/bin/chromium",
	"/snap/bin/chromium",
	"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
	"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
	"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
}

func findChromeBinary() string {
	if env := os.Getenv("CHROME_PATH"); env != "" {
		if _, err := os.Stat(env); err == nil {
			fmt.Println("[SSRIMG] Using CHROME_PATH:", env)
			return env
		}
	}
	for _, name := range chromeCandidates {
		if path, err := exec.LookPath(name); err == nil {
			fmt.Println("[SSRIMG] Found Chrome via PATH:", path)
			return path
		}
		if _, err := os.Stat(name); err == nil {
			fmt.Println("[SSRIMG] Found Chrome at:", name)
			return name
		}
	}
	fmt.Println("[SSRIMG] No Chrome/Chromium binary found")
	return ""
}

// ssrImageResponse renders the page with headless Chrome and returns a JPEG.
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
		return fmt.Errorf("no Chrome/Chromium binary found")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 40*time.Second)
	defer cancel()

	opts := append(chromedp.DefaultExecAllocatorOptions[:],
		chromedp.ExecPath(chromePath),
		chromedp.WindowSize(240, 480),
		chromedp.DisableGPU,
		chromedp.NoSandbox,
		chromedp.Headless,
		chromedp.Flag("disable-dev-shm-usage", true),
	)
	allocCtx, allocCancel := chromedp.NewExecAllocator(ctx, opts...)
	defer allocCancel()
	chromeCtx, chromeCancel := chromedp.NewContext(allocCtx)
	defer chromeCancel()

	var buf []byte
	err := chromedp.Run(chromeCtx,
		chromedp.Navigate(pageURL),
		chromedp.Sleep(3*time.Second),
		chromedp.CaptureScreenshot(&buf),
	)
	if err != nil {
		fmt.Println("[SSRIMG] Render error:", err)
		return err
	}

	img, err := jpeg.Decode(bytes.NewReader(buf))
	if err != nil {
		resp.Body = io.NopCloser(bytes.NewReader(buf))
		resp.ContentLength = int64(len(buf))
		resp.Header.Set("Content-Type", "image/jpeg")
		return nil
	}
	var out bytes.Buffer
	_ = jpeg.Encode(&out, img, &jpeg.Options{Quality: 40})

	resp.Body = io.NopCloser(bytes.NewReader(out.Bytes()))
	resp.ContentLength = int64(out.Len())
	resp.Header.Set("Content-Type", "image/jpeg")
	resp.Header.Set("Content-Length", itoa(out.Len()))
	resp.Header.Del("Content-Encoding")
	fmt.Println("[SSRIMG] Screenshot OK, size:", out.Len())
	return nil
}
