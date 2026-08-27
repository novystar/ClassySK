package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.classes.SkriptClass.AnonymousClass;
import com.novystxr.classysk.api.util.SimpleErrorHandler;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

import java.util.*;

public abstract class Validator<T extends AccessModifiable> implements RuntimeErrorProducer {
    private ClassInstance instance;
    private final SkriptClass contextClass;

    private T product = null;
    private final List<T> guesses = new ArrayList<>();

    private final ErrorSource errorSource;

    public final T product() {
        return product;
    }

    /**
     * Extend with extra data that is necessary
     */
    public Validator(ErrorSource errorSource, SkriptClass contextClass) {
        this.errorSource = errorSource;
        this.contextClass = contextClass;
    }

    protected abstract boolean validate(T product, SkriptClass contextClass);

    protected abstract @Nullable T getProductFromClass(SkriptClass skriptClass);
    protected abstract @Nullable T getProductFromInstance(ClassInstance instance);

    protected SkriptClass contextClass() {
        if (contextClass == null) return null;
        return contextClass instanceof AnonymousClass ? contextClass : ClassManager.getClass(contextClass.name);
    }

    /**
     *
     * Assures that the resulting array matches the required type, converting it if needed.
     * This returns null if the conversion failed or found mismatched plurality.
     *
     * @param value The value array to check
     * @param targetType The type to match and convert against
     * @param plural Whether the resulting array may contain more than 1 element
     * @return The safe converted array
     */
    public static Object @Nullable [] getSafeConverted(Object[] value, Class<?> targetType, boolean plural) {
        if (!Arrays.stream(value).allMatch(targetType::isInstance)) {
            value = Converters.convert(value, targetType);

            if (value.length == 0) {
                return null;
            }
        }
        if (!plural && value.length > 1) {
            return null;
        }
        return value;
    }

    /**
     * A helper method to get all possible return types based off of previous guesses from {@link Validator#validateUnknown(SkriptClass)}
     * @return The {@link Validator#product} return type, OR all return types of {@link Validator#guesses}
     */
    public final Class<?>[] possibleTypes() {
        if (product != null) return new Class<?>[]{product.type()};
        if (guesses.isEmpty()) return new Class<?>[]{Object.class};

        Class<?>[] possibleTypes = new Class[guesses.size()];

        int i = 0;
        for (T guess : guesses) {
            Class<?> type = guess.type();
            if (type != null) possibleTypes[i++] = guess.type();
        }
        if (i == 0) return new Class<?>[]{Object.class};
        return i == possibleTypes.length ? possibleTypes : Arrays.copyOf(possibleTypes, i);
    }


    /**
     * Tries to find the best possible return type for that pattern to report
     *
     * @param possibleTypes Must contain at least one class, see {@link Validator#possibleTypes()}
     *
     * @return The highest denominator of possible return types
     */
    public static Class<?> bestReturnType(Class<?>[] possibleTypes) {
        if (possibleTypes.length == 1) {
            return possibleTypes[0];
        }
        return Utils.highestDenominator(Object.class, possibleTypes);
    }

    public final Class<?> exactTypeOr(@Nullable Class<?> type) {
        if (product == null) return type;
        return product.type();
    }

    /**
     *
     * @return TRUE if the pattern is known to be single,
     * FALSE if the pattern is known to be plural,
     * UNKNOWN if the correct class could not be determined at parse time
     */
    public final Kleenean shouldBeSingle() {
        if (product != null) return Kleenean.get(!product.isPlural());
        if (guesses.isEmpty()) return Kleenean.UNKNOWN;

        boolean hasSingle = false;
        boolean hasPlural = false;
        for (T guess : guesses) {
            if (guess.isPlural()) hasPlural = true;
            else hasSingle = true;
        }
        if (hasSingle != hasPlural) return Kleenean.get(hasSingle);
        return Kleenean.UNKNOWN;
    }

    /**
     * Helper method for validating instances at runtime
     *
     * @param event The event to grab the instance with
     * @param instanceExpr The expression to grab the instance from
     * @param hintClass The hint class retrieved at init, null if unspecified
     * @return The instance which has been validated against the reference OR null if it wasn't valid
     */
    public final @Nullable ClassInstance getValidInstance(Event event, Expression<ClassInstance> instanceExpr, @Nullable SkriptClass hintClass) {
        ClassInstance newInstance = instanceExpr.getSingle(event);
        if (hintClass == contextClass) hintClass = null;

        if (newInstance == null) {
            error("Target instance was not set");
            return null;
        }
        SkriptClass parent = newInstance.getParent();
        if (parent == null) {
            error("Target instance is not accessible");
            return null;
        }

        if (this.instance == newInstance) return newInstance;

        if (hintClass != null && !hintClass.inherits(parent)) {
            error("Given instance does not match '"+ hintClass.getEffectiveName() +"'");
            return null;
        }
        if (validateInstance(newInstance)) {
            return newInstance;
        }
        return null;
    }

    /**
     * Used for validating instances at parse time
     *
     * @param hintClass Either the context class from 'self' or user defined. If null, the validator will attempt to check every class
     * @return TRUE if could find the right class,
     * FALSE if could find the right class but the reference is not valid,
     * UNKNOWN if could not find the right class
     */
    public final Kleenean validateUnknown(@Nullable SkriptClass hintClass) {
        Collection<SkriptClass> check = hintClass == null ? ClassManager.getClasses() : List.of(hintClass);
        LogEntry error;
        try (var handler = new SimpleErrorHandler().start()) {
            for (SkriptClass skriptClass : check) {

                T product = getProductFromClass(skriptClass);
                if (product == null || !validate(product, contextClass()))
                    continue;

                guesses.add(product);
            }
            error = handler.getLastError();
        }
        if (guesses.isEmpty()) {
            if (error != null)
                Skript.error(error.getMessage());
            return Kleenean.FALSE;
        }
        if (guesses.size() != 1) {
            return Kleenean.UNKNOWN;
        }
        this.product = guesses.getFirst();
        return Kleenean.TRUE;
    }

    /**
     * Validates a class against the reference
     * @param skriptClass The class to validate
     * @return true if the class was valid, false if it was not
     */
    public final boolean validateStatic(@NotNull SkriptClass skriptClass) {
        this.product = getProductFromClass(skriptClass);
        if (product == null) return false;

        return validate(product, contextClass());
    }

    /**
     * Validates an instance against the reference
     *
     * @param newInstance The instance to validate
     * @return true if the instance was valid, false if it was not
     *
     */
    public final boolean validateInstance(@NotNull ClassInstance newInstance) {
        LogEntry error;
        try (var handler = new SimpleErrorHandler().start()) {
            this.product = getProductFromInstance(newInstance);
            if (product != null) {
                if (validate(product, contextClass())) {
                    this.instance = newInstance;
                    return true;
                }
            }
            error = handler.getLastError();
        }
        if (error != null) {
            error(error.getMessage());
        }
        return false;
    }

    @Override
    public @NotNull ErrorSource getErrorSource() {
        return errorSource;
    }
}
