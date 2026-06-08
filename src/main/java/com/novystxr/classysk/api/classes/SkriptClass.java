package com.novystxr.classysk.api.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.methods.SkriptMethod;
import org.jetbrains.annotations.Nullable;

public class SkriptClass {
    public final String name;
    private final Map<String, SkriptField> fieldMap = new HashMap<>();

    // set on deserialization for when parent class becomes known
    final Map<String, Object[]> awaitingFields = new HashMap<>();

    public void putAwaitingField(String name, Object[] value) {
        awaitingFields.put(name, value);
    }

    public SkriptClass(String name) {
        this.name = name;
    }

    public void createField(SkriptField field) {
        if (fieldExists(field.signature.name())) return;

        fieldMap.put(field.signature.name(), field);

    }

    public void removeField(String name) {
        fieldMap.remove(name);
    }

    public Object[] getFieldValue(String name) {
        SkriptField field = fieldMap.get(name);
        AbstractSkriptClass parent = getParent();

        Object[] value = null;

        if (field != null) {
            value = field.getValue();
        } else if (parent.hasFieldSignature(name)) {
            value = parent.getFieldSignature(name).defaultValue();
        }

        return value;
    }

    // if field does not yet exist, instantiates one
    public @Nullable SkriptField getField(String name) {
        SkriptField field = fieldMap.get(name);
        if (field != null) return field;

        AbstractSkriptClass parent = getParent();
        if (parent == null) return null;

        SkriptField.FieldSignature signature = parent.getFieldSignature(name);
        field = new SkriptField(signature);
        createField(field);

        return field;
    }

    public @Nullable SkriptMethod getAccessibleMethod(String name) {
        AbstractSkriptClass parent = getParent();

        if (parent == null) return null;
        if (!parent.hasMethod(name)) return null;

        SkriptMethod method = parent.getMethod(name);
        if (method.signature.isStatic() == isInstance()) return null;

        return method;
    }

    public String getEffectiveName() {
        return "%"+name+"%";
    }

    protected Map<String, SkriptField> getFieldMap() {
        return fieldMap;
    }

    public Map<String, Object[]> getFieldValueMap() {
        Map<String, Object[]> result = new TreeMap<>();
        for (Entry<String, SkriptField> entry : fieldMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }

        return result;
    }

    public @Nullable AbstractSkriptClass getParent() {
        return ClassManager.getClass(name);
    }

    public boolean fieldExists(String name) {
        return fieldMap.containsKey(name);
    }

    public int getHashCode() {
        return Objects.hash(name, fieldMap);
    }

    public boolean isInstance() {
        return true;
    }


}
