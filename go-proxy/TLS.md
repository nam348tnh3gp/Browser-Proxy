# Mã hóa đường tới proxy

Bật TLS để điện thoại nói chuyện với proxy qua HTTPS, không còn plaintext.

## Chạy
```bash
PROXY_TLS=1 PROXY_LISTEN=0.0.0.0:8080 go run .
```

Chứng chỉ riêng (nên dùng, Android tin được):
```bash
PROXY_TLS_CERT=fullchain.pem PROXY_TLS_KEY=privkey.pem PROXY_LISTEN=0.0.0.0:8080 go run .
```

## Trong app
Nhập `https://host:port`  
Ví dụ Railway TCP: `https://shuttle.proxy.rlwy.net:15140`

Self-signed (`PROXY_TLS=1` không có cert) mã hóa được đường truyền, nhưng Android thường từ chối chứng chỉ không tin cậy. Dùng cert thật nếu cần app chấp nhận.
