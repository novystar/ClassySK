package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.ClassManager;
import com.novystxr.classysk.api.FieldValidator;
import com.novystxr.classysk.api.SkriptClass;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.api.util.ExpressionUtils;
import com.novystxr.classysk.api.util.Logger;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExprFieldAccess extends SimpleExpression<Object> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprFieldAccess.class, Object.class)
                        .addPatterns(
                                "%classs%\\:\\:<(\\w+)>",
                                "<(\\w+)>\\:\\:<(\\w+)>"
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
        String fieldName = StringUtils.getLowerCase(parseResult.regexes.get(matchedPattern));
        fieldValidator = new FieldValidator(fieldName);

        if (matchedPattern == 0) {
            skriptClassExpr = (Expression<SkriptClass>) expressions[0];

        } else {
            String className = StringUtils.getLowerCase(parseResult.regexes.getFirst());
            AbstractSkriptClass skriptClass = ClassManager.getClass(className);

            // parse time validation for static access
            fieldValidator.validateSignature(skriptClass);
            fieldValidator.checkAccess();
            return (fieldValidator.isValid().isTrue());
        }

        return true;
    }

    // currently holds onto the first class it receives at runtime
    // proper dynamic access will be added with reflective expressions down the line
    // TODO: revalidate if parent class changed
    @Override
    protected Object @Nullable [] get(Event event) {
        if (fieldValidator.isValid().isUnknown()) {
            fieldValidator.validateSignature(skriptClassExpr.getSingle(event));
            fieldValidator.checkAccess();
        }
        if (!fieldValidator.isValid().isTrue()) return null;
        return fieldValidator.get();
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (fieldValidator.isValid().isUnknown()) {
            fieldValidator.validateSignature(skriptClassExpr.getSingle(event));
            fieldValidator.checkAccess();
        }
        if (!fieldValidator.isValid().isTrue()) return;
        if (!ConverterUtils.canConvert(fieldValidator.signature().type().getC(), delta)) return;

        switch (mode) {
            case SET:
                fieldValidator.attemptSetValue(delta);
            case RESET, DELETE:
                fieldValidator.skriptClass().removeField(fieldValidator.fieldName());
            case REMOVE, ADD:
                if (delta == null) return;
                if (!ConverterUtils.canConvert(fieldValidator.signature().type().getC(), delta)) return;

                Object[] initialValue = fieldValidator.skriptClass().getFieldValue(fieldValidator.fieldName());
                if (fieldValidator.signature().isPlural()) {
                    fieldValidator.attemptSetValue(ExpressionUtils.mutatePlural(initialValue, delta, mode));
                    return;
                }
                fieldValidator.attemptSetValue(ExpressionUtils.mutatePlural(initialValue, delta, mode));
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
                return CollectionUtils.array(fieldValidator.signature().type().getC());
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
