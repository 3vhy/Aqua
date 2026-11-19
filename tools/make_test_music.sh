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
echo "Test music fixture ready in $DIR"
