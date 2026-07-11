package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.FieldValidator;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.ExprUtils;
import com.novystxr.classysk.main.elements.classes.ExprThisInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;

import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;
import static com.novystxr.classysk.Classysk.NAME_PATTERN;
import static com.novystxr.classysk.api.methods.MethodParser.HINT_PATTERN;
import static com.novystxr.classysk.api.util.StringUtils.getLowerCase;
import static ch.njol.skript.classes.Changer.ChangeMode.*;
import static com.novystxr.classysk.api.util.StringUtils.titleCase;

public class ExprFieldAccess extends SimpleExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprFieldAccess.class, Object.class)
                .addPatterns(
                "%classinstance%<"+HINT_PATTERN+"::("+NAME_PATTERN+")>",
                    "<("+CLASSNAME_PATTERN+")::("+NAME_PATTERN+")>"
                )
                .supplier(ExprFieldAccess::new)
                .priority(Classysk.SHADOW_REALM)
                .build()
        );
    }
    private FieldValidator validator;
    private boolean isStaticReference;
    private String fieldName;

    private SkriptClass skriptClass;
    private Expression<ClassInstance> instanceExpr;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStaticReference = pattern == 1;

        MatchResult regex = result.regexes.getFirst();
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String className = getLowerCase(regex.group(1));
        fieldName = getLowerCase(regex.group(2));

        if (className != null && (skriptClass = ClassManager.getClass(className)) == null) {
            Skript.error("Class '%s' does not exist", titleCase(className));
            return false;
        }
        validator = new FieldValidator(getErrorSource(), contextClass, fieldName);

        if (isStaticReference)
            return validator.validateInstance(skriptClass, false);
        else {
            instanceExpr = (Expression<ClassInstance>) exprs[0];
            if (instanceExpr.getSource() instanceof ExprThisInstance)
                skriptClass = contextClass;

            return !validator.validateUnknown(skriptClass).isFalse();
        }
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return null;

        return instance.getFieldValue(fieldName);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return;

        FieldSignature signature = validator.getProduct();
        switch (mode) {
            case SET -> setValueAndSave(delta, instance, event);
            case DELETE -> {
                instance.removeField(fieldName);
                save(event);
            }
            case RESET -> {
                instance.resetField(signature);
                save(event);
            }
            case ADD, REMOVE -> {
                if (delta == null) return;
                Object[] initialValue = instance.getFieldValue(fieldName);

                if (signature.isPlural()) {
                    List<Object> mutatedValue = new ArrayList<>(Arrays.asList(initialValue));

                    if (mode == ADD) mutatedValue.addAll(Arrays.asList(delta));
                    else mutatedValue.removeAll(Arrays.asList(delta));

                    setValueAndSave(mutatedValue.toArray(), instance, event);
                } else {
                    Object singleValue = initialValue != null ? initialValue[0] : null;
                    ExprUtils.mutateSingle(singleValue, delta, mode, value ->  {
                        if (!instance.fieldExists(fieldName) && value == null) return;
                        setValueAndSave(new Object[]{value}, instance, event);
                    });
                }
            }
        }
    }

    private void setValueAndSave(Object[] value, ClassInstance instance, Event event) {
        if (instance.setFieldValue(fieldName, value)) save(event);
    }

    private void save(Event event) {
        if (isStaticReference) return;
        if (instanceExpr.getSource() instanceof Variable<?> variable) {
            // set variable to the same value it is to trigger serialization
            variable.changeInPlace(event, value -> value);
        }
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        boolean result = switch (mode) {
            case SET, RESET, DELETE, REMOVE, ADD -> true;
            default -> false;
        };
        if (result) {
            if (isStaticReference) {
                return CollectionUtils.array(validator.getProduct().type());
            }
            return CollectionUtils.array(Object.class);
        }
        return null;
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
    public boolean isSingle() {
        Kleenean shouldBeSingle = validator.shouldBeSingle();
        if (shouldBeSingle.isUnknown()) {
            return false;
        }
        return shouldBeSingle.isTrue();
    }

    @Override
    public boolean canBeSingle() {
        Kleenean shouldBeSingle = validator.shouldBeSingle();
        if (shouldBeSingle.isUnknown()) {
            return true;
        }
        return shouldBeSingle.isTrue();
    }

    @Override
    public Class<?> getReturnType() {
        return validator.getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return validator.possibleReturnTypes();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field "+fieldName;
    }
}