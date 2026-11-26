package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Auto Reconnect (reference video): after an unexpected disconnect from a server, the disconnect
 * screen gets a Reconnect button, and (if you want) reconnects by itself after a countdown.
 */
public final class AutoReconnect extends Module {
    private final BoolSetting auto = add(new BoolSetting("auto", "Reconnect by itself", true)
            .describe("Otherwise only the Reconnect button is added"));
    private final NumberSetting delay = add(new NumberSetting("delay", "Wait", 5, 2, 60, 1).unit(" s"));
    @Nullable private ServerInfo last;
    private long disconnectedAt = -1;

    public AutoReconnect() {
        super("auto_reconnect", "Auto Reconnect", "Reconnects to the last server after an unexpected disconnect.", Category.QOL, false,
                "reconnect", "relog", "auto relog", "disconnect");
    }

    /** Remembered when you join a server. */
    public void onJoin() {
        if (mc.getCurrentServerEntry() != null) {
            last = mc.getCurrentServerEntry();
        } else if (mc.isInSingleplayer()) {
            last = null;
        }
    }

    @Nullable
    public ServerInfo lastServer() {
        return last;
    }

    /** For the in-game tests: pretend you were on this server. */
    public void setLastServer(@Nullable ServerInfo info) {
        last = info;
    }

    /** Seconds left before reconnecting, or -1. */
    public int secondsLeft() {
        if (!isEnabled() || !auto.on() || last == null || disconnectedAt < 0 || !(mc.currentScreen instanceof DisconnectedScreen)) {
            return -1;
        }
        long left = Math.round(delay.d() * 1000) - (System.currentTimeMillis() - disconnectedAt);
        return (int) Math.max(0, Math.ceil(left / 1000.0));
    }

    public void onDisconnectedScreen() {
        disconnectedAt = System.currentTimeMillis();
    }

    public void reconnect() {
        if (last == null) {
            return;
        }
        disconnectedAt = -1;
        ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(last.address), last, false, null);
    }

    @Override
    public void onTick() {
        if (!(mc.currentScreen instanceof DisconnectedScreen)) {
            disconnectedAt = -1;
            return;
        }
        if (auto.on() && last != null && disconnectedAt > 0 && secondsLeft() == 0) {
            reconnect();
        }
    }
}
