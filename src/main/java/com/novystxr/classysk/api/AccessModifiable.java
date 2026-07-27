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
        PUBLIC(1),
        PROTECTED(2),
        PRIVATE(3);

        public final int weight;

        AccessType(int weight) {
            this.weight = weight;
        }
    }

    enum Modifier {
        STATIC,
        OVERRIDE
    }
}
