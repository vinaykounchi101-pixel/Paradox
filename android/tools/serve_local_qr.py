#!/usr/bin/env python3
"""
Paradox Local Wi-Fi APK Server & QR Code Generator
Allows instant phone camera scanning and download over local Wi-Fi.
"""

import http.server
import os
import socket
import socketserver
import sys
from pathlib import Path

def get_local_ip():
    """Retrieve local Wi-Fi IP address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Connect to public DNS to determine default routing interface
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip

def render_terminal_qr(data_url: str):
    """Render ASCII QR Code directly in the terminal."""
    try:
        import qrcode
        qr = qrcode.QRCode(box_size=1, border=2)
        qr.add_data(data_url)
        qr.make(fit=True)
        print("\n" + "="*50)
        print("📲 SCAN WITH PHONE CAMERA TO INSTALL PARADOX:")
        print("="*50 + "\n")
        qr.print_ascii(invert=True)
        print("\n" + "="*50)
    except ImportError:
        print("\n[Tip: Run 'pip install qrcode' for terminal ASCII QR rendering]")
        print(f"🔗 Direct Download URL: {data_url}")

def run_server(port: int = 8080):
    ip = get_local_ip()
    base_dir = Path(__file__).resolve().parent.parent
    apk_dir = base_dir / "app" / "build" / "outputs" / "apk" / "debug"
    
    # If built apk exists, serve that directory, else serve project root
    serve_path = apk_dir if apk_dir.exists() else base_dir
    os.chdir(str(serve_path))
    
    download_url = f"http://{ip}:{port}/app-debug.apk"
    
    print("\n" + "#"*60)
    print("🚀 PARADOX LOCAL WI-FI APK DISTRIBUTOR")
    print(f"📡 Local Server Running at: http://{ip}:{port}/")
    print(f"📥 Direct APK Link: {download_url}")
    print("#"*60)
    
    render_terminal_qr(download_url)
    
    Handler = http.server.SimpleHTTPRequestHandler
    with socketserver.TCPServer(("", port), Handler) as httpd:
        print(f"\n⚡ Serving from: {serve_path}")
        print("💡 Keep this terminal open while installing on your phone.\n")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Server stopped.")

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    run_server(port)
