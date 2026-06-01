package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

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
        if (matchedPattern == 0) {
            abstractSkriptClassExpr = (Expression<AbstractSkriptClass>) expressions[0];
            return true;
        } else {
            String name = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());
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
