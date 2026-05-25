package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.ClassManager;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprAbstractClass extends SimpleExpression<AbstractSkriptClass> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprAbstractClass.class, AbstractSkriptClass.class)
                        .addPattern("abstract instance of [class] <(\\w+)>")
                        .supplier(ExprAbstractClass::new)
                        .build()
        );
    }

    private AbstractSkriptClass abstractSkriptClass;

    @Override
    public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String name = StringUtils.getLowerCase(parseResult.regexes.getFirst());

        if (!ClassManager.isAccessible(name)) {
            Skript.error("Class named " + name + " does not exist");
            return false;
        }

        abstractSkriptClass = ClassManager.getClass(name);
        return true;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends AbstractSkriptClass> getReturnType() {
        return AbstractSkriptClass.class;
    }

    @Override
    protected AbstractSkriptClass @Nullable [] get(Event event) {
        return new AbstractSkriptClass[]{abstractSkriptClass};

    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "Abstract Class Expression (" + abstractSkriptClass.name + ")";
    }
}
