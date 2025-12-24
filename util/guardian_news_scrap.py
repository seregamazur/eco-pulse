import csv
import os
import requests
import time
from bs4 import BeautifulSoup
from datetime import date, timedelta

API_KEY = "NOT_SET"
pwd = os.getcwd()
OUTPUT_DIR = os.path.join(pwd, "raw_news")
PAGE_SIZE = 20

def clean_html(text):
    return BeautifulSoup(text or "", "html.parser").get_text(separator=" ", strip=True)

def fetch_day_articles(d):
    url = "https://content.guardianapis.com/search"
    params = {
        "section": "environment",
        "from-date": d,
        "to-date": d,
        "api-key": API_KEY,
        "page-size": PAGE_SIZE,
        "show-fields": "body,headline",
        "tag": "-environment/series/country-diary"
    }
    res = requests.get(url, params=params, timeout=20)
    res.raise_for_status()
    data = res.json()
    if "response" not in data:
        return []
    results = data["response"].get("results", [])
    articles = []
    for r in results:
        fields = r.get("fields", {})
        title = fields.get("headline")
        body = clean_html(fields.get("body"))
        if title and body:
            articles.append((title, body))
    return articles

def save_csv(d, articles):
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
    path = os.path.join(OUTPUT_DIR, f"{d}.csv")
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Title", "Article Text"])
        writer.writerows(articles)

def run():
    start = date(2025, 12, 10)
    end = date(2025, 12, 22)
    cur = start
    while cur <= end:
        d = cur.isoformat()
        articles = fetch_day_articles(d)
        if articles:
            save_csv(d, articles)
        time.sleep(1.0)
        cur += timedelta(days=1)

if __name__ == "__main__":
    run()
