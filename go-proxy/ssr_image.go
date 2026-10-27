package main

import (
	"bytes"
	"context"
	"encoding/binary"
	"fmt"
	"image"
	"image/draw"
	"image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"time"

	"github.com/chromedp/cdproto/emulation"
	"github.com/chromedp/chromedp"
)

const ssrImageMode = "SSRIMG"
const imglMagic = "IML2"
const maxMobileTiles = 32
const mobileTileHeight = 500
const defaultMobileViewportWidth = 240
const minMobileViewportWidth = 160
const maxMobileViewportWidth = 360
const maxMobilePacketBytes = 3 << 20

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
	Text string `json:"text"`
	URL  string `json:"url"`
	X    int    `json:"x"`
	Y    int    `json:"y"`
	W    int    `json:"w"`
	H    int    `json:"h"`
}

type pageLinkCapture struct {
	Links []pageLink `json:"links"`
	DocW  int        `json:"docW"`
	DocH  int        `json:"docH"`
}

func ssrImageResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil {
		return nil
	}
	format := resp.Request.Header.Get("X-TGFN-Format")
	if format != ssrImageMode && format != "SSRHQ" {
		return nil
	}
	hq := format == "SSRHQ"
	fmt.Println("[SSRIMG] requested for", resp.Request.URL.String(), "hq=", hq)

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

	ctx, cancel := context.WithTimeout(context.Background(), 55*time.Second)
	defer cancel()

	viewportWidth := mobileViewportWidth(resp.Request.Header.Get("X-TGFN-Viewport-Width"))
	shotQuality := 55
	jpegQuality := 55
	scale := 1.0
	packetLimit := maxMobilePacketBytes
	if hq {
		if viewportWidth < 360 {
			viewportWidth = 360
		}
		if viewportWidth > 480 {
			viewportWidth = 480
		}
		shotQuality = 90
		jpegQuality = 88
		scale = 2
		packetLimit = 4 << 20
	}
	opts := append(chromedp.DefaultExecAllocatorOptions[:],
		chromedp.ExecPath(chromePath),
		chromedp.WindowSize(viewportWidth, 500),
		chromedp.DisableGPU,
		chromedp.NoSandbox,
		chromedp.Headless,
		chromedp.Flag("disable-dev-shm-usage", true),
		chromedp.Flag("disable-extensions", true),
	)
	allocCtx, allocCancel := chromedp.NewExecAllocator(ctx, opts...)
	defer allocCancel()
	chromeCtx, chromeCancel := chromedp.NewContext(allocCtx)
	defer chromeCancel()

	var buf []byte
	var capture pageLinkCapture
	err := chromedp.Run(chromeCtx,
		emulation.SetDeviceMetricsOverride(int64(viewportWidth), 500, scale, true),
		emulation.SetTouchEmulationEnabled(true),
		emulation.SetUserAgentOverride("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"),
		chromedp.Navigate(pageURL),
		chromedp.Evaluate(`(function() {
			try {
				var style = document.createElement('style');
				style.textContent = '*,*::before,*::after{animation:none!important;transition:none!important;scroll-behavior:auto!important}';
				(document.head || document.documentElement).appendChild(style);
				var lazy = document.querySelectorAll('img[loading="lazy"]');
				for (var i = 0; i < lazy.length; i++) lazy[i].loading = 'eager';
				window.scrollTo(0, document.documentElement.scrollHeight || document.body.scrollHeight);
				window.scrollTo(0, 0);
			} catch (e) {}
		})()`, nil),
		chromedp.Sleep(450*time.Millisecond),
		chromedp.FullScreenshot(&buf, shotQuality),
		chromedp.Evaluate(`
			(function() {
				var out = [];
				var as = document.querySelectorAll('a[href]');
				var docW = window.innerWidth || document.documentElement.clientWidth || 1;
				var docH = Math.max(document.documentElement.scrollHeight || 0, document.body ? document.body.scrollHeight : 0, 1);
				for (var i = 0; i < as.length && out.length < 120; i++) {
					var a = as[i];
					var href = a.href || '';
					if (!href || href.indexOf('javascript:') === 0) continue;
					var t = (a.innerText || a.textContent || a.getAttribute('title') || a.getAttribute('aria-label') || '').replace(/\s+/g, ' ').trim();
					if (!t) {
						var img = a.querySelector('img');
						t = img ? (img.getAttribute('alt') || img.getAttribute('title') || 'image') : 'link';
					}
					if (t.length > 80) t = t.substring(0, 80);
					var rects = a.getClientRects();
					var sx = window.pageXOffset || document.documentElement.scrollLeft || 0;
					var sy = window.pageYOffset || document.documentElement.scrollTop || 0;
					var added = 0;
					for (var r = 0; r < rects.length && out.length < 120 && added < 4; r++) {
						var box = rects[r];
						if (!box || box.width < 2 || box.height < 2) continue;
						out.push({
							text: t,
							url: href,
							x: Math.max(0, Math.round(box.left + sx)),
							y: Math.max(0, Math.round(box.top + sy)),
							w: Math.max(1, Math.round(box.width)),
							h: Math.max(1, Math.round(box.height))
						});
						added++;
					}
				}
				return {links: out, docW: docW, docH: docH};
			})()
		`, &capture),
	)
	if err != nil {
		fmt.Println("[SSRIMG] chromedp error:", err)
		return err
	}
	if len(buf) < 200 {
		return fmt.Errorf("screenshot too small")
	}

	img, _, err := image.Decode(bytes.NewReader(buf))
	if err != nil {
		return err
	}
	maxWidth := viewportWidth
	if hq {
		maxWidth = viewportWidth * 2
	}
	img = resizeMobileImage(img, maxWidth)
	bounds := img.Bounds()
	links := scaleLinksToImage(capture.Links, capture.DocW, capture.DocH, bounds.Dx(), bounds.Dy())
	fmt.Println("[SSRIMG] link map doc=", capture.DocW, "x", capture.DocH, "img=", bounds.Dx(), "x", bounds.Dy(), "links=", len(links))
	tileCount := (bounds.Dy() + mobileTileHeight - 1) / mobileTileHeight
	if tileCount > maxMobileTiles {
		// Preserve the captured page scale and hitbox coordinates. Older MIDP
		// devices cannot safely hold an unlimited number of decoded image tiles.
		bounds = image.Rect(bounds.Min.X, bounds.Min.Y, bounds.Max.X, bounds.Min.Y+maxMobileTiles*mobileTileHeight)
		tileCount = maxMobileTiles
	}

	var tiles [][]byte
	for y := 0; y < bounds.Dy(); y += mobileTileHeight {
		endY := y + mobileTileHeight
		if endY > bounds.Dy() {
			endY = bounds.Dy()
		}
		tile := image.NewRGBA(image.Rect(0, 0, bounds.Dx(), endY-y))
		draw.Draw(tile, tile.Bounds(), img, image.Point{X: bounds.Min.X, Y: bounds.Min.Y + y}, draw.Src)
		var encoded bytes.Buffer
		if err := jpeg.Encode(&encoded, tile, &jpeg.Options{Quality: jpegQuality}); err != nil {
			return err
		}
		tiles = append(tiles, encoded.Bytes())
	}

	packet, err := encodeMobilePacket(tiles, links)
	if err != nil {
		return err
	}
	if len(packet) > packetLimit {
		return fmt.Errorf("mobile packet too large: %d", len(packet))
	}
	resp.Body = io.NopCloser(bytes.NewReader(packet))
	resp.ContentLength = int64(len(packet))
	resp.Header.Set("Content-Type", "application/x-tgfn-imgl")
	resp.Header.Set("Content-Length", itoa(len(packet)))
	resp.Header.Del("Content-Encoding")
	fmt.Println("[SSRIMG] OK tiles=", len(tiles), "bytes=", len(packet), "links=", len(links))
	return nil
}

