#!/usr/bin/env python3
"""Fetch the 30 public-domain texts for Eruditio (ticket 05).

Downloads each text from Project Gutenberg's plain-text endpoint, strips PG
branding per PG's developer guidance (they charge royalties if the PG name is
traded on; bundled files ship clean), verifies non-empty UTF-8 content, and
writes assets/texts/<id>.txt plus assets/texts/index.json.

Usage: python tools/fetch-texts.py <app-src-assets-dir>
"""
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

SOURCES = [
    # id, file, title, author, group
    ("apology", 1656, "Apology", "Plato", "ancient"),
    ("crito", 1576, "Crito", "Plato", "ancient"),
    ("euthyphro", 1642, "Euthyphro", "Plato", "ancient"),
    ("meno", 1800, "Meno", "Plato", "ancient"),
    ("phaedo", 1655, "Phaedo", "Plato", "ancient"),
    ("phaedrus", 1636, "Phaedrus", "Plato", "ancient"),
    ("republic", 1497, "The Republic", "Plato", "ancient"),
    ("meditations", 2680, "Meditations", "Marcus Aurelius", "ancient"),
    ("enchiridion", 45109, "Enchiridion", "Epictetus", "ancient"),
    ("golden-sayings", 4709, "The Golden Sayings of Epictetus", "Epictetus", "ancient"),
    ("odyssey", 1727, "The Odyssey", "Homer", "ancient"),
    ("aeneid", 228, "The Aeneid", "Virgil", "ancient"),
    ("consolation", 34911, "The Consolation of Philosophy", "Boethius", "ancient"),
    ("pilgrims-progress", 131, "The Pilgrim's Progress", "John Bunyan", "christian"),
    ("imitation", 4783, "The Imitation of Christ", "Thomas a Kempis", "christian"),
    ("presence", 5658, "The Practice of the Presence of God", "Brother Lawrence", "christian"),
    ("confessions", 3296, "The Confessions of St. Augustine", "Augustine", "christian"),
    ("city-of-god", 45304, "The City of God", "Augustine", "christian"),
    ("sermons-1", 1901, "Spurgeon's Sermons, Volume 1", "C. H. Spurgeon", "christian"),
    ("sermons-2", 1902, "Spurgeon's Sermons, Volume 2", "C. H. Spurgeon", "christian"),
    ("sermons-3", 1903, "Spurgeon's Sermons, Volume 3", "C. H. Spurgeon", "christian"),
    ("kjv-psalms", 10, "Psalms", "KJV", "christian"),
    ("kjv-proverbs", 10, "Proverbs", "KJV", "christian"),
    ("kjv-ecclesiastes", 10, "Ecclesiastes", "KJV", "christian"),
    ("kjv-john", 10, "The Gospel of John", "KJV", "christian"),
    ("kjv-matthew", 10, "The Gospel of Matthew", "KJV", "christian"),
    ("retrospect", 10739, "A Retrospect", "J. Hudson Taylor", "biography"),
    ("woolman", 22613, "The Journal of John Woolman", "John Woolman", "biography"),
    ("carey", 6303, "The Life of William Carey", "George Smith", "biography"),
    ("livingstone", 13256, "The Personal Life of David Livingstone", "J. Gilchrist Lawson", "biography"),
]

START = re.compile(r"^\*\*\*\s*START OF (THE|THIS) PROJECT GUTENBERG EBOOK.*$", re.I | re.M)
END = re.compile(r"^\*\*\*\s*END OF (THE|THIS) PROJECT GUTENBERG EBOOK.*$", re.I | re.M)


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "eruditio-build/1.0"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def strip_branding(text: str) -> str:
    start = START.search(text)
    end = END.search(text)
    if not start or not end:
        raise ValueError("PG markers not found")
    body = text[start.end(): end.start()]
    return body.strip() + "\n"


def extract_kjv(text: str, book_header: str) -> str:
    """Cut one book out of the KJV plain text (PG#10).

    Each book header appears twice: once in the TOC at the front and once at
    the start of the actual text. The real start is the last occurrence
    followed within a few lines by a verse marker like '1:1 '.
    """
    verse = re.compile(r"^\d+:\d+ ", re.M)
    starts = [m.start() for m in re.finditer(re.escape(book_header), text)]
    idx = None
    for cand in starts:
        if verse.search(text[cand: cand + 500]):
            idx = cand
            break
    if idx is None:
        raise ValueError(f"KJV book not found: {book_header}")
    end = len(text)
    for off in range(idx + 1, len(text)):
        if re.match(r"(The Book|The Gospel|The First|The Second|The Third|The Epistle|The Acts|The Revelation|The General|The Lamentations|The Song)", text[off:]) and not verse.search(text[off: off + 500]):
            end = off
            break
    return text[idx: end].strip() + "\n"


def main(outdir: str) -> None:
    out = Path(outdir)
    texts_dir = out / "texts"
    texts_dir.mkdir(parents=True, exist_ok=True)
    index = []
    failures = []
    for slug, gid, title, author, group in SOURCES:
        dest = texts_dir / f"{slug}.txt"
        url = f"https://www.gutenberg.org/cache/epub/{gid}/pg{gid}.txt"
        try:
            raw = fetch(url).decode("utf-8", "replace")
            if gid == 10:  # KJV: cut the requested book
                header_map = {
                    "Psalms": "The Book of Psalms",
                    "Proverbs": "The Proverbs",
                    "Ecclesiastes": "Ecclesiastes",
                    "The Gospel of John": "The Gospel According to Saint John",
                    "The Gospel of Matthew": "The Gospel According to Saint Matthew",
                }
                body = extract_kjv(raw, header_map.get(title, title))
            else:
                body = strip_branding(raw)
            if len(body) < 5_000:
                raise ValueError(f"too small: {len(body)} chars")
            dest.write_text(body, encoding="utf-8")
            index.append({"id": slug, "title": title, "author": author, "file": f"{slug}.txt", "group": group})
            print(f"ok   {slug:16s} {len(body):>9,} chars  pg{gid}")
        except Exception as e:
            failures.append((slug, gid, str(e)))
            print(f"FAIL {slug:16s} pg{gid}: {e}")
        time.sleep(0.4)  # gentle on the source
    (texts_dir / "index.json").write_text(json.dumps(index, indent=2), encoding="utf-8")
    print(f"\n{len(index)}/30 bundled; {len(failures)} failures")
    if failures:
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1])
