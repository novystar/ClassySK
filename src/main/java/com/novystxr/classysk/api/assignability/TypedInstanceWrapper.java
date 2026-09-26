package com.novystxr.classysk.api.assignability;

import com.novystxr.classysk.api.classes.ClassInstance;

/**
 * Used as an intermediary type that is implicitly assignable to (skript) subclasses.
 * This will wrap and unwrap when necessary using converters.
 */
public class TypedInstanceWrapper implements AssignabilityBridge {
    private static final SubclassManager<TypedInstanceWrapper>
        subclassManager = new SubclassManager<>(TypedInstanceWrapper.class, "wrapper", ClassInstance.class);

    public static TypedInstanceWrapper newInstance(ClassInstance instance, String key) {
        return subclassManager.newInstance(key, instance);
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
