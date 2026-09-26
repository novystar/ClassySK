package com.novystxr.classysk.api.anonymous;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.methods.MethodEvent;
import com.novystxr.classysk.api.methods.SkriptMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AnonymousMethod extends SkriptMethod {
    public AnonymousMethod(SkriptMethod method) {
        super(method.name, method.arguments, method.modifiers, method.type, method.isPlural, method.origin);
    }

    @Override
    public Object @Nullable [] run(Event contextEvent, MethodEvent runEvent, @NotNull Map<String, Expression<?>> args) {
        // assume not null because anonymous methods can't be static
        runEvent.instance.getData(AnonymousData.class).setLocalVariables(runEvent);
        return super.run(contextEvent, runEvent, args);
    }
}
