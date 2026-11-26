package dev.aqua.client.module.qol;

import com.google.gson.JsonObject;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.StringSetting;
import dev.aqua.client.util.Problems;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Discord Presence (reference video): shows Aqua Client, and optionally the server you are on, in
 * your Discord status. It talks to the Discord app on this computer over Discord's local IPC
 * socket; nothing goes over the internet from Aqua. Discord needs an application ID, which you
 * create once in the Discord developer portal and paste into the settings.
 */
public final class DiscordPresence extends Module {
    private final StringSetting appId = add(new StringSetting("app_id", "Application ID", "", 24)
            .describe("From discord.com/developers/applications (an app named \"Aqua Client\" shows that name)"));
    private final BoolSetting showServer = add(new BoolSetting("show_server", "Show the server", true)
            .describe("Hidden while Streamer Mode hides the server address"));
    private final BoolSetting showTime = add(new BoolSetting("show_time", "Show play time", true));

    private Thread worker;
    private volatile boolean running;
    private volatile String status = "Off";
    private final long started = System.currentTimeMillis() / 1000L;

    public DiscordPresence() {
        super("discord_presence", "Discord Presence", "Shows Aqua Client and the server you are on in your Discord status.", Category.QOL, false,
                "discord", "rich presence", "rpc", "status");
    }

    public String status() {
        return status;
    }

    @Override
    protected void onEnable() {
        if (appId.get().isBlank()) {
            status = "Needs an application ID";
            Aqua.notifications().warn("Discord Presence", "Paste your Discord application ID in its settings");
        }
        running = true;
        worker = new Thread(this::loop, "Aqua Discord presence");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    protected void onDisable() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
        status = "Off";
    }

    private String details() {
        if (mc.world == null) {
            return "In the menus";
        }
        if (mc.isInSingleplayer()) {
            return "Playing singleplayer";
        }
        StreamerMode sm = Aqua.modules().get(StreamerMode.class);
        boolean hide = sm != null && sm.isEnabled() && sm.hideServer();
        if (showServer.on() && !hide && mc.getCurrentServerEntry() != null) {
            return "Playing on " + mc.getCurrentServerEntry().address;
        }
        return "Playing multiplayer";
    }

    private void loop() {
        while (running) {
            String id = appId.get().trim();
            if (!id.matches("\\d{5,24}")) {
                sleep(2000);
                continue;
            }
            try (Ipc ipc = Ipc.open()) {
                if (ipc == null) {
                    status = "Discord is not running";
                    sleep(15000);
                    continue;
                }
                JsonObject hs = new JsonObject();
                hs.addProperty("v", 1);
                hs.addProperty("client_id", id);
                ipc.send(0, hs.toString());
                ipc.read();
                status = "Connected";
                String lastSent = "";
                while (running && id.equals(appId.get().trim())) {
                    String d = details();
                    if (!d.equals(lastSent)) {
                        ipc.send(1, activity(d).toString());
                        ipc.read();
                        lastSent = d;
                    }
                    sleep(5000);
                }
            } catch (IOException e) {
                status = "Disconnected";
                sleep(10000);
            } catch (RuntimeException e) {
                Problems.report("Discord Presence failed", e);
                status = "Failed";
                running = false;
            }
        }
    }

    private JsonObject activity(String details) {
        JsonObject act = new JsonObject();
        act.addProperty("details", details);
        act.addProperty("state", "Aqua Client • Minecraft 1.21.11");
        if (showTime.on()) {
            JsonObject ts = new JsonObject();
            ts.addProperty("start", started);
            act.add("timestamps", ts);
        }
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", act);
        JsonObject o = new JsonObject();
        o.addProperty("cmd", "SET_ACTIVITY");
        o.add("args", args);
        o.addProperty("nonce", UUID.randomUUID().toString());
        return o;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** Discord's local IPC pipe (Windows named pipe, or a Unix socket elsewhere). */
    private static final class Ipc implements AutoCloseable {
        private final SocketChannel unix;
        private final RandomAccessFile pipe;

        private Ipc(SocketChannel unix, RandomAccessFile pipe) {
            this.unix = unix;
            this.pipe = pipe;
        }

        static Ipc open() {
            String override = System.getProperty("aqua.discord.ipc");
            for (int i = 0; i < 10; i++) {
                if (override == null && System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    try {
                        return new Ipc(null, new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
                    } catch (IOException ignored) {
                        continue;
                    }
                }
                for (Path dir : dirs(override)) {
                    Path p = dir.resolve("discord-ipc-" + i);
                    if (Files.exists(p)) {
                        try {
                            SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                            ch.connect(UnixDomainSocketAddress.of(p));
                            return new Ipc(ch, null);
                        } catch (IOException ignored) {
                            // try the next one
                        }
                    }
                }
            }
            return null;
        }

        private static Path[] dirs(String override) {
            if (override != null) {
                return new Path[]{Path.of(override)};
            }
            java.util.List<Path> l = new java.util.ArrayList<>();
            for (String env : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
                String v = System.getenv(env);
                if (v != null) {
                    l.add(Path.of(v));
                    l.add(Path.of(v, "app", "com.discordapp.Discord"));
                    l.add(Path.of(v, "snap.discord"));
                }
            }
            l.add(Path.of("/tmp"));
            return l.toArray(new Path[0]);
        }

        void send(int op, String json) throws IOException {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ByteBuffer b = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(op).putInt(body.length).put(body).flip();
            if (unix != null) {
                while (b.hasRemaining()) {
                    unix.write(b);
                }
            } else {
                pipe.write(b.array());
            }
        }

        String read() throws IOException {
            ByteBuffer head = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            readFully(head);
            head.flip();
            head.getInt();
            int len = head.getInt();
            if (len < 0 || len > 1 << 20) {
                throw new IOException("bad frame");
            }
            ByteBuffer body = ByteBuffer.allocate(len);
            readFully(body);
            return new String(body.array(), StandardCharsets.UTF_8);
        }

        private void readFully(ByteBuffer b) throws IOException {
            if (unix != null) {
                while (b.hasRemaining()) {
                    if (unix.read(b) < 0) {
                        throw new IOException("closed");
                    }
                }
            } else {
                byte[] a = new byte[b.remaining()];
                pipe.readFully(a);
                b.put(a);
            }
        }

        @Override
        public void close() {
            try {
                if (unix != null) {
                    unix.close();
                }
                if (pipe != null) {
                    pipe.close();
                }
            } catch (IOException ignored) {
                // closing anyway
            }
        }
    }
}
