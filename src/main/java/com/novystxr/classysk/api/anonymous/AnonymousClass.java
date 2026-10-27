package com.novystxr.classysk.api.anonymous;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;

public class AnonymousClass extends SkriptClass {

    public AnonymousClass(String name) {
        super(name, name, Modifier.none());
    }

    @Override
    public AnonymousInstance createInstance(Event event) {
        AnonymousInstance instance = AnonymousInstance.newInstance(name, this, event);
        instance.setDefaults();
        return instance;
    }

    @Override
    public Class<? extends AnonymousInstance> getSubclass() {
        return AnonymousInstance.getSubclass(name);
    }

    @Override
    public SkriptClass refresh() {
        return this;
    }
}
