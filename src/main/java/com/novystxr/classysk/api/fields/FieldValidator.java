package com.novystxr.classysk.api.fields;

import com.novystxr.classysk.api.AccessValidator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.log.runtime.ErrorSource;

public class FieldValidator extends AccessValidator<FieldSignature> {

    private final String fieldName;

    public FieldValidator(ErrorSource errorSource, SkriptClass contextClass, String fieldName) {
        super(errorSource, contextClass);
        this.fieldName = fieldName;
    }

    @Override
    protected boolean validate(@NotNull ClassInstance instance) {
        this.signature = instance.getFieldSignature(fieldName);

        if (signature == null) {
            error("Could not resolve field signature");
            return false;
        }
        if (!signature.checkAccess(contextClass)) {
            error("This field can't be accessed here");
            return false;
        }
        if (!signature.checkContext(isStatic)) {
            error("Field accessed from improper context");
            return false;
        }
        return true;
    }
}
