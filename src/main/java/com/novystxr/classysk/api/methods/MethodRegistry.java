package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;

import java.util.*;

public class MethodRegistry {

    public record MethodIdentifier(
        String name,
        int minArgCount,
        Class<?>[] argTypes
    ) {
        @Override
        public boolean equals(Object o) {
            if (o instanceof MethodIdentifier that) {
                return minArgCount == that.minArgCount &&
                    Objects.equals(name, that.name) &&
                    Arrays.equals(argTypes, that.argTypes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, minArgCount, Arrays.hashCode(argTypes));
        }
    }

    private Map<MethodIdentifier, SkriptMethod> registry = new HashMap<>();

    public static List<SkriptMethod> getCandidatesFromChain(List<SkriptClass> chain, MethodReference reference) {
        Map<MethodIdentifier, SkriptMethod> result = new HashMap<>();
        for (SkriptClass target : chain.reversed())  {
            result.putAll(target.methodRegistry.getCandidates(reference));
        }

        return result.values().stream().toList();
    }

    public Map<MethodIdentifier, SkriptMethod> getCandidates(MethodReference reference) {
        Map<MethodIdentifier, SkriptMethod> result = new HashMap<>();

        for (var entry : registry.entrySet()) {
            MethodIdentifier identifier = entry.getKey();
            SkriptMethod method = entry.getValue();

            if (!identifier.name.equals(reference.name())) continue;

            int refArgCount = reference.args().size();

            if (refArgCount < identifier.minArgCount ||
                refArgCount > identifier.argTypes.length) continue;

            result.put(identifier, method);
        }
        return result;
    }

    public void init() {
        registry = new HashMap<>();
    }

    public boolean registerMethod(SkriptMethod method) {
        String name = method.signature.name();
        SequencedMap<String, MethodArgument> args = method.signature.arguments();
        Class<?>[] argTypes;

        int minArgCount = 0;
        if (args != null) {
            argTypes = new Class<?>[args.size()];
            int i = 0;

            for (MethodArgument arg : args.sequencedValues()) {
                argTypes[i] = arg.type();
                if (arg.defaultValue() == null) minArgCount++;
                i++;
            }
        } else {
            argTypes = new Class<?>[]{};
        }
        MethodIdentifier identifier = new MethodIdentifier(name, minArgCount, argTypes);

        SkriptMethod existing = registry.putIfAbsent(identifier, method);
        return (existing == null);
    }

}
