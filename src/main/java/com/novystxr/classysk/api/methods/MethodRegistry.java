package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class MethodRegistry {

    public record MethodIdentifier(
        String name,
        Class<?>[] argTypes
    ) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodIdentifier identifier)) return false;
            return name.equals(identifier.name) && Arrays.equals(argTypes, identifier.argTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(argTypes));
        }

        public static MethodIdentifier from(SkriptMethod method) {
            Class<?>[] argTypes = method.arguments.sequencedValues().stream()
                .map(MethodArgument::type)
                .toArray(Class[]::new);

            return new MethodIdentifier(method.name, argTypes);
        }
    }

    private Map<MethodIdentifier, SkriptMethod> registry = new HashMap<>();

    public @Nullable SkriptMethod getExactMethod(MethodIdentifier identifier) {
        return registry.get(identifier);
    }

    public Map<MethodIdentifier, SkriptMethod> candidates(MethodReference reference, boolean isStatic) {
        int refArgs = reference.args().size();

        Map<MethodIdentifier, SkriptMethod> result = new HashMap<>();
        for (var entry : registry.entrySet()) {
            MethodIdentifier key = entry.getKey();
            SkriptMethod method = entry.getValue();

            if (!key.name.equals(reference.name()))
                continue;
            if (refArgs < method.minArgCount)
                continue;
            if (refArgs > key.argTypes.length)
                continue;
            if (isStatic != method.isStatic())
                continue;

            result.put(key, method);
        }
        return result;
    }

    public void init() {
        registry = new HashMap<>();
    }

    public boolean registerMethod(SkriptMethod method) {
        return registry.putIfAbsent(MethodIdentifier.from(method), method) == null;
    }

    public boolean hasAbstract() {
        return registry.values().stream()
            .anyMatch(method -> method.hasModifier(Modifier.ABSTRACT));
    }

    public static boolean validateOverride(@NotNull SkriptMethod method, @Nullable SkriptMethod target) {
        if (target == null) {
            if (method.hasModifier(Modifier.OVERRIDE)) {
                Skript.error("Method '%s' does not override any method from its extending class.", method.name);
                return false;
            }
            return true;
        }
        method.origin = target.origin; // inherit origin from original method

        if (!method.hasModifier(Modifier.OVERRIDE) && !method.hasModifier(Modifier.ABSTRACT)) {
            Skript.error("Method '%s' would override a method from its extending class. Mark it with 'override' or rename it.", method.name);
            return false;
        } else if (method.hasModifier(Modifier.ABSTRACT) && !target.hasModifier(Modifier.ABSTRACT)) {
            Skript.error("Method '%s' already exists as a concrete method, so it cannot be re-declared as 'abstract'.", target.name);
            return false;
        } else if (target.hasAnyModifier(Modifier.FINAL, Modifier.PRIVATE)) {
            Skript.error("Method '%s' would override a method that is final.", method.name);
            return false;
        } else if (method.accessType().ordinal() > target.accessType().ordinal()) {
            Skript.error("Method '%s' cannot have a lower access-type than the target method.", method.name);
            return false;
        } else if (method.type() != target.type()) {
            Skript.error("Method '%s' does not match the return-type of the target method.", method.name);
            return false;
        }
        return true;
    }

    public boolean validateOverrides(@Nullable SkriptClass extendsClass) {

        List<SkriptMethod> abstractMethods = extendsClass != null ? extendsClass.methodRegistry.registry.values().stream()
            .filter(method -> method.hasModifier(Modifier.ABSTRACT))
            .collect(Collectors.toList()) : new ArrayList<>();

        for (var entry : registry.entrySet()) {
            SkriptMethod method = entry.getValue();
            MethodIdentifier identifier = entry.getKey();

            if (extendsClass == null) {
                if (method.hasModifier(Modifier.OVERRIDE)) {
                    Skript.error("This class does not extend any other");
                    return false;
                }
            } else {
                SkriptMethod overridden = extendsClass.getExactMethod(identifier, false);
                abstractMethods.remove(overridden);

                if (!validateOverride(method, overridden)) {
                    return false;
                }
            }
        }
        if (!abstractMethods.isEmpty()) {
            Skript.error("The target class contains abstract methods that have not been implemented by this subclass. Implement them or re-declare them as 'abstract' on this class.");
            return false;
        }

        return true;
    }

}
