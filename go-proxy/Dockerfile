FROM golang:1.23-alpine AS build
WORKDIR /src
COPY go.mod go.sum* ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /proxy .

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /proxy /app/proxy
ENV PROXY_STATS_FILE=/app/proxy-stats.json
EXPOSE 8080
CMD ["/app/proxy"]
