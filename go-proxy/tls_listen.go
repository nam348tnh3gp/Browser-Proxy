package main

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"log"
	"math/big"
	"net"
	"net/http"
	"os"
	"time"
)

func tlsEnabled() bool {
	v := os.Getenv("PROXY_TLS")
	return v == "1" || v == "true" || v == "yes"
}

func listenAndServe(server *http.Server) error {
	if !tlsEnabled() && os.Getenv("PROXY_TLS_CERT") == "" {
		return server.ListenAndServe()
	}
	certFile := os.Getenv("PROXY_TLS_CERT")
	keyFile := os.Getenv("PROXY_TLS_KEY")
	var cert tls.Certificate
	var err error
	if certFile != "" && keyFile != "" {
		cert, err = tls.LoadX509KeyPair(certFile, keyFile)
		if err != nil {
			return err
		}
		log.Printf("TLS enabled with certificate %s", certFile)
	} else {
		cert, err = selfSignedCert()
		if err != nil {
			return err
		}
		log.Printf("TLS enabled with a self-signed certificate (Android may reject it)")
	}
	server.TLSConfig = &tls.Config{
		MinVersion:   tls.VersionTLS12,
		Certificates: []tls.Certificate{cert},
	}
	return server.ListenAndServeTLS("", "")
}

func selfSignedCert() (tls.Certificate, error) {
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		return tls.Certificate{}, err
	}
	tmpl := &x509.Certificate{
		SerialNumber: big.NewInt(time.Now().UnixNano()),
		Subject:      pkix.Name{CommonName: "tgfn-proxy"},
		NotBefore:    time.Now().Add(-time.Hour),
		NotAfter:     time.Now().Add(365 * 24 * time.Hour),
		KeyUsage:     x509.KeyUsageDigitalSignature | x509.KeyUsageKeyEncipherment,
		ExtKeyUsage:  []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
		DNSNames:     []string{"localhost", "tgfn-proxy"},
		IPAddresses:  []net.IP{net.ParseIP("127.0.0.1")},
	}
	der, err := x509.CreateCertificate(rand.Reader, tmpl, tmpl, &key.PublicKey, key)
	if err != nil {
		return tls.Certificate{}, err
	}
	keyDER, err := x509.MarshalECPrivateKey(key)
	if err != nil {
		return tls.Certificate{}, err
	}
	certPEM := pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: der})
	keyPEM := pem.EncodeToMemory(&pem.Block{Type: "EC PRIVATE KEY", Bytes: keyDER})
	return tls.X509KeyPair(certPEM, keyPEM)
}
