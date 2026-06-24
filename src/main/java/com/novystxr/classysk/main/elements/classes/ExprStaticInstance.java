package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Static Instance")
@Description("Get the static instance of a class, allowing you to access non-instance fields and methods at runtime. May be useful in some scenarios.")
@Example("""
    class MyClass:
    \tpublic static myField: integer = 1
    
    set {_myClass} to static instance of MyClass
    broadcast {_myClass}::myField
    """)
@Since("1.0")
public class ExprStaticInstance extends SimpleExpression<SkriptClass> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprStaticInstance.class, SkriptClass.class)
                .addPattern("static instance of [class] <"+ Classysk.CLASSNAME_PATTERN +">")
                .supplier(ExprStaticInstance::new)
                .build()
        );
    }

    private SkriptClass skriptClass;

    @Override
    public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String name = StringUtils.getLowerCase(parseResult.regexes.getFirst());

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
