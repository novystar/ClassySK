package com.novystxr.classysk.api.fields;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.Arrays;

public class SkriptField {
    public record FieldSignature (
        String name,
        Class<?> type,
        @Nullable Expression<?> defaultExpr,

        Modifier[] modifiers,
        boolean isPlural

    ) implements AccessModifiable {

        /**
         * Creates a field signature with sensible defaults that should be able to hold the target data -
         * should only be used as a last resort to preserve data if the field no longer exists on deserialization.
         */
        public static FieldSignature fromSerializableField(String fieldName, SerializableField sField) {

            return new FieldSignature(fieldName, sField.signatureType, null,
                Modifier.PUBLIC.array(), sField.isPlural);
        }
        public boolean canConvert(@NotNull Object[] values) {
            if (values.length > 1 && !isPlural) return false;

            return Arrays.stream(values)
                .allMatch(value -> Converters.converterExists(value.getClass(), type));
        }
    }

    public FieldSignature signature;
    public Object[] value = new Object[0];

    public SkriptField(FieldSignature signature) {
        this.signature = signature;
    }

}
