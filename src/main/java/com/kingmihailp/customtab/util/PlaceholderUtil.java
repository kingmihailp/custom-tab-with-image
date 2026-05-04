package com.kingmihailp.customtab.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Replaces {placeholder} tokens in raw config strings before colour parsing.
 *
 * Global placeholders (available everywhere):
 *   {online}       — number of players currently online
 *   {max_players}  — server player limit
 *   {server_name}  — server name / motd
 *   {time}         — current server time  (HH:mm:ss)
 *   {date}         — current server date  (yyyy-MM-dd)
 *   {tps}          — server TPS (0.0–20.0)
 *   {cpu}          — JVM process CPU load (e.g. "42.3%"), or "N/A"
 *   {ram_used}     — used heap memory in MB
 *   {ram_max}      — max heap memory in MB
 *
 * Per-player placeholders (empty string when no player context):
 *   {player}       — player's in-game name
 *   {ping}         — player's ping in ms
 */
public final class PlaceholderUtil {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PlaceholderUtil() {}

    /** Full replacement with per-player context and live TPS. */
    public static String apply(String raw, MinecraftServer server,
                               @Nullable ServerPlayer player, float tps) {
        int online     = server.getPlayerCount();
        int maxPlayers = server.getMaxPlayers();
        String name    = server.getMotd();
        String time    = LocalTime.now().format(TIME_FMT);
        String date    = LocalDate.now().format(DATE_FMT);
        String tpsStr  = String.format("%.1f", (double) tps);
        String cpu     = getCpuLoad();
        long   used    = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
        long   max     = Runtime.getRuntime().maxMemory() >> 20;

        String playerName = player != null ? player.getGameProfile().getName() : "";
        String ping       = player != null ? String.valueOf(player.connection.latency()) : "N/A";

        return raw
                .replace("{online}",      String.valueOf(online))
                .replace("{max_players}", String.valueOf(maxPlayers))
                .replace("{server_name}", name)
                .replace("{time}",        time)
                .replace("{date}",        date)
                .replace("{tps}",         tpsStr)
                .replace("{cpu}",         cpu)
                .replace("{ram_used}",    String.valueOf(used))
                .replace("{ram_max}",     String.valueOf(max))
                .replace("{player}",      playerName)
                .replace("{ping}",        ping);
    }

    /** Backward-compatible overload — no player context, assumes 20 TPS. */
    public static String apply(String raw, MinecraftServer server) {
        return apply(raw, server, null, 20.0f);
    }

    // ------------------------------------------------------------------

    private static String getCpuLoad() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            double load = sunOs.getCpuLoad();
            if (load >= 0) return String.format("%.1f%%", load * 100.0);
        }
        return "N/A";
    }
}
