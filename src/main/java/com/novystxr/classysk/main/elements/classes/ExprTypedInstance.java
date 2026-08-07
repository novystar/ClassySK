package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.novystxr.classysk.api.TypeWrappable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprTypedInstance extends SimpleExpression<Object> {

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, DefaultSyntaxInfos.Expression.builder(ExprTypedInstance.class, Object.class)
            .addPattern("[:un]typed %classinstance/typedinstance%")
            .supplier(ExprTypedInstance::new)
            .build()
        );
    }

    private boolean typed;
    private Expression<TypeWrappable<?, ?>> wrappable;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        typed = !result.hasTag("un");
        wrappable = (Expression<TypeWrappable<?, ?>>) exprs[0];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        var value = this.wrappable.getSingle(event);
        if (value == null) return null;

        return CollectionUtils.array(typed ? value.wrap() : value.unwrap());
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return TypeWrappable.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "";
    }
}
