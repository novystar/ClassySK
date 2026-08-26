package com.novystxr.classysk.api.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import static com.novystxr.classysk.api.Modifier.PRIVATE;
import static com.novystxr.classysk.api.Modifier.PROTECTED;

public class FieldValidator extends Validator<FieldSignature> {

    private final String fieldName;
    private final boolean isStatic;

    public FieldValidator(ErrorSource errorSource, SkriptClass contextClass, boolean isStatic, String fieldName) {
        super(errorSource, contextClass);
        this.isStatic = isStatic;
        this.fieldName = fieldName;
    }

    public FieldHolder getValidHolder(Event event, Expression<ClassInstance> instanceExpr, SkriptClass skriptClass) {
        if (isStatic) return skriptClass;
        return getValidInstance(event, instanceExpr, skriptClass);
    }

    @Override
    protected boolean validate(FieldSignature signature, SkriptClass contextClass) {
        SkriptClass origin = signature.getOrigin();

        if (signature.hasModifier(PRIVATE) && origin != contextClass) {
            Skript.error("Private fields can only be accessed from within their own class");
            return false;
        }
        if (signature.hasModifier(PROTECTED) && (contextClass == null || !contextClass.inherits(origin))) {
            Skript.error("Protected fields can only be accessed from inheritors");
            return false;
        }
        if (signature.isStatic() && !isStatic) {
            Skript.error("Static fields do not belong to any instance");
            return false;
        }
        if (!signature.isStatic() && isStatic) {
            Skript.error("This field is only accessible from instances");
            return false;
        }
        return true;
    }

    @Override
    protected @Nullable FieldSignature getProductFromClass(SkriptClass skriptClass) {
        return getProductFromHolder(skriptClass);
    }

    @Override
    protected @Nullable FieldSignature getProductFromInstance(ClassInstance instance) {
        return getProductFromHolder(instance);
    }

    private @Nullable FieldSignature getProductFromHolder(FieldHolder holder) {
        FieldSignature signature = holder.getFieldSignature(fieldName);
        if (signature == null) {
            Skript.error("Could not resolve field named %s", fieldName);
        }
        return signature;
    }
}
