package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.methods.ArgumentParser;
import com.novystxr.classysk.api.methods.ArgumentValidator;
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
    private ArgumentValidator argValidator;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String methodName = ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        String argsString = (parseResult.hasTag("args")) ? ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern+1)) : null;

        if (matchedPattern == 1) {
            argValidator = new ArgumentValidator(methodName, argsString, true, true);
            String className = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());
            argValidator.validate(className);
            argValidator.checkAccess(getParser());

            return argValidator.isValid().isTrue();
        } else {
            argValidator = new ArgumentValidator(methodName, argsString, true, false);
        }
        classExpr = (Expression<SkriptClass>) expressions[0];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (argValidator.isValid().isUnknown()) {
            argValidator.validate(classExpr.getSingle(event));
            argValidator.checkAccess(event);
        } else {
            argValidator.updateInstance(classExpr, event);
        }
        if (!argValidator.isValid().isTrue()) return null;
        return argValidator.parsedMethod.run(event, argValidator.skriptClass, argValidator.parsedArgs);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean canBeSingle() {
        return true;
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
