package com.signition.arclink;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ArcLinkPlugin extends JavaPlugin implements Listener {

    private DiscordBot bot;
    private String chatChannelId;
    private String guildId;
    private String fmtMcToDiscord;
    private String fmtDiscordToMc;
    private boolean broadcastAll;
    private String requiredPerm;

    // notify config
    private boolean notifyLifecycle;
    private boolean notifyJoinQuit;
    private boolean summaryOnShutdown;
    private String statusChannelId;
    private String fmtStartup;
    private String fmtShutdown;
    private String fmtJoin;
    private String fmtQuit;

    private final AtomicInteger joinCount = new AtomicInteger(0);
    private final AtomicInteger quitCount = new AtomicInteger(0);
    private long bootNano;
    private final AtomicBoolean startupAnnounced = new AtomicBoolean(false);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();
        getServer().getPluginManager().registerEvents(this, this);

        bootNano = System.nanoTime();
        joinCount.set(0);
        quitCount.set(0);

        // Start bot async
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                bot = new DiscordBot(this);
                bot.start(getConfig().getString("discord.token"), guildId);
                getLogger().info("Discord bot startup submitted.");
                if (notifyLifecycle) {
                    scheduleStartupAnnounce();
                }
            } catch (Exception e) {
                getLogger().severe("Failed to start Discord bot: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDisable() {
        if (notifyLifecycle) {
            String serverName = Bukkit.getServer().getName();
            int online = Bukkit.getOnlinePlayers().size();
            String uptime = readableUptime();
            String msg = fmtShutdown
                    .replace("{server}", serverName)
                    .replace("{online}", String.valueOf(online))
                    .replace("{uptime}", uptime)
                    .replace("{joins}", String.valueOf(joinCount.get()))
                    .replace("{quits}", String.valueOf(quitCount.get()));
            sendToDiscordStatus(msg);
            if (summaryOnShutdown && (joinCount.get() > 0 || quitCount.get() > 0)) {
                String summary = "📊 세션 요약 — 접속: " + joinCount + "명, 퇴장: " + quitCount + "명, 가동시간: " + uptime;
                sendToDiscordStatus(summary);
            }
        }
        if (bot != null) {
            try { bot.shutdown(); } catch (Exception ignored) {}
        }
    }

    private void reloadLocalConfig() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        chatChannelId = cfg.getString("discord.chat_channel_id", "");
        guildId = cfg.getString("discord.guild_id", "");
        fmtMcToDiscord = cfg.getString("discord.format.minecraft_to_discord", "[MC] {player}: {message}");
        fmtDiscordToMc = cfg.getString("discord.format.discord_to_minecraft", "§9[디스코드] §f{author}: §7{content}");
        broadcastAll = cfg.getBoolean("minecraft.broadcast_discord_to_all", true);
        requiredPerm = cfg.getString("minecraft.required_permission_to_receive", "");

        // notify section
        notifyLifecycle = cfg.getBoolean("discord.notify.server_lifecycle", true);
        notifyJoinQuit  = cfg.getBoolean("discord.notify.player_join_quit", true);
        summaryOnShutdown = cfg.getBoolean("discord.notify.summary_on_shutdown", true);
        statusChannelId = cfg.getString("discord.notify.status_channel_id", "");
        if (statusChannelId == null || statusChannelId.isEmpty()) statusChannelId = chatChannelId;
        fmtStartup = cfg.getString("discord.notify.startup_message", "🟢 서버가 시작되었습니다: {server} (현재 온라인 {online}명).");
        fmtShutdown = cfg.getString("discord.notify.shutdown_message", "🔴 서버가 곧 종료됩니다: {server}. 가동시간 {uptime}, 접속 {joins}명, 퇴장 {quits}명.");
        fmtJoin = cfg.getString("discord.notify.join_format", "✅ {player} 님이 접속했습니다. 현재 {online}명.");
        fmtQuit = cfg.getString("discord.notify.quit_format", "❌ {player} 님이 퇴장했습니다. 현재 {online}명.");
    }

    // Use BukkitRunnable to avoid capturing a not-yet-initialized task id
    private void scheduleStartupAnnounce() {
        new org.bukkit.scheduler.BukkitRunnable() {
            int tries = 0;
            @Override
            public void run() {
                tries++;
                if (startupAnnounced.get() || tries > 200) {
                    cancel();
                    return;
                }
                if (bot == null || bot.getChatChannel(statusChannelId) == null) return;

                String serverName = Bukkit.getServer().getName();
                int online = Bukkit.getOnlinePlayers().size();
                String msg = fmtStartup
                        .replace("{server}", serverName)
                        .replace("{online}", String.valueOf(online));

                sendToDiscordStatus(msg);
                startupAnnounced.set(true);
                cancel();
            }
        }.runTaskTimer(this, 20L, 10L);
    }

    private String readableUptime() {
        long nanos = System.nanoTime() - bootNano;
        long sec = Duration.ofNanos(nanos).toSeconds();
        long h = sec / 3600; sec %= 3600;
        long m = sec / 60; sec %= 60;
        return String.format("%dh %dm %ds", h, m, sec);
    }

    public String getChatChannelId() {
        return chatChannelId;
    }

    public String getFmtDiscordToMc() {
        return fmtDiscordToMc;
    }

    public boolean shouldBroadcastTo(Player p) {
        if (broadcastAll) return true;
        if (requiredPerm == null || requiredPerm.isEmpty()) return true;
        return p.hasPermission(requiredPerm);
    }

    public void sendToDiscord(String content) {
        DiscordBot b = this.bot;
        if (b == null) return;
        MessageChannel ch = b.getChatChannel(chatChannelId);
        if (ch != null) {
            ch.sendMessage(content).queue();
        }
    }

    public void sendToDiscordStatus(String content) {
        DiscordBot b = this.bot;
        if (b == null) return;
        String target = (statusChannelId == null || statusChannelId.isEmpty()) ? chatChannelId : statusChannelId;
        MessageChannel ch = b.getChatChannel(target);
        if (ch != null) {
            ch.sendMessage(content).queue();
        }
    }

    public void broadcastFromDiscord(String author, String content) {
        final String msg = getFmtDiscordToMc()
                .replace("{author}", author)
                .replace("{content}", content);
        Bukkit.getScheduler().runTask(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (shouldBroadcastTo(p)) {
                    p.sendMessage(msg);
                }
            }
            getLogger().info("[Discord] " + author + ": " + content);
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String out = fmtMcToDiscord
                .replace("{player}", e.getPlayer().getName())
                .replace("{message}", e.getMessage());
        sendToDiscord(out);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!notifyJoinQuit) return;
        int online = Bukkit.getOnlinePlayers().size(); // includes the joining player
        int j = joinCount.incrementAndGet();
        String msg = fmtJoin
                .replace("{player}", e.getPlayer().getName())
                .replace("{online}", String.valueOf(online))
                .replace("{joins}", String.valueOf(j))
                .replace("{quits}", String.valueOf(quitCount.get()));
        sendToDiscordStatus(msg);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!notifyJoinQuit) return;
        int online = Math.max(0, Bukkit.getOnlinePlayers().size() - 1); // minus the quitting player
        int q = quitCount.incrementAndGet();
        String msg = fmtQuit
                .replace("{player}", e.getPlayer().getName())
                .replace("{online}", String.valueOf(online))
                .replace("{joins}", String.valueOf(joinCount.get()))
                .replace("{quits}", String.valueOf(q));
        sendToDiscordStatus(msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("discordreload")) {
            if (!sender.hasPermission("arclink.admin")) {
                sender.sendMessage("§c권한이 없습니다.");
                return true;
            }
            reloadLocalConfig();
            sender.sendMessage("§aArcLink 설정을 다시 읽었습니다.");
            return true;
        }
        if (cmd.equals("discord")) {
            if (args.length == 0) {
                sender.sendMessage("§7/discord say <메시지> | /discord reload | /discord stats");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                return onCommand(sender, Bukkit.getPluginCommand("discordreload"), "discordreload", new String[0]);
            }
            if (args[0].equalsIgnoreCase("say")) {
                if (!sender.hasPermission("arclink.use")) {
                    sender.sendMessage("§c권한이 없습니다.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /discord say <메시지>");
                    return true;
                }
                String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                sendToDiscord("[서버] " + sender.getName() + ": " + msg);
                sender.sendMessage("§a전송됨.");
                return true;
            }
            if (args[0].equalsIgnoreCase("stats")) {
                String uptime = readableUptime();
                String stats = String.format("§a접속: %d명, 퇴장: %d명, 가동시간: %s",
                        joinCount.get(), quitCount.get(), uptime);
                sender.sendMessage(stats);
                return true;
            }
            sender.sendMessage("§7알 수 없는 하위 명령입니다.");
            return true;
        }
        return false;
    }
}
