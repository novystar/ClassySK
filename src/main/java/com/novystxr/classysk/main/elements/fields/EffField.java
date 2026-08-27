package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.util.ParserUtils;
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
        .addPattern("(:public|:protected|:private) [:static] [:const] <"+ Classysk.NAME_PATTERN +">\\: %*classinfo% [= <.+>]")
        .supplier(EffField::new)
        .build();

    public String name;
    private SkriptField field;

    private String unparsedDefault = null;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        name = StringUtils.getConfigLowerCase(result.regexes.getFirst());
        if (result.regexes.size() == 2) {
            unparsedDefault = result.regexes.get(1).group().trim();
        }

        ClassInfoReference reference = ExprUtils.getClassRef(exprs[0]);
        boolean isPlural = reference.isPlural().isTrue();

        Class<?> type = reference.getClassInfo().getC();
        Modifier[] modifiers = Modifier.collect(result.tags);

        this.field = new SkriptField(name, type, modifiers, isPlural);
        return true;
    }

    public SkriptField withOrigin(String origin) {
        field.origin = origin;
        return field;
    }

    public boolean parseDefault() {
        if (unparsedDefault == null) return true;

        Expression<?> defaultExpr = ParserUtils.parseExprNode(unparsedDefault, getNode(), field.type());
        if (defaultExpr == null) {
            return false;
        }
        if (!defaultExpr.isSingle() && !field.isPlural) {
            Skript.error("Default value is plural but field only accepts single values");
            return false;
        }

        field.defaultExpr = defaultExpr;
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
