package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprNewClassInstance extends SimpleExpression<ClassInstance> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprNewClassInstance.class, ClassInstance.class)
                        .addPatterns(
                                "new [instance of [class]] %classs%",
                                "new instance of [class] <"+ Classysk.classNamePattern +">"
                        )
                        .supplier(ExprNewClassInstance::new)
                        .build()
        );
    }

    Expression<SkriptClass> abstractSkriptClassExpr;
    SkriptClass skriptClass;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            abstractSkriptClassExpr = (Expression<SkriptClass>) expressions[0];
            return true;
        } else {
            String name = ClassyStringUtils.getLowerCase(parseResult.regexes.getFirst());
            if (!ClassManager.isAccessible(name)) {
                Skript.error("Class named " + name + " does not exist");
                return false;
            }
            skriptClass = ClassManager.getClass(name);
        }

        return true;
    }

    @Override
    protected ClassInstance @Nullable [] get(Event event) {
        SkriptClass parent = skriptClass;
        if (parent == null) parent = abstractSkriptClassExpr.getSingle(event);
        assert parent != null;

        ClassInstance instance = parent.createInstance();
        return new ClassInstance[]{instance};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends ClassInstance> getReturnType() {
        return ClassInstance.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("new class instance of");
        if (skriptClass == null) {
            builder.append(abstractSkriptClassExpr);
        } else {
            builder.append(skriptClass);
        }
        return builder.toString();

    }
}
