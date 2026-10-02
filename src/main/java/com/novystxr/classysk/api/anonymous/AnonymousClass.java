package com.novystxr.classysk.api.anonymous;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;

public class AnonymousClass extends SkriptClass {

    public AnonymousClass(String name) {
        super(name, name, Modifier.none());
    }

    public AnonymousInstance createInstance(Event event) {
        return AnonymousInstance.newInstance(name, this, event);
    }

    @Override
    public SkriptClass refresh() {
        return this;
    }
}
