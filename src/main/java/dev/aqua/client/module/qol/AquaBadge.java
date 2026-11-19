package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.net.Presence;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.object.AtlasTextObjectContents;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Shows an Aqua badge next to the names of players who use Aqua, in the tab list and on nametags.
 * Aqua users are discovered with the {@link Presence} handshake.
 */
public final class AquaBadge extends Module {
    public static final Identifier SPRITE = Identifier.of("aqua", "badge");

    private final BoolSetting tab = add(new BoolSetting("tab", "Badge in tab list", true));
    private final BoolSetting nametags = add(new BoolSetting("nametags", "Badge on nametags", true));
    private final BoolSetting announce = add(new BoolSetting("announce", "Let others see that I use Aqua", true)
            .describe("Sends a small hello on servers that support Aqua's channel (singleplayer and LAN hosted by Aqua)")
            .onChange(v -> Presence.trySendHello()));
    private final BoolSetting ownNametag = add(new BoolSetting("own_nametag", "Show my nametag in third person", false));

    public AquaBadge() {
        super("aqua_badge", "Aqua Badge", "Marks players who use Aqua in the tab list and on nametags.", Category.QOL, true,
                "badge", "tab", "nametag", "users", "icon");
        hideFromModuleList();
    }

    public boolean announce() {
        return announce.on();
    }

    public boolean ownNametag() {
        return isEnabled() && ownNametag.on();
    }

    public boolean inTab() {
        return isEnabled() && tab.on();
    }

    public boolean onNametags() {
        return isEnabled() && nametags.on();
    }

    public static MutableText badge() {
        return Text.object(new AtlasTextObjectContents(Atlases.GUI, SPRITE));
    }

    /** Prefixes the badge when the player uses Aqua. */
    public static Text decorate(Text name, UUID id) {
        if (name == null || !Presence.isAquaUser(id)) {
            return name;
        }
        return badge().append(Text.literal(" ")).append(name);
    }
}
