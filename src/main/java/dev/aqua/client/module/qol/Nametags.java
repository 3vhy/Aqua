package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.social.Friends;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Nametags (reference video), without the parts that would be an advantage: bigger, easier to
 * read player nametags with the player's ping and distance, and friends in green. Nametags still
 * follow the normal rules (hidden behind walls when sneaking, and so on); no health or armour is
 * added.
 */
public final class Nametags extends Module {
    private final NumberSetting scale = add(new NumberSetting("scale", "Size", 1.3, 0.8, 2.5, 0.05).unit("x"));
    private final BoolSetting ping = add(new BoolSetting("ping", "Show ping", true));
    private final BoolSetting distance = add(new BoolSetting("distance", "Show distance", true));
    private final BoolSetting friends = add(new BoolSetting("friends", "Friends in green", true));

    public Nametags() {
        super("nametag_style", "Nametag Style", "Bigger player nametags with ping, distance and friends in green.", Category.QOL, false,
                "nametag", "name tag", "ping", "distance", "friends");
    }

    public float scale() {
        return scale.f();
    }

    /** Adds ping and distance to another player's nametag. */
    public Text decorate(Text name, PlayerEntity p) {
        if (mc.player == null || p == mc.player) {
            return name;
        }
        MutableText out = name.copy();
        if (friends.on() && Friends.isFriend(p.getGameProfile().name())) {
            out = out.copy().styled(s -> s.withColor(Friends.COLOR & 0xFFFFFF));
        }
        if (ping.on() && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (e != null) {
                int ms = e.getLatency();
                Formatting f = ms < 80 ? Formatting.GREEN : ms < 180 ? Formatting.YELLOW : Formatting.RED;
                out.append(Text.literal(" " + ms + "ms").formatted(f));
            }
        }
        if (distance.on()) {
            out.append(Text.literal(" " + Math.round(mc.player.distanceTo(p)) + "m").formatted(Formatting.GRAY));
        }
        return out;
    }
}
