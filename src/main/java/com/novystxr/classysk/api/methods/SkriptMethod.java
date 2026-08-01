package com.novystxr.classysk.api.methods;

import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable.AccessType;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
        boolean isStatic,

        @Nullable Class<?> returnType,
        boolean returnPlural

    ) {}

    public SkriptMethod(MethodSignature signature) {
        this.signature = signature;
    }

    private Trigger trigger;
    public final MethodSignature signature;

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Object @Nullable [] run(Event event, @Nullable ClassInstance instance, @NotNull Map<String, Expression<?>> args) {
        if (trigger == null) return null;
        MethodRunEvent runEvent = new MethodRunEvent(instance);
        for (var entry : args.entrySet()) {
            Expression<?> expr = entry.getValue();
            String key = entry.getKey();

            if (signature.arguments.get(key).isPlural()) {
                Object[] values = expr.getArray(event);
                String[] keys = KeyProviderExpression.areKeysRecommended(expr) ?
                    ((KeyProviderExpression<?>) expr).getArrayKeys(event) : null;
                KeyedValue<?>[] keyedValues = KeyedValue.zip(values, keys);

                for (KeyedValue<?> keyedValue : keyedValues) {
                    Variables.setVariable(key+"::"+keyedValue.key(), keyedValue.value(), runEvent, true);
                }
            } else {
                Variables.setVariable(key, expr.getSingle(event), runEvent, true);
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
