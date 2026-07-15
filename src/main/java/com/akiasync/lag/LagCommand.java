package com.akiasync.lag;

import com.akiasync.AkiAsyncBridge;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LagCommand implements BasicCommand {
    private static final String PERMISSION = "akiasync.lag";
    private static final List<String> ROOT_SUGGESTIONS = List.of(
            "status", "report", "inspect", "top", "ranges", "categories", "arm"
    );
    private final LagProfilerService service;
    private final AkiAsyncBridge bridge;

    public LagCommand(LagProfilerService service, AkiAsyncBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length < 2 || !args[0].equalsIgnoreCase("lag")) {
            sendHelp(sender);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "status" -> sendStatus(sender);
            case "report" -> sendReport(sender, service.latestSlow().or(service::latest));
            case "inspect" -> inspect(sender, args);
            case "top" -> sendTop(sender, args);
            case "ranges" -> sendRanges(sender);
            case "categories" -> sendCategories(sender);
            case "arm" -> arm(sender, args);
            default -> sendHelp(sender);
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return "lag".startsWith(prefix) ? List.of("lag") : List.of();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("lag")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return ROOT_SUGGESTIONS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("lag") && args[1].equalsIgnoreCase("inspect")) {
            return service.recentSlow(10).stream().map(snapshot -> Long.toString(snapshot.tickId())).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("lag") && args[1].equalsIgnoreCase("arm")) {
            return List.of("200", "600", "1200");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("lag") && args[1].equalsIgnoreCase("top")) {
            return List.of("5", "10", "20");
        }
        return List.of();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(PERMISSION);
    }

    @Override
    public String permission() {
        return PERMISSION;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(Component.text("Aki-Async 卡顿探针运行中", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(
                "已保留 " + service.size() + "/600 ticks，详细追踪剩余 "
                        + bridge.detailedTicksRemaining() + " ticks",
                NamedTextColor.GRAY
        ));
        service.latest().ifPresent(snapshot -> sender.sendMessage(Component.text(
                "最近 tick #" + snapshot.tickId() + "：" + millis(snapshot.wallNanos()) + " ms ["
                        + LagSeverity.fromNanos(snapshot.wallNanos()).displayName() + "]",
                severityColor(LagSeverity.fromNanos(snapshot.wallNanos()))
        )));
    }

    private void sendTop(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length >= 3) {
            try {
                limit = Math.max(1, Math.min(20, Integer.parseInt(args[2])));
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("数量必须是整数。", NamedTextColor.RED));
                return;
            }
        }

        List<LagTickRecord> ticks = service.topSlow(limit);
        if (ticks.isEmpty()) {
            sender.sendMessage(Component.text("当前历史窗口内没有慢 tick。", NamedTextColor.GREEN));
            return;
        }
        sender.sendMessage(Component.text("Aki-Async 慢 tick 排行", NamedTextColor.AQUA));
        for (int index = 0; index < ticks.size(); index++) {
            LagTickRecord tick = ticks.get(index);
            LagSeverity severity = LagSeverity.fromNanos(tick.wallNanos());
            sender.sendMessage(Component.text(
                    "#" + (index + 1) + " tick " + tick.tickId() + "  " + millis(tick.wallNanos()) + " ms  ["
                            + severity.displayName() + "]  " + primaryCause(tick),
                    severityColor(severity)
            ));
        }
    }

    private void sendRanges(CommandSender sender) {
        sender.sendMessage(Component.text("Aki-Async 卡顿等级范围", NamedTextColor.AQUA));
        for (LagSeverity severity : LagSeverity.values()) {
            String range;
            if (severity.minimumMillis() == 0) {
                range = "< " + severity.maximumMillis() + " ms";
            } else if (severity.maximumMillis() == Long.MAX_VALUE) {
                range = ">= " + severity.minimumMillis() + " ms";
            } else {
                range = ">= " + severity.minimumMillis() + " 且 < " + severity.maximumMillis() + " ms";
            }
            sender.sendMessage(Component.text(severity.displayName() + "：" + range, severityColor(severity)));
        }
    }

    private void sendCategories(CommandSender sender) {
        sender.sendMessage(Component.text("Aki-Async 卡顿来源分类", NamedTextColor.AQUA));
        String categories = String.join("、", java.util.Arrays.stream(LagCauseCategory.values())
                .filter(category -> category != LagCauseCategory.UNKNOWN)
                .map(LagCauseCategory::displayName)
                .toList());
        sender.sendMessage(Component.text(categories, NamedTextColor.GRAY));
    }

    private void inspect(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("用法：/akiasync lag inspect <tickId>", NamedTextColor.YELLOW));
            return;
        }
        try {
            sendReport(sender, service.find(Long.parseLong(args[2])));
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("tickId 必须是整数。", NamedTextColor.RED));
        }
    }

    private void arm(CommandSender sender, String[] args) {
        int ticks = 200;
        if (args.length >= 3) {
            try {
                ticks = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("ticks 必须是整数。", NamedTextColor.RED));
                return;
            }
        }
        ticks = Math.max(1, Math.min(12_000, ticks));
        bridge.requestDetailedTicks(ticks);
        sender.sendMessage(Component.text("已开启 " + ticks + " ticks 的实体级详细追踪。", NamedTextColor.GREEN));
    }

    private void sendReport(CommandSender sender, Optional<LagTickRecord> optional) {
        if (optional.isEmpty()) {
            sender.sendMessage(Component.text("暂时还没有 tick 数据。", NamedTextColor.YELLOW));
            return;
        }

        LagTickRecord tick = optional.get();
        LagSeverity severity = LagSeverity.fromNanos(tick.wallNanos());
        sender.sendMessage(Component.text(
                "Aki-Async tick #" + tick.tickId() + " — " + millis(tick.wallNanos()) + " ms"
                        + " [" + severity.displayName() + "]" + (tick.detailed() ? " [详细]" : ""),
                severityColor(severity)
        ));
        if (tick.cpuNanos() >= 0) {
            sender.sendMessage(Component.text(
                    "主线程 CPU " + millis(tick.cpuNanos()) + " ms；下列耗时为包含式，不能直接相加。",
                    NamedTextColor.DARK_GRAY
            ));
        }
        LagSystemRecord system = tick.system();
        double heapPercent = system.heapMaxBytes() > 0
                ? system.heapUsedBytes() * 100.0 / system.heapMaxBytes()
                : -1;
        sender.sendMessage(Component.text(
                "堆内存 " + formatBytes(system.heapUsedBytes())
                        + (heapPercent >= 0 ? String.format(Locale.ROOT, " / %.1f%%", heapPercent) : "")
                        + "，tick 增量 " + signedBytes(system.heapDeltaBytes())
                        + "，GC " + system.gcCollections() + " 次 / " + millis(system.gcPauseNanos()) + " ms",
                system.gcPauseNanos() >= 10_000_000L || heapPercent >= 90
                        ? NamedTextColor.RED
                        : NamedTextColor.DARK_GRAY
        ));

        if (tick.sources().isEmpty() && tick.stackSamples().isEmpty()) {
            sender.sendMessage(Component.text("本 tick 没有超过记录阈值的已知边界。", NamedTextColor.GRAY));
            return;
        }
        tick.sources().stream().limit(12).forEach(source -> sender.sendMessage(formatSource(source, tick.wallNanos())));
        if (!tick.stackSamples().isEmpty()) {
            sender.sendMessage(Component.text("卡顿来源栈（2ms 采样，按命中次数排序）", NamedTextColor.YELLOW));
            tick.stackSamples().stream().limit(3).forEach(stack -> sendStack(sender, stack));
        }
    }

    private void sendStack(CommandSender sender, LagStackRecord stack) {
        sender.sendMessage(Component.text(
                "[" + stack.category().displayName() + "] " + stack.samples() + " samples  " + stack.source(),
                NamedTextColor.GOLD
        ));
        stack.frames().stream().limit(6).forEach(frame -> sender.sendMessage(
                Component.text("  at " + frame, NamedTextColor.DARK_GRAY)
        ));
    }

    private Component formatSource(LagSourceRecord source, long tickNanos) {
        double ratio = tickNanos <= 0 ? 0 : Math.min(1.0, (double) source.totalNanos() / tickNanos);
        int filled = (int) Math.round(ratio * 12);
        String bar = "█".repeat(filled) + "░".repeat(12 - filled);
        String owner = source.owner().isBlank() ? "" : source.owner() + " / ";
        String world = source.world().isBlank() ? "" : " @ " + source.world();
        String location = source.hasLocation()
                ? " [" + source.x() + "," + source.y() + "," + source.z() + "]"
                : "";
        String line = String.format(
                Locale.ROOT,
                "%s %6.2f ms  %s%s%s%s ×%d (max %.2f ms)",
                bar,
                source.totalNanos() / 1_000_000.0,
                owner,
                source.category().displayName() + "/" + source.type() + ":" + source.detail(),
                world,
                location,
                source.count(),
                source.maxNanos() / 1_000_000.0
        );
        return Component.text(line, source.totalNanos() >= 10_000_000L ? NamedTextColor.RED : NamedTextColor.GRAY);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/akiasync lag status", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/akiasync lag report", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/akiasync lag inspect <tickId>", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/akiasync lag top [count]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/akiasync lag ranges", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/akiasync lag categories", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/akiasync lag arm [ticks]", NamedTextColor.AQUA));
    }

    private static String primaryCause(LagTickRecord tick) {
        LagSystemRecord system = tick.system();
        if (system.gcPauseNanos() >= 5_000_000L
                && system.gcPauseNanos() * 5 >= tick.wallNanos()) {
            return LagCauseCategory.GC.displayName() + " / " + millis(system.gcPauseNanos()) + " ms";
        }
        if (system.heapMaxBytes() > 0 && system.heapUsedBytes() * 100L / system.heapMaxBytes() >= 90) {
            return LagCauseCategory.MEMORY.displayName() + " / heap >= 90%";
        }
        if (system.heapDeltaBytes() >= 64L * 1024 * 1024) {
            return LagCauseCategory.MEMORY.displayName() + " / +" + formatBytes(system.heapDeltaBytes());
        }
        for (LagStackRecord stack : tick.stackSamples()) {
            if (stack.category() != LagCauseCategory.UNKNOWN) {
                return stack.category().displayName() + " / " + stack.source();
            }
        }
        for (LagSourceRecord source : tick.sources()) {
            if (source.category() != LagCauseCategory.UNKNOWN) {
                return source.category().displayName() + " / " + source.detail();
            }
        }
        if (tick.cpuNanos() >= 0 && tick.cpuNanos() * 100L / Math.max(1, tick.wallNanos()) >= 80) {
            return LagCauseCategory.CPU.displayName() + " / main thread";
        }
        if (!tick.stackSamples().isEmpty()) {
            return LagCauseCategory.UNKNOWN.displayName() + " / " + tick.stackSamples().getFirst().source();
        }
        if (!tick.sources().isEmpty()) {
            return LagCauseCategory.UNKNOWN.displayName() + " / " + tick.sources().getFirst().detail();
        }
        return LagCauseCategory.UNKNOWN.displayName();
    }

    private static NamedTextColor severityColor(LagSeverity severity) {
        return switch (severity) {
            case NORMAL -> NamedTextColor.GREEN;
            case MINOR -> NamedTextColor.YELLOW;
            case MODERATE -> NamedTextColor.GOLD;
            case SEVERE -> NamedTextColor.RED;
            case CRITICAL -> NamedTextColor.DARK_RED;
        };
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }

    private static String formatBytes(long bytes) {
        double absolute = Math.abs((double) bytes);
        if (absolute >= 1024 * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f GiB", bytes / (1024.0 * 1024 * 1024));
        }
        return String.format(Locale.ROOT, "%.2f MiB", bytes / (1024.0 * 1024));
    }

    private static String signedBytes(long bytes) {
        return (bytes >= 0 ? "+" : "") + formatBytes(bytes);
    }
}
