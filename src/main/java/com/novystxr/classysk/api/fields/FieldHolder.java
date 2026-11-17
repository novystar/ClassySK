package com.novystxr.classysk.api.fields;

import ch.njol.skript.SkriptConfig;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.api.util.DefaultValue;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface FieldHolder {

    Map<String, Object[]> fieldValues();
    Map<String, String[]> fieldKeys();

    void setDefaults();

    @Nullable SkriptField getField(String fieldName);

    default List<String> activeFieldNames() {
        return new ArrayList<>(fieldValues().keySet());
    }

    default void removeField(String fieldName) {
        fieldValues().remove(fieldName);
        fieldKeys().remove(fieldName);
    }

    default boolean fieldExists(String fieldName) {
        return fieldValues().containsKey(fieldName);
    }

    /**
     *
     * @param fieldName The name of the target field
     * @param toType The desired conversion type
     * @param safe When true, the field won't be modified if no values were able to convert
     * @see ConverterUtils#convertInPlace(Object[], String[], Class, boolean, BiConsumer)
     */
    default <To> void convertFieldInPlace(String fieldName, Class<To> toType, boolean safe) {
        BiConsumer<To[], String @Nullable []> setter = (values, keys) -> {
            if (values.length == 0) {
                removeField(fieldName);
                return;
            }

            if (keys == null) fieldKeys().remove(fieldName); else fieldKeys().put(fieldName, keys);
            fieldValues().put(fieldName, values);
        };

        Object[] values = fieldValues().get(fieldName);
        if (values == null) return;
        ConverterUtils.convertInPlace(values, fieldKeys().get(fieldName), toType, safe, setter);
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

        values = ConverterUtils.getConverted(values, field.type);
        if (keys != null && keys.length != 0) {
            keys = ConverterUtils.filterKeys(keys, values);
            fieldKeys().put(fieldName, keys);
        }
        values = ArrayUtils.removeAllOccurrences(values, null);
        if (values.length == 0) return;

        fieldValues().put(fieldName, values);
    }

    default Object @NotNull [] getFieldValue(String fieldName) {
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
