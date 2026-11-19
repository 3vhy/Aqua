#!/usr/bin/env python3
"""TEST-ONLY stand-in for Spotify's media-player interface (MPRIS) on the D-Bus session bus.

Used by the in-game tests to check that Aqua picks up a track playing in another app. It plays
nothing: it only publishes metadata for two public-domain songs and answers play/pause/next/
previous/seek, like Spotify does. Spotify on Linux reports Position as 0, so this does too.

    python3 tools/mock_mpris_player.py [--art /path/to/cover.png] [--log calls.log]

Needs PyGObject (python3-gi) and a session bus (DBUS_SESSION_BUS_ADDRESS).
"""
import argparse
import sys

from gi.repository import Gio, GLib

XML = """
<node>
  <interface name="org.mpris.MediaPlayer2">
    <method name="Raise"/>
    <method name="Quit"/>
    <property name="Identity" type="s" access="read"/>
    <property name="CanQuit" type="b" access="read"/>
    <property name="CanRaise" type="b" access="read"/>
    <property name="HasTrackList" type="b" access="read"/>
    <property name="SupportedUriSchemes" type="as" access="read"/>
    <property name="SupportedMimeTypes" type="as" access="read"/>
  </interface>
  <interface name="org.mpris.MediaPlayer2.Player">
    <method name="Next"/>
    <method name="Previous"/>
    <method name="Pause"/>
    <method name="PlayPause"/>
    <method name="Stop"/>
    <method name="Play"/>
    <method name="Seek"><arg direction="in" name="Offset" type="x"/></method>
    <method name="SetPosition"><arg direction="in" name="TrackId" type="o"/><arg direction="in" name="Position" type="x"/></method>
    <method name="OpenUri"><arg direction="in" name="Uri" type="s"/></method>
    <property name="PlaybackStatus" type="s" access="read"/>
    <property name="LoopStatus" type="s" access="read"/>
    <property name="Rate" type="d" access="read"/>
    <property name="Shuffle" type="b" access="read"/>
    <property name="Metadata" type="a{sv}" access="read"/>
    <property name="Volume" type="d" access="read"/>
    <property name="Position" type="x" access="read"/>
    <property name="MinimumRate" type="d" access="read"/>
    <property name="MaximumRate" type="d" access="read"/>
    <property name="CanGoNext" type="b" access="read"/>
    <property name="CanGoPrevious" type="b" access="read"/>
    <property name="CanPlay" type="b" access="read"/>
    <property name="CanPause" type="b" access="read"/>
    <property name="CanSeek" type="b" access="read"/>
    <property name="CanControl" type="b" access="read"/>
  </interface>
</node>
"""

# Public-domain songs (texts from 1779 and 1848). The second title contains an apostrophe on purpose.
TRACKS = [
    ("Amazing Grace", "Traditional", "Hymns", 240_000_000, "/com/spotify/track/aquatest0001"),
    ("'Tis the Gift to Be Simple", "Joseph Brackett", "Shaker Songs", 150_000_000, "/com/spotify/track/aquatest0002"),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--art", default="")
    ap.add_argument("--log", default="")
    args = ap.parse_args()
    state = {"track": 0, "playing": True}
    log = open(args.log, "a", buffering=1) if args.log else sys.stdout

    def metadata():
        title, artist, album, length, tid = TRACKS[state["track"]]
        md = {
            "mpris:trackid": GLib.Variant("o", tid),
            "mpris:length": GLib.Variant("t", length),
            "xesam:title": GLib.Variant("s", title),
            "xesam:artist": GLib.Variant("as", [artist]),
            "xesam:album": GLib.Variant("s", album),
        }
        if args.art:
            md["mpris:artUrl"] = GLib.Variant("s", "file://" + args.art)
        return GLib.Variant("a{sv}", md)

    props = {
        "org.mpris.MediaPlayer2": {
            "Identity": lambda: GLib.Variant("s", "Spotify"),
            "CanQuit": lambda: GLib.Variant("b", False),
            "CanRaise": lambda: GLib.Variant("b", False),
            "HasTrackList": lambda: GLib.Variant("b", False),
            "SupportedUriSchemes": lambda: GLib.Variant("as", []),
            "SupportedMimeTypes": lambda: GLib.Variant("as", []),
        },
        "org.mpris.MediaPlayer2.Player": {
            "PlaybackStatus": lambda: GLib.Variant("s", "Playing" if state["playing"] else "Paused"),
            "LoopStatus": lambda: GLib.Variant("s", "None"),
            "Rate": lambda: GLib.Variant("d", 1.0),
            "Shuffle": lambda: GLib.Variant("b", False),
            "Metadata": metadata,
            "Volume": lambda: GLib.Variant("d", 1.0),
            "Position": lambda: GLib.Variant("x", 0),
            "MinimumRate": lambda: GLib.Variant("d", 1.0),
            "MaximumRate": lambda: GLib.Variant("d", 1.0),
            "CanGoNext": lambda: GLib.Variant("b", True),
            "CanGoPrevious": lambda: GLib.Variant("b", True),
            "CanPlay": lambda: GLib.Variant("b", True),
            "CanPause": lambda: GLib.Variant("b", True),
            "CanSeek": lambda: GLib.Variant("b", True),
            "CanControl": lambda: GLib.Variant("b", True),
        },
    }

    def on_call(conn, sender, path, iface, method, params, invocation):
        print(f"call {method} {params.unpack() if params is not None else ''}", file=log)
        if method == "PlayPause":
            state["playing"] = not state["playing"]
        elif method == "Play":
            state["playing"] = True
        elif method in ("Pause", "Stop"):
            state["playing"] = False
        elif method == "Next":
            state["track"] = (state["track"] + 1) % len(TRACKS)
        elif method == "Previous":
            state["track"] = (state["track"] - 1) % len(TRACKS)
        invocation.return_value(None)

    def on_get(conn, sender, path, iface, name):
        return props[iface][name]()

    node = Gio.DBusNodeInfo.new_for_xml(XML)

    def on_bus(conn, name):
        for iface in node.interfaces:
            conn.register_object("/org/mpris/MediaPlayer2", iface, on_call, on_get, None)

    def on_name(conn, name):
        print(f"ready {name}", file=log)

    Gio.bus_own_name(Gio.BusType.SESSION, "org.mpris.MediaPlayer2.spotify", Gio.BusNameOwnerFlags.NONE,
                     on_bus, on_name, lambda *a: (print("name lost", file=log), loop.quit()))
    loop = GLib.MainLoop()
    loop.run()


if __name__ == "__main__":
    main()
