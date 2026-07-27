package com.novystxr.classysk.api;

public interface AccessModifiable {
    Modifier modifier();
    AccessType accessType();
    Class<?> type();
    boolean isPlural();

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
