#!/usr/bin/env python3
"""Download and combine five completed seasons of Football-Data.co.uk CSVs.

Source files remain on the source website. This script creates a normalized
matches.csv suitable for importing into Football Quant Lab.
"""
import csv
import io
import os
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

BASE = "https://www.football-data.co.uk"
SEASONS = ["2122", "2223", "2324", "2425", "2526"]
MAIN_DIVISIONS = [
    "E0", "E1", "E2", "E3", "EC",
    "SC0", "SC1", "SC2", "SC3",
    "D1", "D2", "I1", "I2", "SP1", "SP2", "F1", "F2",
    "N1", "B1", "P1", "T1", "G1",
]
EXTRA_COUNTRIES = [
    "ARG", "AUT", "BRA", "CHN", "DNK", "FIN", "IRL", "JPN",
    "MEX", "NOR", "POL", "ROU", "RUS", "SWE", "SWZ", "USA",
]
OUT = Path("dist")
RAW = OUT / "raw"
OUT.mkdir(exist_ok=True)
RAW.mkdir(exist_ok=True)

def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "FootballQuantLab-CSV-Downloader/1.0"})
    with urllib.request.urlopen(req, timeout=35) as response:
        return response.read()

def decode(raw):
    for enc in ("utf-8-sig", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            pass
    return raw.decode("cp1252", errors="replace")

def keymap(row):
    return {str(k).strip().lower(): (v or "").strip() for k, v in row.items() if k}

def get(row, *names):
    for name in names:
        value = row.get(name.lower())
        if value is not None and value != "":
            return value
    return ""

def season_year(value):
    s = str(value or "").strip()
    digits = "".join(ch for ch in s if ch.isdigit())
    if len(digits) >= 4:
        # Common forms: 2021, 2021/22, 2122, 2021-2022
        if digits.startswith("20"):
            return int(digits[:4])
        return 2000 + int(digits[:2])
    return None

def parse_csv(raw):
    return csv.DictReader(io.StringIO(decode(raw)))

def normalize(rows, label, country, season_code=None):
    result = []
    for original in rows:
        row = keymap(original)
        date = get(row, "date", "match_date")
        home = get(row, "hometeam", "home_team", "home")
        away = get(row, "awayteam", "away_team", "away")
        hg = get(row, "fthg", "home_goals", "hg")
        ag = get(row, "ftag", "away_goals", "ag")
        if not (date and home and away and hg.isdigit() and ag.isdigit()):
            continue
        season = get(row, "season", "season_code") or (season_code or "")
        if season:
            year = season_year(season)
            if year is not None and year not in range(2021, 2026):
                continue
        div = get(row, "div", "league", "competition") or label
        result.append({
            "Date": date,
            "HomeTeam": home,
            "AwayTeam": away,
            "FTHG": hg,
            "FTAG": ag,
            "Div": f"{country}-{div}",
            "Season": season or (season_code or ""),
            "Country": country,
        })
    return result

all_matches = []
downloaded = 0
failed = []

for season in SEASONS:
    for div in MAIN_DIVISIONS:
        url = f"{BASE}/mmz4281/{season}/{div}.csv"
        try:
            raw = fetch(url)
            (RAW / f"{season}_{div}.csv").write_bytes(raw)
            all_matches.extend(normalize(parse_csv(raw), div, "MAIN", season))
            downloaded += 1
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, OSError) as exc:
            failed.append(f"{season}/{div}: {exc}")
        time.sleep(0.15)

for country in EXTRA_COUNTRIES:
    url = f"{BASE}/new/{country}.csv"
    try:
        raw = fetch(url)
        (RAW / f"{country}.csv").write_bytes(raw)
        all_matches.extend(normalize(parse_csv(raw), country, country))
        downloaded += 1
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, OSError) as exc:
        failed.append(f"new/{country}: {exc}")
    time.sleep(0.25)

# De-duplicate identical match rows from overlapping source feeds.
unique = {}
for match in all_matches:
    key = (match["Date"], match["HomeTeam"], match["AwayTeam"], match["FTHG"], match["FTAG"], match["Div"])
    unique[key] = match
matches = sorted(unique.values(), key=lambda x: (x["Date"], x["Div"], x["HomeTeam"]))

csv_path = OUT / "matches.csv"
with csv_path.open("w", newline="", encoding="utf-8-sig") as f:
    writer = csv.DictWriter(f, fieldnames=["Date", "HomeTeam", "AwayTeam", "FTHG", "FTAG", "Div", "Season", "Country"])
    writer.writeheader()
    writer.writerows(matches)

with zipfile.ZipFile(OUT / "football-data-5-seasons.zip", "w", zipfile.ZIP_DEFLATED) as z:
    z.write(csv_path, arcname="matches.csv")
    for p in RAW.iterdir():
        z.write(p, arcname=f"raw/{p.name}")

print(f"Successful source files: {downloaded}")
print(f"Combined completed matches: {len(matches)}")
print(f"CSV: {csv_path}")
print(f"ZIP: {OUT / 'football-data-5-seasons.zip'}")
if failed:
    print(f"Unavailable source files: {len(failed)}")
    for item in failed:
        print(f"WARNING: {item}")
