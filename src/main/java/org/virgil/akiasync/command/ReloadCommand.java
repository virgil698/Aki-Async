package org.virgil.akiasync.command;
import org.bukkit.Bukkit;
import org.virgil.akiasync.event.ConfigReloadEvent;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
@NullMarked
public class ReloadCommand implements BasicCommand {
    @Override
    public void execute(CommandSourceStack source, String[] args) {
        Bukkit.getPluginManager().callEvent(new ConfigReloadEvent());
        source.getSender().sendMessage("搂a[AkiAsync] Configuration hot-reloaded, thread pools smoothly restarted.");
    }
    @Override
    public @Nullable String permission() {
        return "akiasync.reload";
    }
}