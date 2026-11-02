package com.novystxr.classysk.api.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodParser.ReferenceArgument;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.main.elements.methods.ExprSuper;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import java.util.*;

import static com.novystxr.classysk.api.Modifier.*;

public class MethodValidator extends Validator<SkriptMethod> {

    private final MethodReference reference;
    private final boolean expectsReturn;
    private final boolean isSuper;

    private Expression<?>[] args;

    public @NotNull Expression<?>[] getArgs() {
        return args;
    }

    public MethodValidator(ErrorSource errorSource, SkriptClass contextClass, @NotNull MethodReference reference, boolean expectsReturn, boolean isSuper) {
        super(errorSource, contextClass);

        this.reference = reference;
        this.expectsReturn = expectsReturn;
        this.isSuper = isSuper;
    }

    @Override
    protected @Nullable SkriptMethod getProductFromClass(SkriptClass skriptClass) {
        if (isSuper) skriptClass = skriptClass.getExtends();

        SkriptMethod[] candidates = skriptClass.getCandidates(reference);
        if (candidates.length == 0) {
            Skript.error("Could not identify method %s", reference);
            return null;
        }
        SkriptMethod method;
        if (candidates.length == 1) {
             args = validateArgs(candidates[0].arguments, true);
             method = candidates[0];
        } else {
            method = resolveOverload(candidates);
            if (method == null) {
                Skript.error("Could not identify method %s out of %s overloads", reference, candidates.length);
                return null;
            }
        }
        return args == null ? null : method;
    }

    @Override
    protected @Nullable SkriptMethod getProductFromInstance(ClassInstance instance) {
        SkriptMethod product = product(); SkriptClass parent = parent();

        // full validation if exact method is not yet known, or it can't possibly assign to this instance's concrete type
        if (product == null || !parent.inherits(product.getOrigin())) {
            return getProductFromClass(parent);
        }
        // if we are at this validation stage the parent class isn't identical but still assignable
        // so we try to shortcut to the new method using information from the previous known one
        SkriptClass target = isSuper ? contextClass().getExtends() : parent;
        SkriptMethod method = target.getExactMethod(product.identifier);

        if (method == null) { // if that failed completely re-validate
            return getProductFromClass(parent);
        }

        return method;
    }

    @Override
    protected boolean validate(SkriptMethod method, SkriptClass contextClass) {
        SkriptClass origin = method.getOrigin();
        if (expectsReturn && method.type() == null) {
            Skript.error("This method can't return anything");
            return false;
        }
        if (method.hasModifier(PRIVATE) && contextClass != origin) {
            Skript.error("Private methods can only be accessed from within their own class");
            return false;
        }
        if (method.hasModifier(PROTECTED) && (contextClass == null || !contextClass.inherits(origin))) {
            Skript.error("Protected methods can only be accessed from inheritors");
            return false;
        }
        if (isSuper && method.hasModifier(ABSTRACT)) {
            Skript.error("You cannot call abstract methods directly");
            return false;
        }
        return true;
    }

    @Override
    public boolean validateExpression(Expression<ClassInstance> expr) {
        if (isSuper) expr = ExprSuper.newInstance();
        return expr != null && super.validateExpression(expr);
    }

    private @Nullable SkriptMethod resolveOverload(SkriptMethod[] candidates) {
        SkriptMethod overload = null;
        for (SkriptMethod candidate : candidates) {
            Expression<?>[] result = validateArgs(candidate.arguments, false);
            if (result != null) {
                // if already found a valid candidate and this is also valid we can't determine which one to target
                if (overload != null) return null;

                overload = candidate;
                args = result;
            }
        }
        return overload;
    }

    private @Nullable Expression<?>[] validateArgs(MethodArgument[] targetArgs, boolean printErrors) {
        List<String> targetNames = null;

        ReferenceArgument[] refArgs = reference.args();
        Expression<?>[] result = new Expression<?>[targetArgs.length];

        for (int i = 0; i < refArgs.length; i++) {
            int index = i;
            String name = refArgs[i].name();
            if (name != null && !targetArgs[i].name().equals(name)) {
                if (targetNames == null) {
                    targetNames = Arrays.stream(targetArgs).map(MethodArgument::name).toList();
                }
                index = targetNames.indexOf(name);
                if (index == -1) { // there are no arguments with this name
                    if (printErrors) Skript.error("Unknown argument '%s'", name);
                    return null;
                }
            }
            MethodArgument targetArg = targetArgs[index];
            if (result[index] != null) { // argument already populated means duplicate/order issue
                if (printErrors) Skript.error("Duplicate/out of order argument '%s'", targetArg.name());
                return null;
            }
            //noinspection unchecked
            Expression<?> expr = refArgs[i].expr().getConvertedExpression(targetArg.type());
            if (expr == null) {
                if (printErrors) Skript.error("Argument '%s' could not convert to required type '%s'", targetArg.name(), Classes.getSuperClassInfo(targetArg.type()));
                return null;
            }
            if (!expr.isSingle() && !targetArg.isPlural()) {
                if (printErrors) Skript.error("Argument '%s' only accepts single values", targetArg.name());
                return null;
            }
            result[index] = expr;
        }
        if (targetArgs.length != refArgs.length) { // fill in unpopulated defaults
            for (int i = 0; i < targetArgs.length; i++) {
                if (result[i] != null) continue;
                if (targetArgs[i].defaultValue() == null) {
                    if (printErrors) Skript.error("Could not resolve argument '%s'", targetArgs[i].name());
                    return null;
                }
                result[i] = targetArgs[i].defaultValue();
            }
        }
        return result;
    }
}
