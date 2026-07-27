package com.novystxr.classysk.api.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SectionSkriptEvent;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class SkriptMethod {

    public record MethodArgument(
        Class<?> type,

        @Nullable Expression<?> defaultValue,
        boolean isPlural
    ) {}

    public record MethodSignature(
        String name,
        SequencedMap<String, MethodArgument> arguments,
        AccessType accessType,

        @Nullable Class<?> type,
        boolean isPlural,
        @Nullable Modifier modifier

    ) implements AccessModifiable {}

    public SkriptMethod(MethodSignature signature) {
        this.signature = signature;
    }

    private Trigger trigger;
    public final MethodSignature signature;

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Object @Nullable [] run(Event event, ClassInstance instance, @Nullable Map<String, Expression<?>> args) {
        if (trigger == null) {
            return null;
        }
        MethodRunEvent runEvent = new MethodRunEvent(instance);
        if (args != null) {
            for (Entry<String, Expression<?>> arg : args.entrySet()) {
                Object[] values = arg.getValue().getArray(event);
                String key = arg.getKey();

                if (values.length == 0) {
                    continue;
                }
                if (signature.arguments.get(key).isPlural) {
                    int i = 0;
                    for (Object value : values) {
                        i++;
                        Variables.setVariable(key+"::"+i, value, runEvent, true);
                    }
                } else {
                    Variables.setVariable(key, values[0], runEvent, true);
                }
            }
        }
        return trigger.execute(runEvent) ? runEvent.returnObject : null;
    }

    public static boolean isMethodBody(ParserInstance parser) {
        if (parser.getCurrentStructure() instanceof SectionSkriptEvent secSkriptEvent) {
            return secSkriptEvent.getSection() instanceof SecMethod;
        }
        return false;
    }

    public static @Nullable SkriptClass getContextClass(ParserInstance parser) {
        if (parser.getCurrentStructure() instanceof SectionSkriptEvent secSkriptEvent) {
            if (secSkriptEvent.getSection() instanceof SecMethod secMethod) {
                return secMethod.contextClass;
            }
        }
        return null;
    }

}
