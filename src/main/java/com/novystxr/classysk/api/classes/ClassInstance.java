package com.novystxr.classysk.api.classes;

import java.util.*;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.fields.SkriptField;
import org.jetbrains.annotations.Nullable;

public class ClassInstance implements FieldHolder {

    static final SubclassManager<ClassInstance, ClassInstance> subclassManager =
        new SubclassManager<>("subclass", String.class);

    public static ClassInstance newInstance(String name) {
        return ClassManager.trackInstance( Classysk.TYPES_ALLOWED ?
            subclassManager.newInstance(name, ClassInstance.class) : new ClassInstance(name));
    }

    public final String name;
    public final Map<String, Object[]> fieldValueMap = new HashMap<>();

    protected ClassInstance(String name) {
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

    public SkriptClass getParent() {
        return ClassManager.getClass(name);
    }
}
