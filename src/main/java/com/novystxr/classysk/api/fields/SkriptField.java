package com.novystxr.classysk.api.fields;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.ConverterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            if (values.length != 1 && !isPlural) return false;

            return ConverterUtils.canConvert(type, values);
        }
    }

    public FieldSignature signature;
    private Object[] value;

    public SkriptField(FieldSignature signature) {
        this.signature = signature;
    }

    public void setValue(@Nullable Object[] value) {
        this.value = value;
    }

    public @Nullable Object[] getValue() {
        return this.value;
    }

}
