package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.ReflectUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Constructor;
import java.util.*;

public class ClassManager {

    private static final Map<String, SkriptClass> classMap = new HashMap<>();
    static final Map<String, Map<String, SkriptField>> staticFields = new HashMap<>();

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
        // static field validation
        // attempt to convert, if failed, static context changes or no longer exists, remove field
        skriptClass.fieldMap().values().removeIf(field -> {
            FieldSignature signature = skriptClass.getFieldSignature(field.signature.name());

            // if signature no longer exists or static context changed, ignore and use existing signature
            if (signature == null || signature.isStatic() != field.signature.isStatic()) return true;

            if (signature.canConvert(field.value)) {
                field.signature = signature;
            } else {
                return true;
            }
            return false;
        });

        // init any non-existing static fields with default values
        skriptClass.setDefaults(skriptClass);

        Set<ClassInstance> instances = skriptClass.instances();
        if (instances == null)
            return;

        for (ClassInstance instance : instances) {
            for (SkriptField field : instance.fieldMap.values()) {
                String fieldName = field.signature.name();
                FieldSignature signature = skriptClass.getFieldSignature(fieldName);

                // if signature no longer exists or static context changed, ignore and use existing signature
                if (signature == null || signature.isStatic() != field.signature.isStatic()) {
                    continue;
                }
                // attempt to convert to new signature
                if (signature.canConvert(field.value)) {
                    field.signature = signature;
                }
            }
        }
    }

    public static void checkAwaitingParent(SkriptClass parent) {
        Set<ClassInstance> awaiting = awaitingParent.get(parent.name);
        if (awaiting == null)
            return;

        for (ClassInstance instance : awaiting) {
            for (var entry : instance.awaitingFields.entrySet()) {
                String fieldName = entry.getKey();
                SerializableField sField = entry.getValue();

                Object[] value = sField.value;
                FieldSignature targetSignature = instance.getFieldSignature(fieldName);

                if (targetSignature == null) {
                    FieldSignature newSignature = FieldSignature.fromSerializableField(fieldName, sField, parent.name);
                    instance.createField(newSignature).value = value;

                } else if (targetSignature.canConvert(value)) {
                    instance.createField(targetSignature).value = Converters.convert(value, targetSignature.type());

                } else {
                    FieldSignature newSignature = sField.mergeSignature(targetSignature);
                    instance.createField(newSignature).value = value;
                }
            }
            instance.awaitingFields.clear();
        }
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
