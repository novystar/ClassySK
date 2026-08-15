package com.novystxr.classysk.api;

import org.jetbrains.annotations.NotNull;

public interface AccessModifiable {
    Modifier[] modifiers();

    boolean isPlural();
    Class<?> type();

    default boolean hasModifier(Modifier modifier) {
        return modifiers()[modifier.index] == modifier;
    }

    default boolean isStatic() {
        return hasModifier(Modifier.STATIC);
    }

    default @NotNull Modifier accessType() {
        return modifiers()[0];
    }
}
