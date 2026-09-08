import json
import urllib.request
import zipfile
import io

REPO = "vinaykounchi101-pixel/Paradox"
RUN_ID = "34274477351"
API_URL = f"https://api.github.com/repos/{REPO}/actions/runs/{RUN_ID}/jobs"
HEADERS = {"User-Agent": "Mozilla/5.0"}

def fetch_jobs():
    req = urllib.request.Request(API_URL, headers=HEADERS)
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
    
    for job in data.get("jobs", []):
        print(f"Job: {job['name']}, Conclusion: {job['conclusion']}")
        for step in job.get("steps", []):
            if step.get("conclusion") == "failure":
                print(f"  FAILED STEP: {step['name']}")

if __name__ == "__main__":
    fetch_jobs()
