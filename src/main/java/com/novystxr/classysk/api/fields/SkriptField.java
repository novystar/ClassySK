package com.novystxr.classysk.api.fields;

import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.ConverterUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SkriptField {
    public record FieldSignature (
            String name,
            Class<?> type,
            @Nullable List<Object> defaultValue,

            AccessType accessType,
            boolean isStatic,
            boolean isPlural,

            AbstractSkriptClass parentClass

    ) implements AccessModifiable {

        @Override
        public boolean checkAccess(@Nullable AbstractSkriptClass contextClass) {
            if (accessType == AccessType.PRIVATE && parentClass != contextClass) return false;
            return true;
        }

        @Override
        public boolean checkContext(boolean isStatic) {
            return isStatic == this.isStatic;
        }

        public @Nullable Object[] getDefaultValueArray() {
            if (defaultValue == null) return null;
            return defaultValue.toArray();
        }

        public boolean canConvert(Object[] values) {
            if (values == null) return true;
            if (values.length != 1 && !isPlural) return false;

            return ConverterUtils.canConvert(type, values);
        }

        // constructor with array for convenience
        public FieldSignature(String name, Class<?> type, Object[] defaultValue, AccessType accessType, boolean isStatic, boolean isPlural, AbstractSkriptClass parentClass) {
            List<Object> listValue = null;
            if (defaultValue != null) listValue = List.of(defaultValue);

            this(name, type, listValue, accessType, isStatic, isPlural, parentClass);
        }

    }

    public static String getEffectiveName(SkriptClass parentClass, String fieldName) {
        return parentClass.getEffectiveName()+"::"+fieldName;
    }

    public FieldSignature signature;
    private Object[] value;

    public SkriptField(FieldSignature signature) {
        this.signature = signature;
        this.value = signature.getDefaultValueArray();
    }

    public void setValue(@Nullable Object[] value) {
        this.value = value;
    }

    public @Nullable Object[] getValue() {
        return this.value;
    }

}
