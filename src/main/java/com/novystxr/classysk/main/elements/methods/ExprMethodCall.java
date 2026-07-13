package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodValidator;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.main.elements.classes.ExprSelf;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.regex.MatchResult;

import static com.novystxr.classysk.api.util.StringUtils.getLowerCase;
import static com.novystxr.classysk.api.util.StringUtils.titleCase;

public class ExprMethodCall extends SimpleExpression<Object> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprMethodCall.class, Object.class)
                .addPatterns(MethodParser.METHOD_PATTERN, MethodParser.STATIC_METHOD_PATTERN)
                .priority(Classysk.SHADOW_REALM)
                .supplier(ExprMethodCall::new)
                .build()
        );
    }

    private MethodValidator validator;
    private boolean isStaticReference;

    private SkriptClass skriptClass = null;
    private Expression<ClassInstance> instanceExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStaticReference = pattern == 1;

        MatchResult regex = result.regexes.getFirst();
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String className = getLowerCase(regex.group(1));
        String name = getLowerCase(regex.group(2));
        String args = getLowerCase(regex.group(3));

        MethodReference reference = MethodParser.parseReference(name, args);
        if (reference == null) return false;

        validator = new MethodValidator(getErrorSource(), contextClass, reference, true);

        if (!isStaticReference) {
            instanceExpr = (Expression<ClassInstance>) exprs[0];
        }
        if (className != null) {
            if (className.isEmpty()) return true;

            skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
        }
        if (isStaticReference)
            return validator.validateInstance(skriptClass);
        else {
            if (instanceExpr.getSource() instanceof ExprSelf)
                skriptClass = contextClass;

            return !validator.validateUnknown(skriptClass).isFalse();
        }
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return null;

        SkriptMethod method = validator.product();
        return method.run(event, instance, validator.getValidatedArgs());
    }

    @Override
    public boolean isSingle() {
        Kleenean shouldBeSingle = validator.shouldBeSingle();
        if (shouldBeSingle.isUnknown()) {
            return false;
        }
        return shouldBeSingle.isTrue();
    }

    @Override
    public boolean canBeSingle() {
        Kleenean shouldBeSingle = validator.shouldBeSingle();
        if (shouldBeSingle.isUnknown()) {
            return true;
        }
        return shouldBeSingle.isTrue();
    }

    @Override
    public Class<?> getReturnType() {
        return validator.getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return validator.possibleReturnTypes();
    }

    private @Nullable ClassInstance getValidInstance(Event event) {
        if (isStaticReference) return skriptClass;
        return validator.getValidInstance(event, instanceExpr, skriptClass);
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "method call";
    }
}
