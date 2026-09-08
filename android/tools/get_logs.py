import json
import urllib.request
import zipfile
import io

REPO = "vinaykounchi101-pixel/Paradox"
RUN_ID = "34274477351"
API_URL = f"https://api.github.com/repos/{REPO}/actions/runs/{RUN_ID}/logs"
HEADERS = {"User-Agent": "Mozilla/5.0"}

def download_logs():
    req = urllib.request.Request(API_URL, headers=HEADERS)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read()
            with zipfile.ZipFile(io.BytesIO(content)) as z:
                for filename in z.namelist():
                    if "Build Debug APK" in filename or "build" in filename.lower():
                        log_data = z.read(filename).decode('utf-8', errors='ignore')
                        lines = log_data.splitlines()
                        print(f"=== {filename} ===")
                        for line in lines[-50:]:
                            print(line)
    except Exception as e:
        print(f"Error fetching logs: {e}")

if __name__ == "__main__":
    download_logs()
