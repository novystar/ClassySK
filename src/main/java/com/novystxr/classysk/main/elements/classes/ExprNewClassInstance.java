package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.ClassManager;
import com.novystxr.classysk.api.SkriptClass;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Locale;
import java.util.regex.MatchResult;

public class ExprNewClassInstance extends SimpleExpression<SkriptClass> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprNewClassInstance.class, SkriptClass.class)
                        .addPatterns(
                                "new [instance of [class]] %abstractclasss%",
                                "new instance of [class] <(\\w+)>"
                        )
                        .supplier(ExprNewClassInstance::new)
                        .build()
        );
    }

    Expression<AbstractSkriptClass> abstractSkriptClassExpr;
    AbstractSkriptClass abstractSkriptClass;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String name;

        if (matchedPattern == 0) {
            abstractSkriptClassExpr = (Expression<AbstractSkriptClass>) expressions[0];
            return true;
        } else {
            MatchResult regex = parseResult.regexes.getFirst();
            name = regex.group(1).trim().toLowerCase(Locale.ENGLISH);

            if (!ClassManager.isAccessible(name)) {
                Skript.error("Class named " + name + " does not exist");
                return false;
            }

            abstractSkriptClass = ClassManager.getClass(name);
        }

        return true;
    }

    @Override
    protected SkriptClass @Nullable [] get(Event event) {
        AbstractSkriptClass parent = abstractSkriptClass;
        if (parent == null) parent = abstractSkriptClassExpr.getSingle(event);

        assert parent != null;

        SkriptClass skriptClass = parent.createInstance();

        return new SkriptClass[]{skriptClass};
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
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);

        builder.append("new class instance of");
        if (abstractSkriptClass == null) {
            builder.append(abstractSkriptClassExpr);
        } else {
            builder.append(abstractSkriptClass);
        }

        return builder.toString();

    }
}
