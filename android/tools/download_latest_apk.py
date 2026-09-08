import os
import sys
import urllib.request
import subprocess
from pathlib import Path

REPO = "vinaykounchi101-pixel/Paradox"
RELEASE_URL = f"https://github.com/{REPO}/releases/download/android-latest/app-debug.apk"
BASE_DIR = Path(__file__).resolve().parent.parent
OUTPUT_DIR = BASE_DIR / "app" / "build" / "outputs" / "apk" / "debug"
APK_TARGET = OUTPUT_DIR / "app-debug.apk"
ADB_PATH = BASE_DIR / "tools" / "platform-tools" / "adb.exe"

def download_and_install():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    print(f"📥 Downloading latest built APK from: {RELEASE_URL} ...")
    
    headers = {"User-Agent": "Mozilla/5.0"}
    req = urllib.request.Request(RELEASE_URL, headers=headers)
    
    try:
        with urllib.request.urlopen(req) as resp, open(APK_TARGET, "wb") as f:
            total_size = int(resp.headers.get('content-length', 0))
            downloaded = 0
            block_size = 65536
            while True:
                buffer = resp.read(block_size)
                if not buffer:
                    break
                downloaded += len(buffer)
                f.write(buffer)
                if total_size:
                    percent = (downloaded / total_size) * 100
                    print(f"\rProgress: {percent:.1f}% ({downloaded//1024} KB)", end="")
        print(f"\n✅ APK saved locally to: {APK_TARGET}")
    except Exception as e:
        print(f"\n❌ Error downloading APK: {e}")
        return False

    # Check USB install
    if ADB_PATH.exists():
        print("\n🔍 Checking for connected USB devices via ADB...")
        res = subprocess.run([str(ADB_PATH), "devices"], capture_output=True, text=True)
        print(res.stdout)
        
        if "device\n" in res.stdout or "\tdevice" in res.stdout:
            print("📱 Connected device found! Installing APK over USB...")
            install_res = subprocess.run([str(ADB_PATH), "install", "-r", "-d", str(APK_TARGET)], capture_output=True, text=True)
            print(install_res.stdout)
            if install_res.returncode == 0:
                print("🎉 Paradox successfully installed on your phone!")
                subprocess.run([str(ADB_PATH), "shell", "am", "start", "-n", "com.paradox.finance.debug/com.paradox.finance.MainActivity"])
                print("🚀 Paradox app launched on your phone screen!")
                return True
            else:
                print(f"⚠️ Install failed: {install_res.stderr}")
        else:
            print("ℹ️ No authorized device detected. Make sure USB Debugging is ON.")
    return True

if __name__ == "__main__":
    download_and_install()
