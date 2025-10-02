package com.signition.legendary;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LegendarySpawnDiscordPlugin extends JavaPlugin {

    private DiscordClient discord;
    private NeoForgeBridge bridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();

        String token = cfg.getString("discord.token", "");
        String channelId = cfg.getString("discord.channel_id", "");

        if (token == null || token.isBlank() || channelId == null || channelId.isBlank()) {
            getLogger().severe("discord.token / discord.channel_id 가 설정되어 있지 않습니다. config.yml 을 확인하세요.");
        } else {
            try {
                this.discord = new DiscordClient(this, token, channelId,
                        cfg.getBoolean("discord.mention_everyone", false),
                        cfg.getString("discord.mention_role_id", ""));
                getLogger().info("Discord 봇 초기화 완료.");
            } catch (Exception e) {
                getLogger().severe("Discord 클라이언트 초기화 실패: " + e.getMessage());
            }
        }

        // Pixelmon Legendary 이벤트 브리지 등록 (리플렉션 기반, Pixelmon/API 미존재 시 자동 비활성)
        this.bridge = new NeoForgeBridge(this);
        if (this.bridge.register()) {
            getLogger().info("NeoForge EVENT_BUS 등록 완료 (LegendarySpawnEvent.DoSpawn 감지).");
        } else {
            getLogger().warning("NeoForge EVENT_BUS 등록 실패 또는 Pixelmon 미탑재. 전설 스폰 알림 비활성화.");
        }
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            discord.shutdown();
        }
    }

    public DiscordClient discord() {
        return discord;
    }
}
