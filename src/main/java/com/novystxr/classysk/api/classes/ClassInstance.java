package com.novystxr.classysk.api.classes;

import java.util.*;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.assignability.AssignabilityBridge;
import com.novystxr.classysk.api.assignability.SubclassManager;
import com.novystxr.classysk.api.assignability.TypedInstanceWrapper;
import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.fields.SkriptField;
import org.jetbrains.annotations.Nullable;

public class ClassInstance implements FieldHolder, AssignabilityBridge {

    private static final SubclassManager<ClassInstance, ClassInstance> subclassManager =
        new SubclassManager<>("subclass", String.class);

    public static Class<? extends ClassInstance> getSubclass(String name) {
        return Classysk.TYPES_ALLOWED ? subclassManager.getSubclass(name, ClassInstance.class) : ClassInstance.class;
    }

    public static ClassInstance newInstance(String name) {
        return ClassManager.trackInstance( Classysk.TYPES_ALLOWED ?
            subclassManager.newInstance(name, ClassInstance.class) : new ClassInstance(name));
    }

    public final String name;
    public final Map<String, Object[]> fieldValueMap = new HashMap<>();

    public ClassInstance(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Object[]> fieldValueMap() {
        return fieldValueMap;
    }

    @Override
    public void setDefaults() {
        getParent().inheritanceStream().forEach(target -> target.fields.values().stream()
            .filter(field -> !field.isStatic() && !fieldExists(field.name))
            .forEach(field -> resetField(field.name))
        );
    }

    @Override
    public @Nullable SkriptField getField(String fieldName) {
        return getParent().getField(fieldName);
    }

    @Override
    public ClassInstance unwrap() {
        return this;
    }

    private final Map<String, TypedInstanceWrapper> wrappers = new HashMap<>();

    public TypedInstanceWrapper wrap(String asName) {
        return wrappers.computeIfAbsent(asName, k -> TypedInstanceWrapper.newInstance(this, asName));
    }

    public SkriptClass getParent() {
        return ClassManager.getClass(name);
    }
}
