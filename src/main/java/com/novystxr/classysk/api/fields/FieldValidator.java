package com.novystxr.classysk.api.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class FieldValidator {

    String fieldName;
    SkriptClass skriptClass;

    Kleenean isValid = Kleenean.UNKNOWN;
    FieldSignature signature;
    boolean isStatic;

    public FieldValidator(String fieldName, boolean isStatic) {
        this.fieldName = fieldName;
        this.isStatic = isStatic;
    }

    public void validate(@Nullable SkriptClass skriptClass) {
        if (skriptClass == null) {
            Skript.error("Illegal Access! This class does not exist");
            isValid = Kleenean.FALSE; return;
        }

        signature = skriptClass.getParent().getFieldSignature(fieldName);
        if (signature == null) {
            Skript.error("Unable to resolve field signature '%s'", SkriptField.getEffectiveName(skriptClass, fieldName));
            isValid = Kleenean.FALSE; return;
        }
        this.skriptClass = skriptClass;
    }

    public void checkAccess(Event event) {
        if (isValid.isFalse()) return;
        if (!signature.isAccessible(event, isStatic)) {
            illegalAccess(); return;
        }
        isValid = Kleenean.TRUE;
    }

    public void checkAccess(ParserInstance parser) {
        if (isValid.isFalse()) return;
        if (!signature.isAccessible(parser, isStatic)) {
            illegalAccess(); return;
        }
        isValid = Kleenean.TRUE;
    }

    private void illegalAccess() {
        isValid = Kleenean.FALSE;
        Skript.error("Illegal Access! Tried to access non-existent field '%s' or tried to access it from improper context", SkriptField.getEffectiveName(skriptClass, fieldName));
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
            checkAccess(event);
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
        if (!skriptClass.getParent().accessible) return null;
        return skriptClass.getFieldValue(fieldName);
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
