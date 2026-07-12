package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.event.MethodRunEvent;
import com.novystxr.classysk.api.methods.SkriptMethod;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("This Instance")
@Keywords("self")
@Description("Only available in non-static methods. Refers to the current class instance from whatever method is running it, Allowing your code to behave differently depending on what instance is running it.")
@Example("""
    class MyClass:
    \tprivate counter: integer
    
    \tpublic getCount() :: integer:
    \t\treturn self::counter

    \tpublic increaseCounter():
    \t\tadd 1 to self::counter
    """)
@Since("1.0")
public class ExprSelf extends SimpleExpression<ClassInstance> {
    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprSelf.class, ClassInstance.class)
                .addPatterns("self", "(this|[the] current) instance")
                .supplier(ExprSelf::new)
                .build()
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (SkriptMethod.isMethodBody(getParser())) {
            return true;
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
        return "self";
    }
}
