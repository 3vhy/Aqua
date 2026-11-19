#!/usr/bin/env bash
# Runs Aqua's in-game test suite twice (launch 1, then a fresh launch 2 that verifies persistence)
# under a virtual display with software OpenGL and a null audio device.
#
#   tools/run_gametests.sh            # development environment (Loom runClientGameTest)
#   tools/run_gametests.sh production # remapped release jar in the production (intermediary) game
#   tools/run_gametests.sh compat     # production + Sodium, Iris and Mod Menu from Modrinth
#
# Results and screenshots are collected in build/test-results-<mode>/.
set -uo pipefail
cd "$(dirname "$0")/.."
MODE="${1:-dev}"
case "$MODE" in
  dev) TASK=runClientGameTest; DIR=build/run/clientGameTest; STATE=$DIR ;;
  production) TASK=runProductionGameTest; DIR=build/run/productionTest; STATE=build/run/productionTest-state ;;
  compat) TASK=runCompatGameTest; DIR=build/run/compatTest; STATE=build/run/compatTest-state ;;
  *) echo "unknown mode $MODE"; exit 2 ;;
esac
OUT=build/test-results-$MODE
rm -rf "$DIR" "$STATE" "$OUT"
mkdir -p "$DIR" "$STATE" "$OUT"
./tools/make_test_music.sh build/test-fixtures/music >/dev/null
./tools/make_test_music.sh "$DIR/config/aqua/music" >/dev/null
export ALSOFT_DRIVERS=null LP_NUM_THREADS=3
# Private D-Bus session for the test that checks music from another app (Spotify) is picked up. The
# test starts tools/mock_mpris_player.py on it; nothing else is on this bus.
if command -v dbus-daemon >/dev/null; then
  BUS_INFO=$(dbus-daemon --session --fork --print-address=1 --print-pid=1)
  export DBUS_SESSION_BUS_ADDRESS=$(echo "$BUS_INFO" | sed -n 1p)
  BUS_PID=$(echo "$BUS_INFO" | sed -n 2p)
  trap 'kill $BUS_PID 2>/dev/null' EXIT
fi
for py in python3 python3.12 python3.13 python3.11; do
  if command -v $py >/dev/null && $py -c "from gi.repository import Gio" 2>/dev/null; then export AQUA_TEST_PYTHON=$(command -v $py); break; fi
done
# Spotify's cover art is JPEG, so the stand-in player serves a JPEG cover too.
mkdir -p build/test-fixtures && ffmpeg -v error -y -i branding/icons/aqua_256.png build/test-fixtures/spotify_cover.jpg
export AQUA_TEST_MOCK_MPRIS="$PWD/tools/mock_mpris_player.py" AQUA_TEST_MOCK_ART="$PWD/build/test-fixtures/spotify_cover.jpg"
for phase in 1 2; do
  echo "=== Aqua game tests ($MODE): launch $phase ==="
  xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew "$TASK" --no-daemon
  code=$?
  if [ -d "$DIR/screenshots" ]; then mkdir -p "$OUT/screenshots"; cp "$DIR"/screenshots/*.png "$OUT/screenshots/" 2>/dev/null; fi
  cp "$STATE"/aqua-test-results-phase*.txt "$OUT/" 2>/dev/null
  cp "$DIR"/logs/latest.log "$OUT/latest-launch$phase.log" 2>/dev/null
  [ $code -eq 0 ] || exit $code
done
cat "$OUT"/aqua-test-results-phase1.txt "$OUT"/aqua-test-results-phase2.txt
