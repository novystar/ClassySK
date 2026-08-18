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
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.Classysk.CLASSNAME_PATTERN;

@Name("Instance Of")
@Description("Used to check if a class instance belongs to a specified class")
@Example("if {_instance} is an instance of MyClass:")
@Since("1.0.0, 1.3.0 (exact)")
public class CondInstanceOf extends Condition {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.CONDITION,
            SyntaxInfo.builder(CondInstanceOf.class)
                .addPattern("%classinstance% is [a[n]] [:exact] instance of <"+ CLASSNAME_PATTERN +">")
                .addPattern("%classinstance% (isn't|is not) [a[n]] [:exact] instance of <"+ CLASSNAME_PATTERN +">")
                .addPattern("%classinstance% belongs to <"+ CLASSNAME_PATTERN+">")
                .addPattern("%classinstance% (doesn't|does not) belong to <"+ CLASSNAME_PATTERN+">")
                .supplier(CondInstanceOf::new)
                .build()
        );
    }

    private Expression<ClassInstance> instanceExpr;
    private SkriptClass targetClass;
    private boolean isExact;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        instanceExpr = (Expression<ClassInstance>) exprs[0];
        setNegated(pattern == 1 || pattern == 3);

        String name = StringUtils.getLowerCase(result.regexes.getFirst());
        targetClass = ClassManager.getClass(name);
        isExact = result.hasTag("exact");

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

        SkriptClass parent = instance.getParent();
        if (parent == null) return false;

        boolean isInstanceOf = isExact ? parent == targetClass : parent.inherits(targetClass);
        return isNegated() != isInstanceOf;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "instance of";
    }
}
