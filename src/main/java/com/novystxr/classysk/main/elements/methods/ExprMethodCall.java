package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.MethodParser;
import com.novystxr.classysk.api.MethodParser.ArgumentParser;
import com.novystxr.classysk.api.SkriptClass;
import com.novystxr.classysk.api.util.ClassyUtils;
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
                        .addPatterns(MethodParser.methodPattern, MethodParser.staticMethodPattern)
                        .supplier(ExprMethodCall::new)
                        .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
                        .build()
        );
    }

    private Expression<SkriptClass> classExpr;
    private ArgumentParser argsParser;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String methodName = ClassyUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        String argsString = (parseResult.hasTag("args")) ? ClassyUtils.getLowerCase(parseResult.regexes.get(matchedPattern+1)) : null;

        argsParser = new ArgumentParser(methodName, argsString, true);
        if (matchedPattern == 1) {
            String className = ClassyUtils.getLowerCase(parseResult.regexes.getFirst());
            argsParser.parseSignature(className);
            argsParser.parse();

            return argsParser.canParse().isTrue();
        }
        classExpr = (Expression<SkriptClass>) expressions[0];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (argsParser.canParse().isUnknown()) {
            argsParser.parseSignature(classExpr.getSingle(event));
            argsParser.parse();
        }
        if (!argsParser.canParse().isTrue()) return null;
        return argsParser.parsedSignature.run(event, argsParser.skriptClass, argsParser.parsedArgs);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean canBeSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
