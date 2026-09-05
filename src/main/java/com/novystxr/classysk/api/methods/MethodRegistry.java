package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
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

    public List<SkriptMethod> getAbstract() {
        return registry.values().stream()
            .filter(method -> method.hasModifier(Modifier.ABSTRACT))
            .collect(Collectors.toList());
    }

}
