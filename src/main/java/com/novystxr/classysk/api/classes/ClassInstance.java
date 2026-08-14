package com.novystxr.classysk.api.classes;

import java.util.*;

import com.novystxr.classysk.api.FieldHolder;
import com.novystxr.classysk.api.TypeWrappable;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public class ClassInstance implements FieldHolder, TypeWrappable<TypedInstanceWrapper, ClassInstance> {
    public final String name;

    public final Map<String, SkriptField> fieldMap = new HashMap<>();

    // set on deserialization for when parent class becomes known
    final Map<String, SerializableField> awaitingFields = new HashMap<>();

    public ClassInstance(String name) {
        this.name = name;
    }

    public void putAwaitingField(String name, SerializableField field) {
        awaitingFields.put(name, field);
    }

    @Override
    public Map<String, SkriptField> fieldMap() {
        return fieldMap;
    }

    // gets existing signature and falls back to parent
    @Override
    public @Nullable FieldSignature getFieldSignature(String name) {
        SkriptField field = fieldMap.get(name);
        if (field == null) {
            SkriptClass parent = getParent();
            if (parent == null) return null;
            return parent.getFieldSignature(name);
        }
        return field.signature;
    }

    public SkriptClass getParent() {
        return ClassManager.getClass(name);
    }

    TypedInstanceWrapper wrapper = null;

    @Override
    public TypedInstanceWrapper wrap() {
        return wrapper != null ? wrapper : Converters.convert(this, getSubclass());
    }

    @Override
    public ClassInstance unwrap() {
        return this;
    }

    @Override
    public Class<? extends TypedInstanceWrapper> getSubclass() {
        return ClassManager.getSubclass(name);
    }
}
