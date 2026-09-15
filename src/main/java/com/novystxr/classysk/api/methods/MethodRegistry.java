package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MethodRegistry {

    public record MethodIdentifier(
        String name,
        Class<?>[] argTypes,
        boolean isStatic
    ) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodIdentifier identifier)) return false;
            return name.equals(identifier.name) && Arrays.equals(argTypes, identifier.argTypes) && isStatic == identifier.isStatic;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(argTypes));
        }

        public static MethodIdentifier from(SkriptMethod method) {
            Class<?>[] argTypes = method.arguments.sequencedValues().stream()
                .map(MethodArgument::type)
                .toArray(Class[]::new);

            return new MethodIdentifier(method.name, argTypes, method.isStatic());
        }
    }

    private final Map<MethodIdentifier, SkriptMethod> registry = new HashMap<>();

    public @Nullable SkriptMethod getExactMethod(MethodIdentifier identifier) {
        return registry.get(identifier);
    }

    public List<SkriptMethod> candidates(MethodReference reference) {
        int refArgs = reference.args().size();

        List<SkriptMethod> result = new ArrayList<>();
        for (var entry : registry.entrySet()) {
            MethodIdentifier key = entry.getKey();
            SkriptMethod method = entry.getValue();

            if (!key.name.equals(reference.name()))
                continue;
            if (refArgs < method.minArgCount)
                continue;
            if (refArgs > key.argTypes.length)
                continue;
            if (reference.isStatic() != method.isStatic())
                continue;

            result.add(method);
        }
        return result;
    }

    public boolean registerMethod(SkriptMethod method) {
        return registry.putIfAbsent(MethodIdentifier.from(method), method) == null;
    }

}
