package com.novystxr.classysk.api.classes;

import java.util.*;
import java.util.function.Function;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.assignability.AssignabilityBridge;
import com.novystxr.classysk.api.assignability.SubclassManager;
import com.novystxr.classysk.api.assignability.TypedInstanceWrapper;
import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.fields.SkriptField;
import org.jetbrains.annotations.Nullable;

public class ClassInstance implements FieldHolder, AssignabilityBridge {

    private static final SubclassManager<ClassInstance> subclassManager =
        new SubclassManager<>(ClassInstance.class, "subclass", String.class);

    public static Class<? extends ClassInstance> getSubclass(String name) {
        return Classysk.TYPES_ALLOWED ? subclassManager.getSubclass(name) : ClassInstance.class;
    }

    public static ClassInstance newInstance(String name) {
        return ClassManager.trackInstance( Classysk.TYPES_ALLOWED ?
            subclassManager.newInstance(name) : new ClassInstance(name));
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
        return data == null ? ClassManager.getClass(name) : data.getDataClass();
    }

    // instance data
    // preferring composition because inheritance is not very feasible with generated classes

    public ClassInstance withData(Function<ClassInstance, InstanceData> data) {
        this.data = data.apply(this);
        return this;
    }

    private InstanceData data;

    public <T extends InstanceData> T getData(Class<T> clazz) {
        return clazz.isInstance(data) ? clazz.cast(data) : null;
    }

    public class InstanceData {

        public SkriptClass getDataClass() {
            return ClassManager.getClass(name);
        }
    }
}
