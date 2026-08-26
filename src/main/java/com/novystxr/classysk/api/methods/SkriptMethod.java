package com.novystxr.classysk.api.methods;

import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.AnonymousInstance;
import com.novystxr.classysk.api.classes.*;
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
        Modifier[] modifiers,

        @Nullable Class<?> type,
        boolean isPlural

    ) implements AccessModifiable {
        public boolean matches(MethodSignature signature) {
            return isPlural == signature.isPlural
                && type == signature.type
                && Arrays.equals(modifiers, signature.modifiers);
        }

        public MethodSignature withModifiers(Modifier... modifiers) {
            modifiers = Modifier.collect(modifiers);
            return new MethodSignature(name, arguments, modifiers, type, isPlural);
        }
    }

    public SkriptMethod(MethodSignature signature, String origin) {
        this(signature);
        this.origin = origin;
    }

    public SkriptMethod(MethodSignature signature) {
        this.signature = signature;
        this.minArgCount = signature.arguments.values().stream()
            .filter(arg -> arg.defaultValue() == null)
            .mapToInt(arg -> 1).sum();

    }
    private Trigger trigger;

    public final MethodSignature signature;
    public final int minArgCount;

    public String origin;

    public SkriptClass getOrigin() {
        return ClassManager.getClass(origin);
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Object @Nullable [] run(Event event, @Nullable ClassInstance instance, @NotNull Map<String, Expression<?>> args) {
        if (trigger == null) return null;
        MethodRunEvent runEvent = new MethodRunEvent(instance);
        if (this instanceof AnonymousMethod && instance != null) {
            ((AnonymousInstance) instance).setLocalVariables(runEvent);
        }
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

    public static SkriptClass getContextClass(ParserInstance parser) {
        if (parser.getCurrentStructure() instanceof SectionSkriptEvent secSkriptEvent) {
            if (secSkriptEvent.getSection() instanceof SecMethod secMethod) {
                return secMethod.contextClass;
            }
        }
        return null;
    }

}
