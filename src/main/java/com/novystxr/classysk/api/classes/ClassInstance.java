package com.novystxr.classysk.api.classes;

import java.util.*;
import java.util.Map.Entry;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.event.FieldEvalEvent;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public class ClassInstance {
    public final String name;

    public String name() {
        return name;
    }

    public final Map<String, SkriptField> fieldMap = new HashMap<>();

    // set on deserialization for when parent class becomes known
    final Map<String, SerializableField> awaitingFields = new HashMap<>();

    public void putAwaitingField(String name, SerializableField field) {
        awaitingFields.put(name, field);
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

    public void resetField(String fieldName) {
        removeField(fieldName);

        FieldSignature signature = getFieldSignature(fieldName);
        if (signature == null) return;

        Expression<?> defaultExpr = signature.defaultExpr();
        if (defaultExpr != null) {

            Object[] convertedValue = Converters.convert(defaultExpr.getArray(new FieldEvalEvent()), signature.type());
            if (convertedValue.length == 0) return;

            createField(signature).value = convertedValue;
        }
    }

    // lazy initialization
    public boolean setFieldValue(String fieldName, @Nullable Object[] value) {
        if (value == null) value = new Object[0];

        FieldSignature signature = getFieldSignature(fieldName);
        if (signature == null) return false;

        SkriptField field = fieldMap.get(fieldName);
        if (field == null) {
            if (value.length == 0) {
                return false; // nothing would have changed so we don't initialize the field
            }
            field = createField(signature);
        } else if (value.length == 0) {
            field.value = new Object[0]; // field was intentionally set to null
            return true;
        }

        Object[] convertedValue = Converters.convert(value, signature.type());
        if (convertedValue.length == 0) return false; // could not convert

        field.value = convertedValue;
        return true;
    }

    public @NotNull Object[] getFieldValue(String name) {
        SkriptField field = fieldMap.get(name);
        if (field == null) return new Object[0];

        Object[] value = field.value;
        return Arrays.copyOf(value, value.length);
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
        return "%"+ StringUtils.titleCase(name) +"%";
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
        return Objects.hash(isInstance(), name, getFieldValueMap());
    }

    public boolean isInstance() {
        return true;
    }

}
