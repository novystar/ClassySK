package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SectionSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.main.elements.methods.SecMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprThisInstance extends SimpleExpression<ClassInstance> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
                SyntaxRegistry.EXPRESSION,
                DefaultSyntaxInfos.Expression.builder(ExprThisInstance.class, ClassInstance.class)
                        .addPatterns(
                                "this instance",
                                "self"
                        )
                        .supplier(ExprThisInstance::new)
                        .build()
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (getParser().getCurrentStructure() instanceof SectionSkriptEvent secSkriptEvent) {
            if (secSkriptEvent.getSection() instanceof SecMethod) {
                return true;
            }
        }
        Skript.error("This expression can only be used within a method section.");
        return false;
    }

    @Override
    protected ClassInstance @Nullable [] get(Event event) {
        if (event instanceof MethodRunEvent runEvent) {
            if (runEvent.instance.isInstance()) {
                return new ClassInstance[]{runEvent.instance};
            }
        }
        return null;
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
        return "instance";
    }
}
