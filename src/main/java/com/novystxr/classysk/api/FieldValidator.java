package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.ConverterUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class FieldValidator {

    String fieldName;
    SkriptClass skriptClass;

    Kleenean isValid = Kleenean.UNKNOWN;
    FieldSignature signature;

    public FieldValidator(String fieldName) {
        this.fieldName = fieldName;
    }

    public void validate(@Nullable SkriptClass skriptClass) {
        if (skriptClass == null) {
            isValid = Kleenean.FALSE;
            return;
        }

        signature = skriptClass.getParent().getFieldSignature(fieldName);
        if (signature == null) {
            Skript.error("Unable to resolve field signature '%s'", SkriptField.getEffectiveName(skriptClass, fieldName));
            isValid = Kleenean.FALSE;
            return;
        }
        this.skriptClass = skriptClass;

        checkAccess();
    }

    private void checkAccess() {
        if (skriptClass == null) {
            isValid = Kleenean.FALSE;
            Skript.error("Illegal Access! This class does not exist");
            return;
        }

        if (!skriptClass.checkFieldAccess(fieldName)) {
            isValid = Kleenean.FALSE;
            Skript.error("Illegal Access! Tried to access non-existent field '%s' or tried to access it from improper context", SkriptField.getEffectiveName(skriptClass, fieldName));
            return;
        }

        isValid = Kleenean.TRUE;
    }

    public void updateInstance(@Nullable Expression<SkriptClass> skriptClassExpr, Event event) {
        // static access
        if (skriptClassExpr == null) {
            return;
        }
        SkriptClass newClass = skriptClassExpr.getSingle(event);
        if (newClass == null) {
            isValid = Kleenean.FALSE;
            return;
        }
        if (this.skriptClass == null || newClass.getParent() != this.skriptClass.getParent()) {
            validate(newClass);
        }
    }

    public void attemptSetValue(@Nullable Object[] delta) {
        if (!isValid.isTrue()) return;
        if (delta == null) return;
        if (!skriptClass.getParent().accessible) return;

        if (signature.canConvert(delta)) {
            skriptClass.getField(fieldName).setValue(delta);
        }
    }

    public Object[] get() {
        if (!isValid.isTrue()) return null;
        if (!skriptClass.getParent().accessible) return null;
        return skriptClass.getFieldValue(fieldName);
    }

    public boolean canConvert(Object @Nullable [] delta) {
        return ConverterUtils.canConvert(signature.type().getC(), delta);
    }

    public String fieldName() {
        return fieldName;
    }
    public FieldSignature signature() {
        return signature;
    }
    public SkriptClass skriptClass() {
        return skriptClass;
    }
    public Kleenean isValid() {
        return isValid;
    }

}
