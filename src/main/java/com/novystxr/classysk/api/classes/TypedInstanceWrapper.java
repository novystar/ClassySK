package com.novystxr.classysk.api.classes;

import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.TypeWrappable;

import java.util.regex.Pattern;

public class TypedInstanceWrapper implements TypeWrappable<TypedInstanceWrapper, ClassInstance> {

    public static final Pattern pattern = Pattern.compile("("+ Classysk.CLASSNAME_PATTERN + ") instances?");

    public final ClassInstance instance;

    public TypedInstanceWrapper(ClassInstance instance) {
        this.instance = instance;
    }

    @Override
    public TypedInstanceWrapper wrap() {
        return this;
    }

    @Override
    public ClassInstance unwrap() {
        return instance;
    }

    @Override
    public Class<? extends TypedInstanceWrapper> getSubclass() {
        return getClass();
    }

    @Override
    public int hashCode() {
        return unwrap().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeWrappable<?,?> wrappable) {
            return wrappable.unwrap().equals(wrappable.unwrap());
        }
        return false;
    }
}
