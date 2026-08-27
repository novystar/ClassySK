package com.novystxr.classysk.main.elements.classes;

import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.novystxr.classysk.api.TypeWrappable;
import com.novystxr.classysk.api.classes.ClassInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Typed/Untyped instance")
@Since("1.1.0")
@Keywords({"typed instance", "untyped instance"})
@Description("This expression can be used to wrap/unwrap an instance respective to its given typed wrapper. This is useful is niche scenarios where the concrete type of the instance actually matters.")
@Example("""
    # if this were a generic instance the check would fail, so we make sure it's wrapped before doing a vanilla type check
    # in this scenario it's recommended to use the dedicated 'is an instance of' condition though
    if typed {_instance} is an Example instance
    """)
@Example("""
    # with reflection we wanna make sure it's the correct type before calling methods on it
    set {_value} to (untyped {_instance}).getFieldValue("myField")
    
    # typed and untyped instances are also interfaced, so we could also just call unwrap() or wrap() respectively, which is what this expression does under the hood
    set {_value} to {_instance}.unwrap().getFieldValue("myField")
    """)
public class ExprTypedInstance extends SimpleExpression<Object> {

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, DefaultSyntaxInfos.Expression.builder(ExprTypedInstance.class, Object.class)
            .addPattern("[:un]typed %classinstance/typedinstance%")
            .supplier(ExprTypedInstance::new)
            .build()
        );
    }

    private boolean typed;
    private Expression<TypeWrappable<?, ?>> wrappable;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        typed = !result.hasTag("un");
        wrappable = (Expression<TypeWrappable<?, ?>>) exprs[0];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        var value = this.wrappable.getSingle(event);
        if (value == null) return null;

        return CollectionUtils.array(typed ? value.wrap() : value.unwrap());
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return typed ? Object.class : ClassInstance.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "";
    }
}
