package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.ArrayIterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * SimpleExpression throws an exception if 'getSingle' is called and 'get' returns more than 1 item.
 *
 * <br> <br> This implementation returns null instead which is best for expressions where 'isSingle' is unknown at parse time (e.g. methods/fields).
 *
 * <br> <br> Also includes helper methods for ADD/REMOVE that I stole from PDC because life is short
 * @see ch.njol.skript.lang.util.SimpleExpression
 * @see {@link org.skriptlang.skript.bukkit.pdc.elements.expressions.ExprPersistentData ExprPersistentData}
 */
public abstract class SafePluralityExpression<T> implements Expression<T>, SyntaxRuntimeErrorProducer {

    private Node node;

    @Override
    public boolean preInit() {
        node = getParser().getNode();
        return Expression.super.preInit();
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public final @Nullable T getSingle(Event event) {
        T[] values = getArray(event);
        if (values.length != 1) return null;

        return values[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T[] getArray(Event event) {
        T[] values = get(event);

        if (values == null) {
            return (T[]) Array.newInstance(getReturnType(), 0);
        }

        // check if contains null values
        int numNonNull = 0;
        for (T value : values) {
            if (value != null) {
                numNonNull++;
            }
        }
        if (numNonNull == values.length) {
            return values;
        }

        // if so, return new array without null values
        T[] valueArray = (T[]) Array.newInstance(getReturnType(), numNonNull);
        int i = 0;
        for (T value : values) {
            if (value != null) valueArray[i++] = value;
        }
        return valueArray;
    }

    @Override
    public T[] getAll(Event event) {
        return getArray(event);
    }

    protected abstract T @Nullable [] get(Event event);

    @Override
    public boolean check(Event event, Predicate<? super T> checker, boolean negated) {
        return SimpleExpression.check(getAll(event), checker, negated, getAnd());
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker) {
        return SimpleExpression.check(getAll(event), checker, false, getAnd());
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
        if (CollectionUtils.containsSuperclass(to, getReturnType()))
            return (Expression<? extends R>) this;
        return ConvertedExpression.newInstance(this, to);
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
    protected void mutateSingle(Object originalValue, Object[] delta, ChangeMode mode, Consumer<Object> setSingle) {
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

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {

    }

    @Override
    public final boolean getAnd() {
        return true;
    }

    @Override
    public boolean setTime(int time) {
        return false;
    }

    @Override
    public int getTime() {
        return 0;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public Expression<?> getSource() {
        return this;
    }

    @Override
    public @Nullable Iterator<? extends T> iterator(Event event) {
        return new ArrayIterator<>(getAll(event));
    }

    @Override
    public boolean isLoopOf(String input) {
        return false;
    }
}
