package com.novystxr.classysk.api.anonymous;

import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.assignability.SubclassManager;
import org.bukkit.event.Event;

public class AnonymousInstance extends ClassInstance {

    private static final SubclassManager<ClassInstance, AnonymousInstance> anonymousClassManager =
        new SubclassManager<>("subclass.anonymous", String.class, SkriptClass.class, Event.class);

    public static AnonymousInstance newInstance(String name, SkriptClass parent, Event event) {
        return ClassManager.trackInstance( Classysk.TYPES_ALLOWED ?
            anonymousClassManager.newInstance(name, ClassInstance.getSubclass(name), name, parent, event) :
            new AnonymousInstance(name, parent, event));
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
