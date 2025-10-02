package com.signition.arclink;

import net.dv8tion.jda.api.events.session.ReadyEvent; // JDA5: events.session
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // ignore bot/self
        if (event.getAuthor().isBot()) return;
        var ch = event.getChannel();
        String configured = plugin.getChatChannelId();
        if (configured == null || configured.isEmpty()) return;
        if (!ch.getId().equals(configured)) return; // only bridge the configured chat channel
        String content = event.getMessage().getContentDisplay();
        if (content == null || content.isBlank()) return;
        String author = event.getAuthor().getName();
        plugin.broadcastFromDiscord(author, content);
    }
    

    private final ArcLinkPlugin plugin;
    public DiscordListener(ArcLinkPlugin plugin) { this.plugin = plugin; }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        boolean enableSlash = plugin.getConfig().getBoolean("discord.enable_slash_commands", true);
        String guildId = plugin.getConfig().getString("discord.guild_id", "");
        if (!enableSlash) return;

        if (guildId != null && !guildId.isEmpty()) {
            var guild = event.getJDA().getGuildById(guildId);
            if (guild != null) {
                guild.upsertCommand("list", "현재 접속 중인 플레이어 목록").queue();
                guild.upsertCommand("say", "마인크래프트 서버 채팅으로 전송")
                        .addOption(OptionType.STRING, "message", "보낼 메시지", true)
                        .queue();
                plugin.getLogger().info("Registered guild slash commands.");
            } else {
                plugin.getLogger().warning("Guild not found: " + guildId);
            }
        } else {
            event.getJDA().upsertCommand("list", "현재 접속 중인 플레이어 목록").queue();
            event.getJDA().upsertCommand("say", "마인크래프트 서버 채팅으로 전송")
                    .addOption(OptionType.STRING, "message", "보낼 메시지", true)
                    .queue();
            plugin.getLogger().info("Registered global slash commands.");
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "list" -> {
                StringBuilder sb = new StringBuilder();
                int count = Bukkit.getOnlinePlayers().size();
                sb.append("온라인 플레이어 (").append(count).append("명): ");
                boolean first = true;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!first) sb.append(", ");
                    sb.append(p.getName());
                    first = false;
                }
                event.reply(sb.toString()).queue();
            }
            case "say" -> {
                String msg = event.getOption("message").getAsString();
                String author = event.getUser().getName();
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage("§9[디스코드] §f" + author + ": §7" + msg));
                event.reply("전송됨: " + msg).setEphemeral(true).queue();
            }
            default -> event.reply("알 수 없는 명령입니다.").setEphemeral(true).queue();
        }
    }
}
