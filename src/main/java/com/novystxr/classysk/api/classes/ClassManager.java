package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.util.ReflectUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Constructor;
import java.util.*;

public class ClassManager {

    private static final Map<String, SkriptClass> classMap = new HashMap<>();
    static final Map<String, Map<String, Object[]>> staticFieldMaps = new HashMap<>();

    static final Map<String, Set<ClassInstance>> instances = new HashMap<>();
    static final Map<String, Set<ClassInstance>> awaitingParent = new HashMap<>();

    private static final Map<String, Class<? extends TypedInstanceWrapper>> generatedClasses = new HashMap<>();

    public static Class<? extends TypedInstanceWrapper> getSubclass(String name) {
        return generatedClasses.computeIfAbsent(name, key ->
            new ByteBuddy()
                .subclass(TypedInstanceWrapper.class)
                .name("com.novystxr.generated."+name)
                .make()
                .load(TypedInstanceWrapper.class.getClassLoader(), Default.WRAPPER)
                .getLoaded()
        );
    }

    public static void setAwaitingParent(ClassInstance instance) {
        ClassManager.awaitingParent.computeIfAbsent(instance.name, key ->
                Collections.newSetFromMap(new WeakHashMap<>()))
            .add(instance);
    }

    public static void revalidateFields(SkriptClass skriptClass) {
        // static fields
        skriptClass.fieldValueMap().entrySet().removeIf(entry -> {
            SkriptField field = skriptClass.getField(entry.getKey());

            // if field no longer exists, remove it
            if (field == null || !field.isStatic()) {
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

        Set<ClassInstance> instances = skriptClass.instances();
        if (instances == null)
            return;

        // attempt to convert non-static fields to the new structure
        for (ClassInstance instance : instances) {
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

    public static void checkAwaitingParent(SkriptClass parent) {
        Set<ClassInstance> awaiting = awaitingParent.get(parent.name);
        if (awaiting == null) return;
        instances.get(parent.name).addAll(awaiting);

        awaitingParent.remove(parent.name);
    }

    public static Converter<ClassInstance, ? extends TypedInstanceWrapper> getConditionalConverter(Class<? extends TypedInstanceWrapper> subclass) {
        try {
            final Constructor<? extends TypedInstanceWrapper> constructor = subclass.getDeclaredConstructor(ClassInstance.class);
            return instance -> {
                SkriptClass compare = ClassManager.getClass(subclass.getSimpleName());
                SkriptClass parent = instance.getParent();
                if (parent == null) return null;

                if (!parent.inherits(compare)) {
                    return null;
                }
                try {
                    if (instance.wrapper == null) {
                        instance.wrapper = constructor.newInstance(instance);
                    }
                    return instance.wrapper;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerClass(SkriptClass skriptClass) {
        String name = skriptClass.name;
        if (Classysk.TYPES_ALLOWED) {
            Class<? extends TypedInstanceWrapper> subclass = getSubclass(name);
            ReflectUtils.allowRegistration();

            if (!Converters.exactConverterExists(ClassInstance.class, subclass)) {
                ReflectUtils.removeFromQuickAccess(ClassInstance.class, subclass);
                ReflectUtils.registerConverter(ClassInstance.class, subclass, getConditionalConverter(subclass));
            }
            if (!Converters.exactConverterExists(subclass, ClassInstance.class)) {
                Converters.registerConverter(subclass, ClassInstance.class, from -> from.instance);
            }
            Converters.createChainedConverters();
            ReflectUtils.disableRegistration();
        }
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
