package dev.aqua.client.cosmetic.character;

import dev.aqua.client.cosmetic.Mesh;

/** Aqua's original 3D characters. All designs are original and drawn procedurally. */
public enum Characters {
    DROPLET("Aqua Droplet", "Mascot", "The Aqua mascot: a cheerful water drop.", new Droplet()),
    ROBO("Robo", "Characters", "A friendly service robot with a glowing visor.", new Robo()),
    PENGUIN("Penguin", "Animals", "A round little penguin.", new Penguin()),
    SEA_DRAGON("Sea Dragon", "Creatures", "A teal dragon with horns, wings and a tail.", new SeaDragon()),
    GHOST("Ghost", "Creatures", "A floating sheet ghost.", new Ghost()),
    KNIGHT("Knight", "Characters", "Plate armour with a plumed helmet.", new Knight()),
    SHARK("Shark", "Animals", "A shark costume, fin and all.", new Shark()),
    SNOWMAN("Snowman", "Characters", "Three snowballs, a carrot and a top hat.", new Snowman()),
    BLOB("Blob", "Creatures", "A squishy, glossy little slime with big shiny eyes.", new Blob());

    public final String label;
    public final String group;
    public final String description;
    public final CharacterDef def;

    Characters(String label, String group, String description, CharacterDef def) {
        this.label = label;
        this.group = group;
        this.description = description;
        this.def = def;
    }

    // ------------------------------------------------------------------ helpers

    static int lerp(int a, int b, float t) {
        return dev.aqua.client.render.Colors.lerp(a, b, t);
    }

    /** Rounded ball approximated by stacked slabs (y0 top, y1 bottom). */
    static void ball(Mesh m, float cx, float y0, float y1, float cz, float r, int c, int shade) {
        int n = 8;
        float h = (y1 - y0) / n;
        for (int i = 0; i < n; i++) {
            float mid = (i + 0.5f) / n * 2f - 1f;
            float rr = r * (float) Math.sqrt(Math.max(0.05f, 1f - mid * mid));
            m.box(cx - rr, y0 + i * h, cz - rr, cx + rr, y0 + (i + 1) * h, cz + rr, i > n * 0.7f ? shade : c);
        }
    }

    static void eye(Mesh m, float x, float y, float z, float size, int white, int pupil) {
        m.box(x - size, y - size, z - 0.05f, x + size, y + size, z + 0.4f, white);
        m.box(x - size * 0.5f, y - size * 0.35f, z - 0.15f, x + size * 0.5f, y + size * 0.75f, z, pupil);
    }

    // ------------------------------------------------------------------ characters

    static final class Droplet implements CharacterDef {
        static final int TOP = 0xFFA8F3FF, MID = 0xFF52C8E8, LOW = 0xFF1E86C0;

        @Override
        public void head(Mesh m, float t) {
            // Water drop built from stacked slabs that taper towards the tip.
            float[][] slabs = {{-12.5f, -11f, 1.0f}, {-11f, -9.5f, 2.0f}, {-9.5f, -8f, 3.0f}, {-8f, -6.5f, 4.2f},
                    {-6.5f, -5f, 5.2f}, {-5f, -3f, 6.0f}, {-3f, 3f, 6.6f}, {3f, 6f, 6.2f}, {6f, 8f, 5.2f}, {8f, 9f, 3.8f}};
            for (float[] s : slabs) {
                float f = (s[0] + 12.5f) / 21.5f;
                int c = f < 0.5f ? lerp(TOP, MID, f * 2) : lerp(MID, LOW, (f - 0.5f) * 2);
                m.box(-s[2], s[0], -s[2], s[2], s[1], s[2], c);
            }
            m.box(-4.5f, -4.5f, -6.2f, -2.5f, -1.5f, -5.9f, 0xFFFFFFFF); // highlight
            eye(m, -2.4f, 0f, -6.65f, 1.4f, 0xFFFFFFFF, 0xFF0B1A24);
            eye(m, 2.4f, 0f, -6.65f, 1.4f, 0xFFFFFFFF, 0xFF0B1A24);
            m.box(-1.5f, 3f, -6.75f, 1.5f, 3.8f, -6.55f, 0xFF0B1A24); // smile
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-2.5f, 1f, -1.2f, -0.1f, 7f, 1.2f, MID);
            m.box(-2.8f, 6.5f, -1.5f, 0.2f, 8.5f, 1.5f, TOP);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(0.1f, 1f, -1.2f, 2.5f, 7f, 1.2f, MID);
            m.box(-0.2f, 6.5f, -1.5f, 2.8f, 8.5f, 1.5f, TOP);
        }

