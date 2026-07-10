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
import com.novystxr.classysk.main.elements.classes.ExprThisInstance;
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

        String className = getLowerCase(regex.group(1));
        String name = getLowerCase(regex.group(2));
        String args = getLowerCase(regex.group(3));

        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());
        MethodReference reference = MethodParser.parseReference(name, args);

        if (reference == null) return false;
        validator = new MethodValidator(getErrorSource(), contextClass, reference, true);

        if (className != null && (skriptClass = ClassManager.getClass(className)) == null) {
            Skript.error("Class '%s' does not exist", titleCase(className));
            return false;
        }

        if (isStaticReference)
            return validator.validateInstance(skriptClass, false);
        else {
            instanceExpr = (Expression<ClassInstance>) exprs[0];

            if (instanceExpr.getSource() instanceof ExprThisInstance) {
                skriptClass = contextClass;
            }
            return !validator.validateUnknown(skriptClass).isFalse();
        }
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return null;

        SkriptMethod method = validator.getProduct();
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

    private @Nullable ClassInstance getValidInstance(Event event) {
        if (isStaticReference) return skriptClass;

        ClassInstance newInstance = instanceExpr.getSingle(event);
        if (validator.validateInstance(newInstance, true)) {
            return newInstance;
        }
        return null;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "method call";
    }
}
