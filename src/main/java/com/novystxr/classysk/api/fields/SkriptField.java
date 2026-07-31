package com.novystxr.classysk.api.fields;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.AccessModifiable;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public class SkriptField {
    public record FieldSignature (
        String name,
        Class<?> type,
        @Nullable Expression<?> defaultExpr,

        AccessType accessType,
        boolean isStatic,
        boolean isPlural

    ) implements AccessModifiable {

        /**
         * Creates a field signature with sensible defaults that should be able to hold the target data -
         * should only be used as a last resort to preserve data if the field no longer exists on deserialization.
         */
        public static FieldSignature fromSerializableField(String fieldName, SerializableField sField) {

            return new FieldSignature(fieldName, sField.signatureType, null, AccessType.PUBLIC, false, sField.isPlural);
        }
        public boolean canConvert(@Nullable Object[] values) {
            if (values == null) return true;
            if (values.length > 1 && !isPlural) return false;

            for (Object value : values) {
                if (value == null) continue;
                if (!Converters.converterExists(value.getClass(), type)) {
                    return false;
                }
            }
            return true;
        }
    }

    public FieldSignature signature;
    public Object[] value = new Object[0];

    public SkriptField(FieldSignature signature) {
        this.signature = signature;
    }

}
