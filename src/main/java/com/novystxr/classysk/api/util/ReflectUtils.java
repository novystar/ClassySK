package com.novystxr.classysk.api.util;

import ch.njol.skript.Skript;
import ch.njol.util.*;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Field;
import java.util.Map;

public class ReflectUtils {

    private static final Field acceptRegistrations;
    private static final Field quickAccessConverters;

    static {
        try {
            acceptRegistrations = Skript.class.getDeclaredField("acceptRegistrations");
            acceptRegistrations.setAccessible(true);

            quickAccessConverters = Converters.class.getDeclaredField("QUICK_ACCESS_CONVERTERS");
            quickAccessConverters.setAccessible(true);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void allowRegistration() {
        try {
            acceptRegistrations.setBoolean(null, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void disableRegistration() {
        try {
            acceptRegistrations.setBoolean(null, false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked, removal, SuspiciousMethodCalls")
    public static void removeFromQuickAccess(Class<?> fromType, Class<?> toType) {
        try {
            var quickAccess = (Map<Pair<Class<?>, Class<?>>, ConverterInfo<?, ?>>) quickAccessConverters.get(null);
            quickAccess.remove(new Pair<>(fromType, toType));

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked, rawtypes")
    public static <F> void registerConverter(Class<F> fromType, Class toType, Converter<F, ?> converter) {
        Converters.registerConverter(fromType, toType, converter);
    }

}
