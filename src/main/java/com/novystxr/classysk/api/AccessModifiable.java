package com.novystxr.classysk.api;

public interface AccessModifiable {
    boolean isStatic();
    boolean isPlural();
    AccessType accessType();
    Class<?> type();

    enum AccessType {
        PUBLIC,
        PRIVATE,
        PROTECTED
    }
}
