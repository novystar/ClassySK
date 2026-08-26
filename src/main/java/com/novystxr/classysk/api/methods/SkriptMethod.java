package com.novystxr.classysk.api.methods;

import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.AnonymousInstance;
import com.novystxr.classysk.api.classes.*;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SkriptMethod implements AccessModifiable {

    public record MethodArgument(
        Class<?> type,

        @Nullable Expression<?> defaultValue,
        boolean isPlural
    ) {}

    public final String name;
    public Modifier[] modifiers;
    public final SequencedMap<String, MethodArgument> arguments;
    public final Class<?> type;
    public final boolean isPlural;

    public Trigger trigger;
    public String origin;
    public final int minArgCount;

    public SkriptMethod(String name, SequencedMap<String, MethodArgument> arguments, Modifier[] modifiers, Class<?> type, boolean isPlural, String origin) {
        this(name, arguments, modifiers, type, isPlural);
        this.origin = origin;

    }
    public SkriptMethod(String name, SequencedMap<String, MethodArgument> arguments, Modifier[] modifiers, Class<?> type, boolean isPlural) {
        this.name = name;
        this.arguments = arguments;
        this.modifiers = modifiers;
        this.type = type;
        this.isPlural = isPlural;

        this.minArgCount = arguments.values().stream()
            .filter(arg -> arg.defaultValue() == null)
            .mapToInt(arg -> 1).sum();

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

            if (arguments.get(key).isPlural()) {
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

    public SkriptClass getOrigin() {
        return ClassManager.getClass(origin);
    }

    public MethodIdentifier getIdentifier() {
        return MethodIdentifier.from(this);
    }

    @Override
    public Modifier[] modifiers() { return modifiers; }
    @Override
    public boolean isPlural() { return isPlural; }
    @Override
    public Class<?> type() { return type; }


    public static SkriptClass getContextClass(ParserInstance parser) {
        if (parser.getCurrentStructure() instanceof SectionSkriptEvent secSkriptEvent) {
            if (secSkriptEvent.getSection() instanceof SecMethod secMethod) {
                return secMethod.contextClass;
            }
        }
        return null;
    }

}
