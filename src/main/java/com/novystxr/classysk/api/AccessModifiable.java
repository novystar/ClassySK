package com.novystxr.classysk.api;

public interface AccessModifiable {
    Modifier modifier();
    AccessType accessType();
    Class<?> type();
    boolean isPlural();

    default boolean isStatic() {
        return modifier() == Modifier.STATIC;
    }

    enum AccessType {
        PUBLIC,
        PRIVATE,
        PROTECTED
    }

    enum Modifier {
        STATIC,
        OVERRIDE
    }
}
