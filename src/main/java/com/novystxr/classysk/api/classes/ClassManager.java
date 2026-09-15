package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.fields.SkriptField;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.*;

public class ClassManager {

    private static final Map<String, SkriptClass> classMap = new HashMap<>();
    static final Map<String, Map<String, Object[]>> staticFieldMaps = new HashMap<>();

    static final Map<String, Set<ClassInstance>> instances = new HashMap<>();

    public static Class<? extends ClassInstance> getSubclass(String name) {
        return Classysk.TYPES_ALLOWED ? ClassInstance.subclassManager.getSubclass(name, ClassInstance.class) : ClassInstance.class;
    }

    public static <T extends ClassInstance> T trackInstance(T instance) {
        Set<ClassInstance> instances = ClassManager.instances.computeIfAbsent(instance.name, key -> Collections.newSetFromMap(new WeakHashMap<>()));
        instances.add(instance);
        return instance;
    }

    public static void revalidateFields(SkriptClass skriptClass) {
        // static fields
        skriptClass.fieldValueMap().entrySet().removeIf(entry -> {
            SkriptField field = skriptClass.getField(entry.getKey());

            // if field no longer exists or constant, remove it
            if (field == null || !field.isStatic() || field.hasModifier(Modifier.CONST)) {
                return true;
            }
            Object[] converted = Converters.convert(entry.getValue(), field.type());
            if (converted.length == 0) {
                return true;
            }
            skriptClass.fieldValueMap().put(field.name, converted);
            return false;

        });
        skriptClass.setDefaults();

        // attempt to convert non-static fields to the new structure
        for (ClassInstance instance : skriptClass.instances()) {
            for (var entry : instance.fieldValueMap.entrySet()) {
                SkriptField field = skriptClass.getField(entry.getKey());

                // if field no longer exists, skip it
                if (field == null || field.isStatic()) {
                    continue;
                }
                // attempt to convert, if failed to convert the field is left in an illegal state which may have limited access.
                Object[] converted = Converters.convert(entry.getValue(), field.type());
                if (converted.length != 0) {
                    instance.fieldValueMap.put(field.name, converted);
                }
            }
        }
    }

    public static void registerClass(SkriptClass skriptClass) {
        String name = skriptClass.name;
        classMap.put(name, skriptClass);
    }


    public static void removeClass(String name) {
        classMap.remove(name);
    }

    public static SkriptClass getClass(String name) {
        return classMap.get(name);
    }

    public static boolean classExists(String name) {
        return classMap.containsKey(name);
    }

    public static Collection<SkriptClass> getClasses() {
        return classMap.values();
    }

}
