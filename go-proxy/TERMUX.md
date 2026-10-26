# Chạy proxy trên Termux

## Cài đặt
```bash
pkg update
pkg install golang git
```

## Lấy mã và chạy
```bash
git clone https://github.com/nam348tnh3gp/Browser-Proxy.git
cd Browser-Proxy/go-proxy
PROXY_LISTEN=0.0.0.0:8080 go run .
```

## Nhập trong app
- Cùng máy Termux: `127.0.0.1:8080`
- Máy khác cùng Wi-Fi: `IP-điện-thoại:8080`

Không dùng `127.0.0.1` khi proxy chạy trên thiết bị khác.
