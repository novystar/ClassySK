package com.novystxr.classysk.api.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.classes.ClassInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class FieldValidator {

    String fieldName;
    ClassInstance instance;

    Kleenean isValid = Kleenean.UNKNOWN;
    FieldSignature signature;
    boolean isStatic;

    public FieldValidator(String fieldName, boolean isStatic) {
        this.fieldName = fieldName;
        this.isStatic = isStatic;
    }

    public void validate(@Nullable ClassInstance instance) {
        if (instance == null) {
            Skript.error("Illegal Access! This class does not exist");
            isValid = Kleenean.FALSE; return;
        }
        SkriptClass parentClass = instance.getParent();
        if (parentClass == null) {
            Skript.error("Class structure of this instance no longer exists");
            isValid = Kleenean.FALSE; return;
        }
        signature = parentClass.getFieldSignature(fieldName);

        if (signature == null) {
            Skript.error("Unable to resolve field signature '%s'", SkriptField.getEffectiveName(instance, fieldName));
            isValid = Kleenean.FALSE; return;
        }
        this.instance = instance;
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
        Skript.error("Illegal Access! Tried to access non-existent field '%s' or tried to access it from improper context", SkriptField.getEffectiveName(instance, fieldName));
    }

    public void updateInstance(@Nullable Expression<ClassInstance> skriptClassExpr, Event event) {
        // static access
        if (skriptClassExpr == null) {
            return;
        }
        ClassInstance newClass = skriptClassExpr.getSingle(event);
        if (newClass == null) {
            isValid = Kleenean.FALSE;
            return;
        }
        if (this.instance == null || newClass.getParent() != this.instance.getParent()) {
            validate(newClass);
            checkAccess(event);
        }
    }

    public void attemptSetValue(@Nullable Object[] delta) {
        if (delta == null) return;

        if (signature.canConvert(delta)) {
            SkriptField field = instance.getField(fieldName);
            if (field == null) return;
            field.setValue(delta);
        }
    }

    public Object[] get() {
        return instance.getFieldValue(fieldName);
    }

    public String fieldName() {
        return fieldName;
    }
    public FieldSignature signature() {
        return signature;
    }
    public ClassInstance instance() {
        return instance;
    }
    public Kleenean isValid() {
        return isValid;
    }

}
