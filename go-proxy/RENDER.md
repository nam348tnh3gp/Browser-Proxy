# Deploy Render + UptimeRobot

## Deploy
1. Push repo lên GitHub.
2. Render → New → Web Service → chọn repo, root `go-proxy`.
3. Runtime Docker, plan Free.
4. Health check path: `/health`
5. Deploy. URL dạng `https://tgfn-go-proxy.onrender.com`

## Giữ service sống
Free web service ngủ sau 15 phút không có request.

1. Tạo monitor tại https://uptimerobot.com
2. Type: HTTP(s)
3. URL: `https://<tên>.onrender.com/health`
4. Interval: **5 phút**
5. Keyword (tuỳ chọn): `ok`

Mỗi lần ping đánh thức service, nên nó không ngủ.

## App Android
Cài đặt → Proxy: `https://<tên>.onrender.com`  
Hoặc `https://<tên>.onrender.com:443`

Lưu ý: file stats trên free tier mất khi service ngủ hoặc restart.
