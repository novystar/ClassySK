package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Validator;
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
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.util.StringUtils.*;

public class ExprMethodCall extends SimpleExpression<Object> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprMethodCall.class, Object.class)
                .addPatterns(MethodParser.METHOD_PATTERN, MethodParser.STATIC_METHOD_PATTERN)
                .priority(Classysk.SHADOW_REALM)
                .supplier(ExprMethodCall::new)
                .build()
        );
    }

    private MethodValidator validator;
    private boolean isStatic;

    private SkriptClass skriptClass = null;
    private Expression<ClassInstance> instanceExpr;

    private Kleenean shouldBeSingle;
    private Class<?>[] possibleTypes;
    private Class<?> bestReturnType;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStatic = pattern == 1;
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String className = getLowerCase(result.regexes.getFirst().group(1));
        String name = getConfigLowerCase(result.regexes.getFirst().group(2));
        String args = result.regexes.size() == 1
            ? "" : result.regexes.get(1).group().trim();

        MethodReference reference = MethodParser.parseReference(name, args);
        if (reference == null) return false;

        instanceExpr = isStatic ? null : (Expression<ClassInstance>) exprs[0];
        validator = new MethodValidator(getErrorSource(), contextClass, reference, true);
        if (className != null) {
            if (className.isEmpty()) return postInit();

            skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
        }
        if (isStatic) {
            return validator.validateStatic(skriptClass) && postInit();
        }
        if (instanceExpr.getSource() instanceof ExprSelf) {
            skriptClass = contextClass;
        }
        return !validator.validateUnknown(skriptClass).isFalse() && postInit();
    }

    private boolean postInit() {
        shouldBeSingle = validator.shouldBeSingle();
        possibleTypes = validator.possibleTypes();
        bestReturnType = Validator.bestReturnType(possibleTypes);
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ClassInstance instance = isStatic ? null : validator.getValidInstance(event, instanceExpr, skriptClass);
        if (!isStatic && instance == null) return null;

        ValidReference reference = validator.product();
        if (shouldBeSingle.isTrue() && reference.isPlural()) {
            error("Method returns multiple values while reporting as single. Try reloading the script or using a safe call: %instance%<>::method()");
            return null;
        }
        if (!reference.type().isAssignableFrom(bestReturnType)) {
            error("Method doesn't match its reported type. Try reloading the script or using a safe call: %instance%<>::method()");
            return null;
        }
        return reference.method().run(event, instance, reference.args());
    }

    @Override
    public boolean isSingle() {
        return shouldBeSingle.isTrue();
    }

    @Override
    public boolean canBeSingle() {
        return shouldBeSingle.isUnknown() || shouldBeSingle.isTrue();
    }

    @Override
    public Class<?> getReturnType() {
        return bestReturnType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return possibleTypes;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "method call";
    }
}
