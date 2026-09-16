package com.novystxr.classysk.api.anonymous;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;

public class AnonymousClass extends SkriptClass {

    public AnonymousClass(String name) {
        super(name, name, Modifier.none());
    }
    public ClassInstance createInstance(Event event) {
        return AnonymousInstance.newInstance(name, this, event);
    }

    @Override
    public ClassInstance createInstance() {
        throw new IllegalStateException();
    }
}
