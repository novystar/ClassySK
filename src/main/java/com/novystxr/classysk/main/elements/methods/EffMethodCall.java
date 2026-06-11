package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.methods.ArgumentParser;
import com.novystxr.classysk.api.methods.MethodValidator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffMethodCall extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffMethodCall.class)
                        .addPatterns(ArgumentParser.methodPattern, ArgumentParser.staticMethodPattern)
                        .supplier(EffMethodCall::new)
                        .priority(SyntaxInfo.SIMPLE)
                        .build()
        );
    }

    private Expression<ClassInstance> classExpr;
    private MethodValidator validator;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String methodName = ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        String argsString = (parseResult.hasTag("args")) ? ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern+1)) : null;
        if (matchedPattern == 1) {
            validator = new MethodValidator(methodName, argsString, false, true);
            String className = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());
            validator.validate(className);
            validator.checkAccess(getParser());

            return validator.isValid().isTrue();
        } else {
            validator = new MethodValidator(methodName, argsString, false, false);
        }
        classExpr = (Expression<ClassInstance>) expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (validator.isValid().isUnknown()) {
            validator.validate(classExpr.getSingle(event));
            validator.checkAccess(event);
        } else {
            validator.updateInstance(classExpr, event);
        }
        if (!validator.isValid().isTrue()) return;
        validator.parsedMethod.run(event, validator.instance, validator.parsedArgs);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
