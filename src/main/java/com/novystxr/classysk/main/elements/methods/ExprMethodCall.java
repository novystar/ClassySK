package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.MethodParser;
import com.novystxr.classysk.api.MethodParser.ArgumentParser;
import com.novystxr.classysk.api.SkriptClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Locale;

public class ExprMethodCall extends SimpleExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprMethodCall.class, Object.class)
                        .addPattern(MethodParser.methodPattern)
                        .supplier(ExprMethodCall::new)
                        .build()
        );
    }

    private Expression<SkriptClass> classExpr;
    private ArgumentParser argsParser;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String methodName = parseResult.regexes.get(0).group(0).trim().toLowerCase(Locale.ENGLISH);
        String argsString = null;

        if (parseResult.hasTag("args")) argsString = parseResult.regexes.get(1).group(0);
        argsParser = new ArgumentParser(methodName, argsString);
        classExpr = (Expression<SkriptClass>) expressions[0];

        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        SkriptClass skriptClass = classExpr.getSingle(event);

        if (argsParser.canParse().isUnknown()) {
            argsParser.parseSignature(skriptClass);
            argsParser.parse();
        }
        if (!argsParser.canParse().isTrue()) return null;

        return argsParser.parsedSignature.run(event, skriptClass, argsParser.parsedArgs);
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
