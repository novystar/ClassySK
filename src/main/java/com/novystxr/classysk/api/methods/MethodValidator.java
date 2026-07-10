package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import com.novystxr.classysk.api.AccessValidator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodParser.ReferenceArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import java.util.*;

public class MethodValidator extends AccessValidator<SkriptMethod> {

    private final MethodReference reference;
    private final boolean expectsReturn;

    private Map<String, Expression<?>> validatedArgs;

    public MethodValidator(ErrorSource errorSource, SkriptClass contextClass, @NotNull MethodReference reference, boolean expectsReturn) {
        super(errorSource, contextClass);
        this.reference = reference;
        this.expectsReturn = expectsReturn;
    }

    public Map<String, Expression<?>> getValidatedArgs() {
        return validatedArgs;
    }

    @Override
    protected @Nullable SkriptMethod getProductFromClass(SkriptClass skriptClass) {
        List<SkriptMethod> candidates = skriptClass.methodRegistry.getCandidates(reference);
        if (candidates.isEmpty()) {
            Skript.error("Could not identify method signature from reference: "+reference.name());
            return null;
        }
        SkriptMethod method;

        if (candidates.size() == 1) {
            method = candidates.getFirst();
            if (!validateReference(method, true)) {
                return null;
            }

        } else {
            List<SkriptMethod> valid = new ArrayList<>();
            for (SkriptMethod candidate : candidates) {
                if (validateReference(candidate, false)) valid.add(candidate);
            }
            if (valid.size() != 1) {
                Skript.error("Could not identify method out of %s overloads", candidates.size());
                return null;
            }
            method = valid.getFirst();
        }
        return method;
    }

    @Override
    protected @Nullable SkriptMethod getProductFromInstance(ClassInstance instance) {
        return getProductFromClass(instance.getParent());
    }

    @Override
    public String productName() {
        return "method";
    }

    @Override
    protected boolean validate(SkriptMethod method, boolean isStatic, boolean isSameContext) {
        if (method.accessType().isPrivate() && !isSameContext) {
            Skript.error("This method can't be accessed here");
            return false;
        }
        if (method.isStatic() != isSameContext) {
            Skript.error("Method accessed from improper context");
            return false;
        }
        if (expectsReturn && method.signature.returnType() == null) {
            Skript.error("This method can't return anything");
            return false;
        }
        return true;
    }

    private boolean validateReference(SkriptMethod target, boolean printErrors) {
        MethodSignature signature = target.signature;

        SequencedMap<String, MethodArgument> arguments = signature.arguments();
        Map<String, Expression<?>> result = null;

        if (arguments != null) {
            result = new HashMap<>();
            List<String> argNames = new ArrayList<>(arguments.sequencedKeySet());
            int i = -1;

            boolean hasUnnamedArgs = false;
            boolean hasNamedArgs = false;

            for (ReferenceArgument arg : reference.args()) {
                i++;
                String name = arg.name();
                if (name == null) {
                    name = argNames.get(i);
                    hasUnnamedArgs = true;
                } else {
                    if (!signature.arguments().containsKey(name)) {
                        if (printErrors) Skript.error("Unknown method argument: "+name);
                        return false;
                    }
                    hasNamedArgs = true;
                }
                MethodArgument targetArg = arguments.get(name);
                Class<?> toClass = targetArg.type();

                //noinspection unchecked
                Expression<?> convertedExpr = arg.expr().getConvertedExpression(toClass);
                if (convertedExpr == null) {
                    if (printErrors) Skript.error("Argument '%s' is not of required type: %s", name, Classes.getExactClassName(toClass));
                    return false;
                }
                if (!convertedExpr.isSingle() && !targetArg.isPlural()) {
                    if (printErrors) Skript.error("Argument '%s' is single but given expression is plural", name);
                    return false;
                }

                if (result.putIfAbsent(name, convertedExpr) != null) {
                    if (printErrors) Skript.error("Reference contains duplicate arguments");
                    return false;
                }
            }
            List<String> referenceArgNames = new ArrayList<>(result.keySet());

            if (hasNamedArgs && hasUnnamedArgs) {
                i = -1; // check mixed arguments order
                for (String name : argNames) {
                    i++;
                    if (!referenceArgNames.get(i).equals(name)) {
                        Skript.error("Mixed method arguments must be in order");
                        return false;
                    }
                }
            }
            // attempt to resolve remaining defaults
            for (var entry : signature.arguments().entrySet()) {
                String name = entry.getKey();
                if (referenceArgNames.contains(name)) continue;

                Expression<?> defaultValue = entry.getValue().defaultValue();
                if (defaultValue == null) {
                    Skript.error("Could not resolve some argument(s) for this method call");
                    return false;
                }
                result.put(name, defaultValue);
            }
        }
        this.validatedArgs = result;
        return true;
    }
}
