package com.novystxr.classysk.api.classes;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;

public class ClassManager {

    // freshly deserialized instances that are waiting for the corresponding class structure to be registered
    private static final List<WeakReference<SkriptClass>> awaitingParent = new ArrayList<>();

    private static final Map<String, AbstractSkriptClass> classMap = new HashMap<>();

    public static void setAwaitingParent(SkriptClass skriptClass) {
        awaitingParent.add(new WeakReference<>(skriptClass));
    }

    public static void checkAwaitingParent(AbstractSkriptClass parent) {
        awaitingParent.removeIf(ref -> {
            SkriptClass instance = ref.get();
            if (instance == null) return true;

            if (instance.name.equals(parent.name)) {
                parent.instances.add(ref);

                for (Entry<String, Object[]> entry : instance.awaitingFields.entrySet()) {
                    FieldSignature signature = parent.getFieldSignature(entry.getKey());
                    Object[] value = entry.getValue();
                    if (signature == null) continue;

                    if (signature.canConvert(value)) {
                        //noinspection DataFlowIssue dw gang
                        instance.getField(entry.getKey()).setValue(value);
                    }
                }
                return true;
            }
            return false;
        });
    }

    public static void createClass(AbstractSkriptClass newClass) {
        classMap.put(newClass.name, newClass);
    }

    public static void removeClass(String name) {
        classMap.remove(name);
    }

    public static AbstractSkriptClass getClass(String name) {
        return classMap.get(name);
    }

    public static boolean classExists(String name) {
        return classMap.containsKey(name);
    }

    public static boolean isAccessible(String name) {
        if (!classExists(name)) return false;
        return getClass(name).accessible;
    }

    public static Collection<AbstractSkriptClass> getClasses() {
        return classMap.values();
    }

}
