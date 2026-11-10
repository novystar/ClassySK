package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyReceiverExpression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Validator;
import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.fields.FieldValidator;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.api.util.ExprUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static ch.njol.skript.classes.Changer.ChangeMode.*;
import static ch.njol.util.coll.CollectionUtils.array;
import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;
import static com.novystxr.classysk.Classysk.NAME_PATTERN;
import static com.novystxr.classysk.api.Modifier.CONST;
import static com.novystxr.classysk.api.util.StringUtils.*;

public class ExprFieldAccess extends SimpleExpression<Object> implements KeyProviderExpression<Object>, KeyReceiverExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprFieldAccess.class, Object.class)
                .addPatterns("%classinstance%\\:\\:<"+NAME_PATTERN+">", "<"+CLASSNAME_PATTERN+">\\:\\:<"+NAME_PATTERN+">")
                .supplier(ExprFieldAccess::new)
                .priority(Classysk.SHADOW_REALM)
                .build()
        );
    }
    private FieldValidator validator;
    private boolean isStatic;
    private String fieldName;

    private SkriptClass skriptClass;

    private Variable<?> variable;

    private String[] keys;
    private int toGenerate;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        isStatic = pattern == 1;
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());
        fieldName = getConfigLowerCase(result.regexes.get(pattern));

        validator = new FieldValidator(getErrorSource(), contextClass, fieldName, isStatic);
        if (isStatic) {
            String className = getLowerCase(result.regexes.getFirst());
            skriptClass = ClassManager.getClass(className);
            if (skriptClass == null) {
                Skript.error("Class '%s' does not exist", titleCase(className));
                return false;
            }
            return validator.validateStatic(skriptClass) && postInit();
        }
        if (exprs[0].getSource() instanceof Variable<?> var) {
            variable = var;
        }
        return validator.validateExpression((Expression<ClassInstance>) exprs[0]) && postInit();
    }

    private Kleenean shouldBeSingle;
    private Class<?>[] possibleTypes;
    private Class<?> bestReturnType;

    private boolean postInit() {
        possibleTypes = validator.possibleTypes();
        bestReturnType = Validator.bestReturnType(possibleTypes);
        shouldBeSingle = validator.shouldBeSingle();
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        keys = null; toGenerate = 0;
        FieldHolder holder = isStatic ? skriptClass : validator.getValidInstance(event);
        if (holder == null) return null;

        Object[] values = holder.getFieldValue(fieldName);
        String[] keys = holder.getFieldKeys(fieldName);

        if (values.length == 0) return null;

        values = ConverterUtils.getConverted(values, bestReturnType, possibleTypes);
        keys = ConverterUtils.filterKeys(keys, values);
        values = ArrayUtils.removeAllOccurrences(values, null);

        if (values.length == 0) {
            error("The result of this field call couldn't convert to its reported type.");
            return null;
        }
        if (shouldBeSingle.isTrue() && values.length > 1) {
            error("The result of this field call was plural but was supposed return single.");
            return null;
        }
        if (keys == null) {
            toGenerate = values.length;
        }
        this.keys = keys;
        return values;
    }

    @Override
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        return keys != null ? keys : ExprUtils.generateNumericalKeys(toGenerate);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        SkriptField field = validator.product();
        if (field != null && field.hasModifier(CONST)) {
            Skript.error("Constant fields can't be changed after definition");
            return null;
        }
        if (mode == DELETE || mode == RESET) {
            return new Class[0];
        }
        Class<?> type = validator.exactTypeOr(Object.class);
        boolean isPlural = !canBeSingle();
        if (isPlural) {
            type = type.arrayType();
        }
        Class<?>[] typeArray = array(type);

        if ((mode == REMOVE_ALL && isPlural) || mode == SET || mode == ADD || mode == REMOVE) {
            return typeArray;
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        change(event, delta, mode, null);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode, @NotNull String @Nullable [] keys) {
        FieldHolder holder = isStatic ? skriptClass : validator.getValidInstance(event);
        if (holder == null) return;

        SkriptField field = validator.product();
        if (field.hasModifier(CONST)) {
            error("Constant fields can't be changed after definition");
            return;
        }

        switch (mode) {
            case SET -> {
                holder.setFieldValue(fieldName, delta, keys);
                save(event);
            }
            case DELETE -> {
                holder.removeField(fieldName);
                save(event);
            }
            case RESET -> {
                holder.resetField(fieldName);
                save(event);
            }
            case ADD, REMOVE, REMOVE_ALL -> {
                if (delta == null) return;
                Object[] initialValue = holder.getFieldValue(fieldName);

                if (field.isPlural()) {
                    ExprUtils.mutatePlural(initialValue, delta, mode, result -> {
                       holder.setFieldValue(fieldName, result, keys);
                       save(event);
                    });
                } else {
                    Object singleValue = initialValue.length == 1 ? initialValue[0] : null;
                    ExprUtils.mutateSingle(singleValue, delta, mode, value -> {
                       holder.setFieldValue(fieldName, array(value), keys);
                       save(event);
                    });
                }
            }
        }
    }

    private void save(Event event) {
        if (variable != null) {
            // set variable to the same value it is to trigger serialization
            variable.changeInPlace(event, value -> value);
        }
    }

    @Override
    public boolean areKeysRecommended() {
        return false;
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
    public String toString(@Nullable Event event, boolean debug) {
        return "field "+fieldName;
    }
}