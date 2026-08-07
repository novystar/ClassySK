package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.api.classes.ClassInstance.TypedInstanceWrapper;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.ReflectUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

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

                FieldSignature targetSignature = instance.getFieldSignature(fieldName);
                SkriptField createdField;

                if (targetSignature == null) {
                    FieldSignature newSignature = FieldSignature.fromSerializableField(fieldName, sField);
                    createdField = instance.createField(newSignature);

                } else if (targetSignature.canConvert(sField.value)) {
                    createdField = instance.createField(targetSignature);
                } else {
                    FieldSignature newSignature = sField.mergeSignature(targetSignature);
                    createdField = instance.createField(newSignature);
                }
                createdField.value = sField.value;
            }
            instance.awaitingFields.clear();
        }
        awaitingParent.remove(parent.name);
    }

    public static Converter<ClassInstance, ?> getConditionalConverter(Class<?> subclass) {
        return instance -> {
            try {
                if (subclass != getSubclass(instance.name)) {
                    return null;
                }
                return subclass.getDeclaredConstructor(ClassInstance.class).newInstance(instance);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static void registerClass(SkriptClass skriptClass) {
        String name = skriptClass.name;
        Class<?> subclass = getSubclass(name);

        if (!Converters.exactConverterExists(ClassInstance.class, subclass)) {
            ReflectUtils.registerConverter(ClassInstance.class, subclass, getConditionalConverter(subclass));
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
