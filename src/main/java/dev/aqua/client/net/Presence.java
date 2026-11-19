package dev.aqua.client.net;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.qol.AquaBadge;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets Aqua users see each other's badge. Each Aqua client announces itself with {@code aqua:hello};
 * a server that understands the channel answers with {@code aqua:users}. Aqua answers these itself in
 * singleplayer and in LAN worlds hosted by an Aqua user (the integrated server runs in the same game).
 * Servers without the channel are never sent anything.
 */
public final class Presence {
    public record Hello(String version) implements CustomPayload {
        public static final Id<Hello> ID = new Id<>(Identifier.of("aqua", "hello"));
        public static final PacketCodec<RegistryByteBuf, Hello> CODEC = PacketCodec.tuple(PacketCodecs.string(32), Hello::version, Hello::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record Users(List<UUID> users) implements CustomPayload {
        public static final Id<Users> ID = new Id<>(Identifier.of("aqua", "users"));
        public static final PacketCodec<RegistryByteBuf, Users> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC.collect(PacketCodecs.toList(1024)), Users::users, Users::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** UUIDs of Aqua users known to this client (from the server's list). */
    private static final Set<UUID> KNOWN = ConcurrentHashMap.newKeySet();
    /** Server side (integrated server): Aqua users per running server. */
    private static final Map<MinecraftServer, Set<UUID>> SERVER_USERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean sentHello;

    private Presence() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(Hello.ID, Hello.CODEC);
        PayloadTypeRegistry.playS2C().register(Users.ID, Users.CODEC);

        // ---- client
        ClientPlayNetworking.registerGlobalReceiver(Users.ID, (payload, context) -> {
            KNOWN.clear();
            KNOWN.addAll(payload.users());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sentHello = false;
            trySendHello();
        });
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(Hello.ID.id())) {
                client.execute(Presence::trySendHello);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            KNOWN.clear();
            sentHello = false;
        });

        // ---- integrated server (singleplayer / LAN host)
        ServerPlayNetworking.registerGlobalReceiver(Hello.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = context.server();
            SERVER_USERS.computeIfAbsent(server, s -> ConcurrentHashMap.newKeySet()).add(player.getUuid());
            broadcast(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Set<UUID> set = SERVER_USERS.get(server);
            if (set != null && set.remove(handler.player.getUuid())) {
                broadcast(server);
            }
        });
    }

    private static void broadcast(MinecraftServer server) {
        Set<UUID> set = SERVER_USERS.get(server);
        if (set == null) {
            return;
        }
        Users payload = new Users(new ArrayList<>(set));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(p, Users.ID)) {
                ServerPlayNetworking.send(p, payload);
            }
        }
    }

    /** Announces this client if the server understands Aqua's channel and the user allows it. */
    public static void trySendHello() {
        AquaBadge badge = Aqua.modules() != null ? Aqua.modules().get(AquaBadge.class) : null;
        if (sentHello || badge == null || !badge.announce() || MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        if (ClientPlayNetworking.canSend(Hello.ID)) {
            ClientPlayNetworking.send(new Hello(Aqua.VERSION));
            sentHello = true;
            AquaClient.LOGGER.info("Announced Aqua presence to the server");
        }
    }

    /** Whether the given player is known to use Aqua (always true for yourself). */
    public static boolean isAquaUser(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getUuid().equals(id)) {
            return true;
        }
        return KNOWN.contains(id);
    }

    public static int knownCount() {
        return KNOWN.size();
    }
}
