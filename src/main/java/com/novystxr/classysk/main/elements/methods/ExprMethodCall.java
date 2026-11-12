package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.methods.MethodEvent;
import com.novystxr.classysk.api.methods.MethodParser;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodValidator;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.api.util.ExprUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.util.StringUtils.*;

public class ExprMethodCall extends SimpleExpression<Object> implements KeyProviderExpression<Object> {

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

    private String[] keys;
    private int toGenerate;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStatic = pattern == 1;
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String methodName = getConfigLowerCase(result.regexes.get(pattern));
        String args = result.regexes.size() > pattern + 1
            ? result.regexes.get(pattern + 1).group() : null;

        MethodReference reference = MethodParser.parseReference(methodName, args, isStatic);
        if (reference == null) return false;

        validator = new MethodValidator(getErrorSource(), contextClass, reference, true, result.hasTag("super"));
        if (isStatic) {
            String className = getLowerCase(result.regexes.getFirst());
            SkriptClass skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
            if (!validator.validateStatic(skriptClass))
                return false;
        } else if (!validator.validateExpression((Expression<ClassInstance>) exprs[0]))
            return false;

        shouldBeSingle = validator.shouldBeSingle();
        possibleTypes = validator.possibleTypes();
        bestReturnType = Validator.bestReturnType(possibleTypes);
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        keys = null; toGenerate = 0;
        ClassInstance instance = isStatic ? null : validator.getValidInstance(event);
        if (!isStatic && instance == null) return null;

        MethodEvent methodEvent = new MethodEvent(instance);
        validator.product().run(event, methodEvent, validator.getArgs());

        Object[] values = methodEvent.returnValues;
        String[] keys = methodEvent.returnKeys;

        if (values == null || values.length == 0) return null;

        values = ConverterUtils.getConverted(values, bestReturnType);
        keys = ConverterUtils.filterKeys(keys, values);
        values = ArrayUtils.removeAllOccurrences(values, null);

        if (values.length == 0) {
            error("The result of this method call couldn't convert to its reported type.");
            return null;
        }
        if (shouldBeSingle.isTrue() && values.length > 1) {
            error("The result of this method call was plural but was supposed return single.");
            return null;
        }
        if (keys == null) {
            toGenerate = values.length;
        }
        this.keys = keys;
        return values;
    }

    @Override
    public boolean areKeysRecommended() {
        return false;
    }

    @Override
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        return keys != null ? keys : ExprUtils.generateNumericalKeys(toGenerate);
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
