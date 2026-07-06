package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.util.ClassInfoReference;

public class ExprUtils {

    /**
     * A helper method to get the literal reference of a classinfo.
     */
    public static ClassInfoReference getClassRef(Expression<?> expr) {
        //noinspection unchecked
        var classInfoLit = (Literal<ClassInfo<?>>) expr;
        var ref = ((Literal<ClassInfoReference>) ClassInfoReference.wrap(classInfoLit));
        return ref.getSingle();
    }
}
