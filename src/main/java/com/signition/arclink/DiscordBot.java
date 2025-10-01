package com.signition.arclink;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordBot {
    private final ArcLinkPlugin plugin;
    private final AtomicReference<JDA> jdaRef = new AtomicReference<>(null);

    public DiscordBot(ArcLinkPlugin plugin) { this.plugin = plugin; }

    public void start(String token, String guildIdIgnored) {
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT
        );
        JDA jda = JDABuilder.createLight(token, intents)
                .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                .addEventListeners(new DiscordListener(plugin))
                .build();
        jdaRef.set(jda);
        plugin.getLogger().info("JDA build initiated.");
    }

    public void shutdown() {
        JDA jda = jdaRef.getAndSet(null);
        if (jda != null) jda.shutdown();
    }

    public MessageChannel getChatChannel(String channelId) {
        JDA jda = jdaRef.get();
        if (jda == null || channelId == null || channelId.isEmpty()) return null;
        try {
            return jda.getChannelById(MessageChannel.class, channelId);
        } catch (Exception e) {
            plugin.getLogger().warning("Cannot resolve Discord channel: " + e.getMessage());
            return null;
        }
    }
}
