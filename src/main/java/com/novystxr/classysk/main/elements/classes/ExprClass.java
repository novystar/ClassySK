package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.SkriptClassWrapper;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Wrapper Class")
@Keywords({"from name", "class"})
@Description("Gets the wrapper class for a given name, useful for comparisons")
@Example("if class of {_instance} is class named MyClass:")
@Since("1.0.0")
public class ExprClass extends SimpleExpression<SkriptClassWrapper> {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprClass.class, SkriptClassWrapper.class)
                .addPattern("[skript|wrapper] class [named] <"+ Classysk.CLASSNAME_PATTERN+">")
                .supplier(ExprClass::new)
                .build()
        );
    }

    private SkriptClass skriptClass;
    private String className;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        className = StringUtils.getLowerCase(result.regexes.getFirst());
        skriptClass = ClassManager.getClass(className);

        if (skriptClass == null || !skriptClass.accessible) {
            Skript.error("Class named '%s' does not exist", StringUtils.titleCase(className));
            return false;
        }

        return true;
    }

    @Override
    protected SkriptClassWrapper @Nullable [] get(Event event) {
        return new SkriptClassWrapper[]{SkriptClassWrapper.of(skriptClass)};
    }

    @Override
    public Class<? extends SkriptClassWrapper> getReturnType() {
        return SkriptClassWrapper.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "wrapper class "+className;
    }
}
