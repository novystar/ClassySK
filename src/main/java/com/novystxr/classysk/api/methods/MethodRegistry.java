package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MethodRegistry {

    public record MethodIdentifier(
        String name,
        int minArgCount,
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
            Collection<MethodArgument> args = signature.arguments().sequencedValues();

            Class<?>[] argTypes = args.stream()
                .map(MethodArgument::type)
                .toArray(Class[]::new);

            int minArgCount = args.stream()
                .filter(arg -> arg.defaultValue() == null)
                .mapToInt(arg -> 1).sum();

            return new MethodIdentifier(name, minArgCount, argTypes);
        }
    }

    private Map<MethodIdentifier, SkriptMethod> registry = new HashMap<>();

    public @Nullable SkriptMethod getExactMethod(MethodIdentifier identifier) {
        return registry.get(identifier);
    }

    public Map<MethodIdentifier, SkriptMethod> candidates(MethodReference reference) {
        final String refName = reference.name();
        final int refArgs = reference.args().size();

        return registry.entrySet().stream()
            .filter(entry -> entry.getKey().name.equals(refName))
            .filter(entry ->
                refArgs >= entry.getKey().minArgCount && refArgs <= entry.getKey().argTypes.length)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public void init() {
        registry = new HashMap<>();
    }

    public boolean registerMethod(SkriptMethod method) {
        return registry.putIfAbsent(MethodIdentifier.from(method.signature), method) == null;
    }

    public static boolean validateOverride(MethodSignature signature, MethodSignature overridden) {
        if (overridden == null) {
            Skript.error("Method '%s' does not override any method from it's extending class.", signature.name());
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
        for (var entry : registry.entrySet()) {
            MethodSignature signature = entry.getValue().signature;
            MethodIdentifier identifier = entry.getKey();

            if (extendsClass == null) {
                if (signature.hasModifier(Modifier.OVERRIDE)) {
                    Skript.error("This class does not extend any other");
                    return false;
                }
                continue;
            }
            SkriptMethod overridden = extendsClass.getExactMethod(identifier, false);

            if (!signature.hasModifier(Modifier.OVERRIDE)) {
                if (overridden != null) {
                    Skript.error("Method '%s' would override a method from it's extending class. Mark it with 'override' or rename it.", signature.name());
                    return false;
                }
                return true;
            }

            return validateOverride(signature, overridden.signature);
        }
        return true;
    }

}
