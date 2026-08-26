package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
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

        public static MethodIdentifier from(MethodSignature signature) {
            String name = signature.name();

            Class<?>[] argTypes = signature.arguments().sequencedValues().stream()
                .map(MethodArgument::type)
                .toArray(Class[]::new);

            return new MethodIdentifier(name, argTypes);
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
            if (isStatic != method.signature.isStatic())
                continue;

            result.put(key, method);
        }
        return result;
    }

    public void init() {
        registry = new HashMap<>();
    }

    public boolean registerMethod(SkriptMethod method) {
        return registry.putIfAbsent(MethodIdentifier.from(method.signature), method) == null;
    }

    public boolean hasAbstract() {
        return registry.values().stream()
            .anyMatch(method -> method.signature.hasModifier(Modifier.ABSTRACT));
    }

    public static boolean validateOverride(@NotNull SkriptMethod method, @Nullable SkriptMethod target) {
        MethodSignature signature = method.signature;

        if (target == null) {
            if (signature.hasModifier(Modifier.OVERRIDE)) {
                Skript.error("Method '%s' does not override any method from it's extending class.", signature.name());
                return false;
            }
            return true;
        }
        MethodSignature overridden = target.signature;
        method.origin = target.origin; // inherit origin from original method

        if (!signature.hasModifier(Modifier.OVERRIDE) && !signature.hasModifier(Modifier.ABSTRACT)) {
            Skript.error("Method '%s' would override a method from it's extending class. Mark it with 'override' or rename it.", signature.name());
            return false;
        } else if (signature.hasModifier(Modifier.ABSTRACT) && !overridden.hasModifier(Modifier.ABSTRACT)) {
            Skript.error("Method '%s' already exists as a concrete method, so it cannot be re-declared as 'abstract'.", overridden.name());
            return false;
        } else if (overridden.hasAnyModifier(Modifier.FINAL, Modifier.PRIVATE)) {
            Skript.error("Method '%s' would override a method that is final.", signature.name());
            return false;
        } else if (signature.accessType().ordinal() > overridden.accessType().ordinal()) {
            Skript.error("Method '%s' cannot have a lower access-type than the target method.", signature.name());
            return false;
        } else if (signature.type() != overridden.type()) {
            Skript.error("Method '%s' does not match the return-type of the target method.", signature.name());
            return false;
        }
        return true;
    }

    public boolean validateOverrides(@Nullable SkriptClass extendsClass) {

        List<SkriptMethod> abstractMethods = extendsClass != null ? extendsClass.methodRegistry.registry.values().stream()
            .filter(method -> method.signature.hasModifier(Modifier.ABSTRACT))
            .collect(Collectors.toList()) : new ArrayList<>();

        for (var entry : registry.entrySet()) {
            SkriptMethod method = entry.getValue();
            MethodIdentifier identifier = entry.getKey();

            if (extendsClass == null) {
                if (method.signature.hasModifier(Modifier.OVERRIDE)) {
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
