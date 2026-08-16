package com.novystxr.classysk.api.methods;

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

    public @Nullable SkriptMethod getExactMethod(MethodSignature signature) {
        if (signature == null) return null;
        SkriptMethod method = registry.get(MethodIdentifier.from(signature));
        if (method == null) return null;
        return method.signature.matches(signature) ? method : null;
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

}
