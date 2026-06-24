package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Instance Of")
@Description("Used to check if a class instance belongs to a specified class")
@Example("if {_instance} is an instance of MyClass:")
@Since("1.0")
public class CondInstanceOf extends Condition {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.CONDITION,
            SyntaxInfo.builder(CondInstanceOf.class)
                .addPattern("%classinstance% is[negated:(n[']t| not)] a[n] instance of [class] <"+ Classysk.CLASSNAME_PATTERN +">")
                .supplier(CondInstanceOf::new)
                .build()
        );
    }

    private Expression<ClassInstance> skriptClassExpr;
    private Expression<SkriptClass> abstractClassExpr;
    private SkriptClass abstractClass;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        skriptClassExpr = (Expression<ClassInstance>) expressions[0];

        setNegated(parseResult.hasTag("negated"));

        if (matchedPattern == 1) {
            abstractClassExpr = (Expression<SkriptClass>) expressions[1];
        } else {
            String name = StringUtils.getLowerCase(parseResult.regexes.getFirst());
            if (!ClassManager.isAccessible(name)) {
                Skript.error("Class named " + name + " does not exist");
                return false;
            }
            abstractClass = ClassManager.getClass(name);
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        SkriptClass targetAbstractClass = (abstractClass == null) ? abstractClassExpr.getSingle(event) : abstractClass;
        if (targetAbstractClass == null) return false;

        ClassInstance targetClass = skriptClassExpr.getSingle(event);
        if (targetClass == null) return false;

        return negate(targetClass.getParent() == targetAbstractClass);
    }

    private boolean negate(boolean condition) {
        if (isNegated()) return !condition;
        return condition;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "instance of";
    }
}
