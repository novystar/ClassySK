package com.novystxr.classysk.api.event;

import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MethodRegistrationEvent extends Event {
    public final SkriptClass skriptClass;

    public MethodRegistrationEvent(SkriptClass skriptClass) {
        this.skriptClass = skriptClass;

    }

    @Override
    public @NotNull HandlerList getHandlers() {
        throw new IllegalArgumentException("This should not be called");
    }
}
