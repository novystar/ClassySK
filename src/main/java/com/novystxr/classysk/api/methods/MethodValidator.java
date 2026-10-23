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
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.MethodValidator.ValidReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodArgument;
import com.novystxr.classysk.main.elements.methods.ExprSuper;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

import java.util.*;

import static com.novystxr.classysk.api.Modifier.*;

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

        List<SkriptMethod> candidates = skriptClass.getCandidates(reference);
        if (candidates.isEmpty()) {
            Skript.error("Could not identify method %s", reference);
            return null;
        }
        if (candidates.size() == 1) {
            return validateReference(candidates.getFirst(), true);
        } else {
            ValidReference reference = validateFromCandidates(candidates);
            if (reference == null) {
                Skript.error("Could not identify method %s out of %s overloads", this.reference, candidates.size());
                return null;
            }
            return reference;
        }
    }

    @Override
    protected @Nullable ValidReference getProductFromInstance(ClassInstance instance) {
        SkriptClass parent = instance.getParent();
        if (product() == null || !parent.inherits(product().getOrigin())) {
            return getProductFromClass(parent);
        }
        SkriptMethod method = (isSuper ? contextClass().getExtends() : parent).getExactMethod(MethodIdentifier.from(product().method));
        if (method == null) {
            return getProductFromClass(parent);
            }
        return new ValidReference(method, product().args);
    }

    @Override
    protected boolean validate(ValidReference reference, SkriptClass contextClass) {
        SkriptClass origin = reference.getOrigin();
        if (expectsReturn && reference.type() == null) {
            Skript.error("This method can't return anything");
            return false;
        }
        if (reference.hasModifier(PRIVATE) && contextClass != origin) {
            Skript.error("Private methods can only be accessed from within their own class");
            return false;
        }
        if (reference.hasModifier(PROTECTED) && (contextClass == null || !contextClass.inherits(origin))) {
            Skript.error("Protected methods can only be accessed from inheritors");
            return false;
        }
        if (isSuper && reference.method().hasModifier(ABSTRACT)) {
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

    private @Nullable ValidReference validateReference(SkriptMethod target, boolean printErrors) {
        MethodArgument[] targetArgs = target.arguments;
        List<String> targetNames = Arrays.stream(targetArgs).map(MethodArgument::name).toList();

        ReferenceArgument[] refArgs = reference.args();
        Expression<?>[] result = new Expression<?>[targetArgs.length];

        for (int i = 0; i < targetArgs.length; i++) {
            ReferenceArgument refArg = (i >= refArgs.length) ? targetArgs[i].toDefaultRef() : refArgs[i];
            String name = refArg.name();
            int index = name == null || targetArgs[i].name().equals(name) ? i : targetNames.indexOf(name);
            if (index == -1) { // there are no arguments with this name
                if (printErrors) Skript.error("Unknown argument '%s'", name);
                return null;
            }
            MethodArgument targetArg = targetArgs[index];
            if (result[index] != null) { // argument already populated means duplicate/order issue
                if (printErrors) Skript.error("Duplicate/out of order argument '%s'", targetArg.name());
                return null;
            }
            if (refArg.expr() == null) {
                if (printErrors) Skript.error("Could not resolve 1 or more arguments for this method call");
                return null;
            }
            //noinspection unchecked
            Expression<?> expr = refArg.expr().getConvertedExpression(targetArg.type());
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

    public record ValidReference(@NotNull SkriptMethod method, @NotNull Expression<?>[] args) implements AccessModifiable {
        @Override
        public boolean isPlural() {
            return method.isPlural();
        }
        @Override
        public Modifier[] modifiers() {
            return method.modifiers();
        }
        @Override
        public Class<?> type() {
            return method.type();
        }
        @Override
        public SkriptClass getOrigin() {
            return method.getOrigin();
        }

        public MethodEvent run(Event event, ClassInstance instance) {
            return method.run(event, new MethodEvent(instance), args);
        }
    }
}
