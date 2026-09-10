package com.novystxr.classysk.api.classes;

import java.util.*;

import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.fields.SkriptField;
import org.jetbrains.annotations.Nullable;

public class ClassInstance implements FieldHolder {
    public final String name;

    public final Map<String, Object[]> fieldValueMap = new HashMap<>();

    public ClassInstance(String name) {
        this.name = name;
    }

    public SkriptClass getParent() {
        return ClassManager.getClass(name);
    }

    @Override
    public Map<String, Object[]> fieldValueMap() {
        return fieldValueMap;
    }

    @Override
    public void setDefaults() {
        getParent().fields.values().stream()
            .filter(field -> !field.isStatic() && !fieldExists(field.name))
            .forEach(field -> resetField(field.name));
    }

    @Override
    public @Nullable SkriptField getField(String fieldName) {
        return getParent().getField(name);
    }
}
