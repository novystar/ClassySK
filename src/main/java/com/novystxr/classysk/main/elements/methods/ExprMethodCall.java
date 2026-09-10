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
import com.novystxr.classysk.api.methods.SkriptMethod;
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

    private Kleenean shouldBeSingle;
    private Class<?>[] possibleTypes;
    private Class<?> bestReturnType;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStatic = pattern == 1;
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String methodName = getConfigLowerCase(result.regexes.get(pattern));
        String args = result.regexes.size() > pattern + 1
            ? getConfigLowerCase(result.regexes.get(pattern + 1)) : null;

        MethodReference reference = MethodParser.parseReference(methodName, args);
        if (reference == null) return false;

        validator = new MethodValidator(getErrorSource(), contextClass, reference, true);
        if (isStatic) {
            String className = getConfigLowerCase(result.regexes.getFirst());
            SkriptClass skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
            return validator.validateStatic(skriptClass) && postInit();
        }
        return validator.validateExpression((Expression<ClassInstance>) exprs[0]) && postInit();
    }

    private boolean postInit() {
        shouldBeSingle = validator.shouldBeSingle();
        possibleTypes = validator.possibleTypes();
        bestReturnType = Validator.bestReturnType(possibleTypes);
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ClassInstance instance = isStatic ? null : validator.getValidInstance(event);
        if (!isStatic && instance == null) return null;

        Object[] result = validator.product().run(event, instance);
        if (result == null) return null;

        result = validator.getSafeConverted(result, shouldBeSingle.isTrue());
        if (result == null) {
            error("The result of this method call couldn't convert to its reported type.");
        }
        return result;
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
