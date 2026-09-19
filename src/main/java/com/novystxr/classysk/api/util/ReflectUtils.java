package com.novystxr.classysk.api.util;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.*;
import com.novystxr.classysk.api.assignability.AssignabilityBridge;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.main.elements.Types;
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
    private static final Field exactClassInfos;
    private static final Field localizedLanguage;

    static {
        try {
            acceptRegistrations = Skript.class.getDeclaredField("acceptRegistrations");
            acceptRegistrations.setAccessible(true);

            quickAccessConverters = Converters.class.getDeclaredField("QUICK_ACCESS_CONVERTERS");
            quickAccessConverters.setAccessible(true);

            converters = Converters.class.getDeclaredField("CONVERTERS");
            converters.setAccessible(true);

            exactClassInfos = Classes.class.getDeclaredField("exactClassInfos");
            exactClassInfos.setAccessible(true);

            localizedLanguage = Language.class.getDeclaredField("localizedLanguage");
            localizedLanguage.setAccessible(true);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void createLanguageNode(String name) {
        String key = "types."+name+"classinstance";
        if (Language.keyExists(key)) return;

        try {
            var localizedLanguageMap = (Map<String, String>) localizedLanguage.get(null);
            localizedLanguageMap.put(key, StringUtils.titleCase(name) + " instance");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AssignabilityBridge> void registerClassInfo(String name, Class<T> clazz) {
        if (Classes.getExactClassInfo(clazz) == null) {
            name = StringUtils.getLowerCase(name);
            createLanguageNode(name);
            ClassInfo<?> info = new ClassInfo<>(clazz, name + "classinstance")
                .serializeAs(ClassInstance.class)
                .parser((Parser<? extends T>) Types.classParser);
            try {

                var exactClassInfosMap = (Map<Class<?>, ClassInfo<?>>) exactClassInfos.get(null);
                exactClassInfosMap.put(clazz, info);

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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

            removeFromQuickAccess(fromType, toType);
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
        getQuickAccessConverters().values().removeIf(info -> info != null && info.getFrom() == fromType);
    }

    public static void unregisterAllTo(Class<?> toType) {
        getConverters().removeIf(info -> info.getTo() == toType);
        getQuickAccessConverters().values().removeIf(info -> info != null && info.getTo() == toType);
    }

}
