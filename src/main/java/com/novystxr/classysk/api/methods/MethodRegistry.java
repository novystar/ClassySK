package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class MethodRegistry {

    public record MethodIdentifier(
        String name,
        int minArgCount,
        Class<?>[] argTypes
    ) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodIdentifier(String otherName, int otherCount, Class<?>[] otherTypes))) return false;
            return minArgCount == otherCount && name.equals(otherName) && Arrays.equals(argTypes, otherTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, minArgCount, Arrays.hashCode(argTypes));
        }
    }

    private Map<MethodIdentifier, SkriptMethod> registry = new HashMap<>();

    public Stream<SkriptMethod> candidates(MethodReference reference) {
        final String refName = reference.name();
        final int refArgs = reference.args().size();

        return registry.entrySet().stream()
            .filter(entry -> entry.getKey().name.equals(refName))
            .filter(entry ->
                refArgs >= entry.getKey().minArgCount && refArgs <= entry.getKey().argTypes.length)
            .map(Entry::getValue);
    }

    public void init() {
        registry = new HashMap<>();
    }

    public boolean registerMethod(SkriptMethod method) {
        String name = method.signature.name();
        Collection<MethodArgument> args = method.signature.arguments().sequencedValues();

        Class<?>[] argTypes = args.stream()
            .map(MethodArgument::type)
            .toArray(Class[]::new);

        int minArgCount = Math.toIntExact(args.stream()
            .filter(arg -> arg.defaultValue() != null)
            .count());

        MethodIdentifier identifier = new MethodIdentifier(name, minArgCount, argTypes);
        return registry.putIfAbsent(identifier, method) == null;
    }

}
