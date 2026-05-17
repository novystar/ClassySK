package com.novystxr.classysk.api;

import ch.njol.skript.classes.ClassInfo;
import com.novystxr.classysk.api.util.ConverterUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SkriptField {
    public record FieldSignature(
            String name,
            ClassInfo<?> type,
            @Nullable List<Object> defaultValue,

            AccessType accessType,
            boolean isStatic,
            boolean isPlural
    ) {
        public @Nullable Object[] getDefaultValueArray() {
            if (defaultValue == null) return null;
            return defaultValue.toArray();
        }

        // constructor with array for convenience
        public FieldSignature(String name, ClassInfo<?> type, Object[] defaultValue, AccessType accessType, boolean isStatic, boolean isPlural) {
            List<Object> listValue = null;
            if (defaultValue != null) listValue = List.of(defaultValue);

            this(name, type, listValue, accessType, isStatic, isPlural);
        }

    }

    FieldSignature signature;
    private Object[] value;

    SkriptField(FieldSignature signature) {
        this.signature = signature;
        this.value = signature.getDefaultValueArray();
    }

    public void setValue(@Nullable Object[] value) {
        this.value = value;
    }

    public @Nullable Object[] getValue() {
        return this.value;
    }

    public boolean canConvert(@Nullable Object... values) {
        return ConverterUtils.canConvert(this.signature, values);

    }

}
