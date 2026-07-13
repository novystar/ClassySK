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
import com.novystxr.classysk.main.elements.classes.ExprSelf;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.regex.MatchResult;

import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;
import static com.novystxr.classysk.Classysk.NAME_PATTERN;
import static com.novystxr.classysk.api.methods.MethodParser.HINT_PATTERN;
import static com.novystxr.classysk.api.util.StringUtils.getLowerCase;
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

        validator = new FieldValidator(getErrorSource(), contextClass, fieldName);

        if (!isStaticReference) {
            instanceExpr = (Expression<ClassInstance>) exprs[0];
        }
        if (className != null) {
            if (className.isEmpty()) return true;

            skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
        }
        if (isStaticReference)
            return validator.validateInstance(skriptClass);
        else {
            if (instanceExpr.getSource() instanceof ExprSelf)
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

        switch (mode) {
            case SET -> setValueAndSave(delta, instance, event);
            case DELETE -> {
                instance.removeField(fieldName);
                save(event);
            }
            case RESET -> {
                instance.resetField(fieldName);
                save(event);
            }
            case ADD, REMOVE, REMOVE_ALL -> {
                if (delta == null) return;
                Object[] initialValue = instance.getFieldValue(fieldName);
                FieldSignature signature = validator.product();

                if (signature.isPlural()) {
                    ExprUtils.mutatePlural(initialValue, delta, mode, result ->
                        setValueAndSave(result, instance, event));
                } else {
                    Object singleValue = initialValue != null ? initialValue[0] : null;
                    ExprUtils.mutateSingle(singleValue, delta, mode, value ->
                        setValueAndSave(new Object[]{value}, instance, event));
                }
            }
        }
    }

    private void setValueAndSave(Object[] value, ClassInstance instance, Event event) {
        if (instance.setFieldValue(fieldName, value)) {
            save(event);
        }
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
        return switch (mode) {
            case DELETE, RESET -> new Class[]{};
            case REMOVE_ALL -> {
                if (validator.shouldBeSingle().isTrue()) yield null;
                yield CollectionUtils.array(
                    validator.exactTypeOr(Object.class));
            }
            case SET, ADD, REMOVE -> CollectionUtils.array(
                validator.exactTypeOr(Object.class));
        };
    }

    private @Nullable ClassInstance getValidInstance(Event event) {
        if (isStaticReference) return skriptClass;
        return validator.getValidInstance(event, instanceExpr, skriptClass);
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