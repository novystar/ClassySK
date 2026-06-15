package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
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
import com.novystxr.classysk.api.util.ExpressionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.util.ClassyStringUtils.getLowerCase;

public class ExprFieldAccess extends SimpleExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprFieldAccess.class, Object.class)
                .addPatterns(
                    "%classinstance%\\:\\:<"+ Classysk.namePattern +">",
                    "<"+ Classysk.classNamePattern +">\\:\\:<"+ Classysk.namePattern +">"
                )
                .supplier(ExprFieldAccess::new)
                .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
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
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, ParseResult parseResult) {
        isStaticReference = pattern == 1;

        if (isStaticReference) {
            String className = getLowerCase(parseResult.regexes.get(0));
            fieldName = getLowerCase(parseResult.regexes.get(1));
            skriptClass = ClassManager.getClass(className);
        } else {
            fieldName = getLowerCase(parseResult.regexes.getFirst());
            instanceExpr = (Expression<ClassInstance>) expressions[0];
        }
        SkriptClass contextClass = SkriptMethod.getContextClass(getParser());
        validator = new FieldValidator(getNode(), contextClass, fieldName);

        if (isStaticReference) {
            return validator.validateInstance(skriptClass);
        }
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return null;

        return instance.getParent().getFieldValue(fieldName);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        ClassInstance instance = getValidInstance(event);
        if (instance == null) return;
        switch (mode) {
            case SET -> instance.setFieldValue(fieldName, delta);
            case DELETE, RESET -> instance.removeField(fieldName);
            case ADD, REMOVE -> {
                Object[] initialValues = instance.getFieldValue(fieldName);
                FieldSignature signature = instance.getFieldSignature(fieldName);
                Object[] result;

                //noinspection DataFlowIssue
                if (signature.isPlural()) {
                    result = ExpressionUtils.mutatePlural(initialValues, delta, mode);
                } else {
                    result = ExpressionUtils.mutateSingle(initialValues, delta, mode, signature.type());
                }
                if (result != null) instance.setFieldValue(fieldName, result);
            }
        }
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        boolean result = switch (mode) {
            case SET, RESET, DELETE, REMOVE, ADD -> true;
            default -> false;
        };

        if (result) {
            FieldSignature signature = validator.getSignature();
            if (signature != null) {
                return CollectionUtils.array(signature.type());
            }
            return CollectionUtils.array(Object.class);
        }
        return null;
    }

    private @Nullable ClassInstance getValidInstance(Event event) {
        if (isStaticReference) return skriptClass;
        ClassInstance newInstance = instanceExpr.getSingle(event);

        if (validator.validateInstance(newInstance)) {
            return newInstance;
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        FieldSignature signature = validator.getSignature();
        if (signature == null) return false;
        return !signature.isPlural();
    }

    @Override
    public boolean canBeSingle() {
        FieldSignature signature = validator.getSignature();
        if (signature == null) return true;
        return !signature.isPlural();
    }

    @Override
    public Class<?> getReturnType() {
        FieldSignature signature = validator.getSignature();
        if (signature == null) return Object.class;
        return signature.type();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field "+fieldName;
    }
}
