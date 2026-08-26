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
import com.novystxr.classysk.api.methods.MethodValidator.ValidReference;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.main.elements.classes.ExprSelf;
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

    private SkriptClass skriptClass = null;
    private Expression<ClassInstance> instanceExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        boolean isStatic = pattern == 1;
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String className = getConfigLowerCase(result.regexes.getFirst().group(1));
        String name = getConfigLowerCase(result.regexes.getFirst().group(2));
        String args = result.regexes.size() == 1
            ? "" : result.regexes.get(1).group().trim();

        MethodReference reference = MethodParser.parseReference(name, args, isStatic);
        if (reference == null) return false;

        boolean isSuper = result.hasTag("super");
        instanceExpr = isStatic ? null : (Expression<ClassInstance>) exprs[0];

        validator = new MethodValidator(getErrorSource(), contextClass, reference, false, isSuper);
        if (className != null) {
            if (className.isEmpty()) return true;

            skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
        }
        if (isStatic) {
            return validator.validateStatic(skriptClass);
        }
        if (isSuper) {
            instanceExpr = new ExprSuper();
            if (!instanceExpr.init(null, 0, null, null))
                return false;
        }

        if (instanceExpr.getSource() instanceof ExprSelf) {
            skriptClass = contextClass;
        }
        return !validator.validateUnknown(skriptClass).isFalse();
    }

    @Override
    protected void execute(Event event) {
        ClassInstance instance = validator.getValidInstance(event, instanceExpr, skriptClass);

        ValidReference reference = validator.product();
        reference.method().run(event, instance, reference.args());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "method call";
    }
}
