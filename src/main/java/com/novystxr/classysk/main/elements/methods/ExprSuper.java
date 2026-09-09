package com.novystxr.classysk.main.elements.methods;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.main.elements.classes.ExprSelf;

public class ExprSuper extends ExprSelf {
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result) {
        if (!super.init(exprs, pattern, isDelayed, result))
            return false;

        if (contextClass.getExtends() == null) {
            Skript.error("Super can't be used here because the relevant class does not extend any other.");
            return false;
        }
        return true;
    }
}
