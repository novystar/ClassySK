package com.novystxr.classysk.api.fields;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.AccessValidator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import static com.novystxr.classysk.api.AccessModifiable.AccessType.PRIVATE;
import static com.novystxr.classysk.api.AccessModifiable.AccessType.PROTECTED;

public class FieldValidator extends AccessValidator<FieldSignature> {

    private final String fieldName;

    public FieldValidator(ErrorSource errorSource, SkriptClass contextClass, String fieldName) {
        super(errorSource, contextClass);
        this.fieldName = fieldName;
    }

    @Override
    protected boolean validate(FieldSignature signature, ClassInstance targetClass) {

        if (signature.accessType() == PRIVATE && targetClass.getParent() != contextClass) {
            Skript.error("Private fields can only be accessed from the same class");
            return false;
        }
        if (signature.accessType() == PROTECTED && !contextClass.inherits(targetClass.getParent())) {
            Skript.error("Protected fields can only be accessed from inheritors of the original class");
            return false;
        }
        if (signature.isStatic() == targetClass.isInstance()) {
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
