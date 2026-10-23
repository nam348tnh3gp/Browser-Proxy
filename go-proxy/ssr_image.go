package main

import (
	"bytes"
	"context"
	"encoding/binary"
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
const imglMagic = "IMGL"

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

type pageLink struct {
	text string
	url  string
}

func ssrImageResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil || resp.Request.Header.Get("X-TGFN-Format") != ssrImageMode {
		return nil
	}
	fmt.Println("[SSRIMG] requested for", resp.Request.URL.String())

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

	ctx, cancel := context.WithTimeout(context.Background(), 50*time.Second)
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
	var links []pageLink
	err := chromedp.Run(chromeCtx,
		chromedp.Navigate(pageURL),
		chromedp.Sleep(3*time.Second),
		chromedp.CaptureScreenshot(&buf),
		chromedp.Evaluate(`
			(function() {
				var out = [];
				var as = document.querySelectorAll('a[href]');
				for (var i = 0; i < as.length && out.length < 80; i++) {
					var a = as[i];
					var t = (a.innerText || a.textContent || '').replace(/\s+/g, ' ').trim();
					if (t.length > 1 && t.length < 120) {
						out.push({text: t, url: a.href});
					}
				}
				return out;
			})()
		`, &links),
	)
	if err != nil {
		fmt.Println("[SSRIMG] chromedp error:", err)
		return err
	}
	if len(buf) < 100 {
		return fmt.Errorf("empty screenshot")
	}

	img, _, err := image.Decode(bytes.NewReader(buf))
	if err != nil {
		return err
	}
	var jpegBuf bytes.Buffer
	if err := jpeg.Encode(&jpegBuf, img, &jpeg.Options{Quality: 38}); err != nil {
		return err
	}

	// Build IMGL packet: magic + jpegLen(4) + jpeg + linkCount(2) + links
	var packet bytes.Buffer
	packet.WriteString(imglMagic)
	_ = binary.Write(&packet, binary.BigEndian, uint32(jpegBuf.Len()))
	packet.Write(jpegBuf.Bytes())
	if len(links) > 80 {
		links = links[:80]
	}
	_ = binary.Write(&packet, binary.BigEndian, uint16(len(links)))
	for _, l := range links {
		writeLenStr(&packet, l.text)
		writeLenStr(&packet, l.url)
	}

	resp.Body = io.NopCloser(bytes.NewReader(packet.Bytes()))
	resp.ContentLength = int64(packet.Len())
	resp.Header.Set("Content-Type", "application/x-tgfn-imgl")
	resp.Header.Set("Content-Length", itoa(packet.Len()))
	resp.Header.Del("Content-Encoding")
	fmt.Println("[SSRIMG] Success, JPEG", jpegBuf.Len(), "links", len(links))
	return nil
}

func writeLenStr(b *bytes.Buffer, s string) {
	if len(s) > 2000 {
		s = s[:2000]
	}
	_ = binary.Write(b, binary.BigEndian, uint16(len(s)))
	b.WriteString(s)
}
