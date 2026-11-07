package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.main.elements.classes.ExprSelf;
import org.jetbrains.annotations.Nullable;

public class ExprSuper extends ExprSelf {

    @SuppressWarnings("unchecked")
    public static @Nullable Expression<ClassInstance> newInstance() {
        ExprSuper exprSuper = new ExprSuper();
        if (exprSuper.init(null, 0, null, null)) {
            return (Expression<ClassInstance>) exprSuper.getConvertedExpression(ClassInstance.class);
        }
        return null;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        if (!super.init(exprs, pattern, isDelayed, result))
            return false;

        if (getContextClass().getExtends() == null) {
            Skript.error("Super can't be used here because the relevant class does not extend any other.");
            return false;
        }
        return true;
    }
}
