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
                .addPattern("%classinstance% is[negated:(n[']t| not)] a[n] instance of <"+ Classysk.CLASSNAME_PATTERN +">")
                .supplier(CondInstanceOf::new)
                .build()
        );
    }

    private Expression<ClassInstance> instanceExpr;
    private SkriptClass targetClass;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        instanceExpr = (Expression<ClassInstance>) expressions[0];
        setNegated(parseResult.hasTag("negated"));

        String name = StringUtils.getLowerCase(parseResult.regexes.getFirst());
        targetClass = ClassManager.getClass(name);

        if (targetClass == null) {
            Skript.error("Class named " + name + " does not exist");
            return false;
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        ClassInstance instance = instanceExpr.getSingle(event);
        if (instance == null) return false;

        return negate(instance.getParent() == targetClass);
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
