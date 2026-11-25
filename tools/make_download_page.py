#!/usr/bin/env python3
"""Builds dist/index.html: a single-file download page with the release jar embedded in it.

The jar is stored inside the page (base64) so the page works on its own, wherever it is opened or
hosted. The page checks the SHA-256 of the embedded jar before the download starts.

    python3 tools/make_download_page.py
"""
import base64
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAR_NAME = "aqua-client-1.0.0+mc1.21.11.jar"
# Screenshot shown on the page (from the release-jar test run).
SHOT = "docs/screenshots/7-glass_menu_full.jpg"


def main() -> None:
    jar = (ROOT / "dist" / JAR_NAME).read_bytes()
    sha = hashlib.sha256(jar).hexdigest()
    recorded = (ROOT / "dist" / (JAR_NAME + ".sha256")).read_text().split()[0]
    if sha != recorded:
        raise SystemExit(f"dist/{JAR_NAME}.sha256 does not match the jar ({recorded} != {sha})")
    data = base64.b64encode(jar).decode()
    wrapped = "\n".join(data[i:i + 120] for i in range(0, len(data), 120))
    page = (ROOT / "tools" / "download_page.template.html").read_text()
    page = (page.replace("@@JARDATA@@", wrapped)
                .replace("@@LOGO@@", base64.b64encode((ROOT / "branding/icons/aqua_128.png").read_bytes()).decode())
                .replace("@@FAVICON@@", base64.b64encode((ROOT / "branding/icons/aqua_32.png").read_bytes()).decode())
                .replace("@@SHOT@@", base64.b64encode((ROOT / SHOT).read_bytes()).decode())
                .replace("@@SHA256@@", sha)
                .replace("@@SIZE@@", f"{len(jar) / 1_000_000:.1f} MB")
                .replace("@@JAR@@", JAR_NAME))
    if "@@" in page:
        raise SystemExit("unfilled placeholder in the template")
    out = ROOT / "dist" / "index.html"
    out.write_text(page)
    print(f"Wrote {out.relative_to(ROOT)} ({out.stat().st_size / 1_000_000:.1f} MB, jar SHA-256 {sha})")


if __name__ == "__main__":
    main()
