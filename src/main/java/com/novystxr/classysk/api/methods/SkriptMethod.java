package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.util.DefaultValue;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SkriptMethod implements AccessModifiable {

    public record MethodArgument(
        String name,
        Class<?> type,

        @Nullable DefaultValue<?> defaultValue,
        boolean isPlural
    ) {}

    public final String name;
    public Modifier[] modifiers;
    public final MethodArgument[] arguments;
    public final Class<?> type;
    public final boolean isPlural;

    public Trigger trigger;
    public String origin;

    public final int minArgCount;
    public final int maxArgCount;

    public final MethodIdentifier identifier;

    public SkriptMethod(String name, MethodArgument[] arguments, Modifier[] modifiers, Class<?> type, boolean isPlural, String origin) {
        this(name, arguments, modifiers, type, isPlural);
        this.origin = origin;

    }

    public SkriptMethod(String name, MethodArgument[] arguments, Modifier[] modifiers, Class<?> type, boolean isPlural) {
        this.name = name;
        this.arguments = arguments;
        this.modifiers = modifiers;
        this.type = type;
        this.isPlural = isPlural;

        Class<?>[] argTypes = Arrays.stream(arguments)
            .map(MethodArgument::type)
            .toArray(Class[]::new);

        this.identifier = new MethodIdentifier(name, argTypes, isStatic());
        this.maxArgCount = identifier.argTypes().length;
        this.minArgCount = Arrays.stream(arguments)
            .filter(arg -> arg.defaultValue() == null)
            .mapToInt(arg -> 1).sum();

    }

    public MethodEvent run(Event contextEvent, MethodEvent methodEvent, @NotNull Expression<?>[] args) {
        if (trigger == null) return null;
        for (int i = 0; i < args.length; i++) {
            MethodArgument arg = this.arguments[i];
            Expression<?> expr = args[i];
            String key = arg.name;

            if (arg.isPlural) {
                Object[] values = expr.getArray(contextEvent);
                String[] keys = KeyProviderExpression.areKeysRecommended(expr) ?
                    ((KeyProviderExpression<?>) expr).getArrayKeys(contextEvent) : null;
                KeyedValue<?>[] keyedValues = KeyedValue.zip(values, keys);

                for (KeyedValue<?> keyedValue : keyedValues) {
                    Variables.setVariable(key+"::"+keyedValue.key(), keyedValue.value(), methodEvent, true);
                }
            } else {
                Variables.setVariable(key, expr.getSingle(contextEvent), methodEvent, true);
            }
        }
        return trigger.execute(methodEvent) ? methodEvent : null;
    }

    public SkriptClass getOrigin() {
        return ClassManager.getClass(origin);
    }

    public boolean validateOverride(@NotNull SkriptMethod target) {
        origin = target.origin;
        if (hasModifier(Modifier.ABSTRACT) && !target.hasModifier(Modifier.ABSTRACT)) {
            Skript.error("The method this would re-declare is concrete.");
            return false;
        }
        if (!target.hasModifier(Modifier.ABSTRACT) && !hasModifier(Modifier.OVERRIDE)) {
            Skript.error("This would override a method from the super class. Mark it with 'override'.");
            return false;
        }
        if (target.hasAnyModifier(Modifier.FINAL, Modifier.PRIVATE)) {
            Skript.error("This method cannot be overridden");
            return false;
        }
        if (accessType().ordinal() > target.accessType().ordinal()) {
            Skript.error("This override declared a lower access-type than the target.");
            return false;
        }
        if (type() != target.type()) {
            Skript.error("The return type of this override does not match the target.");
            return false;
        }
        if (!Arrays.equals(arguments, target.arguments)) {
            Skript.error("Argument names must match when overriding a method.");
            return false;
        }
        return true;
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
