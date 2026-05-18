package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.MethodParser;
import com.novystxr.classysk.api.MethodParser.ArgumentParser;
import com.novystxr.classysk.api.SkriptClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Locale;

public class EffMethodCall extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffMethodCall.class)
                        .addPattern(MethodParser.methodPattern)
                        .supplier(EffMethodCall::new)
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
    protected void execute(Event event) {
        SkriptClass skriptClass = classExpr.getSingle(event);

        if (argsParser.canParse().isUnknown()) {
            argsParser.parseSignature(skriptClass);
            argsParser.parse();
        }
        if (!argsParser.canParse().isTrue()) return;

        argsParser.parsedSignature.run(event, skriptClass, argsParser.parsedArgs);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
