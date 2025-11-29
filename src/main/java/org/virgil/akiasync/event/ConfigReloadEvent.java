package org.virgil.akiasync.event;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
public class ConfigReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public ConfigReloadEvent() {
        super(false);
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
