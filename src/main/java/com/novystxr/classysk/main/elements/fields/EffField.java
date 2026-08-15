package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.ExprUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffField extends Effect {

    public static void register(SyntaxRegistry registry) {
        //noinspection ThrowableInstanceNeverThrown
        Skript.exception(new IllegalStateException("EffField should not be registered"));

        registry.register(SyntaxRegistry.EFFECT, INFO);
    }

    public static SyntaxInfo<EffField> INFO = SyntaxInfo.builder(EffField.class)
        .addPattern("(:public|:private) [:static] [:const] <"+ Classysk.NAME_PATTERN +">\\: %*classinfo% [= %-objects%]")
        .supplier(EffField::new)
        .build();


    public String fieldName;
    public FieldSignature signature;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        fieldName = StringUtils.getConfigLowerCase(result.regexes.getFirst());

        ClassInfoReference reference = ExprUtils.getClassRef(exprs[0]);
        boolean isPlural = reference.isPlural().isTrue();
        Class<?> type = reference.getClassInfo().getC();

        Expression<?> defaultExpr = null;

        if (exprs[1] != null) {
            defaultExpr = exprs[1].getConvertedExpression(type);
            if (defaultExpr == null) {
                Skript.error("Default value can't convert to type: "+reference.getClassInfo());
                return false;
            }

            if (!defaultExpr.isSingle() && !isPlural) {
                Skript.error("Default value is plural but field only accept single values");
                return false;
            }
        }
        signature = new FieldSignature(fieldName, type, defaultExpr, Modifier.collect(result.tags), isPlural);
        return true;
    }

    @Override
    protected void execute(Event event) {
        throw new IllegalStateException();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field declaration";
    }
}
