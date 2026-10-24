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
	"strings"
	"time"

	"github.com/chromedp/cdproto/emulation"
	"github.com/chromedp/chromedp"
)

const ssrImageMode = "SSRIMG"
const imglMagic = "IML2"
const maxMobileTiles = 32
const mobileTileHeight = 500
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

	ctx, cancel := context.WithTimeout(context.Background(), 55*time.Second)
	defer cancel()

	opts := append(chromedp.DefaultExecAllocatorOptions[:],
		chromedp.ExecPath(chromePath),
		chromedp.WindowSize(240, 500),
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
	var links []pageLink
	err := chromedp.Run(chromeCtx,
		emulation.SetDeviceMetricsOverride(240, 500, 1, true),
		emulation.SetTouchEmulationEnabled(true),
		emulation.SetUserAgentOverride("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"),
		chromedp.Navigate(pageURL),
		chromedp.Sleep(650*time.Millisecond),
		chromedp.FullScreenshot(&buf, 55),
		chromedp.Evaluate(`
			(function() {
				var out = [];
				var seen = {};
				var as = document.querySelectorAll('a[href]');
				for (var i = 0; i < as.length && out.length < 60; i++) {
					var a = as[i];
					var href = a.href || '';
					if (!href || href.indexOf('javascript:') === 0) continue;
					var t = (a.innerText || a.textContent || a.getAttribute('title') || '').replace(/\s+/g, ' ').trim();
					if (t.length < 2 || t.length > 100) continue;
					var key = t + '|' + href;
					if (seen[key]) continue;
					seen[key] = true;
					var r = a.getBoundingClientRect();
					if (!r || r.width < 1 || r.height < 1) continue;
					var sx = window.pageXOffset || document.documentElement.scrollLeft || 0;
					var sy = window.pageYOffset || document.documentElement.scrollTop || 0;
					out.push({text: t, url: href, x: Math.max(0, Math.round(r.left + sx)), y: Math.max(0, Math.round(r.top + sy)), w: Math.max(1, Math.round(r.width)), h: Math.max(1, Math.round(r.height))});
				}
				return out;
			})()
		`, &links),
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
	img = resizeMobileImage(img, 240)
	bounds := img.Bounds()
	tileCount := (bounds.Dy() + mobileTileHeight - 1) / mobileTileHeight
	if tileCount > maxMobileTiles {
		// Bound MIDP memory use while retaining a tall, scrollable mobile page.
		newHeight := maxMobileTiles * mobileTileHeight
		img = resizeMobileImageHeight(img, newHeight)
		bounds = img.Bounds()
		tileCount = (bounds.Dy() + mobileTileHeight - 1) / mobileTileHeight
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
		if err := jpeg.Encode(&encoded, tile, &jpeg.Options{Quality: 55}); err != nil {
			return err
		}
		tiles = append(tiles, encoded.Bytes())
	}

	packet, err := encodeMobilePacket(tiles, links)
	if err != nil {
		return err
	}
	resp.Body = io.NopCloser(bytes.NewReader(packet))
	resp.ContentLength = int64(len(packet))
	resp.Header.Set("Content-Type", "application/x-tgfn-imgl")
	resp.Header.Set("Content-Length", itoa(len(packet)))
	resp.Header.Del("Content-Encoding")
	fmt.Println("[SSRIMG] OK tiles=", len(tiles), "bytes=", len(packet), "links=", len(links))
	return nil
}

// encodeMobilePacket serializes the IML2 image tiles and their on-page link hitboxes.
func encodeMobilePacket(tiles [][]byte, links []pageLink) ([]byte, error) {
	if len(tiles) < 1 || len(tiles) > maxMobileTiles {
		return nil, fmt.Errorf("invalid mobile tile count: %d", len(tiles))
	}
	if len(links) > 60 {
		links = links[:60]
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
