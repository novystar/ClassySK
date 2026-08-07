package com.novystxr.classysk.api.util;

import ch.njol.skript.Skript;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Field;

public class ReflectUtils {

    private static final Field acceptRegistrationsField;

    static {
        try {
            acceptRegistrationsField = Skript.class.getDeclaredField("acceptRegistrations");
            acceptRegistrationsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void allowRegistration() {
        try {
            acceptRegistrationsField.setBoolean(null, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void disableRegistration() {
        try {
            acceptRegistrationsField.setBoolean(null, false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked, rawtypes")
    public static <F> void registerConverter(Class<F> fromType, Class toType, Converter<F, ?> converter) {
        allowRegistration();
        Converters.registerConverter(fromType, toType, converter);
        disableRegistration();
    }

}
