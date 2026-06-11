package com.novystxr.classysk.api;

import ch.njol.skript.lang.SectionSkriptEvent;
import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public interface AccessModifiable {
    boolean checkAccess(@Nullable SkriptClass contextClass);
    boolean checkContext(boolean isStatic);

    default boolean isAccessible(ParserInstance parser, boolean isStatic) {
        if (!checkContext(isStatic)) return false;

        if (parser.getCurrentStructure() instanceof SectionSkriptEvent secSkriptEvent) {
            if (secSkriptEvent.getSection() instanceof SecMethod secMethod) {
                return checkAccess(secMethod.contextClass);
            }
        }
        return checkAccess(null);
    }

    default boolean isAccessible(Event event, boolean isStatic) {
        if (!checkContext(isStatic)) return false;

        if (event instanceof MethodRunEvent runEvent) {
            return checkAccess(runEvent.instance.getParent());
        }
        return checkAccess(null);
    }

    enum AccessType {
        PUBLIC,
        PRIVATE
    }
}
