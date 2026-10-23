package main

import (
	"bytes"
	"context"
	"fmt"
	"image"
	"image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/chromedp/chromedp"
)

const ssrImageMode = "SSRIMG"

var chromeCandidates = []string{
	"/data/data/com.termux/files/usr/bin/chromium-browser",
	"/data/data/com.termux/files/usr/bin/chromium",
	"chromium-browser",
	"chromium",
	"google-chrome",
	"google-chrome-stable",
	"/usr/bin/chromium-browser",
	"/usr/bin/chromium",
	"/usr/bin/google-chrome",
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
			fmt.Println("[SSRIMG] Found via PATH:", path)
			return path
		}
		if _, err := os.Stat(name); err == nil {
			fmt.Println("[SSRIMG] Found at path:", name)
			return name
		}
	}
	fmt.Println("[SSRIMG] No Chrome/Chromium binary found")
	return ""
}

func ssrImageResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil {
		return nil
	}
	fmt.Println("[SSRIMG] Header X-TGFN-Format =", resp.Request.Header.Get("X-TGFN-Format"))
	if resp.Request.Header.Get("X-TGFN-Format") != ssrImageMode {
		return nil
	}
	fmt.Println("[SSRIMG] SSRIMG requested for", resp.Request.URL.String())

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
		return fmt.Errorf("no Chrome/Chromium")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 45*time.Second)
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
		fmt.Println("[SSRIMG] chromedp error:", err)
		return err
	}
	if len(buf) < 100 {
		return fmt.Errorf("empty screenshot")
	}

	// chromedp.CaptureScreenshot returns PNG by default
	img, _, err := image.Decode(bytes.NewReader(buf))
	if err != nil {
		fmt.Println("[SSRIMG] Image decode failed:", err)
		return err
	}

	var out bytes.Buffer
	if err := jpeg.Encode(&out, img, &jpeg.Options{Quality: 40}); err != nil {
		fmt.Println("[SSRIMG] JPEG encode failed:", err)
		return err
	}

	resp.Body = io.NopCloser(bytes.NewReader(out.Bytes()))
	resp.ContentLength = int64(out.Len())
	resp.Header.Set("Content-Type", "image/jpeg")
	resp.Header.Set("Content-Length", itoa(out.Len()))
	resp.Header.Del("Content-Encoding")
	fmt.Println("[SSRIMG] Success, JPEG bytes:", out.Len())
	return nil
}
