import json
import os
import sys
import time
import urllib.request
from pathlib import Path

REPO = "vinaykounchi101-pixel/Paradox"
API_URL = f"https://api.github.com/repos/{REPO}/actions/runs"
HEADERS = {"User-Agent": "Mozilla/5.0"}

def check_status():
    req = urllib.request.Request(API_URL, headers=HEADERS)
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
    
    if not data.get("workflow_runs"):
        print("No workflow runs found.")
        return None
    
    run = data["workflow_runs"][0]
    print(f"Workflow: {run.get('name')}")
    print(f"Status:   {run.get('status')}")
    print(f"Result:   {run.get('conclusion')}")
    print(f"URL:      {run.get('html_url')}")
    return run

if __name__ == "__main__":
    check_status()
