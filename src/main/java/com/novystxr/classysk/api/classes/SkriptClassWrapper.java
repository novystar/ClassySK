package com.novystxr.classysk.api.classes;

import org.jetbrains.annotations.Nullable;

/**
 * When getting a class via syntax this should be returned, can be expanded in the future
 */
public record SkriptClassWrapper(SkriptClass skriptClass) {
    public String name() {
        return skriptClass.name();
    }

    public static SkriptClassWrapper of(@Nullable SkriptClass skriptClass) {
        if (skriptClass == null) return null;
        return skriptClass.getWrapper();
    }
}