// mobileViewportWidth accepts the MIDP canvas width while bounding Chromium memory use.
func mobileViewportWidth(value string) int {
	width := defaultMobileViewportWidth
	if n, err := strconv.Atoi(strings.TrimSpace(value)); err == nil {
		width = n
	}
	if width < minMobileViewportWidth {
		return minMobileViewportWidth
	}
	if width > maxMobileViewportWidth {
		return maxMobileViewportWidth
	}
	return width
}

// scaleLinksToImage maps CSS document coordinates onto the captured screenshot.
func scaleLinksToImage(links []pageLink, docW, docH, imgW, imgH int) []pageLink {
	if docW < 1 {
		docW = 1
	}
	if docH < 1 {
		docH = 1
	}
	if imgW < 1 || imgH < 1 || len(links) == 0 {
		return links
	}
	sx := float64(imgW) / float64(docW)
	sy := float64(imgH) / float64(docH)
	out := make([]pageLink, 0, len(links))
	for _, l := range links {
		x := int(float64(l.X)*sx + 0.5)
		y := int(float64(l.Y)*sy + 0.5)
		w := int(float64(l.W)*sx + 0.5)
		h := int(float64(l.H)*sy + 0.5)
		if w < 1 {
			w = 1
		}
		if h < 1 {
			h = 1
		}
		if x >= imgW || y >= imgH {
			continue
		}
		if x+w > imgW {
			w = imgW - x
		}
		if y+h > imgH {
			h = imgH - y
		}
		l.X, l.Y, l.W, l.H = x, y, w, h
		out = append(out, l)
	}
	return out
}

