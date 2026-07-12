package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
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
import com.novystxr.classysk.main.elements.classes.ExprThisInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.regex.MatchResult;

import static com.novystxr.classysk.api.util.StringUtils.getLowerCase;
import static com.novystxr.classysk.api.util.StringUtils.titleCase;

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

    private SkriptClass skriptClass = null;
    private Expression<ClassInstance> instanceExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStaticReference = pattern == 1;

        MatchResult regex = result.regexes.getFirst();
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String className = getLowerCase(regex.group(1));
        String name = getLowerCase(regex.group(2));
        String args = getLowerCase(regex.group(3));

        MethodReference reference = MethodParser.parseReference(name, args);
        if (reference == null) return false;

        if (className != null && (skriptClass = ClassManager.getClass(className)) == null) {
            Skript.error("Class '%s' does not exist", titleCase(className));
            return false;
        }
        validator = new MethodValidator(getErrorSource(), contextClass, reference, false);

        if (isStaticReference)
            return validator.validateInstance(skriptClass);
        else {
            instanceExpr = (Expression<ClassInstance>) exprs[0];
            if (instanceExpr.getSource() instanceof ExprThisInstance)
                skriptClass = contextClass;

            return !validator.validateUnknown(skriptClass).isFalse();
        }
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
        return validator.getValidInstance(event, instanceExpr, skriptClass);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
