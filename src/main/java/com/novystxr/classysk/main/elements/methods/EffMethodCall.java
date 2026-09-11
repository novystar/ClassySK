package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
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

import static com.novystxr.classysk.api.util.StringUtils.getConfigLowerCase;
import static com.novystxr.classysk.api.util.StringUtils.titleCase;

public class EffMethodCall extends Effect {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EFFECT,
            SyntaxInfo.builder(EffMethodCall.class)
                .addPatterns(MethodParser.METHOD_PATTERN, MethodParser.STATIC_METHOD_PATTERN)
                .priority(Classysk.SHADOW_REALM)
                .supplier(EffMethodCall::new)
                .build()
        );
    }

    private MethodValidator validator;
    private boolean isStatic;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStatic = pattern == 1;
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String methodName = getConfigLowerCase(result.regexes.get(pattern));
        String args = result.regexes.size() > pattern + 1
            ? getConfigLowerCase(result.regexes.get(pattern + 1)) : null;

        MethodReference reference = MethodParser.parseReference(methodName, args, isStatic);
        if (reference == null) return false;

        validator = new MethodValidator(getErrorSource(), contextClass, reference, false);
        if (isStatic) {
            String className = getConfigLowerCase(result.regexes.getFirst());
            SkriptClass skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
            return validator.validateStatic(skriptClass);
        }
        return validator.validateExpression((Expression<ClassInstance>) exprs[0]);
    }

    @Override
    protected void execute(Event event) {
        ClassInstance instance = isStatic ? null : validator.getValidInstance(event);
        if (!isStatic && instance == null) return;

        validator.product().run(event, instance);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
