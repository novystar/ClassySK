package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ClassInfoReference;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import static ch.njol.skript.classes.Changer.ChangeMode.*;

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
     * Helper for adding/removing list values
     *
     * @param initialValue The previous existing values
     * @param delta The values to remove/add
     * @param mode ADD, REMOVE or REMOVE_ALL
     * @param setPlural A consumer used to set the resulting value
     */
    public static void mutatePlural(Object[] initialValue, Object[] delta, ChangeMode mode, Consumer<Object[]> setPlural) {
        List<Object> result = new ArrayList<>(Arrays.asList(initialValue));

        if (mode == ADD) {
            Collections.addAll(result, delta);
        } else if (mode == REMOVE || mode == REMOVE_ALL) {
            List<Object> deltaList = new ArrayList<>(Arrays.asList(delta));

            for (Iterator<?> resultIterator = result.iterator(); resultIterator.hasNext();) {
                Object value = resultIterator.next();

                for (Iterator<?> deltaIterator = deltaList.iterator(); deltaIterator.hasNext();) {
                    if (Relation.EQUAL.isImpliedBy(Comparators.compare(value, deltaIterator.next()))) {
                        resultIterator.remove();

                        if (mode == REMOVE) {
                            deltaIterator.remove();
                            break;
                        }
                    }
                }
            }
        } else return;

        setPlural.accept(result.toArray());
    }

    /**
     * Helper for adding/removing values from a given value. Falls back to arithmetic, then type changers.
     * @see org.skriptlang.skript.bukkit.pdc.elements.expressions.ExprPersistentData
     * @param initialValue The previous existing value.
     * @param delta The values to add/remove
     * @param mode Whether to add or remove.
     * @param setSingle A consumer used to set the new value if arithmetic is used.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void mutateSingle(Object initialValue, Object[] delta, ChangeMode mode, Consumer<Object> setSingle) {
        Class<?> clazz = initialValue == null ? null : initialValue.getClass();
        Operator operator = mode == ADD ? Operator.ADDITION : Operator.SUBTRACTION;
        Changer<?> changer;
        Class<?>[] acceptedClasses;
        // attempt to find arithmetic for each value in delta
        if (clazz == null || !Arithmetics.getOperations(operator, clazz).isEmpty()) {
            boolean changed = false;
            for (Object newValue : delta) {
                var info = Arithmetics.getOperationInfo(operator, clazz != null ? (Class) clazz : newValue.getClass(), newValue.getClass());
                if (info == null)
                    continue;
                Object value = initialValue == null ? Arithmetics.getDefaultValue(info.left()) : initialValue;
                if (value == null)
                    continue;
                initialValue = info.operation().calculate(value, newValue);
                changed = true;
            }
            if (changed)
                setSingle.accept(initialValue);
            // attempt to use the class's changer
        } else if ((changer = Classes.getSuperClassInfo(clazz).getChanger()) != null && (acceptedClasses = changer.acceptChange(mode)) != null) {
            Object[] originalValueArray = (Object[]) Array.newInstance(initialValue.getClass(), 1);
            originalValueArray[0] = initialValue;

            Class<?>[] singularAcceptedClasses = new Class<?>[acceptedClasses.length];
            for (int i = 0; i < acceptedClasses.length; i++)
                singularAcceptedClasses[i] = acceptedClasses[i].isArray() ? acceptedClasses[i].getComponentType() : acceptedClasses[i];

            Object[] convertedDelta = Converters.convert(delta, singularAcceptedClasses, Object.class);
            Changer.ChangerUtils.change(changer, originalValueArray, convertedDelta, mode);
        }
    }
}
