package com.novystxr.classysk.api;

import java.util.Arrays;

public interface AccessModifiable {
    Modifier[] modifiers();

    boolean isPlural();
    Class<?> type();

    default boolean hasModifier(Modifier modifier) {
        return modifiers()[modifier.index] == modifier;
    }

    default boolean hasModifiers(Modifier... modifiers) {
        return Arrays.stream(modifiers)
            .allMatch(this::hasModifier);
    }

    default boolean hasAnyModifier(Modifier... modifiers) {
        return Arrays.stream(modifiers)
            .anyMatch(this::hasModifier);
    }

    default boolean isStatic() {
        return hasModifier(Modifier.STATIC);
    }

    default Modifier accessType() {
        return modifiers()[0];
    }
}
