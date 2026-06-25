package com.novystxr.classysk.api.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.event.FieldEvalEvent;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
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

    public SkriptField createField(FieldSignature signature) {
        SkriptField field = new SkriptField(signature);
        fieldMap.put(signature.name(), field);
        return field;
    }

    public boolean isAccessible() {
        SkriptClass parent = getParent();
        if (parent == null) return false;

        return parent.accessible;
    }

    public boolean fieldExists(String name) {
        return fieldMap.containsKey(name);
    }

    public void removeField(String name) {
        fieldMap.remove(name);
    }

    public void removeField(SkriptField value) {
        fieldMap.remove(value.signature.name());
    }

    public void resetField(FieldSignature signature) {
        Expression<?> defaultExpr = signature.defaultExpr();
        String fieldName = signature.name();

        if (defaultExpr == null) {
            removeField(fieldName);
            return;
        }
        Object[] defaultValue = defaultExpr.getArray(new FieldEvalEvent());
        if (!setFieldValue(fieldName, defaultValue)) {
             removeField(fieldName);
        }
    }

    // lazy initialization
    public boolean setFieldValue(String name, @Nullable Object[] value) {
        FieldSignature signature = getFieldSignature(name);
        if (signature == null) return false;
        if (signature.canConvert(value)) {
            createField(signature).setValue(value);
            return true;
        }
        return false;
    }

    public Object[] getFieldValue(String name) {
        SkriptField field = fieldMap.get(name);
        if (field != null) {
            return field.getValue();
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

    public int getHashCode() {
        return Objects.hash(name, fieldMap);
    }

    public boolean isInstance() {
        return true;
    }

}
