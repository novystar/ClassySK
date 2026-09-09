package com.novystxr.classysk.api.classes;

import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;

public class AnonymousInstance extends ClassInstance {

    private final SkriptClass parent;

    private final Object variablesMap;

    public void setLocalVariables(Event event) {
        Variables.setLocalVariables(event, variablesMap);
    }

    public AnonymousInstance(String name, SkriptClass parent, Event event) {
        super(name);
        this.parent = parent;
        variablesMap = Variables.copyLocalVariables(event);
    }

    @Override
    public SkriptClass getParent() {
        return parent;
    }
}
