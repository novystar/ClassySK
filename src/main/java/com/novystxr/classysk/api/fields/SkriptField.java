package com.novystxr.classysk.api.fields;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import org.jetbrains.annotations.Nullable;

public class SkriptField implements AccessModifiable {

    public static SkriptField UNKNOWN = new SkriptField("$unknown", Object.class, Modifier.PUBLIC.array(), true, null);

    public final String name;
    public final Class<?> type;
    public final Modifier[] modifiers;
    public final boolean isPlural;
    public final Expression<?> defaultValue;

    public String origin = null;

    public SkriptField(String name, Class<?> type, Modifier[] modifiers, boolean isPlural, @Nullable Expression<?> defaultValue) {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.isPlural = isPlural;
        this.defaultValue = defaultValue;
    }

    @Override
    public Modifier[] modifiers() { return modifiers; }
    @Override
    public boolean isPlural() { return isPlural; }
    @Override
    public Class<?> type() { return type; }

}
