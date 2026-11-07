package com.novystxr.classysk.api.fields;

import ch.njol.skript.SkriptConfig;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.util.DefaultValue;
import com.novystxr.classysk.api.util.ExprUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.Arrays;
import java.util.Map;

public interface FieldHolder {

    Map<String, Object[]> fieldValues();
    Map<String, String[]> fieldKeys();

    void setDefaults();

    @Nullable SkriptField getField(String fieldName);

    default void removeField(String fieldName) {
        fieldValues().remove(fieldName);
        fieldKeys().remove(fieldName);
    }

    default boolean fieldExists(String fieldName) {
        return fieldValues().containsKey(fieldName);
    }

    default void resetField(String fieldName) {
        removeField(fieldName);

        SkriptField field = getField(fieldName);
        if (field == null) return;

        DefaultValue<?> defaultValue = field.defaultValue;
        if (defaultValue != null) {
            Object[] convertedValue = Converters.convert(defaultValue.getArray(), field.type());
            if (convertedValue.length == 0) return;

            fieldValues().put(fieldName, convertedValue);
        }
    }

    default void setFieldValue(String fieldName, Object @Nullable [] values, String @Nullable [] keys) {
        SkriptField field = getField(fieldName);
        if (field == null) return;

        if (values == null || values.length == 0) {
            removeField(fieldName); // field was set to null
            return;
        }

        values = Validator.getConverted(values, field.type);
        if (keys != null && keys.length != 0) {
            keys = ExprUtils.filterKeys(keys, values);
            fieldKeys().put(fieldName, keys);
        }
        values = ArrayUtils.removeAllOccurrences(values, null);
        if (values.length == 0) return;

        fieldValues().put(fieldName, values);
    }

    default @NotNull Object[] getFieldValue(String fieldName) {
        Object[] value = fieldValues().get(fieldName);
        if (value == null) return new Object[0];

        return convertIfOldPlayer(fieldName, Arrays.copyOf(value, value.length));
    }

    default String @Nullable [] getFieldKeys(String fieldName) {
        return fieldKeys().get(fieldName);
    }

    @SuppressWarnings("unchecked")
    default <T> T[] convertIfOldPlayer(String fieldName, T[] objects) {
        if (!SkriptConfig.enablePlayerVariableFix.value())
            return objects;

        boolean anyChanged = false;
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof Player oldPlayer) {
                objects[i] = (T) Bukkit.getPlayer(oldPlayer.getUniqueId());
                anyChanged = true;
            }
        }
        if (anyChanged)
            setFieldValue(fieldName, objects, getFieldKeys(fieldName));
        return objects;
    }
}
