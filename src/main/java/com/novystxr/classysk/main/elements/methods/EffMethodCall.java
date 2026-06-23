package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
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
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.util.StringUtils.getLowerCase;

public class EffMethodCall extends Effect {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EFFECT,
            SyntaxInfo.builder(EffMethodCall.class)
                .addPatterns(MethodParser.METHOD_PATTERN, MethodParser.STATIC_METHOD_PATTERN)
                .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
                .supplier(EffMethodCall::new)
                .build()
        );
    }

    private MethodValidator validator;
    private boolean isStaticReference;

    private SkriptClass skriptClass;
    private Expression<ClassInstance> instanceExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, ParseResult parseResult) {
        isStaticReference = pattern == 1;

        String methodName = getLowerCase(parseResult.regexes.get(pattern));
        String args = (parseResult.hasTag("args")) ?
            getLowerCase(parseResult.regexes.get(pattern+1)) : null;

        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());
        MethodReference reference = MethodParser.parseReference(methodName, args);

        if (reference == null) return false;
        validator = new MethodValidator(getErrorSource(), contextClass, reference);

        if (isStaticReference) {
            String className = getLowerCase(parseResult.regexes.getFirst());
            skriptClass = ClassManager.getClass(className);
            return validator.validateInstance(skriptClass, false);
        } else {
            instanceExpr = (Expression<ClassInstance>) expressions[0];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return;

        SkriptMethod method = validator.getProduct();
        method.run(event, instance, validator.getValidatedArgs());
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
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
