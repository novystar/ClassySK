package com.novystxr.classysk.api.fields;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;

public class SkriptField implements AccessModifiable {

    public static SkriptField UNKNOWN = new SkriptField("$unknown", Object.class, Modifier.PUBLIC.array(), true);

    public final String name;
    public final Class<?> type;
    public final Modifier[] modifiers;
    public final boolean isPlural;

    public String origin = null;
    public Expression<?> defaultExpr = null;

    public SkriptField(String name, Class<?> type, Modifier[] modifiers, boolean isPlural) {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.isPlural = isPlural;
    }

    public SkriptClass getOrigin() {
        return origin == null ? null : ClassManager.getClass(origin);
    }

    @Override
    public Modifier[] modifiers() { return modifiers; }
    @Override
    public boolean isPlural() { return isPlural; }
    @Override
    public Class<?> type() { return type; }

}
