package main

import (
	"bytes"
	"image"
	"image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"net/url"
	"strconv"
)

const imageCompressPath = "/__tgfn/img"
const maxImageWidth = 240
const jpegQuality = 60

// serveCompressedImage fetches, resizes and JPEG-compresses an image for J2ME clients.
func serveCompressedImage(w http.ResponseWriter, r *http.Request) bool {
	if r.URL == nil || r.URL.IsAbs() || r.URL.Path != imageCompressPath {
		return false
	}
	if r.Method != http.MethodGet {
		http.Error(w, "Only GET", http.StatusMethodNotAllowed)
		return true
	}
	raw := r.URL.Query().Get("url")
	if raw == "" || len(raw) > 4096 {
		http.Error(w, "Missing image URL", http.StatusBadRequest)
		return true
	}
	target, err := url.Parse(raw)
	if err != nil || (target.Scheme != "http" && target.Scheme != "https") {
		http.Error(w, "Invalid image URL", http.StatusBadRequest)
		return true
	}
	if err := validateRemoteTarget(r.Context(), target); err != nil {
		http.Error(w, "Disallowed image URL", http.StatusBadRequest)
		return true
	}

	client := &http.Client{Timeout: 15 * 1000000000}
	req, _ := http.NewRequestWithContext(r.Context(), http.MethodGet, target.String(), nil)
	req.Header.Set("User-Agent", "TGFN-BrowserMini-Image/1.0")
	resp, err := client.Do(req)
	if err != nil || resp.StatusCode != 200 {
		http.Error(w, "Image fetch failed", http.StatusBadGateway)
		return true
	}
	defer resp.Body.Close()

	data, err := io.ReadAll(io.LimitReader(resp.Body, 2<<20))
	if err != nil || len(data) == 0 {
		http.Error(w, "Empty image", http.StatusBadGateway)
		return true
	}

	img, _, err := image.Decode(bytes.NewReader(data))
	if err != nil {
		http.Error(w, "Unsupported image format", http.StatusBadRequest)
		return true
	}

	// Simple nearest-neighbor resize if wider than max
	bounds := img.Bounds()
	if bounds.Dx() > maxImageWidth {
		img = resizeNearest(img, maxImageWidth)
	}

	var buf bytes.Buffer
	if err := jpeg.Encode(&buf, img, &jpeg.Options{Quality: jpegQuality}); err != nil {
		http.Error(w, "Encode failed", http.StatusInternalServerError)
		return true
	}

	w.Header().Set("Content-Type", "image/jpeg")
	w.Header().Set("Cache-Control", "public, max-age=3600")
	w.Header().Set("Content-Length", strconv.Itoa(buf.Len()))
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(buf.Bytes())
	return true
}

// resizeNearest is a tiny nearest-neighbor resizer (no external dependency).
func resizeNearest(src image.Image, maxW int) image.Image {
	b := src.Bounds()
	sw, sh := b.Dx(), b.Dy()
	if sw <= maxW {
		return src
	}
	nh := sh * maxW / sw
	if nh < 1 {
		nh = 1
	}
	dst := image.NewRGBA(image.Rect(0, 0, maxW, nh))
	for y := 0; y < nh; y++ {
		sy := y * sh / nh
		for x := 0; x < maxW; x++ {
			sx := x * sw / maxW
			dst.Set(x, y, src.At(b.Min.X+sx, b.Min.Y+sy))
		}
	}
	return dst
}

func rewriteImageURL(original string) string {
	if original == "" {
		return ""
	}
	return imageCompressPath + "?url=" + url.QueryEscape(original)
}
