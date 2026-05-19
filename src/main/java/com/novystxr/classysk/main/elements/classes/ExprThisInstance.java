package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.SkriptClass;
import com.novystxr.classysk.api.event.MethodRunEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprThisInstance extends SimpleExpression<SkriptClass> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprThisInstance.class, SkriptClass.class)
                        .addPattern("[this ]instance")
                        .supplier(ExprThisInstance::new)
                        .build()
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Class<? extends Event>[] events = getParser().getCurrentEvents();

        if (events == null) return false;

        if (events[0] != MethodRunEvent.class) {
            Skript.error("This expression can only be used within a method section.");
            return false;
        }

        return true;
    }

    @Override
    protected SkriptClass @Nullable [] get(Event event) {
        if (event instanceof MethodRunEvent runEvent) {
            if (runEvent.instance.isInstance()) {
                return new SkriptClass[]{runEvent.instance};
            }
        }

        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends SkriptClass> getReturnType() {
        return SkriptClass.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "instance";
    }
}
