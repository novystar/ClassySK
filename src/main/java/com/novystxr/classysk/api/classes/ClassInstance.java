package com.novystxr.classysk.api.classes;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.FieldHolder;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jetbrains.annotations.Nullable;

public class ClassInstance implements FieldHolder {
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

    public Map<String, Object[]> getFieldValueMap() {
        Map<String, Object[]> result = new TreeMap<>();
        for (Entry<String, SkriptField> entry : fieldMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().value);
        }
        return result;
    }

    public SkriptClass getParent() {
        return ClassManager.getClass(name);
    }

    public int getHashCode() {
        return Objects.hash(name, getFieldValueMap());
    }

    public static class TypedInstanceWrapper {

        public static final Pattern pattern = Pattern.compile("("+Classysk.CLASSNAME_PATTERN + ") instances?");

        public final ClassInstance instance;

        public TypedInstanceWrapper(ClassInstance instance) {
            this.instance = instance;
        }

    }

}
