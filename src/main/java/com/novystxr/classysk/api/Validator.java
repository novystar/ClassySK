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
import com.novystxr.classysk.main.elements.classes.ExprSelf;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

import java.util.*;

public abstract class Validator<T extends AccessModifiable> implements RuntimeErrorProducer {
    private ClassInstance instance;
    protected final SkriptClass contextClass;

    private T product = null;
    private final List<T> guesses = new ArrayList<>();

    private final ErrorSource errorSource;
    private Expression<ClassInstance> instanceExpr;

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

    /**
     * Validate your signature and set any extra data
     */
    protected abstract boolean validate(T product, boolean isStatic, SkriptClass targetClass);

    protected abstract @Nullable T getProductFromClass(SkriptClass skriptClass);
    protected abstract @Nullable T getProductFromInstance(ClassInstance instance);

    /**
     *
     * Assures that the resulting array can be safely returned from a syntax, given the product type and reported plurality.
     * Attempts to convert to the product type if some values within did not match the target type.
     *
     * @param value The array to return
     * @param isSingle If this syntax reports to be single or plural
     * @return null if the conversion/validation failed, otherwise the safe converted array
     */
    public Object @Nullable [] getSafeConverted(Object @NotNull [] value, boolean isSingle) {
        Class<?> convertTo = product().type();

        if (!Arrays.stream(value).allMatch(convertTo::isInstance)) {
            value = Converters.convert(value, product().type());

            if (value.length == 0) { // failed to convert any values, error
                return null;
            }
        }

        if (isSingle && value.length > 1) {
            return null;
        }
        return value;
    }

    /**
     *
     * Gets the inferred class (if possible) from the target expression. Tries to get it from the expressions return type if it is a subclass of {@link ClassInstance}. This may be used to yield more accurate results if type hints are enabled.
     *
     * @param expr The expression to check
     * @return The relevant {@link SkriptClass} or null.
     */
    public static @Nullable SkriptClass getExpressionClass(Expression<ClassInstance> expr) {
        if (expr.getSource() instanceof ExprSelf self) {
            return self.skriptClass;
        }
        Class<?> returnType = expr.getReturnType();
        if (returnType != ClassInstance.class && returnType.isAssignableFrom(ClassInstance.class)) {
            return ClassManager.getClass(returnType.getSimpleName());
        }
        return null;
    }

    /**
     * A helper method to get all possible return types based off of previous guesses from {@link Validator#validateExpression(Expression)}
     * @return The {@link Validator#product} return type, OR all return types of {@link Validator#guesses}
     */
    public final Class<?>[] possibleTypes() {
        if (product != null) return new Class<?>[]{product().type()};
        if (guesses.isEmpty()) return new Class<?>[]{Object.class};

        Set<Class<?>> possibleTypes = new HashSet<>();
        for (T guess : guesses) {
            possibleTypes.add(guess.type());
        }
        return possibleTypes.toArray(Class[]::new);
    }


    /**
     * Tries to find the best possible return type for that pattern to report
     *
     * @param possibleTypes Must contain atleast one class, see {@link Validator#possibleTypes()}
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
     * @return The instance which has been validated against the reference OR null if it wasn't valid
     */
    public final @Nullable ClassInstance getValidInstance(Event event) {
        ClassInstance newInstance = instanceExpr.getSingle(event);

        if (newInstance == null) {
            error("Target instance was not set");
            return null;
        }
        SkriptClass parent = newInstance.getParent();
        if (parent == null) {
            error("Target instance is not accessible");
            return null;
        }
        if (this.instance == newInstance)
            return newInstance;

        LogEntry error;
        try (var handler = new SimpleErrorHandler()) {
            if (validateInstance(newInstance, parent)) {
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
     * Used for validating instances via expression at parse time
     */
    public final boolean validateExpression(Expression<ClassInstance> expr) {
        this.instanceExpr = expr;
        SkriptClass inferredClass = getExpressionClass(expr);

        LogEntry error;
        SkriptClass resultClass = null;

        try (var handler = new SimpleErrorHandler().start()) {
            if (inferredClass != null) {
                T product = getProductFromClass(inferredClass);
                if (product != null) {
                    guesses.add(product);
                    resultClass = inferredClass;
                }
            } else {
                for (SkriptClass skriptClass : ClassManager.getClasses()) {
                    T product = getProductFromClass(skriptClass);
                    if (product != null) {
                        guesses.add(product);
                        resultClass = skriptClass;
                    }
                }
            }
            error = handler.getLastError();
        }
        if (guesses.isEmpty()) {
            if (error != null)
                Skript.error(error.getMessage());
            return false;
        }
        if (guesses.size() != 1) {
            return true; // unknown, exact resolution happens at runtime
        }
        this.product = guesses.getFirst();

        try (var handler = new SimpleErrorHandler().start()) {
            if (validate(product, false, resultClass)) {
                return true;
            }
            error = handler.getLastError();
        }
        if (error != null) Skript.error(error.getMessage());
        return true;
    }

    /**
     * Validates a class against the reference
     * @param skriptClass The class to validate
     * @return true if the class was valid, false if it was not
     */
    public final boolean validateStatic(@NotNull SkriptClass skriptClass) {
        this.product = getProductFromClass(skriptClass);
        if (product == null) return false;

        return validate(product, true, skriptClass);
    }

    /**
     * Validates an instance against the reference
     *
     * @param newInstance The instance to validate
     * @return true if the instance was valid, false if it was not
     *
     */
    public final boolean validateInstance(@NotNull ClassInstance newInstance, SkriptClass parent) {
        this.product = getProductFromInstance(newInstance);
        if (product != null) {
            if (validate(product, false, parent)) {
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
