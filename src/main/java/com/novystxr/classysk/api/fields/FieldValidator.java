package com.novystxr.classysk.api.fields;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import static com.novystxr.classysk.api.Modifier.PRIVATE;

public class FieldValidator extends Validator<SkriptField> {

    private final String fieldName;

    public FieldValidator(ErrorSource errorSource, SkriptClass contextClass, String fieldName) {
        super(errorSource, contextClass);
        this.fieldName = fieldName;
    }

    @Override
    protected boolean validate(SkriptField field, boolean isStatic, SkriptClass target) {
        if (field.accessType() == PRIVATE && target != contextClass) {
            Skript.error("Private fields can only be accessed from within their own class");
            return false;
        }
        if (field.isStatic() && !isStatic) {
            Skript.error("Static fields do not belong to any instance");
            return false;
        }
        if (!field.isStatic() && isStatic) {
            Skript.error("This field is only accessible from instances");
            return false;
        }
        return true;
    }

    @Override
    protected @Nullable SkriptField getProductFromClass(SkriptClass skriptClass) {
        return getProductFromHolder(skriptClass);
    }

    @Override
    protected @Nullable SkriptField getProductFromInstance(ClassInstance instance) {
        return getProductFromHolder(instance);
    }

    private @Nullable SkriptField getProductFromHolder(FieldHolder holder) {
        SkriptField field = holder.getField(fieldName);
        if (field == null) {
            Skript.error("Could not resolve field '%s'", fieldName);
        }
        return field;
    }
}
