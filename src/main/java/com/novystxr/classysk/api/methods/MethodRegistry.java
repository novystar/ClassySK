package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.AccessModifiable.Modifier;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

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

        public static MethodIdentifier of(SkriptMethod method) {
            String name = method.signature.name();

            SequencedMap<String, MethodArgument> args = method.signature.arguments();
            Class<?>[] argTypes = new Class<?>[args.size()];

            int i = 0;
            int minArgCount = 0;
            for (MethodArgument arg : args.sequencedValues()) {
                argTypes[i++] = arg.type();
                if (arg.defaultValue() == null)  {
                    minArgCount++;
                }
            }
            return new MethodIdentifier(name, minArgCount, argTypes);
        }
    }

    public boolean validateOverrides(@Nullable SkriptClass extendsClass) {
        for (var entry : registry.entrySet()) {
            MethodSignature signature = entry.getValue().signature;
            MethodIdentifier identifier = entry.getKey();

            if (extendsClass == null) {
                if (signature.modifier() == Modifier.OVERRIDE) {
                    Skript.error("This class does not extend any other");
                    return false;
                }
                continue;
            }
            MethodSignature overridden = extendsClass.inheritanceStream()
                .map(target -> target.methodRegistry.registry.get(identifier))
                .filter(Objects::nonNull)
                .map(method -> method.signature)
                .findFirst().orElse(null);

            if (signature.modifier() != Modifier.OVERRIDE) {
                if (overridden != null) {
                    Skript.error("Method '%s' would override a method from it's extending class. Mark it with 'override' or rename it.", signature.name());
                    return false;
                }
            } else if (overridden == null) {
                Skript.error("Method '%s' does not override any method from it's extending class.", signature.name());
                return false;
            } else if (signature.accessType().weight > overridden.accessType().weight) {
                Skript.error("Method '%s' cannot have a lower access-type than the target method.", signature.name());
                return false;
            } else if (signature.type() != overridden.type()) {
                Skript.error("Method '%s' does not match the return-type of the target method", signature.name());
                return false;
            }
        }
        return true;
    }

    private Map<MethodIdentifier, SkriptMethod> registry = new HashMap<>();

    public static List<SkriptMethod> getCandidatesFromChain(Stream<SkriptClass> chain, MethodReference reference) {
        Map<MethodIdentifier, SkriptMethod> result = new HashMap<>();
        for (SkriptClass target : chain.toList().reversed())  {
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
        return registry.putIfAbsent(MethodIdentifier.of(method), method) == null;
    }

}
