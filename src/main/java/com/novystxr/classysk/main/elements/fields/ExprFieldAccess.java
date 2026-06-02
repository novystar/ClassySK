package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.fields.FieldValidator;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.api.util.ExpressionUtils;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprFieldAccess extends SimpleExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprFieldAccess.class, Object.class)
                        .addPatterns(
                                "%classs%\\:\\:<"+ Classysk.namePattern +">",
                                "<"+ Classysk.namePattern +">\\:\\:<"+ Classysk.namePattern +">"
                        )
                        .supplier(ExprFieldAccess::new)
                        .priority(SyntaxInfo.PATTERN_MATCHES_EVERYTHING)
                        .build()

        );
    }

    private Expression<SkriptClass> skriptClassExpr;
    private FieldValidator fieldValidator;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String fieldName = ClassyStringUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        if (matchedPattern == 0) {
            fieldValidator = new FieldValidator(fieldName, false);
            skriptClassExpr = (Expression<SkriptClass>) expressions[0];

        } else {
            fieldValidator = new FieldValidator(fieldName, true);
            String className = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());
            AbstractSkriptClass skriptClass = ClassManager.getClass(className);

            // validate time validation for static access
            fieldValidator.validate(skriptClass);
            fieldValidator.checkAccess(getParser());
            return (fieldValidator.isValid().isTrue());
        }
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (fieldValidator.isValid().isUnknown()) {
            fieldValidator.validate(skriptClassExpr.getSingle(event));
            fieldValidator.checkAccess(event);
        } else {
            fieldValidator.updateInstance(skriptClassExpr, event);
        }
        if (!fieldValidator.isValid().isTrue()) return null;
        return fieldValidator.get();
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (fieldValidator.isValid().isUnknown()) {
            fieldValidator.validate(skriptClassExpr.getSingle(event));
            fieldValidator.checkAccess(event);
        } else {
            fieldValidator.updateInstance(skriptClassExpr, event);
        }
        if (!fieldValidator.isValid().isTrue()) return;

        switch (mode) {
            case SET:
                fieldValidator.attemptSetValue(delta);
                break;
            case RESET, DELETE:
                fieldValidator.skriptClass().removeField(fieldValidator.fieldName());
                break;
            case REMOVE, ADD:
                if (delta == null) return;
                if (!ConverterUtils.canConvert(fieldValidator.signature().type(), delta)) return;

                Object[] initialValue = fieldValidator.skriptClass().getFieldValue(fieldValidator.fieldName());
                if (fieldValidator.signature().isPlural()) {
                    fieldValidator.attemptSetValue(ExpressionUtils.mutatePlural(initialValue, delta, mode));
                    return;
                }
                fieldValidator.attemptSetValue(ExpressionUtils.mutatePlural(initialValue, delta, mode));
                break;
        }
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        boolean result = switch (mode) {
            case SET, RESET, DELETE, REMOVE, ADD -> true;
            default -> false;
        };
        if (result) {
            if (fieldValidator.signature() != null) {
                return CollectionUtils.array(fieldValidator.signature().type());
            }
            return CollectionUtils.array(Object.class);
        }
        return null;

    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean canBeSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field access";
    }
}
