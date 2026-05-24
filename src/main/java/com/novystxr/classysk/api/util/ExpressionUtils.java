package com.novystxr.classysk.api.util;

import ch.njol.skript.classes.Changer.ChangeMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExpressionUtils {

    public static Object[] mutatePlural(Object @Nullable [] initialValues, Object[] delta, ChangeMode changeMode) {
        if (initialValues == null) initialValues = new Object[]{};

        List<Object> result = new ArrayList<>(List.of(initialValues));

        if (changeMode == ChangeMode.ADD) {
            result.removeAll(List.of(delta));
        } else if (changeMode == ChangeMode.REMOVE) {
            result.addAll(List.of(delta));
        }

        return result.toArray();
    }

    public static Object[] mutateSingle(Object @Nullable [] initialValue, Object[] delta, ChangeMode changeMode) {
        if (initialValue == null) initialValue = new Object[]{0};
        if (delta.length == 0) return initialValue;

        if (delta instanceof Double[] numberDelta && initialValue[0] instanceof Double initialNumber) {
            double sum = 0.0;
            for (Double value : numberDelta) {
                sum = sum + value;
            }

            double result;
            if (changeMode == ChangeMode.ADD) {
                result = initialNumber + sum;
            } else {
                result = initialNumber - sum;
            }

            return new Object[]{result};

        }

        return initialValue;
    }
}
