package com.novystxr.classysk.api;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum Modifier {
    PUBLIC(0),
    PRIVATE(0),

    STATIC(1);

    public final int index;

    private static final int MAX_SIZE = 1 + Arrays.stream(values())
        .map(mod -> mod.index)
        .max(Integer::compare).orElseThrow();

    Modifier(int index) {
        this.index = index;
    }

    public static Modifier[] without(Modifier[] modifiers, Modifier... without) {
        List<Modifier> withoutList = Arrays.asList(without);
        return Arrays.stream(modifiers)
            .map(modifier -> withoutList.contains(modifier) ? null : modifier)
            .toArray(Modifier[]::new);
    }

    /**
     * Helper method to create a valid array of modifiers, uniquely ordered respectively to their indexes
     *
     * @param modifiers The modifiers to sort
     * @return The resulting array
     */
    public static Modifier[] collect(Modifier... modifiers) {
        Modifier[] result = new Modifier[MAX_SIZE];
        for (Modifier modifier : modifiers) {
            int i = modifier.index;
            if (result[i] != null)
                throw new IllegalArgumentException("Only 1 modifier per index is allowed");

            result[i] = modifier;
        }
        return result;
    }

    /**
     *
     * Helper method to create a valid array of modifiers, uniquely ordered respectively to their indexes
     *
     * @param tags Tags from syntax parse result - must match the enum name
     * @return The resulting array
     *
     * @see ch.njol.skript.lang.SkriptParser.ParseResult
     * @see Modifier#collect(Modifier...)
     */
    public static Modifier[] collect(List<String> tags) {
        return collect(Arrays.stream(values())
            .filter(value -> tags.contains(value.name().toLowerCase(Locale.ENGLISH)))
            .toArray(Modifier[]::new));
    }

    public Modifier[] array() {
        return collect(this);
    }
}
