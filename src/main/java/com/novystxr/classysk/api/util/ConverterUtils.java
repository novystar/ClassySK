package com.novystxr.classysk.api.util;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public class ConverterUtils {

    public static boolean canConvert(Class<?> toClass, Object @Nullable [] values) {
        if (values == null) return true;
        for (Object value : values) {

            Class<?> fromClass = value.getClass();
            if (!canConvert(toClass, fromClass)) return false;
        }
        return true;
    }

    public static boolean canConvert(Class<?> toClass, Class<?> fromClass) {

        if (toClass == Object.class) return true;
        if (toClass == Long.class && fromClass != Long.class) return false;
        if (toClass == fromClass) return true;
        return Converters.converterExists(fromClass, toClass);
    }

}
