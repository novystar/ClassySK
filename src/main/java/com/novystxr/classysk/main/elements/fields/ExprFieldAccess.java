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
import com.novystxr.classysk.api.AccessValidator;
import com.novystxr.classysk.api.FieldHolder;
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

import static ch.njol.skript.classes.Changer.ChangeMode.*;
import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;
import static com.novystxr.classysk.Classysk.NAME_PATTERN;
import static com.novystxr.classysk.api.methods.MethodParser.HINT_PATTERN;
import static com.novystxr.classysk.api.util.StringUtils.getConfigLowerCase;
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

    private Kleenean shouldBeSingle;
    private Class<?>[] possibleTypes;
    private Class<?> bestReturnType;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStaticReference = pattern == 1;

        MatchResult regex = result.regexes.getFirst();
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());

        String className = getConfigLowerCase(regex.group(1));
        fieldName = getConfigLowerCase(regex.group(2));

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
        if (isStaticReference) {
            if (!validator.validateStatic(skriptClass))
                return false;
        } else {
            if (instanceExpr.getSource() instanceof ExprSelf)
                skriptClass = contextClass;

            if (validator.validateUnknown(skriptClass).isFalse())
                return false;
        }
        shouldBeSingle = validator.shouldBeSingle();
        possibleTypes = validator.possibleTypes();
        bestReturnType = AccessValidator.bestReturnType(possibleTypes);
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        FieldHolder holder = getValidHolder(event);
        if (holder == null) return null;

        FieldSignature signature = validator.product();

        if (shouldBeSingle.isTrue() && signature.isPlural()) {
            error("Field returns multiple values while reporting as single. Try reloading the script or using a safe call: %instance%<>::field");
            return null;
        }
        if (!bestReturnType.isAssignableFrom(signature.type())) {
            error("Field doesn't match its reported type. Try reloading the script or using a safe call: %instance%<>::field");
            return null;
        }
        return holder.getFieldValue(fieldName);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        FieldHolder fieldHolder = getValidHolder(event);
        if (fieldHolder == null) return;

        switch (mode) {
            case SET -> setValueAndSave(delta, fieldHolder, event);
            case DELETE -> {
                fieldHolder.removeField(fieldName);
                save(event);
            }
            case RESET -> {
                fieldHolder.resetField(fieldName);
                save(event);
            }
            case ADD, REMOVE, REMOVE_ALL -> {
                if (delta == null) return;
                Object[] initialValue = fieldHolder.getFieldValue(fieldName);
                FieldSignature signature = validator.product();

                if (signature.isPlural()) {
                    ExprUtils.mutatePlural(initialValue, delta, mode, result ->
                        setValueAndSave(result, fieldHolder, event));
                } else {
                    Object singleValue = initialValue.length == 1 ? initialValue[0] : null;
                    ExprUtils.mutateSingle(singleValue, delta, mode, value ->
                        setValueAndSave(new Object[]{value}, fieldHolder, event));
                }
            }
        }
    }

    private void setValueAndSave(Object[] value, FieldHolder holder, Event event) {
        if (holder.setFieldValue(fieldName, value)) {
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
        if (mode == DELETE || mode == RESET) {
            return new Class[]{};
        }
        Class<?> type = validator.exactTypeOr(Object.class);
        boolean isPlural = !canBeSingle();
        if (isPlural) {
            type = type.arrayType();
        }
        Class<?>[] typeArray = CollectionUtils.array(type);

        if ((mode == REMOVE_ALL && isPlural) || mode == SET || mode == ADD || mode == REMOVE) {
            return typeArray;
        }
        return null;
    }

    private @Nullable FieldHolder getValidHolder(Event event) {
        if (isStaticReference) return skriptClass;
        return validator.getValidInstance(event, instanceExpr, skriptClass);
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
        return AccessValidator.bestReturnType(possibleTypes);
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return possibleTypes;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field "+fieldName;
    }
}