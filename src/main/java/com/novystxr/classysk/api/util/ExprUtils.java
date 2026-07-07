package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.skript.variables.Variables;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.function.Consumer;

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

    /**
     * Helper for adding/removing values from a given value. Falls back to arithmetic, then type changers.
     * See {@link Variables} for initial impl. Adapted due to need to maintain type.
     * @param originalValue The previous existing value.
     * @param delta The values to add/remove
     * @param mode Whether to add or remove.
     * @param setSingle A consumer used to set the new value if arithmetic is used.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void mutateSingle(Object originalValue, Object[] delta, ChangeMode mode, Consumer<Object> setSingle) {
        Class<?> clazz = originalValue == null ? null : originalValue.getClass();
        Operator operator = mode == ChangeMode.ADD ? Operator.ADDITION : Operator.SUBTRACTION;
        Changer<?> changer;
        Class<?>[] acceptedClasses;
        // attempt to find arithmetic for each value in delta
        if (clazz == null || !Arithmetics.getOperations(operator, clazz).isEmpty()) {
            boolean changed = false;
            for (Object newValue : delta) {
                var info = Arithmetics.getOperationInfo(operator, clazz != null ? (Class) clazz : newValue.getClass(), newValue.getClass());
                if (info == null)
                    continue;
                Object value = originalValue == null ? Arithmetics.getDefaultValue(info.left()) : originalValue;
                if (value == null)
                    continue;
                originalValue = info.operation().calculate(value, newValue);
                changed = true;
            }
            if (changed)
                setSingle.accept(originalValue);
            // attempt to use the class's changer
        } else if ((changer = Classes.getSuperClassInfo(clazz).getChanger()) != null && (acceptedClasses = changer.acceptChange(mode)) != null) {
            Object[] originalValueArray = (Object[]) Array.newInstance(originalValue.getClass(), 1);
            originalValueArray[0] = originalValue;

            Class<?>[] singularAcceptedClasses = new Class<?>[acceptedClasses.length];
            for (int i = 0; i < acceptedClasses.length; i++)
                singularAcceptedClasses[i] = acceptedClasses[i].isArray() ? acceptedClasses[i].getComponentType() : acceptedClasses[i];

            Object[] convertedDelta = Converters.convert(delta, singularAcceptedClasses, Object.class);
            Changer.ChangerUtils.change(changer, originalValueArray, convertedDelta, mode);
        }
    }
}
