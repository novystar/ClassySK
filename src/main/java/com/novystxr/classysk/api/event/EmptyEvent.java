package com.novystxr.classysk.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EmptyEvent extends Event {

    @Override
    public @NotNull HandlerList getHandlers() {
        throw new IllegalStateException("This should not be called");
    }
}
