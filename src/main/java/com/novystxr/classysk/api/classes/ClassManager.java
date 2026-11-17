package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.assignability.AssignabilityBridge;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.util.ReflectUtils;

import java.util.*;

public class ClassManager {

    private static final Map<String, SkriptClass> classes = new HashMap<>();
    static final Map<String, Set<ClassInstance>> instances = new HashMap<>();

    static final Map<String, Map<String, Object[]>> staticFieldValues = new HashMap<>();
    static final Map<String, Map<String, String[]>> staticFieldKeys = new HashMap<>();

    public static <T extends ClassInstance> T trackInstance(T instance) {
        Set<ClassInstance> instances = ClassManager.instances.computeIfAbsent(instance.name, key -> Collections.newSetFromMap(new WeakHashMap<>()));
        instances.add(instance);
        return instance;
    }

    public static void revalidateFields(SkriptClass skriptClass) {
        // static fields
        for (String fieldName : skriptClass.activeFieldNames()) {
            SkriptField field = skriptClass.getField(fieldName);
            // if field no longer exists or constant, remove it
            if (field == null || !field.isStatic() || field.hasModifier(Modifier.CONST)) {
                skriptClass.removeField(fieldName);
                continue;
            }
            skriptClass.convertFieldInPlace(fieldName, field.type, false);
        }
        skriptClass.setDefaults();

        // attempt to convert non-static fields to the new class
        for (ClassInstance instance : skriptClass.instances()) {
            for (String fieldName : instance.activeFieldNames()) {
                SkriptField field = skriptClass.getField(fieldName);

                // if field no longer exists, skip it
                if (field == null || field.isStatic()) {
                    continue;
                }
                // attempt to convert, if failed the field is left in an illegal state which may have limited access.
                instance.convertFieldInPlace(fieldName, field.type(), true);
            }
        }
    }

    public static void createWrapperConverters(SkriptClass skriptClass) {
        if (Classysk.TYPES_ALLOWED) {
            Class<? extends AssignabilityBridge> bridgeType = skriptClass.getSubInterface();
            skriptClass.inheritanceStream().skip(1).forEach(target ->
                ReflectUtils.registerConverter(bridgeType, target.getSubInterface(), i -> i.wrap(target.name))
            );
        }
    }

    public static void registerClass(SkriptClass skriptClass) {
        String name = skriptClass.name;
        classes.put(name, skriptClass);

        if (Classysk.TYPES_ALLOWED) {
            Class<? extends AssignabilityBridge> bridgeClass = skriptClass.getSubInterface();
            ReflectUtils.registerClassInfo(name, bridgeClass);
            ReflectUtils.registerConverter(bridgeClass, ClassInstance.class, AssignabilityBridge::unwrap);
        }
    }


    public static void unregisterClass(String name) {
        classes.remove(name);

        if (Classysk.TYPES_ALLOWED) { // unregister wrapper converters
            ReflectUtils.unregisterAllFrom(AssignabilityBridge.getSubInterface(name), ClassInstance.class);
        }

    }

    public static SkriptClass getClass(String name) {
        return classes.get(name);
    }

    public static boolean classExists(String name) {
        return classes.containsKey(name);
    }

    public static Collection<SkriptClass> getClasses() {
        return classes.values();
    }

}
