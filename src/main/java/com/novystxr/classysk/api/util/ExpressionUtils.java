package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.ArrayList;
import java.util.List;

public class ExpressionUtils {

    public static Object[] mutatePlural(Object @Nullable [] initialValues, Object[] delta, ChangeMode changeMode) {
        if (initialValues == null) initialValues = new Object[]{};

        List<Object> result = new ArrayList<>(List.of(initialValues));

        if (changeMode == ChangeMode.ADD) {
            result.addAll(List.of(delta));
        } else if (changeMode == ChangeMode.REMOVE) {
            result.removeAll(List.of(delta));
        }

        return result.toArray();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static @Nullable Object[] mutateSingle(Object @Nullable [] arrayValue, Object[] delta, ChangeMode changeMode, Class<?> type) {
        Operator operator = (changeMode == ChangeMode.ADD) ? Operator.ADDITION : Operator.SUBTRACTION;
        boolean changed = false;

        Object unwrappedValue = null;
        if (arrayValue != null && arrayValue.length > 0) {
            unwrappedValue = arrayValue[0];
        }

        if (type == Object.class) {
            if (unwrappedValue != null) type = unwrappedValue.getClass();
        }

        if (!Arithmetics.getOperations(operator, type).isEmpty()) {
            for (Object newValue : delta) {

                Class<?> targetClass = type;
                if (type == Object.class)  {
                    targetClass = newValue.getClass();
                }

                OperationInfo info = Arithmetics.lookupOperationInfo(operator, targetClass, type, newValue.getClass());
                if (info == null) continue;

                Object value = unwrappedValue;
                if (unwrappedValue == null) value = Arithmetics.getDefaultValue(info.left());
                if (value == null) continue;

                unwrappedValue = info.operation().calculate(value, newValue);
                changed = true;
            }
            if (changed) {
                return new Object[]{unwrappedValue};
            } else {
                return null;
            }
        }

        Changer<?> changer = Classes.getSuperClassInfo(type).getChanger();
        if (changer == null) return null;

        Class<?>[] classes = changer.acceptChange(changeMode);
        if (classes == null) return null;

        Class<?>[] componentClasses = new Class<?>[classes.length];
        for (int i = 0; i < classes.length; i++) {
            componentClasses[i] = classes[i].isArray() ? classes[i].getComponentType() : classes[i];
        }

        Object[] convertedDelta = Converters.convert(delta, (Class[]) componentClasses, Utils.getSuperType(componentClasses));

        if (convertedDelta.length > 0) {
            Changer.ChangerUtils.change(changer, arrayValue, convertedDelta, changeMode);
            return arrayValue;
        }
        return null;
    }
}
