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

public class SkriptMethod implements AccessModifiable {

    @Override
    public boolean checkAccess(@Nullable SkriptClass contextClass) {
        return signature.accessType != AccessType.PRIVATE || contextClass == signature.parentClass;
    }

    @Override
    public boolean checkContext(boolean isStatic) {
        return signature.isStatic == isStatic;
    }

    public record MethodArgument(
            Class<?> type,

            @Nullable Expression<?> defaultValue,
            boolean isPlural

    ) {}

    public record MethodSignature(
        String name,
        @Nullable SequencedMap<String, MethodArgument> arguments,
        AccessType accessType,
        boolean isStatic,

        @Nullable Class<?> returnType,
        boolean returnPlural,
        SkriptClass parentClass

    ) {}

    public SkriptMethod(MethodSignature signature) {
        this.signature = signature;
    }

    private Trigger trigger;
    public final MethodSignature signature;

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Object @Nullable [] run(Event event, ClassInstance instance, @Nullable Map<String, Expression<?>> argExprs) {
        if (trigger == null) return null;
        Map<String, Object[]> args;

        if (argExprs != null) {
            args = new HashMap<>();
            for (Map.Entry<String, Expression<?>> entry : argExprs.entrySet()) {
                args.put(entry.getKey(), entry.getValue().getArray(event));
            }
        } else {
            args = null;
        }
        MethodRunEvent runEvent = new MethodRunEvent(instance);
        if (args != null && signature.arguments != null) {

            for (Entry<String, Object[]> arg : args.entrySet()) {
                Object[] values = arg.getValue();
                String key = arg.getKey();

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
        if (trigger.execute(runEvent)) {
            return runEvent.returnObject;
        }
        return null;
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
