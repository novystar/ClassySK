package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;

import java.lang.ref.WeakReference;
import java.util.*;

public class ClassManager {

    // freshly deserialized instances that are waiting for the corresponding class structure to be registered
    private static final List<WeakReference<ClassInstance>> awaitingParent = new ArrayList<>();

    private static final Map<String, SkriptClass> classMap = new HashMap<>();

    public static long classAmount() {
        return classMap.size();
    }

    public static void setAwaitingParent(ClassInstance instance) {
        awaitingParent.add(new WeakReference<>(instance));
    }

    public static void checkAwaitingParent(SkriptClass parent) {
        awaitingParent.removeIf(ref -> {
            ClassInstance instance = ref.get();
            if (instance == null) return true;

            if (instance.name.equals(parent.name)) {
                parent.instances.add(ref);

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
                return true;
            }
            return false;
        });
    }

    public static void createClass(String name, SkriptClass newClass) {
        classMap.put(name, newClass);
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

    public static boolean isAccessible(String name) {
        if (!classExists(name)) return false;
        return getClass(name).accessible;
    }

    public static Collection<SkriptClass> getClasses() {
        return classMap.values();
    }

}
