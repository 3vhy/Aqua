package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.config.ConfigManager;
import dev.aqua.client.gui.PageHost;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.gui.UiSounds;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.List;

/** Local profiles: create, duplicate, rename, delete, switch, import/export (code or file). */
public final class ProfilesPage implements Page {
    private String newName = "";
    private String renaming;
    private String renameText = "";
    private String confirmDelete;
    private long confirmUntil;

    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, PageHost menu) {
        Theme t = Aqua.theme();
        ConfigManager cfg = Aqua.config();
        // Create bar
        Page.card(ctx, t, x, y, w, 24, a);
        newName = ui.textField("pf_new", x + 6, y + 5.5f, w - 6 - 140, 13, newName, "New profile name", 32, Icon.PLUS, s -> newName = s,
                s -> create());
        if (ui.button("pf_create", x + w - 132, y + 6.5f, 60, 11, "Create", Icon.PLUS, true)) {
            create();
        }
        if (ui.button("pf_import", x + w - 68, y + 6.5f, 62, 11, "Paste code", Icon.IMPORT, false)) {
            String code = MinecraftClient.getInstance().keyboard.getClipboard();
            String name = cfg.importCode(code, null);
            if (name != null) {
                Aqua.notifications().success("Profile imported", name);
            } else {
                Aqua.notifications().warn("Import failed", "Clipboard does not contain an Aqua share code");
            }
        }
        y += 28;

        List<String> profiles = cfg.profiles();
        for (String p : profiles) {
            boolean active = p.equals(cfg.activeProfile());
            float h = 24;
            boolean hov = ui.hover(x, y, w, h);
            float ha = ui.anim("pf" + p, 120, 0).target(hov).get();
            Glass.card(ctx, x, y, w, h, 7, a, ha, active ? 1f : 0f);
            if (active) {
                R2D.outline(ctx, x, y, w, h, 7, 0.7f, Colors.fade(Colors.withAlpha(t.accent(), 0.6f), a));
            }
            R2D.icon(ctx, active ? Icon.CHECK : Icon.FOLDER, x + 8, y + h / 2f - 3.5f, 7, Colors.fade(active ? t.accent() : t.muted(), a));
            if (p.equals(renaming)) {
                renameText = ui.textField("pf_rn", x + 20, y + 5.5f, 120, 13, renameText, "Name", 32, null, s -> renameText = s, s -> {
                    if (cfg.rename(renaming, s)) {
                        Aqua.notifications().success("Profile renamed", s);
                    } else {
                        Aqua.notifications().warn("Rename failed", "Use 1-32 letters, numbers, spaces, - or _ (must be unique)");
                    }
                    renaming = null;
                });
            } else {
                R2D.textMid(ctx, p, x + 20, y + h / 2f - 2.5f, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
                R2D.textMid(ctx, active ? "Active profile" : "Click Use to switch", x + 20, y + h / 2f + 5, 4.6f, AquaFont.Weight.REGULAR,
                        Colors.fade(t.muted(), a));
            }
            float bx = x + w - 6;
            float bs = 12;
            bx -= bs;
            boolean confirming = p.equals(confirmDelete) && System.currentTimeMillis() < confirmUntil;
            if (!active && ui.iconButton("pf_del" + p, bx, y + 6, bs, Icon.TRASH, confirming ? 0xFFFF5C5C : t.muted(),
                    confirming ? "Click again to delete" : "Delete")) {
                if (confirming) {
                    if (cfg.delete(p)) {
                        Aqua.notifications().info("Profile deleted", p);
                    }
                    confirmDelete = null;
                } else {
                    confirmDelete = p;
                    confirmUntil = System.currentTimeMillis() + 3000;
                }
            }
            bx -= bs + 2;
            if (ui.iconButton("pf_file" + p, bx, y + 6, bs, Icon.EXPORT, t.muted(), "Export to file")) {
                Path f = cfg.exportFile(p);
                if (f != null) {
                    Aqua.notifications().success("Exported", "config/aqua/exports/" + f.getFileName());
                }
            }
            bx -= bs + 2;
            if (ui.iconButton("pf_code" + p, bx, y + 6, bs, Icon.COPY, t.muted(), "Copy share code")) {
                String code = cfg.exportCode(p);
                if (code != null) {
                    MinecraftClient.getInstance().keyboard.setClipboard(code);
                    Aqua.notifications().success("Share code copied", p + " (" + code.length() + " characters)");
                }
            }
            bx -= bs + 2;
            if (ui.iconButton("pf_dup" + p, bx, y + 6, bs, Icon.GRID, t.muted(), "Duplicate")) {
                String n = cfg.duplicate(p, cfg.uniqueName(p + " copy"));
                if (n != null) {
                    Aqua.notifications().success("Profile duplicated", n);
                }
            }
            bx -= bs + 2;
            if (ui.iconButton("pf_rn" + p, bx, y + 6, bs, Icon.EDIT, t.muted(), "Rename")) {
                renaming = p;
                renameText = p;
                ui.setText("pf_rn", p);
                ui.focus = "pf_rn";
            }
            if (!active) {
                bx -= 34;
                if (ui.button("pf_use" + p, bx, y + 6.5f, 30, 11, "Use", null, true)) {
                    cfg.switchTo(p);
                    Aqua.notifications().success("Profile", "Switched to " + p);
                }
            }
            y += h + 4;
        }

        // Folder import
        Page.card(ctx, t, x, y, w, 34, a);
        Page.heading(ctx, t, "Import files", "Place exported .json files in config/aqua/import", x + 9, y + 6, a);
        if (ui.button("pf_folder_imp", x + w - 9 - 50, y + 11, 50, 12, "Import", Icon.IMPORT, false)) {
            List<String> n = cfg.importFromFolder();
            if (n.isEmpty()) {
                Aqua.notifications().info("Import", "No profile files found");
            } else {
                Aqua.notifications().success("Imported", String.join(", ", n));
            }
        }
        if (ui.button("pf_folder_open", x + w - 9 - 50 - 54, y + 11, 50, 12, "Open folder", Icon.FOLDER, false)) {
            Util.getOperatingSystem().open(cfg.importDir());
        }
        UiSounds.class.getName();
        return y + 38;
    }

    private void create() {
        String name = Aqua.config().create(newName);
        if (name == null) {
            Aqua.notifications().warn("Could not create profile", "Use 1-32 letters, numbers, spaces, - or _ (must be unique)");
        } else {
            Aqua.notifications().success("Profile created", name);
            newName = "";
        }
    }
}
