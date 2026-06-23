package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodParser;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodValidator;
import com.novystxr.classysk.api.methods.SkriptMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.util.StringUtils.getLowerCase;

public class ExprMethodCall extends SimpleExpression<Object> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprMethodCall.class, Object.class)
                    .addPatterns(MethodParser.METHOD_PATTERN, MethodParser.STATIC_METHOD_PATTERN)
                    .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
                    .supplier(ExprMethodCall::new)
                    .build()
        );
    }

    private MethodValidator validator;
    private boolean isStaticReference;

    private SkriptClass skriptClass;
    private Expression<ClassInstance> instanceExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStaticReference = pattern == 1;

        String methodName = getLowerCase(result.regexes.get(pattern));
        String args = (result.hasTag("args")) ?
            getLowerCase(result.regexes.get(pattern+1)) : null;

        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());
        MethodReference reference = MethodParser.parseReference(methodName, args);

        if (reference == null) return false;
        validator = new MethodValidator(getErrorSource(), contextClass, reference, true);

        if (isStaticReference) {
            String className = getLowerCase(result.regexes.getFirst());
            skriptClass = ClassManager.getClass(className);
            return validator.validateInstance(skriptClass, false);
        } else {
            instanceExpr = (Expression<ClassInstance>) exprs[0];
        }
        return true;
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
        if (isStaticReference) {
            return !validator.getProduct().signature.returnPlural();
        }
        return false;
    }

    @Override
    public boolean canBeSingle() {
        if (isStaticReference) {
            return !validator.getProduct().signature.returnPlural();
        }
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        if (isStaticReference) {
            return validator.getProduct().signature.returnType();
        }
        return Object.class;
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
