package com.novystxr.classysk.api.anonymous;

import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.bukkit.event.Event;

public class AnonymousData extends ClassInstance.InstanceData {

    public AnonymousData(ClassInstance instance, SkriptClass parent, Event event) {
        instance.super();
        this.parent = parent;
        variablesMap = Variables.copyLocalVariables(event);
    }

    private final SkriptClass parent;
    private final Object variablesMap;

    public void setLocalVariables(Event event) {
        Variables.setLocalVariables(event, variablesMap);
    }

    @Override
    public SkriptClass getDataClass() {
        return parent;
    }
}
