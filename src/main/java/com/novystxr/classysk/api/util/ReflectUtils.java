package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.*;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.main.elements.Types;

import java.lang.reflect.Field;
import java.util.Map;

public class ReflectUtils {
    private static final Field exactClassInfos;
    private static final Field localizedLanguage;

    static {
        try {
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
            localizedLanguageMap.put("types."+name+"classinstance", StringUtils.titleCase(name) + " instance");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ClassInstance> void registerClassInfo(String name, Class<T> clazz) {
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

}
