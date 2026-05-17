package com.novystxr.classysk.api.util;

import com.novystxr.classysk.api.SkriptField.FieldSignature;
import org.skriptlang.skript.lang.converter.Converters;

public class ConverterUtils {

    public static boolean canConvert(Class<?> toClass, Object[] values) {
        for (Object value : values) {

            Class<?> fromClass = value.getClass();

            // for some reason decimal Number can convert to Long??? what the fuck
            if (toClass == Long.class && fromClass != Long.class) return false;

            if (Converters.convert(value, toClass) == null) return false;
        }
        return true;
    }

    public static boolean canConvert(Class<?> toClass, Class<?> fromClass) {

        if (toClass == Long.class && fromClass != Long.class) return false;
        if (toClass == fromClass) return true;
        return Converters.converterExists(fromClass, toClass);
    }

    public static boolean canConvert(FieldSignature signature, Object[] values) {

        if (values == null) return true;
        if (values.length != 1 && !signature.isPlural()) return false;

        return canConvert(signature.type().getC(), values);
    }

}
