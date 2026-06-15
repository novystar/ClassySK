package com.novystxr.classysk.api.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.SkriptMethod;
import org.jetbrains.annotations.Nullable;

public class ClassInstance {
    public final String name;
    private final Map<String, SkriptField> fieldMap = new HashMap<>();

    // set on deserialization for when parent class becomes known
    final Map<String, Object[]> awaitingFields = new HashMap<>();

    public void putAwaitingField(String name, Object[] value) {
        awaitingFields.put(name, value);
    }

    public ClassInstance(String name) {
        this.name = name;
    }

    public void createField(SkriptField field) {
        if (fieldExists(field.signature.name())) return;

        fieldMap.put(field.signature.name(), field);

    }

    public boolean isAccessible() {
        SkriptClass parent = getParent();
        if (parent == null) return false;

        return parent.accessible;
    }

    public void removeField(String name) {
        fieldMap.remove(name);
    }

    public void removeField(SkriptField value) {
        fieldMap.remove(value.signature.name());
    }

    // lazy initialization
    public void setFieldValue(String name, @Nullable Object[] value) {
        FieldSignature signature = getFieldSignature(name);
        if (signature == null) return;
        if (signature.canConvert(value)) {
            getField(name).setValue(value);
        }
    }

    public Object[] getFieldValue(String name) {
        SkriptField field = fieldMap.get(name);
        if (field != null) {
            return field.getValue();
        }
        SkriptClass parent = getParent();
        if (parent == null) return null;

        FieldSignature signature = parent.getFieldSignature(name);
        if (signature != null) {
            return signature.defaultValue();
        }
        return null;
    }

    // gets existing signature and falls back to parent
    public @Nullable FieldSignature getFieldSignature(String name) {
        SkriptField field = fieldMap.get(name);
        if (field == null) {
            SkriptClass parent = getParent();
            if (parent == null) return null;
            return parent.getFieldSignature(name);
        }
        return field.signature;
    }

    // if field does not yet exist, instantiates one
    SkriptField getField(String name) {
        SkriptField field = fieldMap.get(name);
        if (field != null) return field;

        SkriptClass parent = getParent();
        if (parent == null) return null;

        SkriptField.FieldSignature signature = parent.getFieldSignature(name);
        if (signature == null) return null;

        field = new SkriptField(signature);
        createField(field);

        return field;
    }

    public @Nullable SkriptMethod getAccessibleMethod(String name) {
        SkriptClass parent = getParent();

        if (parent == null) return null;

        SkriptMethod method = parent.getMethod(name);
        if (method == null) return null;
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

    public SkriptClass getParent() {
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
