package com.novystxr.classysk.api.fields;

import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.DefaultValue;
import org.jetbrains.annotations.Nullable;

public class SkriptField implements AccessModifiable {

    public final String name;
    public final Class<?> type;
    public final Modifier[] modifiers;
    public final boolean isPlural;
    public final DefaultValue<?> defaultValue;
    public String origin;

    public SkriptField(String name, Class<?> type, Modifier[] modifiers, boolean isPlural, @Nullable DefaultValue<?> defaultValue) {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.isPlural = isPlural;
        this.defaultValue = defaultValue;
    }

    @Override
    public SkriptClass getOrigin() {
        return ClassManager.getClass(origin);
    }

    @Override
    public Modifier[] modifiers() { return modifiers; }
    @Override
    public boolean isPlural() { return isPlural; }
    @Override
    public Class<?> type() { return type; }

}
