package com.novystxr.classysk.api.assignability;

import com.novystxr.classysk.api.classes.ClassInstance;

/**
 * Used as an intermediary type that is implicitly assignable to (skript) subclasses.
 * This will wrap and unwrap when necessary using converters.
 */
public class TypedInstanceWrapper implements AssignabilityBridge {
    private static final SubclassManager<TypedInstanceWrapper, TypedInstanceWrapper>
        subclassManager = new SubclassManager<>("wrapper", ClassInstance.class);

    public static Class<? extends TypedInstanceWrapper> getSubclass(String key) {
        return subclassManager.getSubclass(key, TypedInstanceWrapper.class);
    }

    public static TypedInstanceWrapper newInstance(ClassInstance instance, String key) {
        Class<? extends TypedInstanceWrapper> subclass = subclassManager.getSubclass(key, TypedInstanceWrapper.class);

        return subclassManager.newInstance(key, subclass, instance);
    }

    public final ClassInstance instance;

    public TypedInstanceWrapper(ClassInstance instance) {
        this.instance = instance;
    }

    @Override
    public ClassInstance unwrap() {
        return instance;
    }
}
