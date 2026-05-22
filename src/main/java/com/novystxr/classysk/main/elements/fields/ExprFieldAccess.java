package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.ClassManager;
import com.novystxr.classysk.api.SkriptClass;
import com.novystxr.classysk.api.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.ClassyUtils;
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
    private AbstractSkriptClass abstractSkriptClass;
    private FieldSignature abstractSignature;
    private String fieldName;

    private boolean isMatchedInstance;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            isMatchedInstance = true;

            skriptClassExpr = (Expression<SkriptClass>) expressions[0];
            fieldName = ClassyUtils.getLowerCase(parseResult.regexes.getFirst());

        } else { // parse time validation if static reference
            isMatchedInstance = false;

            String className = ClassyUtils.getLowerCase(parseResult.regexes.get(0));
            fieldName = ClassyUtils.getLowerCase(parseResult.regexes.get(1));

            abstractSkriptClass = ClassManager.getClass(className);
            if (checkIllegalAccess(abstractSkriptClass)) {
                illegalAccess(abstractSkriptClass, true);
                return false;
            }
            abstractSignature = abstractSkriptClass.getParent().getFieldSignature(fieldName);
            if (abstractSignature == null) {
                Skript.error("Unable to resolve static field signature "+className+"::"+fieldName);
                return false;
            }
        }

        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        SkriptClass skriptClass = getMatchedInstance(event);
        if (skriptClass == null) return null;

        return skriptClass.getFieldValue(fieldName);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        SkriptClass skriptClass = getMatchedInstance(event);
        FieldSignature signature;

        if (skriptClass == null) return;

        if (isMatchedInstance) {
            signature = skriptClass.getParent().getFieldSignature(fieldName);
            if (signature == null) return;

        } else {
            signature = abstractSignature; // runtime access check for safety if class is reloaded
            if (checkIllegalAccess(skriptClass)) {
                illegalAccess(skriptClass, false);
                return;
            }
        }

        switch (mode) {
            case SET:
                attemptSetValue(skriptClass, signature, delta);
                break;
            case RESET, DELETE:
                skriptClass.removeField(fieldName);
                break;
            case REMOVE, ADD:
                Object[] initialValue = skriptClass.getFieldValue(fieldName);
                if (initialValue == null) initialValue = new Object[]{};
                if (delta == null) return;
                Object[] result = null;

                if (signature.isPlural()) {
                    List<Object> initialValueList = new ArrayList<>(Arrays.asList(initialValue));
                    if (mode == Changer.ChangeMode.ADD) {
                        initialValueList.addAll(List.of(delta));
                    } else {
                        initialValueList.removeAll(List.of(delta));
                    }

                    result = initialValueList.toArray();

                } else if (delta.length == 1) {
                    if (delta[0] instanceof Number deltaNumber && initialValue[0] instanceof Number initialNumber) {

                        if (mode == Changer.ChangeMode.ADD) {
                            result = new Object[]{
                                    initialNumber.doubleValue() + deltaNumber.doubleValue()
                            };
                        } else {

                            result = new Object[]{
                                    initialNumber.doubleValue() - deltaNumber.doubleValue()
                            };
                        }

                    }
                }

                if (result == null) return;

                attemptSetValue(skriptClass, signature, result);

                break;
        }
    }

    private @Nullable SkriptClass getMatchedInstance(Event event) {
        if (isMatchedInstance) {
            SkriptClass skriptClass = skriptClassExpr.getSingle(event);
            if (checkIllegalAccess(skriptClass)) return null;
            return skriptClass;
        } else {
            return abstractSkriptClass;
        }
    }

    private void attemptSetValue(SkriptClass skriptClass, FieldSignature signature, Object[] value) {

        if (signature.canConvert(value)) {
            skriptClass.getField(fieldName).setValue(value);
        } else {
            error("Field " + skriptClass.getEffectiveName() + "::" + fieldName + " expects type " + signature.type().getName()+" but got " + ClassyUtils.formatList(value));
        }
    }

    private boolean checkIllegalAccess(@Nullable SkriptClass skriptClass) {

        if (skriptClass == null) return true;
        return !skriptClass.checkAccess(getParser(), fieldName);
    }

    private void illegalAccess(@Nullable SkriptClass skriptClass, boolean throwError) {
        String effectiveName = "unknown";
        if (skriptClass != null) effectiveName = skriptClass.getEffectiveName();

        String text = "Illegal Access Warning! Tried to access non-existent field " + effectiveName + "#" + fieldName + ", or tried to access it from improper context";

        if (throwError) error(text);
        else warning(text);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {

        boolean result = switch (mode) {
            case SET, RESET, DELETE, REMOVE, ADD -> true;
            default -> false;
        };

        return (result) ? CollectionUtils.array(Object.class) : null;

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
