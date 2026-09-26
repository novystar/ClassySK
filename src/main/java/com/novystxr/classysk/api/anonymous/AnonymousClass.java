package com.novystxr.classysk.api.anonymous;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;

public class AnonymousClass extends SkriptClass {

    public AnonymousClass(String name) {
        super(name, name, Modifier.none());
    }

    @Override
    public SkriptClass refresh() {
        return this;
    }
}
