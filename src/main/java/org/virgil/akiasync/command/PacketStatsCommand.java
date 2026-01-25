package org.virgil.akiasync.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.bridge.AkiAsyncBridge;

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
            source.getSender().sendMessage(
                Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                    .append(Component.text("Bridge 未初始化", NamedTextColor.RED))
            );
            return;
        }

        if (bridge.isPacketStatisticsEnabled()) {
            source.getSender().sendMessage(
                Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                    .append(Component.text("发包统计已在运行中", NamedTextColor.YELLOW))
            );
            return;
        }

        bridge.setPacketStatisticsEnabled(true);
        source.getSender().sendMessage(
            Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                .append(Component.text("发包统计已启动，使用 ", NamedTextColor.GREEN))
                .append(Component.text("/aki-packets report", NamedTextColor.AQUA))
                .append(Component.text(" 查看报告", NamedTextColor.GREEN))
        );
    }

    private void stopTracking(CommandSourceStack source) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(
                Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                    .append(Component.text("Bridge 未初始化", NamedTextColor.RED))
            );
            return;
        }

        if (!bridge.isPacketStatisticsEnabled()) {
            source.getSender().sendMessage(
                Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                    .append(Component.text("发包统计未在运行", NamedTextColor.YELLOW))
            );
            return;
        }

        bridge.setPacketStatisticsEnabled(false);
        source.getSender().sendMessage(
            Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                .append(Component.text("发包统计已停止", NamedTextColor.GREEN))
        );
    }

    private void showReport(CommandSourceStack source, String[] args) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(
                Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                    .append(Component.text("Bridge 未初始化", NamedTextColor.RED))
            );
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
        source.getSender().sendMessage(
            Component.text("═══════ ", NamedTextColor.GOLD)
                .append(Component.text("发包统计报告", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" ═══════", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("统计时长: ", NamedTextColor.GRAY)
                .append(Component.text(elapsed + " 秒", NamedTextColor.WHITE))
                .append(Component.text(" | 状态: ", NamedTextColor.GRAY))
                .append(bridge.isPacketStatisticsEnabled()
                    ? Component.text("运行中", NamedTextColor.GREEN)
                    : Component.text("已停止", NamedTextColor.RED))
        );

        long totalOut = bridge.getTotalOutgoingPacketCount();
        long totalIn = bridge.getTotalIncomingPacketCount();
        source.getSender().sendMessage(
            Component.text("总计: ", NamedTextColor.GRAY)
                .append(Component.text("↑ " + formatNumber(totalOut), NamedTextColor.RED))
                .append(Component.text(" 发送 | ", NamedTextColor.GRAY))
                .append(Component.text("↓ " + formatNumber(totalIn), NamedTextColor.GREEN))
                .append(Component.text(" 接收", NamedTextColor.GRAY))
        );

        source.getSender().sendMessage(Component.empty());
        source.getSender().sendMessage(
            Component.text("▼ 发送包 TOP " + limit, NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(" (可能导致客户端卡顿)", NamedTextColor.GRAY))
        );

        List<Object[]> topOut = bridge.getTopOutgoingPackets(limit);
        if (topOut.isEmpty()) {
            source.getSender().sendMessage(Component.text("  暂无数据", NamedTextColor.GRAY));
        } else {
            int rank = 1;
            for (Object[] stat : topOut) {
                sendPacketLine(source, rank++, stat, totalOut);
            }
        }

        source.getSender().sendMessage(Component.empty());
        source.getSender().sendMessage(
            Component.text("▼ 接收包 TOP " + limit, NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(" (可能导致服务端卡顿)", NamedTextColor.GRAY))
        );

        List<Object[]> topIn = bridge.getTopIncomingPackets(limit);
        if (topIn.isEmpty()) {
            source.getSender().sendMessage(Component.text("  暂无数据", NamedTextColor.GRAY));
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
        NamedTextColor percentColor = percent > 50 ? NamedTextColor.RED
                                    : percent > 20 ? NamedTextColor.YELLOW
                                    : NamedTextColor.WHITE;

        source.getSender().sendMessage(
            Component.text("  " + rank + ". ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.AQUA))
        );
        source.getSender().sendMessage(
            Component.text("     ", NamedTextColor.GRAY)
                .append(Component.text(formatNumber(count), NamedTextColor.WHITE))
                .append(Component.text(" 次 (", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f%%", percent), percentColor))
                .append(Component.text(") | ", NamedTextColor.GRAY))
                .append(Component.text(formatNumber(countPerSecond) + "/s", NamedTextColor.YELLOW))
        );
    }

    private void resetStats(CommandSourceStack source) {
        AkiAsyncBridge bridge = getBridge();
        if (bridge == null) {
            source.getSender().sendMessage(
                Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                    .append(Component.text("Bridge 未初始化", NamedTextColor.RED))
            );
            return;
        }

        bridge.resetPacketStatistics();
        source.getSender().sendMessage(
            Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                .append(Component.text("发包统计已重置", NamedTextColor.GREEN))
        );
    }

    private void sendUsage(CommandSourceStack source) {
        source.getSender().sendMessage(
            Component.text("[AkiAsync] ", NamedTextColor.GOLD)
                .append(Component.text("发包检查器用法:", NamedTextColor.YELLOW))
        );
        source.getSender().sendMessage(Component.text("  /aki-packets start", NamedTextColor.AQUA)
            .append(Component.text(" - 开始统计", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("  /aki-packets stop", NamedTextColor.AQUA)
            .append(Component.text(" - 停止统计", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("  /aki-packets report [数量]", NamedTextColor.AQUA)
            .append(Component.text(" - 查看报告", NamedTextColor.GRAY)));
        source.getSender().sendMessage(Component.text("  /aki-packets reset", NamedTextColor.AQUA)
            .append(Component.text(" - 重置统计", NamedTextColor.GRAY)));
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
