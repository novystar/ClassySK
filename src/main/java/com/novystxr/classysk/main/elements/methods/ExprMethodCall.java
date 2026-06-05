package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.methods.ArgumentParser;
import com.novystxr.classysk.api.methods.MethodValidator;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprMethodCall extends SimpleExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprMethodCall.class, Object.class)
                        .addPatterns(ArgumentParser.methodPattern, ArgumentParser.staticMethodPattern)
                        .supplier(ExprMethodCall::new)
                        .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
                        .build()
        );
    }

    private Expression<SkriptClass> classExpr;
    private MethodValidator validator;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String methodName = ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        String argsString = (parseResult.hasTag("args")) ? ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern+1)) : null;

        if (matchedPattern == 1) {
            validator = new MethodValidator(methodName, argsString, true, true);
            String className = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());
            validator.validate(className);
            validator.checkAccess(getParser());

            return validator.isValid().isTrue();
        } else {
            validator = new MethodValidator(methodName, argsString, true, false);
        }
        classExpr = (Expression<SkriptClass>) expressions[0];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (validator.isValid().isUnknown()) {
            validator.validate(classExpr.getSingle(event));
            validator.checkAccess(event);
        } else {
            validator.updateInstance(classExpr, event);
        }
        if (!validator.isValid().isTrue()) return null;
        return validator.parsedMethod.run(event, validator.skriptClass, validator.parsedArgs);
    }

    @Override
    public boolean isSingle() {
        if (validator.parsedMethod == null) return false;
        return !validator.parsedMethod.signature.returnPlural();
    }

    @Override
    public boolean canBeSingle() {
        if (validator.parsedMethod == null) return true;
        return !validator.parsedMethod.signature.returnPlural();
    }

    // TODO: dynamic return type
    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
