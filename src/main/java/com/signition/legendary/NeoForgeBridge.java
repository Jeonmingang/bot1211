package com.signition.legendary;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class NeoForgeBridge {

    private final Plugin plugin;
    private final AtomicLong lastSentAt = new AtomicLong(0);

    public NeoForgeBridge(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Registers this instance to NeoForge.EVENT_BUS via reflection. */
    public boolean register() {
        try {
            // Check Pixelmon event class presence
            Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent$DoSpawn");
        } catch (Throwable t) {
            plugin.getLogger().warning("Pixelmon LegendarySpawnEvent 클래스가 없습니다. (" + t.getMessage() + ")");
            return false;
        }

        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object bus = neoForge.getField("EVENT_BUS").get(null);
            bus.getClass().getMethod("register", Object.class).invoke(bus, this);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("NeoForge EVENT_BUS 등록 실패: " + t.getMessage());
            return false;
        }
    }

    @SubscribeEvent
    public void onAny(Event event) {
        // Filter only LegendarySpawnEvent.DoSpawn
        if (!event.getClass().getName().equals("com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent$DoSpawn"))
            return;

        // Dedupe
        FileConfiguration cfg = plugin.getConfig();
        int dedupeSeconds = Math.max(0, cfg.getInt("message.dedupe_seconds", 10));
        long now = Instant.now().getEpochSecond();
        long last = lastSentAt.get();
        if (dedupeSeconds > 0 && (now - last) < dedupeSeconds) return;
        lastSentAt.set(now);

        try {
            // species = event.getLegendary()
            Method getLegendary = event.getClass().getMethod("getLegendary");
            Object species = getLegendary.invoke(event);

            String name = invokeString(species, "getName", "toString");
            int dex = invokeInt(species, "getDex");

            // action.spawnLocation
            Field actionF = event.getClass().getDeclaredField("action");
            actionF.setAccessible(true);
            Object action = actionF.get(event);
            Field spawnLocF = action.getClass().getField("spawnLocation");
            Object spawnLoc = spawnLocF.get(action);

            // location/world/pos
            Field locationF = spawnLoc.getClass().getField("location");
            Object mutableLocation = locationF.get(spawnLoc);
            Field worldF = mutableLocation.getClass().getField("world");
            Object level = worldF.get(mutableLocation);
            Field posF = mutableLocation.getClass().getField("pos");
            Object pos = posF.get(mutableLocation);

            int x = (int) pos.getClass().getMethod("getX").invoke(pos);
            int y = (int) pos.getClass().getMethod("getY").invoke(pos);
            int z = (int) pos.getClass().getMethod("getZ").invoke(pos);

            // biome/dimension
            String biome = "";
            String dimension = "";
            boolean includeBiome = cfg.getBoolean("message.include_biome", true);
            boolean includeDim = cfg.getBoolean("message.include_dimension", true);

            if (includeBiome) {
                try {
                    Object holder = spawnLoc.getClass().getField("biome").get(spawnLoc);
                    biome = Objects.toString(holder);
                } catch (Throwable ignored) {}
            }
            if (includeDim) {
                try {
                    Object rk = level.getClass().getMethod("dimension").invoke(level);
                    dimension = Objects.toString(rk);
                } catch (Throwable ignored) {}
            }

            // near player (cause)
            String near = "";
            if (cfg.getBoolean("message.include_near_player", true)) {
                try {
                    Object cause = spawnLoc.getClass().getField("cause").get(spawnLoc);
                    if (cause != null) {
                        Object comp = cause.getClass().getMethod("getName").invoke(cause);
                        String playerName = (String) comp.getClass().getMethod("getString").invoke(comp);
                        near = " (근처: " + playerName + ")";
                    }
                } catch (Throwable ignored) {}
            }

            // Build message
            String template = cfg.getString("message.template",
                    "✨ 전설 포켓몬 스폰! **{name}** (#{dex}) — 좌표 {x} {y} {z}{near}");
            template = template.replace("{name}", name)
                    .replace("{dex}", String.valueOf(dex))
                    .replace("{x}", String.valueOf(x))
                    .replace("{y}", String.valueOf(y))
                    .replace("{z}", String.valueOf(z))
                    .replace("{near}", near);

            if (includeBiome) {
                template = template.replace("{biome}", biome);
            } else {
                template = template.replace("{biome}", "-");
            }
            if (includeDim) {
                template = template.replace("{dimension}", dimension);
            } else {
                template = template.replace("{dimension}", "-");
            }

            DiscordClient dc = ((LegendarySpawnDiscordPlugin) plugin).discord();
            if (dc != null) {
                dc.sendLegendary("전설 스폰 감지", template);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Legendary 스폰 처리 중 오류: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static int invokeInt(Object o, String method) throws Exception {
        return (int) o.getClass().getMethod(method).invoke(o);
    }

    private static String invokeString(Object o, String... methods) throws Exception {
        for (String m : methods) {
            try {
                Object r = o.getClass().getMethod(m).invoke(o);
                if (r != null) return r.toString();
            } catch (NoSuchMethodException ignored) {}
        }
        return String.valueOf(o);
    }
}
