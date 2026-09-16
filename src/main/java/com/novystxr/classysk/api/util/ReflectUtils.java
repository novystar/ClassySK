package com.novystxr.classysk.api.util;

import ch.njol.skript.Skript;
import ch.njol.util.*;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ReflectUtils {

    private static final Field acceptRegistrations;
    private static final Field quickAccessConverters;
    private static final Field converters;

    static {
        try {
            acceptRegistrations = Skript.class.getDeclaredField("acceptRegistrations");
            acceptRegistrations.setAccessible(true);

            quickAccessConverters = Converters.class.getDeclaredField("QUICK_ACCESS_CONVERTERS");
            quickAccessConverters.setAccessible(true);

            converters = Converters.class.getDeclaredField("CONVERTERS");
            converters.setAccessible(true);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ConverterInfo<?, ?>> getConverters() {
        try {
            return (List<ConverterInfo<?, ?>>) converters.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked, removal")
    public static Map<Pair<Class<?>, Class<?>>, ConverterInfo<?, ?>> getQuickAccessConverters() {
        try {
            return (Map<Pair<Class<?>, Class<?>>, ConverterInfo<?, ?>>) quickAccessConverters.get(null);
        } catch (IllegalAccessException e) {
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

    @SuppressWarnings("unchecked")
    public static <F, T> void registerConverter(Class<? extends F> fromType, Class<? extends T> toType, Converter<? extends F, ? extends T> converter) {
        if (!Converters.exactConverterExists(fromType, toType)) {
            allowRegistration();

            Converters.registerConverter((Class<F>) fromType, (Class<T>) toType, (Converter<F, T>) converter);
            disableRegistration();
        }
    }

    @SuppressWarnings("removal, SuspiciousMethodCalls")
    public static void removeFromQuickAccess(Class<?> fromType, Class<?> toType) {
        getQuickAccessConverters().remove(new Pair<>(fromType, toType));
    }

    public static void unregisterConverter(Class<?> fromType, Class<?> toType) {
        getConverters().removeIf(info -> info.getFrom() == fromType && info.getTo() == toType);
        removeFromQuickAccess(fromType, toType);
    }

    public static void unregisterAllFrom(Class<?> fromType) {
        getConverters().removeIf(info -> info.getFrom() == fromType);
        getQuickAccessConverters().values().removeIf(info -> info.getFrom() == fromType);
    }

    public static void unregisterAllTo(Class<?> toType) {
        getConverters().removeIf(info -> info.getTo() == toType);
        getQuickAccessConverters().values().removeIf(info -> info.getTo() == toType);
    }

}
