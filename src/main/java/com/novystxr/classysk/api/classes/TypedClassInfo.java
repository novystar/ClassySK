package com.novystxr.classysk.api.classes;

import ch.njol.skript.classes.ClassInfo;
import com.novystxr.classysk.api.util.StringUtils;

public class TypedClassInfo<T> extends ClassInfo<T> {
    /**
     * @param c        The class
     * @param codeName The name used in patterns
     */
    public TypedClassInfo(Class<T> c, String codeName) {
        super(c, codeName);
        this.type = null;
    }

    public TypedClassInfo(Class<T> c, String codeName, String type) {
        super(c, codeName);
        this.type = StringUtils.getLowerCase(type);
    }

    public final String type;

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getC() {
        return (Class<T>) (type == null ? TypedClassInfo.class : ClassManager.getSubclass(type));
    }

    public static class TypedInstanceWrapper {

        public final ClassInstance instance;

        public TypedInstanceWrapper(ClassInstance instance) {
            this.instance = instance;
        }

    }
}
