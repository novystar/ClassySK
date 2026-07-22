package com.novystxr.classysk.api.fields;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.AccessModifiable.AccessType;
import com.novystxr.classysk.api.AccessValidator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

public class FieldValidator extends AccessValidator<FieldSignature> {

    private final String fieldName;

    public FieldValidator(ErrorSource errorSource, SkriptClass contextClass, String fieldName) {
        super(errorSource, contextClass);
        this.fieldName = fieldName;
    }

    @Override
    protected boolean validate(FieldSignature signature, boolean isStatic, boolean isSameContext) {

        if (signature.accessType() == AccessType.PRIVATE && !isSameContext) {
            Skript.error("Private fields can't be accessed here");
            return false;
        }
        if (signature.isStatic() != isStatic) {
            Skript.error("Field accessed from improper context");
            return false;
        }
        return true;
    }

    @Override
    protected @Nullable FieldSignature getProductFromClass(SkriptClass skriptClass) {
        return getProductFromInstance(skriptClass);
    }

    @Override
    protected @Nullable FieldSignature getProductFromInstance(ClassInstance instance) {
        FieldSignature signature = instance.getFieldSignature(fieldName);
        if (signature == null) {
            Skript.error("Could not resolve field signature");
        }
        return signature;
    }
}