// encodeMobilePacket serializes the IML2 image tiles and their on-page link hitboxes.
func encodeMobilePacket(tiles [][]byte, links []pageLink) ([]byte, error) {
	if len(tiles) < 1 || len(tiles) > maxMobileTiles {
		return nil, fmt.Errorf("invalid mobile tile count: %d", len(tiles))
	}
	if len(links) > 120 {
		links = links[:120]
	}
	var packet bytes.Buffer
	packet.WriteString(imglMagic)
	_ = binary.Write(&packet, binary.BigEndian, uint16(len(tiles)))
	for _, tile := range tiles {
		if len(tile) < 10 {
			return nil, fmt.Errorf("invalid mobile tile length: %d", len(tile))
		}
		_ = binary.Write(&packet, binary.BigEndian, uint32(len(tile)))
		packet.Write(tile)
		if packet.Len() > maxMobilePacketBytes {
			return nil, fmt.Errorf("mobile render exceeds %d-byte transfer limit", maxMobilePacketBytes)
		}
	}
	_ = binary.Write(&packet, binary.BigEndian, uint16(len(links)))
	for _, l := range links {
		writeLenStr(&packet, l.Text)
		writeLenStr(&packet, l.URL)
		writeCoord(&packet, l.X)
		writeCoord(&packet, l.Y)
		writeCoord(&packet, l.W)
		writeCoord(&packet, l.H)
		if packet.Len() > maxMobilePacketBytes {
			return nil, fmt.Errorf("mobile render exceeds %d-byte transfer limit", maxMobilePacketBytes)
		}
	}
	return packet.Bytes(), nil
}

func writeCoord(b *bytes.Buffer, v int) {
	if v < 0 {
		v = 0
	}
	if v > 65535 {
		v = 65535
	}
	_ = binary.Write(b, binary.BigEndian, uint16(v))
}

func writeLenStr(b *bytes.Buffer, s string) {
	if len(s) > 1500 {
		s = s[:1500]
	}
	_ = binary.Write(b, binary.BigEndian, uint16(len(s)))
	b.WriteString(s)
}

// resizeMobileImage scales a rendered page to the MIDP-friendly viewport width.
func resizeMobileImage(src image.Image, maxWidth int) image.Image {
	b := src.Bounds()
	if b.Dx() <= maxWidth {
		return src
	}
	width := maxWidth
	height := b.Dy() * width / b.Dx()
	if height < 1 {
		height = 1
	}
	dst := image.NewRGBA(image.Rect(0, 0, width, height))
	for y := 0; y < height; y++ {
		sy := b.Min.Y + y*b.Dy()/height
		for x := 0; x < width; x++ {
			sx := b.Min.X + x*b.Dx()/width
			dst.Set(x, y, src.At(sx, sy))
		}
	}
	return dst
}

// resizeMobileImageHeight bounds very long pages to a fixed number of tiles.
func resizeMobileImageHeight(src image.Image, maxHeight int) image.Image {
	b := src.Bounds()
	if b.Dy() <= maxHeight {
		return src
	}
	height := maxHeight
	width := b.Dx() * height / b.Dy()
	if width < 1 {
		width = 1
	}
	dst := image.NewRGBA(image.Rect(0, 0, width, height))
	for y := 0; y < height; y++ {
		sy := b.Min.Y + y*b.Dy()/height
		for x := 0; x < width; x++ {
			sx := b.Min.X + x*b.Dx()/width
			dst.Set(x, y, src.At(sx, sy))
		}
	}
	return dst
}
