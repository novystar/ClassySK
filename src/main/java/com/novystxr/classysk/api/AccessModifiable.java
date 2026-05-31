package com.novystxr.classysk.api;

import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.event.MethodRunEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public interface AccessModifiable {
    boolean checkAccess(@Nullable AbstractSkriptClass contextClass);
    boolean checkContext(boolean isStatic);

    default boolean isAccessible(ParserInstance parser, boolean isStatic) {
        if (!checkContext(isStatic)) return false;
        ParserClassData data = parser.getData(ParserClassData.class);
        return (checkAccess(data.skriptClass));
    }

    default boolean isAccessible(Event event, boolean isStatic) {
        if (!checkContext(isStatic)) return false;

        AbstractSkriptClass contextClass = null;
        if (event instanceof MethodRunEvent runEvent) {
            contextClass = runEvent.instance.getParent();
        }
        return (checkAccess(contextClass));
    }

    enum AccessType {
        PUBLIC,
        PRIVATE
    }
}