        @Override
        public void rightLeg(Mesh m, float t) {
            m.box(-1.6f, -2f, -1.6f, 1.6f, 10f, 1.6f, LOW);
            m.box(-2.2f, 10f, -3.2f, 2.2f, 12f, 2.2f, 0xFF16648F);
        }

        @Override
        public void leftLeg(Mesh m, float t) {
            rightLeg(m, t);
        }
    }

    static final class Robo implements CharacterDef {
        static final int METAL = 0xFFA4AFBA, DARK = 0xFF5B6672, VISOR = 0xFF52E4F2;

        @Override
        public void head(Mesh m, float t) {
            m.box(-4.5f, -9f, -4.5f, 4.5f, 0f, 4.5f, METAL);
            m.glow(-3.6f, -6.2f, -4.7f, 3.6f, -3.3f, -4.5f, VISOR);
            m.box(-0.4f, -12f, -0.4f, 0.4f, -9f, 0.4f, DARK);
            boolean blink = ((int) (t * 2)) % 2 == 0;
            m.glow(-1f, -13.6f, -1f, 1f, -11.6f, 1f, blink ? 0xFFFF5C5C : 0xFF8A2A2A);
            m.box(-5.5f, -6f, -1.5f, -4.5f, -3f, 1.5f, DARK);
            m.box(4.5f, -6f, -1.5f, 5.5f, -3f, 1.5f, DARK);
            m.box(-2f, -2f, -4.6f, 2f, -1.4f, -4.5f, DARK);
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-5f, 0f, -3f, 5f, 12f, 3f, DARK);
            m.box(-3.5f, 2.5f, -3.15f, 3.5f, 7.5f, -3f, METAL);
            for (int i = 0; i < 3; i++) {
                boolean on = ((int) (t * 3) + i) % 3 == 0;
                m.glow(-2.5f + i * 2, 4f, -3.3f, -1.5f + i * 2, 5f, -3.1f, on ? VISOR : 0xFF2C5A60);
            }
            m.box(-4.5f, 10f, -3.2f, 4.5f, 11f, 3.2f, 0xFF424B55);
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-3.4f, -2f, -2f, 0.6f, 2f, 2f, METAL);
            m.box(-2.8f, 2f, -1.4f, 0f, 8f, 1.4f, DARK);
            m.box(-3.2f, 8f, -1.8f, 0.4f, 10.5f, 1.8f, METAL);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(-0.6f, -2f, -2f, 3.4f, 2f, 2f, METAL);
            m.box(0f, 2f, -1.4f, 2.8f, 8f, 1.4f, DARK);
            m.box(-0.4f, 8f, -1.8f, 3.2f, 10.5f, 1.8f, METAL);
        }

        @Override
        public void rightLeg(Mesh m, float t) {
            m.box(-1.8f, 0f, -1.8f, 1.8f, 9f, 1.8f, METAL);
            m.box(-2.3f, 9f, -3.6f, 2.3f, 12f, 2.3f, DARK);
        }

        @Override
        public void leftLeg(Mesh m, float t) {
            rightLeg(m, t);
        }
    }

    static final class Penguin implements CharacterDef {
        static final int BLACK = 0xFF1E232B, WHITE = 0xFFF3F5F7, ORANGE = 0xFFF5A23B;

        @Override
        public void head(Mesh m, float t) {
            m.box(-4.5f, -8.5f, -4.2f, 4.5f, 0.5f, 4.2f, BLACK);
            m.box(-3.6f, -6.5f, -4.35f, 3.6f, -0.5f, -4.2f, WHITE);
            eye(m, -1.8f, -4.2f, -4.45f, 0.9f, WHITE, BLACK);
            eye(m, 1.8f, -4.2f, -4.45f, 0.9f, WHITE, BLACK);
            m.box(-1.4f, -3f, -7f, 1.4f, -1.4f, -4.3f, ORANGE);
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-6f, -0.5f, -4f, 6f, 13.5f, 4.2f, BLACK);
            m.box(-4.5f, 1f, -4.15f, 4.5f, 13f, -4f, WHITE);
            m.box(-1.5f, 12.5f, 4.2f, 1.5f, 14.5f, 6f, BLACK); // tail
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-1.6f, -0.5f, -2.2f, -0.4f, 10f, 2.2f, BLACK);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(0.4f, -0.5f, -2.2f, 1.6f, 10f, 2.2f, BLACK);
        }

        @Override
        public void rightLeg(Mesh m, float t) {
            m.box(-1.4f, 1f, -1.4f, 1.4f, 10.5f, 1.4f, BLACK);
            m.box(-2.4f, 10.8f, -4.6f, 2.4f, 12f, 1.2f, ORANGE);
        }

        @Override
        public void leftLeg(Mesh m, float t) {
            rightLeg(m, t);
        }
    }

    static final class SeaDragon implements CharacterDef {
        static final int SCALE = 0xFF2BB3A3, DEEP = 0xFF1C7F86, BELLY = 0xFFD7F2E0, HORN = 0xFFF0E6CE, WING = 0xFF56D6E6;

        @Override
        public void head(Mesh m, float t) {
            m.box(-4f, -8f, -4f, 4f, 0f, 4f, SCALE);
            m.box(-3f, -5f, -9f, 3f, -1f, -4f, SCALE);
            m.box(-2.6f, -1.4f, -8.6f, 2.6f, -0.2f, -4f, BELLY);
            m.box(-2f, -5.6f, -8.5f, -1f, -4.8f, -7.5f, DEEP);
            m.box(1f, -5.6f, -8.5f, 2f, -4.8f, -7.5f, DEEP);
            m.glow(-3.6f, -6.5f, -4.15f, -1.8f, -5f, -4f, 0xFFFFD84A);
            m.glow(1.8f, -6.5f, -4.15f, 3.6f, -5f, -4f, 0xFFFFD84A);
            m.box(-3.2f, -11f, 0.5f, -2f, -8f, 2f, HORN);
            m.box(2f, -11f, 0.5f, 3.2f, -8f, 2f, HORN);
            m.box(-0.6f, -9f, 2f, 0.6f, -7.5f, 5f, DEEP);
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-5f, 0f, -3f, 5f, 12f, 4f, SCALE);
            m.box(-3.5f, 1f, -3.15f, 3.5f, 11f, -3f, BELLY);
            for (int i = 0; i < 4; i++) {
                m.box(-0.6f, 1f + i * 3f, 4f, 0.6f, 2.4f + i * 3f, 5.6f, DEEP);
            }
            // Wings: membranes behind the shoulders with a slow flap.
            float flap = (float) Math.sin(t * 2.4f) * 2.5f;
            for (int side = -1; side <= 1; side += 2) {
                float sx = side * 2.5f;
                float tipX = side * 15f, tipZ = 10f + flap;
                m.quad(sx, 1f, 4f, tipX, -6f, tipZ, tipX * 0.8f, 6f, tipZ - 1f, sx, 8f, 4f, WING, 0.9f);
                m.quad(sx, 0.4f, 4.05f, tipX, -6.6f, tipZ + 0.05f, tipX, -5.6f, tipZ + 0.05f, sx, 1.4f, 4.05f, DEEP, 0.9f);
            }
            // Tail
            float sway = (float) Math.sin(t * 1.8f) * 1.5f;
            for (int i = 0; i < 6; i++) {
                float w = 3.2f - i * 0.45f;
                float y = 9f + i * 1.4f;
                float z = 4f + i * 2.4f;
                float x = sway * i / 5f;
                m.box(x - w, y, z, x + w, y + 2.2f, z + 2.6f, i % 2 == 0 ? SCALE : DEEP);
            }
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-3.2f, -2f, -2f, 0.8f, 9f, 2f, SCALE);
            m.box(-3f, 9f, -2.6f, 0.6f, 10.5f, 1f, HORN);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(-0.8f, -2f, -2f, 3.2f, 9f, 2f, SCALE);
            m.box(-0.6f, 9f, -2.6f, 3f, 10.5f, 1f, HORN);
        }

        @Override
        public void rightLeg(Mesh m, float t) {
            m.box(-2.4f, 0f, -2.4f, 2.4f, 10f, 2.4f, SCALE);
            m.box(-2.6f, 10f, -4.4f, 2.6f, 12f, 2.4f, DEEP);
        }

        @Override
        public void leftLeg(Mesh m, float t) {
            rightLeg(m, t);
        }
    }

    static final class Ghost implements CharacterDef {
        static final int SHEET = 0xFFEFF4F8, SHADOW = 0xFFC9D3DC, FACE = 0xFF20262E;

        @Override
        public float bob(float t) {
            return -3f + (float) Math.sin(t * 2.2f) * 1.5f;
        }

        @Override
        public void head(Mesh m, float t) {
            m.box(-5f, -9f, -5f, 5f, 0.5f, 5f, SHEET);
            m.box(-3f, -6f, -5.15f, -1f, -3f, -5f, FACE);
            m.box(1f, -6f, -5.15f, 3f, -3f, -5f, FACE);
            m.box(-1.2f, -2f, -5.15f, 1.2f, -0.6f, -5f, FACE);
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-6f, 0f, -5.5f, 6f, 15f, 5.5f, SHEET);
            for (int i = 0; i < 6; i++) {
                float x = -6f + i * 2f;
                float drop = (float) Math.sin(t * 3f + i * 1.2f) * 1f + (i % 2 == 0 ? 2.5f : 1f);
                m.box(x, 15f, -5.5f, x + 2f, 15f + drop, 5.5f, i % 2 == 0 ? SHEET : SHADOW);
            }
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-2.4f, -1f, -2f, 0.6f, 6f, 2f, SHEET);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(-0.6f, -1f, -2f, 2.4f, 6f, 2f, SHEET);
        }
    }

    static final class Knight implements CharacterDef {
        static final int STEEL = 0xFFC3CCD5, DARK = 0xFF59636D, PLUME = 0xFFE0464E, TABARD = 0xFF2A8FC4, GOLD = 0xFFE7C35A;

        @Override
        public void head(Mesh m, float t) {
            m.box(-4.6f, -8.8f, -4.6f, 4.6f, 0.4f, 4.6f, STEEL);
            m.box(-3.6f, -5.6f, -4.8f, 3.6f, -4.6f, -4.6f, 0xFF15191E);
            m.box(-0.5f, -4.6f, -4.8f, 0.5f, -1f, -4.6f, DARK);
            m.box(-0.8f, -12f, -2f, 0.8f, -8.8f, 5f, PLUME);
            m.box(-0.8f, -10f, 5f, 0.8f, -6f, 6.5f, PLUME);
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-4.4f, 0f, -2.4f, 4.4f, 12f, 2.4f, STEEL);
            m.box(-3.2f, 1f, -2.6f, 3.2f, 13.5f, -2.4f, TABARD);
            m.box(-1.6f, 5f, -2.7f, 1.6f, 8f, -2.6f, GOLD);
            m.box(-1f, 3.6f, -2.7f, 1f, 5f, -2.6f, GOLD);
            m.box(-0.4f, 2.6f, -2.7f, 0.4f, 3.6f, -2.6f, GOLD);
            m.box(-4.6f, 10f, -2.6f, 4.6f, 11f, 2.6f, 0xFF6B4A2E);
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-3.8f, -2.6f, -2.6f, 1.2f, 1.6f, 2.6f, STEEL);
            m.box(-3f, 1.6f, -2f, 1f, 8f, 2f, DARK);
            m.box(-3.4f, 8f, -2.3f, 1.4f, 10.5f, 2.3f, STEEL);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(-1.2f, -2.6f, -2.6f, 3.8f, 1.6f, 2.6f, STEEL);
            m.box(-1f, 1.6f, -2f, 3f, 8f, 2f, DARK);
            m.box(-1.4f, 8f, -2.3f, 3.4f, 10.5f, 2.3f, STEEL);
        }

        @Override
        public void rightLeg(Mesh m, float t) {
            m.box(-2.1f, 0f, -2.1f, 2.1f, 12f, 2.1f, STEEL);
            m.box(-2.3f, 5f, -2.4f, 2.3f, 6.5f, 2.3f, DARK);
            m.box(-2.4f, 10.5f, -3.4f, 2.4f, 12f, 2.4f, DARK);
        }

        @Override
        public void leftLeg(Mesh m, float t) {
            rightLeg(m, t);
        }
    }

    static final class Shark implements CharacterDef {
        static final int SKIN = 0xFF6F90A8, BELLY = 0xFFF1F4F6, DARK = 0xFF3F5466, FACE = 0xFFC68A5E;

        @Override
        public void head(Mesh m, float t) {
            m.box(-5f, -9.5f, -5f, 5f, 0.5f, 5f, SKIN);
            // Upper snout over the face opening
            m.box(-4.6f, -9.5f, -8.5f, 4.6f, -5.2f, -5f, SKIN);
            m.box(-4.6f, -5.4f, -8.5f, 4.6f, -5.2f, -5f, BELLY);
            // Lower jaw
            m.box(-4.4f, -1.2f, -7.5f, 4.4f, 0.5f, -5f, BELLY);
            for (int i = 0; i < 7; i++) {
                float x = -4f + i * 1.25f;
                m.tri(x, -5.2f, -8.55f, x + 1.1f, -5.2f, -8.55f, x + 0.55f, -4.1f, -8.55f, 0xFFFFFFFF, 1f);
                m.tri(x, -1.2f, -7.55f, x + 1.1f, -1.2f, -7.55f, x + 0.55f, -2.2f, -7.55f, 0xFFFFFFFF, 1f);
            }
            // The wearer's face inside the mouth
            m.box(-3.6f, -5.2f, -5.1f, 3.6f, -1.2f, -5f, FACE);
            m.box(-2.4f, -4f, -5.2f, -1.2f, -3f, -5.1f, 0xFF2B1E1A);
            m.box(1.2f, -4f, -5.2f, 2.4f, -3f, -5.1f, 0xFF2B1E1A);
            // Shark eyes on the sides of the snout
            m.box(-4.75f, -8f, -7.6f, -4.55f, -6.8f, -6.4f, 0xFF0D1218);
            m.box(4.55f, -8f, -7.6f, 4.75f, -6.8f, -6.4f, 0xFF0D1218);
            // Dorsal fin
            for (int i = 0; i < 5; i++) {
                float h = 5f - i;
                m.box(-0.6f, -9.5f - h, -1.5f + i * 1.2f, 0.6f, -9.5f, -0.3f + i * 1.2f, DARK);
            }
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-4.5f, 0f, -2.5f, 4.5f, 12f, 2.5f, SKIN);
            m.box(-3f, 1f, -2.65f, 3f, 11.5f, -2.5f, BELLY);
            float sway = (float) Math.sin(t * 3f) * 1.2f;
            m.box(-1.5f + sway, 9f, 2.5f, 1.5f + sway, 12f, 6f, SKIN);
            m.box(-0.6f + sway, 6f, 5f, 0.6f + sway, 15f, 7f, DARK);
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-3f, -2f, -2f, 1f, 10f, 2f, SKIN);
            m.box(-3.6f, 3f, 0f, -3f, 7f, 3.5f, DARK);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(-1f, -2f, -2f, 3f, 10f, 2f, SKIN);
            m.box(3f, 3f, 0f, 3.6f, 7f, 3.5f, DARK);
        }

        @Override
        public void rightLeg(Mesh m, float t) {
            m.box(-2f, 0f, -2f, 2f, 12f, 2f, SKIN);
            m.box(-2.2f, 10.5f, -3f, 2.2f, 12f, 2.2f, BELLY);
        }

        @Override
        public void leftLeg(Mesh m, float t) {
            rightLeg(m, t);
        }
    }

    static final class Snowman implements CharacterDef {
        static final int SNOW = 0xFFF4F8FB, SHADE = 0xFFDCE6EE, COAL = 0xFF1A1C1F, CARROT = 0xFFF08A2B, STICK = 0xFF6B4A2E, SCARF = 0xFFE0464E;

        @Override
        public void head(Mesh m, float t) {
            ball(m, 0f, -8f, 0f, 0f, 4.4f, SNOW, SHADE);
            m.box(-2.4f, -5.4f, -4.6f, -1.2f, -4.2f, -4.3f, COAL);
            m.box(1.2f, -5.4f, -4.6f, 2.4f, -4.2f, -4.3f, COAL);
            m.box(-0.6f, -3.8f, -8f, 0.6f, -2.6f, -4f, CARROT);
            for (int i = 0; i < 5; i++) {
                m.box(-2.5f + i * 1.2f, -1.6f + (i == 0 || i == 4 ? -0.6f : 0), -4.2f, -1.9f + i * 1.2f, -1f + (i == 0 || i == 4 ? -0.6f : 0), -4f, COAL);
            }
            m.box(-5f, -8.6f, -5f, 5f, -8f, 5f, COAL);
            m.box(-3.5f, -14f, -3.5f, 3.5f, -8.6f, 3.5f, COAL);
            m.box(-3.6f, -10f, -3.6f, 3.6f, -9f, 3.6f, SCARF);
        }

        @Override
        public void body(Mesh m, float t) {
            m.box(-4.6f, 0f, -4.6f, 4.6f, 1.6f, 4.6f, SCARF);
            m.box(2f, 1f, -4.8f, 3.6f, 6f, -4.4f, SCARF);
            ball(m, 0f, 0.5f, 10.5f, 0f, 5.4f, SNOW, SHADE);
            for (int i = 0; i < 3; i++) {
                m.box(-0.6f, 3f + i * 2.2f, -5.6f, 0.6f, 4f + i * 2.2f, -5.2f, COAL);
            }
        }

        @Override
        public void root(Mesh m, float t) {
            ball(m, 0f, 9.5f, 24f, 0f, 7.2f, SNOW, SHADE);
        }

        @Override
        public void rightArm(Mesh m, float t) {
            m.box(-1.5f, 1f, -0.4f, -0.7f, 11f, 0.4f, STICK);
            m.box(-3f, 8f, -0.3f, -1.4f, 8.8f, 0.3f, STICK);
        }

        @Override
        public void leftArm(Mesh m, float t) {
            m.box(0.7f, 1f, -0.4f, 1.5f, 11f, 0.4f, STICK);
            m.box(1.4f, 8f, -0.3f, 3f, 8.8f, 0.3f, STICK);
        }
    }

    /** R8-style cute blob: a glossy dome that squishes as it hops along; no arms or legs. */
    static final class Blob implements CharacterDef {
        static final int BODY = 0xFF2E9BF0, DEEP = 0xFF1B6FC4, SHINE = 0xFFD9F1FF, WHITE = 0xFFFFFFFF, PUPIL = 0xFF102040, BLUSH = 0xFFFF8FB0;

        @Override
        public void root(Mesh m, float t) {
            float squish = (float) Math.sin(t * 6f) * 0.06f;
            float h = 17f * (1f - squish), r = 11f * (1f + squish * 0.8f);
            // Dome: a ball cut by the ground (y = 24 is the feet), widest at the bottom.
            int n = 12;
            float y0 = 24f - h;
            for (int i = 0; i < n; i++) {
                float a = (i + 0.5f) / n;
                float rr = r * (float) Math.sqrt(Math.max(0.04f, 1f - (1f - a) * (1f - a)));
                float ya = y0 + i * h / n, yb = y0 + (i + 1) * h / n;
                m.box(-rr, ya, -rr * 0.92f, rr, yb, rr * 0.92f, i > n - 3 ? DEEP : BODY);
            }
            // Gloss highlights
            m.glow(-6.5f, y0 + 2.5f, -8.9f, -4f, y0 + 4f, -8.6f, SHINE);
            m.glow(-7.5f, y0 + 5f, -9.5f, -6.3f, y0 + 6.2f, -9.2f, SHINE);
            // Big eyes with sparkles, blush and a tiny mouth
            float ey = y0 + h * 0.48f;
            for (int side = -1; side <= 1; side += 2) {
                float ex = side * 3.6f;
                m.box(ex - 1.9f, ey - 2.4f, -10.25f, ex + 1.9f, ey + 2.4f, -9.8f, PUPIL);
                m.glow(ex - 1.3f + side * 0.2f, ey - 1.9f, -10.35f, ex - 0.2f + side * 0.2f, ey - 0.8f, -10.25f, WHITE);
                m.glow(ex + 0.6f, ey + 0.8f, -10.35f, ex + 1.1f, ey + 1.3f, -10.25f, WHITE);
                m.box(ex + side * 1.6f - 1.4f, ey + 2.8f, -10.0f, ex + side * 1.6f + 1.4f, ey + 3.8f, -9.7f, BLUSH);
            }
            m.box(-1.1f, ey + 3.2f, -10.15f, 1.1f, ey + 4.4f, -9.9f, 0xFF7A1F3A);
        }

        @Override
        public float bob(float t) {
            return -Math.abs((float) Math.sin(t * 3f)) * 1.2f;
        }
    }
}
