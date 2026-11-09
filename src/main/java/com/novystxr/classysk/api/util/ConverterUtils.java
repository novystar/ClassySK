package com.novystxr.classysk.api.util;

import ch.njol.util.coll.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.function.BiConsumer;

public class ConverterUtils {

    @SuppressWarnings("unchecked")
    public static <To> To @NotNull [] getConverted(Object @NotNull [] value, Class<To> componentType, Class<?>... toTypes) {
        To[] converted = (To[]) Array.newInstance(componentType, value.length);
        if (toTypes.length == 0) toTypes = CollectionUtils.array(componentType);
        Converters.convert(value, converted, toTypes);
        return converted;
    }

    @SuppressWarnings("unchecked")
    public static <To> void convertInPlace(Object[] values, String @Nullable [] keys, Class<To> toType, boolean safe, BiConsumer<To[], String @Nullable []> setter) {
        To[] converted = (To[]) Array.newInstance(toType, values.length);
        Converters.convert(values, converted, CollectionUtils.array(toType));

        keys = filterKeys(keys, converted);
        ArrayUtils.removeAllOccurrences(converted, null);

        if (safe && converted.length == 0)
            return;

        setter.accept(converted, keys);
    }

    public static String[] filterKeys(String @Nullable [] keys, Object @NotNull [] values) {
        if (keys == null) return null;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null)
                keys[i] = null;
        }
        return ArrayUtils.removeAllOccurrences(keys, null);
    }

}
