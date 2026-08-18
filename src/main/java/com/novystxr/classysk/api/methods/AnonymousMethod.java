package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.classes.SkriptClass;

public class AnonymousMethod extends SkriptMethod {

    public AnonymousMethod(MethodSignature signature, SkriptClass origin) {
        super(signature, origin.name);
        this.origin = origin;
    }

    private final SkriptClass origin;

    @Override
    public SkriptClass getOrigin() {
        return origin;
    }
}
