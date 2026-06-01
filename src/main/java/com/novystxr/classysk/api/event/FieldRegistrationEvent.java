package com.novystxr.classysk.api.event;

import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FieldRegistrationEvent extends Event {

    public final AbstractSkriptClass skriptClass;

    public FieldRegistrationEvent(AbstractSkriptClass skriptClass) {
        this.skriptClass = skriptClass;

    }

    @Override
    public @NotNull HandlerList getHandlers() {
        throw new IllegalArgumentException("This should not be called");
    }
}
