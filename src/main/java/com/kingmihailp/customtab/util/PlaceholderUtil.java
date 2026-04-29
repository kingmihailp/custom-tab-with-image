package com.kingmihailp.customtab.util;

import net.minecraft.server.MinecraftServer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Replaces {placeholder} tokens in raw config strings before colour parsing. */
public final class PlaceholderUtil {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PlaceholderUtil() {}

    public static String apply(String raw, MinecraftServer server) {
        int online     = server.getPlayerCount();
        int maxPlayers = server.getMaxPlayers();
        String name    = server.getServerModName().isEmpty()
                ? server.getMotd() : server.getServerModName();
        String time    = LocalTime.now().format(TIME_FMT);

        return raw
                .replace("{online}",      String.valueOf(online))
                .replace("{max_players}", String.valueOf(maxPlayers))
                .replace("{server_name}", name)
                .replace("{time}",        time);
    }
}
