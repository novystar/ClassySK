package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.SimpleErrorHandler;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AccessValidator<T extends AccessModifiable> implements RuntimeErrorProducer {
    private ClassInstance instance;
    protected final SkriptClass contextClass;

    private T product = null;
    private final List<T> guesses = new ArrayList<>();

    private final ErrorSource errorSource;

    public final @NotNull T product() {
        return product;
    }

    /**
     * Extend with extra data that is necessary
     */
    public AccessValidator(ErrorSource errorSource, SkriptClass contextClass) {
        this.errorSource = errorSource;
        this.contextClass = contextClass;
    }

    /**
     * Validate your signature and set any extra data
     */
    protected abstract boolean validate(T product, boolean isStaticContext, SkriptClass targetClass);

    protected abstract @Nullable T getProductFromClass(SkriptClass skriptClass);
    protected abstract @Nullable T getProductFromInstance(ClassInstance instance);

    /**
     * A helper method to get all possible return types based off of previous guesses from {@link AccessValidator#validateUnknown(SkriptClass)}
     * @return The {@link AccessValidator#product} return type, OR all return types of {@link AccessValidator#guesses}
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
     * @param possibleTypes Must contain atleast one class, see {@link AccessValidator#possibleTypes()}
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

        if (newInstance == null || !newInstance.isAccessible()) {
            error("Passed instance is null or non-accessible");
            return null;
        }
        if (this.instance == newInstance) {
            return newInstance;
        }
        if (hintClass != null && !newInstance.getParent().inherits(hintClass)) {
            error("Given instance does not match '"+ hintClass.getEffectiveName() +"'");
            return null;
        }
        LogEntry error;
        try (var handler = new SimpleErrorHandler()) {
            if (validateInstance(newInstance)) {
                return newInstance;
            }
            error = handler.getLastError();
        }
        if (error != null) {
            error(error.getMessage());
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
        LogEntry error;
        SkriptClass resultClass = null;

        try (var handler = new SimpleErrorHandler().start()) {
            if (hintClass != null) {
                T product = validateProductFromClass(hintClass);
                if (product != null) {
                    guesses.add(product);
                    resultClass = hintClass;
                }
            } else {
                for (SkriptClass skriptClass : ClassManager.getClasses()) {
                    T product = validateProductFromClass(skriptClass);
                    if (product != null) {
                        guesses.add(product);
                        resultClass = skriptClass;
                    }
                }
            }
            error = handler.getLastError();
        }
        if (guesses.isEmpty() && error != null)
            Skript.error(error.getMessage());

        if (guesses.isEmpty())
            return Kleenean.FALSE;
        if (guesses.size() != 1)
            return Kleenean.UNKNOWN;

        this.product = guesses.getFirst();

        try (var handler = new SimpleErrorHandler().start()) {
            if (validate(product, false, resultClass)) {
                return Kleenean.TRUE;
            }
            error = handler.getLastError();
        }
        if (error != null) Skript.error(error.getMessage());
        return Kleenean.FALSE;
    }

    public final @Nullable T validateProductFromClass(SkriptClass skriptClass) {
        if (!skriptClass.accessible) {
            Skript.error("This class is not accessible");
            return null;
        }
        return getProductFromClass(skriptClass);

    }

    /**
     * Validates an instance against the reference, also works for {@link SkriptClass} validating it as a static reference
     *
     * @param newInstance The instance to validate
     * @return true if the instance was valid, false if it was not
     *
     */
    public final boolean validateInstance(@NotNull ClassInstance newInstance) {
        boolean isStaticContext = !newInstance.isInstance();

        this.product = getProductFromInstance(newInstance);
        if (product != null) {

            if (validate(product, isStaticContext, newInstance.getParent())) {
                this.instance = newInstance;
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull ErrorSource getErrorSource() {
        return errorSource;
    }
}
