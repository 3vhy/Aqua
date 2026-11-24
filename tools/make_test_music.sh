#!/usr/bin/env bash
# Creates a TEST-ONLY music fixture for the in-game test suite: a generated sine tone (no third-party
# audio) plus a small timed lyric file. It is written to the test run directory only and is never
# packaged into the mod jar.
set -euo pipefail
DIR="${1:-build/run/clientGameTest/config/aqua/music}"
mkdir -p "$DIR"
OUT="$DIR/Aqua Test - Generated Tone.ogg"
if [ ! -f "$OUT" ]; then
  ffmpeg -v error -y -f lavfi -i "sine=frequency=330:duration=40,volume=0.15" \
    -c:a libvorbis -q:a 2 -metadata TITLE="Generated Tone" -metadata ARTIST="Aqua Test" "$OUT"
fi
cat > "$DIR/Aqua Test - Generated Tone.lrc" <<'LRC'
[ti:Generated Tone]
[ar:Aqua Test]
[00:00.50]aqua
[00:03.00]synced lyric lines
[00:06.00]from a local .lrc file
[00:09.00]drawn in the sky
[00:12.00]and on the music card
[00:20.00]end of test lyrics
LRC
# Second track WITHOUT a .lrc file: its synced lyrics are looked up online (LRCLIB) by title and
# artist. "Amazing Grace" is a public-domain hymn (John Newton, 1779); the audio is again only a tone.
OUT2="$DIR/Traditional - Amazing Grace.ogg"
if [ ! -f "$OUT2" ]; then
  ffmpeg -v error -y -f lavfi -i "sine=frequency=262:duration=30,volume=0.12" \
    -c:a libvorbis -q:a 2 -metadata TITLE="Amazing Grace" -metadata ARTIST="Traditional" "$OUT2"
fi
# Third track with a large cover embedded in its tags (METADATA_BLOCK_PICTURE, well over 256 KB), as
# many downloaded songs have. Checks that big tags keep their title/artist and the cover is shown.
OUT3="$DIR/Aqua Test - Picture.ogg"
if [ ! -f "$OUT3" ]; then
  TMP=$(mktemp -d)
  ffmpeg -v error -y -f lavfi -i "nullsrc=s=640x640,geq=r='random(1)*255':g='128+64*sin(X/40)':b='200'" -frames:v 1 "$TMP/cover.png"
  # The picture tag is too long for a command line, so it goes in through an ffmpeg metadata file.
  python3 -I -c '
import base64, struct, sys
img = open(sys.argv[1], "rb").read()
mime = b"image/png"
block = struct.pack(">I", 3) + struct.pack(">I", len(mime)) + mime + struct.pack(">I", 0) + struct.pack(">IIII", 640, 640, 24, 0) + struct.pack(">I", len(img)) + img
esc = lambda v: v.replace("\\", "\\\\").replace("=", "\\=").replace(";", "\\;").replace("#", "\\#")
with open(sys.argv[2], "w") as f:
    f.write(";FFMETADATA1\ntitle=Picture Test\nartist=Aqua Test\nMETADATA_BLOCK_PICTURE=" + esc(base64.b64encode(block).decode()) + "\n")
' "$TMP/cover.png" "$TMP/meta.txt"
  ffmpeg -v error -y -f lavfi -i "sine=frequency=440:duration=20,volume=0.1" -i "$TMP/meta.txt" -map 0:a -map_metadata 1 \
    -c:a libvorbis -q:a 2 "$OUT3"
  rm -rf "$TMP"
fi
echo "Test music fixture ready in $DIR"
