package com.novystxr.classysk.api.classes;

import java.util.*;

import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.TypeWrappable;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.methods.MethodHolder;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public class ClassInstance implements FieldHolder, MethodHolder, TypeWrappable<TypedInstanceWrapper, ClassInstance> {
    public final String name;
    public final Map<String, Object[]> fieldValueMap = new HashMap<>();

    TypedInstanceWrapper wrapper = null;

    public ClassInstance(String name) {
        this.name = name;
    }

    public SkriptClass getParent() {
        return ClassManager.getClass(name);
    }

    @Override
    public Map<String, Object[]> fieldValueMap() {
        return fieldValueMap;
    }

    @Override
    public @Nullable SkriptField getField(String name) {
        SkriptField result = getParent().getField(name);
        if (result == null && fieldValueMap.containsKey(name)) {
            return SkriptField.UNKNOWN;
        }
        return result;
    }

    @Override
    public void setDefaults() {
        getParent().inheritanceStream().forEach(target -> {
            for (SkriptField field : target.fields.values()) {
                if (field.isStatic()) continue;
                if (fieldExists(field.name)) continue;

                resetField(field.name);
            }
        });
    }

    @Override
    public TypedInstanceWrapper wrap() {
        return wrapper != null ? wrapper : Converters.convert(this, getSubclass());
    }

    @Override
    public ClassInstance unwrap() {
        return this;
    }

    @Override
    public Class<? extends TypedInstanceWrapper> getSubclass() {
        return ClassManager.getSubclass(name);
    }

    @Override
    public MethodRegistry getRegistry() {
        return getParent().getRegistry();
    }


    @Override
    public SkriptMethod getExactMethod(MethodIdentifier identifier, boolean isSuper) {
        return getParent().getExactMethod(identifier, isSuper);
    }

    @Override
    public List<SkriptMethod> getCandidates(MethodReference reference) {
        return getParent().getCandidates(reference);
    }
}
