package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.AccessType;
import com.novystxr.classysk.api.SkriptField.FieldSignature;
import com.novystxr.classysk.api.event.FieldRegistrationEvent;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffField extends Effect {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(EffField.class)
                        .addPattern("[:private] [:static] field <(\\w+)>\\: %-classinfo% [=[ ]%-objects%]")
                        .supplier(EffField::new)
                        .build()
        );
    }

    private String fieldName;

    private boolean isPrivate;
    private boolean isStatic = false;
    private boolean isPlural = false;

    private Expression<ClassInfo<?>> exprClassInfo;
    private Expression<Object> exprValue;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {

        ParserInstance parserInstance = getParser();
        if (!parserInstance.isActive()) return false;

        if (!(parserInstance.getCurrentStructure() instanceof StructClass)) {
            Skript.error("Field definition can only be used within a class structure.");
            return false;
        }

        fieldName = StringUtils.getLowerCase(parser.regexes.getFirst());
        exprClassInfo = (Expression<ClassInfo<?>>) exprs[0];

        isPrivate = parser.hasTag("private");
        isStatic = parser.hasTag("static");

        if (exprClassInfo != null) {
            Literal<ClassInfoReference> classInfoReference = (Literal<ClassInfoReference>) ClassInfoReference.wrap(exprClassInfo);
            isPlural = classInfoReference.getSingle().isPlural().isTrue();
        }

        if (exprs[1] != null) {
            exprValue = (Expression<Object>) exprs[1].getConvertedExpression(Object.class);
        }

        return true;
    }

    // I want to return while still using skript's syntax validation
    // so we get this weird effect that should never actually be executed (✿◡‿◡)
    public @Nullable FieldSignature getSignature(FieldRegistrationEvent event) {

        ClassInfo<?> type = null;
        if (exprClassInfo != null) type = exprClassInfo.getSingle(event);

        if (type == null) {
            Skript.error("Could not resolve field type");
            return null;
        }

        AccessType accessType = (isPrivate) ? AccessType.PRIVATE : AccessType.PUBLIC;

        Object[] defaultValue = null;

        if (isPlural) {
            if (exprValue != null) {
                defaultValue = exprValue.getArray(event);
            }
        } else if (exprValue != null) {
            if (exprValue.isSingle()) {
                defaultValue = new Object[]{exprValue.getSingle(event)};
            } else {
                Skript.error("Field specifies single value but default value is plural.");
                return null;
            }
        }


        FieldSignature signature = new FieldSignature(fieldName, type, defaultValue, accessType, isStatic, isPlural);

        if (!signature.canConvert(defaultValue)) {

            Skript.error("Illegal field definition! Default value is not of required type: " + type.getName());
            return null;
        }

        return signature;
    }

    @Override
    protected void execute(Event event) {
        Skript.error("How did we get here?");
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);

        builder.appendIf(isPrivate, "private");
        builder.appendIf(isStatic, "static");
        builder.append(exprClassInfo);
        builder.append("field");
        builder.append(fieldName);
        builder.appendIf(exprValue != null, exprValue);

        return builder.toString();
    }
}
