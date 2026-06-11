package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprStaticInstance extends SimpleExpression<SkriptClass> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprStaticInstance.class, SkriptClass.class)
                        .addPattern("static instance of [class] <"+ Classysk.classNamePattern +">")
                        .supplier(ExprStaticInstance::new)
                        .build()
        );
    }

    private SkriptClass skriptClass;

    @Override
    public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String name = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());

        if (!ClassManager.isAccessible(name)) {
            Skript.error("Class named " + name + " does not exist");
            return false;
        }

        skriptClass = ClassManager.getClass(name);
        return true;
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
    protected SkriptClass @Nullable [] get(Event event) {
        return new SkriptClass[]{skriptClass};

    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "static instance of "+skriptClass.getEffectiveName();
    }
}
