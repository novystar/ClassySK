package com.novystxr.classysk.api.event;

import com.novystxr.classysk.api.classes.ClassInstance;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MethodRunEvent extends Event {

    public final ClassInstance instance;
    public Object[] returnObject;

    public MethodRunEvent(ClassInstance instance, @Nullable Map<String, Object[]> arguments) {
        this.instance = instance;

    }

    @Override
    public @NotNull HandlerList getHandlers() {
        throw new IllegalArgumentException("This should not be called");
    }
}
