package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.MethodParser;
import com.novystxr.classysk.api.MethodParser.ArgumentParser;
import com.novystxr.classysk.api.SkriptClass;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffMethodCall extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffMethodCall.class)
                        .addPatterns(MethodParser.methodPattern, MethodParser.staticMethodPattern)
                        .supplier(EffMethodCall::new)
                        .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
                        .build()
        );
    }

    private Expression<SkriptClass> classExpr;
    private ArgumentParser argsParser;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String methodName = StringUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        String argsString = (parseResult.hasTag("args")) ? StringUtils.getLowerCase(parseResult.regexes.get(matchedPattern+1)) : null;

        argsParser = new ArgumentParser(methodName, argsString, false);
        if (matchedPattern == 1) {
            String className = StringUtils.getLowerCase(parseResult.regexes.getFirst());
            argsParser.parse(className);

            return argsParser.canParse().isTrue();
        }
        classExpr = (Expression<SkriptClass>) expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (argsParser.canParse().isUnknown()) {
            argsParser.parse(classExpr.getSingle(event));
        }
        if (!argsParser.canParse().isTrue()) return;
        argsParser.parsedMethod.run(event, argsParser.skriptClass, argsParser.parsedArgs);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
