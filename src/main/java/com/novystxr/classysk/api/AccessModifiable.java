package com.novystxr.classysk.api;

import com.novystxr.classysk.api.classes.SkriptClass;

public interface AccessModifiable extends ModifierHolder {

    boolean isPlural();
    Class<?> type();

    SkriptClass getOrigin();

    default boolean isStatic() {
        return hasModifier(Modifier.STATIC);
    }

    default Modifier accessType() {
        return modifiers()[0];
    }
}
