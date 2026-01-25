package org.virgil.akiasync.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;
import org.virgil.akiasync.language.LanguageManager;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@NullMarked
public class PacketStatsCommand implements BasicCommand {

    private final AkiAsyncPlugin plugin;

    public PacketStatsCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    private AkiAsyncBridge getBridge() {
        return plugin.getBridge();
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "start" -> startTracking(source);
            case "stop" -> stopTracking(source);
            case "report" -> showReport(source, args);
            case "reset" -> resetStats(source);
            default -> sendUsage(source);
        }
    }

    private void startTracking(CommandSourceStack source) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(lang().prefixed("command.packets.bridge-not-initialized"));
            return;
        }

        if (bridge.isPacketStatisticsEnabled()) {
            source.getSender().sendMessage(lang().prefixed("command.packets.already-running"));
            return;
        }

        bridge.setPacketStatisticsEnabled(true);
        source.getSender().sendMessage(lang().prefixed("command.packets.started"));
    }

    private void stopTracking(CommandSourceStack source) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(lang().prefixed("command.packets.bridge-not-initialized"));
            return;
        }

        if (!bridge.isPacketStatisticsEnabled()) {
            source.getSender().sendMessage(lang().prefixed("command.packets.not-running"));
            return;
        }

        bridge.setPacketStatisticsEnabled(false);
        source.getSender().sendMessage(lang().prefixed("command.packets.stopped"));
    }

    private void showReport(CommandSourceStack source, String[] args) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(lang().prefixed("command.packets.bridge-not-initialized"));
            return;
        }

        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(50, limit));
            } catch (NumberFormatException ignored) {
            }
        }

        long elapsed = bridge.getPacketStatisticsElapsedSeconds();

        source.getSender().sendMessage(Component.empty());
        source.getSender().sendMessage(lang().get("command.packets.report.header"));
        
        Component statusComponent = bridge.isPacketStatisticsEnabled()
            ? lang().get("command.packets.report.status-running")
            : lang().get("command.packets.report.status-stopped");
        source.getSender().sendMessage(
            lang().get("command.packets.report.duration", "seconds", String.valueOf(elapsed))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(statusComponent)
        );

        long totalOut = bridge.getTotalOutgoingPacketCount();
        long totalIn = bridge.getTotalIncomingPacketCount();
        source.getSender().sendMessage(
            lang().get("command.packets.report.total", 
                "out", formatNumber(totalOut),
                "in", formatNumber(totalIn))
        );

        source.getSender().sendMessage(Component.empty());
        source.getSender().sendMessage(lang().get("command.packets.report.outgoing-title", "limit", String.valueOf(limit)));

        List<Object[]> topOut = bridge.getTopOutgoingPackets(limit);
        if (topOut.isEmpty()) {
            source.getSender().sendMessage(lang().get("command.packets.report.no-data"));
        } else {
            int rank = 1;
            for (Object[] stat : topOut) {
                sendPacketLine(source, rank++, stat, totalOut);
            }
        }

        source.getSender().sendMessage(Component.empty());
        source.getSender().sendMessage(lang().get("command.packets.report.incoming-title", "limit", String.valueOf(limit)));

        List<Object[]> topIn = bridge.getTopIncomingPackets(limit);
        if (topIn.isEmpty()) {
            source.getSender().sendMessage(lang().get("command.packets.report.no-data"));
        } else {
            int rank = 1;
            for (Object[] stat : topIn) {
                sendPacketLine(source, rank++, stat, totalIn);
            }
        }

        source.getSender().sendMessage(Component.empty());
    }

    private void sendPacketLine(CommandSourceStack source, int rank, Object[] stat, long total) {
        String name = (String) stat[0];
        long count = (Long) stat[1];
        long countPerSecond = (Long) stat[3];

        double percent = total > 0 ? (count * 100.0 / total) : 0;
        String percentColor = percent > 50 ? "<red>" : percent > 20 ? "<yellow>" : "<white>";
        String percentStr = percentColor + String.format("%.1f%%", percent) + "</" + (percent > 50 ? "red" : percent > 20 ? "yellow" : "white") + ">";

        source.getSender().sendMessage(lang().get("command.packets.report.packet-name", 
            "rank", String.valueOf(rank),
            "name", name));
        source.getSender().sendMessage(lang().get("command.packets.report.packet-stats",
            "count", formatNumber(count),
            "percent", percentStr,
            "rate", formatNumber(countPerSecond)));
    }

    private void resetStats(CommandSourceStack source) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(lang().prefixed("command.packets.bridge-not-initialized"));
            return;
        }

        bridge.resetPacketStatistics();
        source.getSender().sendMessage(lang().prefixed("command.packets.reset"));
    }

    private void sendUsage(CommandSourceStack source) {
        source.getSender().sendMessage(lang().prefixed("command.packets.usage.title"));
        source.getSender().sendMessage(lang().get("command.packets.usage.start"));
        source.getSender().sendMessage(lang().get("command.packets.usage.stop"));
        source.getSender().sendMessage(lang().get("command.packets.usage.report"));
        source.getSender().sendMessage(lang().get("command.packets.usage.reset"));
    }

    private String formatNumber(long num) {
        if (num < 1000) return String.valueOf(num);
        if (num < 1000000) return String.format("%.1fK", num / 1000.0);
        if (num < 1000000000) return String.format("%.1fM", num / 1000000.0);
        return String.format("%.1fB", num / 1000000000.0);
    }

    @Override
    public @Nullable String permission() {
        return "akiasync.packets";
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            return List.of("start", "stop", "report", "reset");
        }
        return List.of();
    }
}
