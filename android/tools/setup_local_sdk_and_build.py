import os
import sys
import urllib.request
import zipfile
import subprocess
import shutil
from pathlib import Path

USERNAME = os.environ.get("USERNAME", "Vinay")
SDK_DIR = Path(f"C:/Users/{USERNAME}/AppData/Local/Android/Sdk")
GRADLE_USER_HOME = Path(f"C:/Users/{USERNAME}/.gradle")
GRADLE_ZIP_URL = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
BASE_DIR = Path(__file__).resolve().parent.parent
TOOLS_DIR = BASE_DIR / "tools"
ADB_PATH = TOOLS_DIR / "platform-tools" / "adb.exe"

def log(msg):
    print(f"\n[Paradox Build] {msg}")

def ensure_gradle():
    # Pre-download Gradle to prevent Java wrapper socket timeout
    gradle_dist_dir = GRADLE_USER_HOME / "wrapper" / "dists" / "gradle-8.9-bin"
    gradle_dist_dir.mkdir(parents=True, exist_ok=True)
    
    # Check if already extracted
    for sub in gradle_dist_dir.glob("*"):
        if (sub / "gradle-8.9" / "bin" / "gradle.bat").exists():
            log(f"Found extracted Gradle 8.9 at: {sub}")
            return sub / "gradle-8.9" / "bin" / "gradle.bat"
            
    log("Downloading Gradle 8.9 Distribution directly via Python...")
    zip_target = TOOLS_DIR / "gradle-8.9-bin.zip"
    
    if not zip_target.exists():
        req = urllib.request.Request(GRADLE_ZIP_URL, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req) as resp, open(zip_target, "wb") as f:
            total = int(resp.headers.get('content-length', 0))
            downloaded = 0
            while True:
                buf = resp.read(131072)
                if not buf:
                    break
                f.write(buf)
                downloaded += len(buf)
                if total:
                    pct = (downloaded / total) * 100
                    print(f"\rDownloading Gradle 8.9: {pct:.1f}% ({downloaded//(1024*1024)} MB)", end="")
    
    log("\nExtracting Gradle 8.9...")
    extract_dir = TOOLS_DIR / "gradle-8.9"
    with zipfile.ZipFile(zip_target, 'r') as zip_ref:
        zip_ref.extractall(TOOLS_DIR)
        
    gradle_bat = extract_dir / "bin" / "gradle.bat"
    log(f"Gradle ready at: {gradle_bat}")
    return gradle_bat

def build_local_apk(gradle_bat):
    log("Building Debug APK locally on your machine with Java 21 & Android SDK 34...")
    os.environ["ANDROID_HOME"] = str(SDK_DIR)
    os.environ["ANDROID_SDK_ROOT"] = str(SDK_DIR)
    
    cmd = [str(gradle_bat), "assembleDebug", "--no-daemon", "--stacktrace"]
    res = subprocess.run(cmd, cwd=str(BASE_DIR))
    
    if res.returncode == 0:
        log("[SUCCESS] Local APK Build Succeeded!")
        apk_file = BASE_DIR / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
        return apk_file
    else:
        log("[ERROR] Build failed. Inspect errors above.")
        return None

def install_on_usb_phone(apk_path):
    log("Checking connected USB device via ADB...")
    res = subprocess.run([str(ADB_PATH), "devices"], capture_output=True, text=True)
    print(res.stdout)
    
    if "device\n" in res.stdout or "\tdevice" in res.stdout:
        log(f"Installing {apk_path.name} directly to connected phone...")
        install_res = subprocess.run([str(ADB_PATH), "install", "-r", "-d", str(apk_path)], capture_output=True, text=True)
        print(install_res.stdout)
        
        if install_res.returncode == 0:
            log("[SUCCESS] Paradox is installed on your phone!")
            subprocess.run([str(ADB_PATH), "shell", "am", "start", "-n", "com.paradox.finance.debug/com.paradox.finance.MainActivity"])
            log("[LAUNCHED] Paradox opened on your phone screen!")
            return True
        else:
            print(f"[INSTALL FAILED]: {install_res.stderr}")
            return False
    else:
        log("[ERROR] No authorized USB device found. Make sure USB Debugging is ON.")
        return False

if __name__ == "__main__":
    gradle_bat = ensure_gradle()
    apk = build_local_apk(gradle_bat)
    if apk and apk.exists():
        install_on_usb_phone(apk)
