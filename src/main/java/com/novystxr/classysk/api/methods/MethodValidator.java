package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import com.novystxr.classysk.api.AccessModifiable;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodParser.ReferenceArgument;
import com.novystxr.classysk.api.methods.MethodValidator.ValidReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import java.util.*;

import static com.novystxr.classysk.api.Modifier.PRIVATE;
import static com.novystxr.classysk.api.Modifier.PROTECTED;

public class MethodValidator extends Validator<ValidReference> {

    private final MethodReference reference;
    private final boolean expectsReturn;
    private final boolean isSuper;

    public MethodValidator(ErrorSource errorSource, SkriptClass contextClass, @NotNull MethodReference reference, boolean expectsReturn, boolean isSuper) {
        super(errorSource, contextClass);
        this.reference = reference;
        this.expectsReturn = expectsReturn;
        this.isSuper = isSuper;
    }

    @Override
    protected @Nullable ValidReference getProductFromClass(SkriptClass skriptClass) {
        if (isSuper) skriptClass = skriptClass.getExtends();
        if (skriptClass == null) return null;

        List<SkriptMethod> candidates = skriptClass.getCandidates(reference);

        if (candidates.isEmpty()) {
            Skript.error("Could not identify method signature from reference: "+reference.name());
            return null;
        }
        if (candidates.size() == 1) {
            return validateReference(candidates.getFirst(), true);
        } else {
            ValidReference reference = validateFromCandidates(candidates);
            if (reference == null) {
                Skript.error("Could not identify method out of %s overloads", candidates.size());
                return null;
            }
            return reference;
        }
    }

    @Override
    protected @Nullable ValidReference getProductFromInstance(ClassInstance instance) {
        SkriptMethod method = product() == null ? null
            : instance.getExactMethod(product().method.signature, isSuper);
        if (method == null) {
            return getProductFromClass(instance.getParent());
        }
        return product().copy(method);
    }

    @Override
    protected boolean validate(ValidReference reference, boolean isStatic) {
        SkriptClass origin = reference.getOrigin();

        if (expectsReturn && reference.type() == null) {
            Skript.error("This method can't return anything");
            return false;
        }
        if (reference.accessType() == PRIVATE && origin != contextClass) {
            Skript.error("Private methods can only be accessed from within their own class");
            return false;
        }
        if (reference.hasModifier(PROTECTED) && (contextClass == null || !contextClass.inherits(origin))) {
            Skript.error("Protected fields can only be accessed from inheritors");
            return false;
        }
        if (reference.isStatic() && !isStatic) {
            Skript.error("Static methods do not belong to any instance");
            return false;
        }
        if (!reference.isStatic() && isStatic) {
            Skript.error("This method is only accessible from instances");
            return false;
        }
        return true;
    }

    private @Nullable ValidReference validateReference(SkriptMethod target, boolean printErrors) {
        MethodSignature signature = target.signature;
        SequencedMap<String, MethodArgument> arguments = signature.arguments();

        Map<String, Expression<?>> result = new HashMap<>();
        if (arguments.isEmpty()) {
            return new ValidReference(target, result);
        }
        List<String> argNames = new ArrayList<>(arguments.sequencedKeySet());

        boolean hasUnnamedArgs = false;
        boolean hasNamedArgs = false;

        int i = -1;
        for (ReferenceArgument arg : reference.args()) {
            i++;
            String name = arg.name();
            if (name == null) {
                name = argNames.get(i);
                hasUnnamedArgs = true;
            } else {
                if (!arguments.containsKey(name)) {
                    if (printErrors) Skript.error("Unknown method argument: "+name);
                    return null;
                }
                hasNamedArgs = true;
            }
            MethodArgument targetArg = arguments.get(name);
            Class<?> toClass = targetArg.type();

            //noinspection unchecked
            Expression<?> convertedExpr = arg.expr().getConvertedExpression(toClass);
            if (convertedExpr == null) {
                if (printErrors) Skript.error("Argument '%s' is not of required type: %s", name, Classes.getExactClassName(toClass));
                return null;
            }
            if (!convertedExpr.isSingle() && !targetArg.isPlural()) {
                if (printErrors) Skript.error("Argument '%s' is single but given expression is plural", name);
                return null;
            }

            if (result.putIfAbsent(name, convertedExpr) != null) {
                if (printErrors) Skript.error("Reference contains duplicate arguments");
                return null;
            }
        }
        List<String> referenceArgNames = new ArrayList<>(result.keySet());

        if (hasNamedArgs && hasUnnamedArgs) {
            i = -1; // check mixed arguments order
            for (String name : argNames) {
                i++;
                if (!referenceArgNames.get(i).equals(name)) {
                    Skript.error("Mixed method arguments must be in order");
                    return null;
                }
            }
        }
        // attempt to resolve remaining defaults
        for (var entry : arguments.entrySet()) {
            String name = entry.getKey();
            if (referenceArgNames.contains(name)) continue;

            Expression<?> defaultValue = entry.getValue().defaultValue();
            if (defaultValue == null) {
                Skript.error("Could not resolve some argument(s) for this method call");
                return null;
            }
            result.put(name, defaultValue);
        }
        return new ValidReference(target, result);
    }

    private @Nullable ValidReference validateFromCandidates(List<SkriptMethod> candidates) {
        ValidReference reference = null;
        for (SkriptMethod candidate : candidates) {
            var result = validateReference(candidate, false);
            if (result == null) {
                continue;
            }
            if (reference != null) {
                return null;
            }
            reference = result;
        }
        return reference;
    }

    public record ValidReference(@NotNull SkriptMethod method, @NotNull Map<String, Expression<?>> args) implements AccessModifiable {
        @Override
        public boolean isPlural() {
            return method.signature.isPlural();
        }
        @Override
        public Modifier[] modifiers() {
            return method.signature.modifiers();
        }
        @Override
        public Class<?> type() {
            return method.signature.type();
        }

        public SkriptClass getOrigin() {
            return method.getOrigin();
        }

        public ValidReference copy(SkriptMethod method) {
            return new ValidReference(method, args);
        }
    }
}
