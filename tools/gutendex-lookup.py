import json
import time
import urllib.request
import urllib.parse

QUERIES = {
    "crito": "Crito Plato",
    "phaedo": "Phaedo Plato",
    "meno": "Meno Plato",
    "golden-sayings": "Golden Sayings Epictetus",
    "imitation": "Imitation of Christ",
    "presence": "Practice of the Presence of God",
    "consolation": "Consolation of Philosophy Boethius",
    "sermons-1": "Spurgeon's Sermons Volume 1",
    "sermons-2": "Spurgeon's Sermons Volume 2",
    "sermons-3": "Spurgeon's Sermons Volume 3",
    "retrospect": "Retrospect Hudson Taylor",
    "carey": "Life of William Carey George Smith",
    "livingstone": "Personal Life of David Livingstone",
}


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "eruditio-build/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read()


def fmt_txt(book: dict) -> bool:
    return any(k.startswith("text/plain") for k in book["formats"])


def main() -> None:
    for slug, q in QUERIES.items():
        url = "https://gutendex.com/books?search=" + urllib.parse.quote(q)
        for attempt in range(3):
            try:
                d = json.loads(fetch(url))
                print("===", slug, "count", d["count"])
                for b in d["results"][:6]:
                    authors = b["authors"][0]["name"] if b["authors"] else "?"
                    print("  ", b["id"], "|", b["title"][:64], "|", authors, "|", "TXT" if fmt_txt(b) else "NOTXT")
                break
            except Exception as e:
                if attempt == 2:
                    print("===", slug, "ERROR", e)
                else:
                    time.sleep(2)
        time.sleep(0.5)


if __name__ == "__main__":
    main()
