package com.novystxr.classysk.api.fields;

import com.novystxr.classysk.api.util.DefaultValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.Arrays;
import java.util.Map;

public interface FieldHolder {

    Map<String, Object[]> fieldValueMap();

    void setDefaults();

    @Nullable SkriptField getField(String fieldName);

    default void removeField(String fieldName) {
        fieldValueMap().remove(fieldName);
    }

    default boolean fieldExists(String fieldName) {
        return fieldValueMap().containsKey(fieldName);
    }

    default void resetField(String fieldName) {
        removeField(fieldName);

        SkriptField field = getField(fieldName);
        if (field == null) return;

        DefaultValue<?> defaultValue = field.defaultValue;
        if (defaultValue != null) {
            Object[] convertedValue = Converters.convert(defaultValue.getArray(), field.type());
            if (convertedValue.length == 0) return;

            fieldValueMap().put(fieldName, convertedValue);
        }
    }

    default boolean setFieldValue(String fieldName, @Nullable Object[] value) {
        SkriptField field = getField(fieldName);
        if (field == null) return false;

        if (value == null || value.length == 0) {
            removeField(fieldName); // field was set to null
            return true;
        }
        Object[] convertedValue = Converters.convert(value, field.type());
        if (convertedValue.length == 0) {
            return false;
        }

        fieldValueMap().put(fieldName, convertedValue);
        return true;
    }

    default @NotNull Object[] getFieldValue(String fieldName) {
        Object[] value = fieldValueMap().get(fieldName);
        return value == null ? new Object[0] : Arrays.copyOf(value, value.length);
    }
}
