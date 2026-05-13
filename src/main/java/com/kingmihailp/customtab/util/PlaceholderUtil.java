package com.kingmihailp.customtab.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Replaces {placeholder} tokens in raw config strings before colour parsing.
 *
 * Global placeholders:
 *   {online}        — players online
 *   {max_players}   — server player limit
 *   {server_name}   — server name / motd
 *   {time}          — HH:mm:ss
 *   {date}          — yyyy-MM-dd
 *   {tps}           — server TPS (0.0–20.0)
 *   {cpu}           — JVM CPU load (e.g. "42.3%"), or "N/A"
 *   {ram_used}      — used heap MB
 *   {ram_max}       — max heap MB
 *
 * Per-player placeholders:
 *   {player}        — raw username (no prefix)
 *   {ping}          — player's ping in ms
 *   {display_name}  — full display name with prefix/suffix (LuckPerms etc.),
 *                     colours preserved. Use buildComponent() to get this as
 *                     a proper styled Component; apply() yields plain text.
 */
public final class PlaceholderUtil {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PlaceholderUtil() {}

    // ------------------------------------------------------------------
    // Main entry point — returns a fully-styled Component.
    // {display_name} is embedded as an actual Component (colours intact).
    // ------------------------------------------------------------------

    /**
     * Applies all placeholders and returns a styled Component.
     * This is the preferred method for building tab header/footer lines.
     *
     * {display_name} is embedded as the player's live display-name Component,
     * so LuckPerms / Essentials prefix colours are fully preserved.
     */
    public static Component buildComponent(String raw, MinecraftServer server,
                                           @Nullable ServerPlayer player, float tps) {
        // Fast path: no {display_name} → plain string → ColorUtil
        if (player == null || !raw.contains("{display_name}")) {
            return ColorUtil.parse(apply(raw, server, player, tps));
        }

        // Split on {display_name} and weave the live Component in between
        String[] segments = raw.split("\\{display_name\\}", -1);
        MutableComponent result = Component.empty();
        Component displayName = resolveDisplayName(player);

        for (int i = 0; i < segments.length; i++) {
            String resolved = apply(segments[i], server, player, tps);
            if (!resolved.isEmpty()) {
                result.append(ColorUtil.parse(resolved));
            }
            if (i < segments.length - 1) {
                result.append(displayName.copy());
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // String-level replacement (no Component embedding).
    // {display_name} → plain text (colours stripped) as a fallback.
    // ------------------------------------------------------------------

    /** Replaces all placeholders with plain strings. */
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

        String playerName   = player != null ? player.getGameProfile().getName() : "";
        String ping         = player != null ? String.valueOf(player.connection.latency()) : "N/A";
        // Plain-text fallback for {display_name} when buildComponent() is not used
        String displayPlain = player != null ? resolveDisplayName(player).getString() : "";

        return raw
                .replace("{online}",       String.valueOf(online))
                .replace("{max_players}",  String.valueOf(maxPlayers))
                .replace("{server_name}",  name)
                .replace("{time}",         time)
                .replace("{date}",         date)
                .replace("{tps}",          tpsStr)
                .replace("{cpu}",          cpu)
                .replace("{ram_used}",     String.valueOf(used))
                .replace("{ram_max}",      String.valueOf(max))
                .replace("{player}",       playerName)
                .replace("{ping}",         ping)
                .replace("{display_name}", displayPlain);
    }

    /** Backward-compatible overload — no player context, assumes 20 TPS. */
    public static String apply(String raw, MinecraftServer server) {
        return apply(raw, server, null, 20.0f);
    }

    // ------------------------------------------------------------------

    /**
     * Returns the player's display name Component including any LuckPerms
     * prefix/suffix. NeoForge fires PlayerEvent.NameFormat when getDisplayName()
     * is called, which LuckPerms and other permission mods use to inject
     * their formatted name.
     */
    private static Component resolveDisplayName(ServerPlayer player) {
        return player.getDisplayName();
    }

    private static String getCpuLoad() {
        try {
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            Method m = os.getClass().getMethod("getCpuLoad");
            Object result = m.invoke(os);
            if (result instanceof Number n && n.doubleValue() >= 0) {
                return String.format("%.1f%%", n.doubleValue() * 100.0);
            }
        } catch (Throwable ignored) {
        }
        return "N/A";
    }
}
