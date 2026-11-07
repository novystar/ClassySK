package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            return Objects.hash(name, isStatic, Arrays.hashCode(argTypes));
        }
    }

    private final Map<String, Map<MethodIdentifier, SkriptMethod>> registry = new HashMap<>();

    public @Nullable SkriptMethod getExactMethod(MethodIdentifier identifier) {
        Map<MethodIdentifier, SkriptMethod> map = registry.get(identifier.name);
        return map == null ? null : map.get(identifier);
    }

    public Stream<SkriptMethod> methodStream() {
        return registry.values().stream()
            .mapMulti((a, b) -> a.values().forEach(b));
    }

    public List<SkriptMethod> getOverloadCandidates(MethodReference reference) {
        List<SkriptMethod> candidates = new ArrayList<>();
        Map<?, SkriptMethod> map = registry.get(reference.name());
        if (map != null) {
            int refArgs = reference.args().length;
            for (SkriptMethod method : map.values()) {
                if (reference.isStatic() != method.isStatic()) continue;
                if (refArgs < method.minArgCount) continue;
                if (refArgs > method.maxArgCount) continue;
                candidates.add(method);
            }
        }
        return candidates;
    }

    public List<SkriptMethod> getAbstract() {
        return methodStream()
            .filter(method -> method.hasModifier(Modifier.ABSTRACT))
            .collect(Collectors.toList());
    }

    public boolean registerMethod(SkriptMethod method) {
        return registry.computeIfAbsent(method.name, (k) -> new HashMap<>())
            .putIfAbsent(method.identifier, method) == null;
    }

}
