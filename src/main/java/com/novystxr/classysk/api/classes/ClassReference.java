package com.novystxr.classysk.api.classes;

import org.jetbrains.annotations.Nullable;

/**
 * When getting a class via syntax this should be returned, can be expanded in the future
 */
public record ClassReference(String name) {
    public boolean isValid() {
        return ClassManager.classExists(name);
    }

    public SkriptClass referent() {
        return ClassManager.getClass(name);
    }

    public static ClassReference of(@Nullable SkriptClass skriptClass) {
        if (skriptClass == null) return null;
        return new ClassReference(skriptClass.name);
    }
}
