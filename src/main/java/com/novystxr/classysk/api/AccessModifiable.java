package com.novystxr.classysk.api;

public interface AccessModifiable extends ModifierHolder {

    boolean isPlural();
    Class<?> type();

    default boolean isStatic() {
        return hasModifier(Modifier.STATIC);
    }

    default Modifier accessType() {
        return modifiers()[0];
    }
}
