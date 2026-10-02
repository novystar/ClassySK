package com.novystxr.classysk.api.anonymous;

import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.assignability.SubclassManager;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;

public class AnonymousInstance extends ClassInstance {

    private static final SubclassManager<AnonymousInstance> subclassManager =
        new SubclassManager<>(AnonymousInstance.class, "anonymous", String.class, SkriptClass.class, Event.class);

    public static AnonymousInstance newInstance(String name, SkriptClass parent, Event event) {
        return subclassManager.newInstance(name, name, parent, event);
    }

    public AnonymousInstance(String name, SkriptClass parent, Event event) {
        super(name);
        this.parent = parent;
        variablesMap = Variables.copyLocalVariables(event);
    }

    private final SkriptClass parent;
    private final Object variablesMap;

    public void setLocalVariables(Event event) {
        Variables.setLocalVariables(event, variablesMap);
    }

    @Override
    public SkriptClass getParent() {
        return parent;
    }
}
