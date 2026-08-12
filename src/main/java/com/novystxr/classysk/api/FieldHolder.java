package com.novystxr.classysk.api;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.event.FieldEvalEvent;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.Arrays;
import java.util.Map;

import static com.novystxr.classysk.api.Modifier.CONST;

public interface FieldHolder {

    Map<String, SkriptField> fieldMap();

    @Nullable FieldSignature getFieldSignature(String fieldName);

    default SkriptField createField(FieldSignature signature) {
        SkriptField field = new SkriptField(signature);
        fieldMap().put(signature.name(), field);
        return field;
    }

    default boolean fieldExists(String fieldName) {
        return fieldMap().containsKey(fieldName);
    }

    default @Nullable SkriptField getField(String fieldName) {
        return fieldMap().get(fieldName);
    }

    default void removeField(String name) {
        fieldMap().remove(name);
    }

    default void resetField(String fieldName) {
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
    default boolean setFieldValue(String fieldName, @Nullable Object[] value) {
        if (value == null) value = new Object[0];

        FieldSignature signature = getFieldSignature(fieldName);
        if (signature == null) return false;
        if (signature.hasModifier(CONST)) return false;

        SkriptField field = getField(fieldName);
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

    default @NotNull Object[] getFieldValue(String name) {
        SkriptField field = getField(name);
        if (field == null) return new Object[0];

        Object[] value = field.value;
        return Arrays.copyOf(value, value.length);
    }
}
