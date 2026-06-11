package com.novystxr.classysk.api.fields;

import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.ConverterUtils;
import org.jetbrains.annotations.Nullable;

public class SkriptField {
    public record FieldSignature (
            String name,
            Class<?> type,
            @Nullable Object[] defaultValue,

            AccessType accessType,
            boolean isStatic,
            boolean isPlural,

            SkriptClass parentClass

    ) implements AccessModifiable {

        @Override
        public boolean checkAccess(@Nullable SkriptClass contextClass) {
            if (accessType == AccessType.PRIVATE && parentClass != contextClass) return false;
            return true;
        }

        @Override
        public boolean checkContext(boolean isStatic) {
            return isStatic == this.isStatic;
        }

        public boolean canConvert(Object[] values) {
            if (values == null) return true;
            if (values.length != 1 && !isPlural) return false;

            return ConverterUtils.canConvert(type, values);
        }

    }

    public static String getEffectiveName(ClassInstance parentClass, String fieldName) {
        return parentClass.getEffectiveName()+"::"+fieldName;
    }

    public FieldSignature signature;
    private Object[] value;

    public SkriptField(FieldSignature signature) {
        this.signature = signature;
        this.value = signature.defaultValue();
    }

    public void setValue(@Nullable Object[] value) {
        this.value = value;
    }

    public @Nullable Object[] getValue() {
        return this.value;
    }

}
