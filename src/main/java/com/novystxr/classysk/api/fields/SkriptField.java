package com.novystxr.classysk.api.fields;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.jetbrains.annotations.NotNull;
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

        @Override
        public boolean checkAccess(@Nullable SkriptClass contextClass, @NotNull ClassInstance instance) {
            return accessType != AccessType.PRIVATE || instance.getParent() == contextClass;
        }

        @Override
        public boolean checkContext(boolean isStatic) {
            return isStatic == this.isStatic;
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
    public Object[] value;

    public SkriptField(FieldSignature signature) {
        this.signature = signature;
    }

}
