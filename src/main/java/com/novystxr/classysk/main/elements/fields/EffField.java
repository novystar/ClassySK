package com.novystxr.classysk.main.elements.fields;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.util.DefaultValue;
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

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        name = StringUtils.getConfigLowerCase(result.regexes.getFirst());

        ClassInfoReference reference = ExprUtils.getClassRef(exprs[0]);
        boolean isPlural = reference.isPlural().isTrue();

        Class<?> type = reference.getClassInfo().getC();
        Modifier[] modifiers = Modifier.collect(result.tags);

        DefaultValue<?> defaultValue = null;
        if (result.regexes.size() == 2) {
            String rawExpr = result.regexes.get(1).group().trim();
            defaultValue = new DefaultValue.Dynamic<>(rawExpr, type, isPlural);
        }

        this.field = new SkriptField(name, type, modifiers, isPlural, defaultValue);
        return true;
    }

    public SkriptField withOrigin(String origin) {
        field.origin = origin;
        return field;
    }

    public boolean parseDefault() {
        Node node = getNode();
        SkriptLogger.setNode(node);
        if (field.defaultValue != null)
            return field.defaultValue.parse(node);
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
