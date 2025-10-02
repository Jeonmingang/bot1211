package com.signition.legendary;

import com.signition.legendaryshade.net.dv8tion.jda.api.EmbedBuilder;
import com.signition.legendaryshade.net.dv8tion.jda.api.JDA;
import com.signition.legendaryshade.net.dv8tion.jda.api.JDABuilder;
import com.signition.legendaryshade.net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import com.signition.legendaryshade.net.dv8tion.jda.api.requests.GatewayIntent;
import com.signition.legendaryshade.net.dv8tion.jda.api.utils.cache.MemberCachePolicy;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class DiscordClient {
    private final Plugin plugin;
    private final String channelId;
    private final boolean mentionEveryone;
    private final String mentionRoleId;
    private JDA jda;

    public DiscordClient(Plugin plugin, String token, String channelId, boolean mentionEveryone, String mentionRoleId) throws Exception {
        this.plugin = plugin;
        this.channelId = channelId;
        this.mentionEveryone = mentionEveryone;
        this.mentionRoleId = mentionRoleId == null ? "" : mentionRoleId;

        this.jda = JDABuilder.create(token,
                        EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT))
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .build();
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public void sendLegendary(String title, String description) {
        if (jda == null) return;
        MessageChannel ch = jda.getChannelById(MessageChannel.class, channelId);
        if (ch == null) {
            plugin.getLogger().warning("Discord 채널을 찾을 수 없습니다: " + channelId);
            return;
        }

        StringBuilder content = new StringBuilder();
        if (mentionEveryone) content.append("@everyone ");
        if (!mentionRoleId.isEmpty()) content.append("<@&").append(mentionRoleId).append("> ");
        // Send an embed for clarity
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setDescription(description);
        eb.setColor(new Color(0x00D1B2));
        eb.setTimestamp(Instant.now());

        ch.sendMessage(content.toString().trim()).queue(msg -> {
            ch.sendMessageEmbeds(eb.build()).queue();
        });
    }
}
