package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.assignability.AssignabilityBridge;
import com.novystxr.classysk.api.classes.ClassContextHolder;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import static com.novystxr.classysk.api.util.StringUtils.titleCase;


@Name("Cast")
@Keywords("instance as type")
@Description("Cast instance(s) to a desired type. " +
    "This may be useful in certain scenarios when working with super classes in methods/functions. " +
    "Also useful for targeting specific overloads. " +
    "When working with type hints, you'll need to do this to access certain methods if the variables type is implied as the super.")
@Example("""
    on load:
    \tset {_y} to new Y
    \texample({_y}) # Y is implicitly casted to X
    
    class X
    
    function example(x: X instance):
    \t# do stuff with {_x}
   
    \tif {_x} is an instance of Y: # good idea to check before casting so you dont pass an empty value
    \t\totherExample({_x} as Y)
    
    class X extends Y
    
    function otherExample(thing: Y instance):
    \t# do stuff with {_y}
    
    """)
@Since("1.4.0")
public class ExprCast extends SimpleExpression<Object> implements ClassContextHolder {

    public static void register(SyntaxRegistry registry) {
        registry.register(
            SyntaxRegistry.EXPRESSION,
            DefaultSyntaxInfos.Expression.builder(ExprCast.class, Object.class)
                .addPattern("%classinstances% as <"+Classysk.CLASSNAME_PATTERN+">")
                .priority(Classysk.SHADOW_REALM)
                .supplier(ExprCast::new)
                .build()
        );
    }

    private String name;
    private SkriptClass skriptClass;

    private Class<? extends AssignabilityBridge> type;
    private Expression<? extends AssignabilityBridge> instanceExpr;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        name = StringUtils.getLowerCase(result.regexes.getFirst());

        skriptClass = ClassManager.getClass(name);
        if (skriptClass == null) {
            Skript.error("Class '%s' does not exist", titleCase(name));
            return false;
        }
        type = skriptClass.getSubInterface();
        instanceExpr = exprs[0].getConvertedExpression(type);
        if (instanceExpr == null) {
            instanceExpr = exprs[0].getConvertedExpression(skriptClass.getSubclass());
        }

        if (instanceExpr == null) {
            Skript.error("This expression can't possibly cast to '%s'", titleCase(name));
            return false;
        }
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        return instanceExpr.getArray(event);
    }

    @Override
    public boolean isSingle() {
        return instanceExpr.isSingle();
    }

    @Override
    public Class<? extends AssignabilityBridge> getReturnType() {
        return type;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return instanceExpr+" as "+titleCase(name);
    }

    @Override
    public SkriptClass getContextClass() {
        return skriptClass;
    }
}
