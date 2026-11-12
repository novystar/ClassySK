package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.HintManager;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.assignability.AssignabilityBridge;
import com.novystxr.classysk.api.classes.ClassContextHolder;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.SimpleErrorHandler;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

import java.util.*;

public abstract class Validator<T extends AccessModifiable> implements RuntimeErrorProducer {
    private final SkriptClass contextClass;

    private T product = null;
    private final List<T> guesses = new ArrayList<>();

    private final ErrorSource errorSource;
    private Expression<ClassInstance> instanceExpr;

    protected SkriptClass parent;

    /**
     * This is set before validation, so it will be up to date for the relevant instance
     * @return The last known parent class to this validator
     */
    public final SkriptClass parent() {
        return parent;
    }

    /**
     * The product of a validator is the result of its respective validation steps.
     * This should only be called at runtime in a syntax after {@link getValidInstance(Event)} or {@link validateInstance(ClassInstance)} has been checked.
     * @return The product of the latest validation call.
     * @see getProductFromClass
     * @see getProductFromInstance
     */
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
    protected abstract boolean validate(T product, @Nullable SkriptClass contextClass);

    protected abstract @Nullable T getProductFromClass(SkriptClass skriptClass);

    protected abstract @Nullable T getProductFromInstance(ClassInstance instance);

    protected SkriptClass contextClass() {
        return contextClass == null ? null : contextClass.refresh();
    }

    /**
     * Gets the inferred classes (if possible) from the target expression.
     * If any specific classes could not be inferred, this returns a collection of every class.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static Collection<SkriptClass> getPossibleClasses(Expression<? extends ClassInstance> expr) {
        if (expr.getSource() instanceof ClassContextHolder holder) {
            return List.of(holder.getContextClass());
        }
        List<SkriptClass> possibleClasses = new ArrayList<>();
        Collection<Class<?>> possibleTypes;

        HintManager hintManager = ParserInstance.get().getHintManager();
        if (hintManager.isActive() && expr instanceof Variable<?> variable && HintManager.canUseHints(variable)) {
            possibleTypes = hintManager.get(variable);
        } else {
            possibleTypes = List.of(expr.possibleReturnTypes());
        }
        for (Class<?> type : possibleTypes) {
            if (type == ClassInstance.class || type == Object.class) {
                return ClassManager.getClasses();
            } else if (AssignabilityBridge.class.isAssignableFrom(type)) {
                possibleClasses.add(ClassManager.getClass(type.getSimpleName()));
            }
        }
        return possibleClasses;
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
        return product().type();
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

        boolean single = false; boolean plural = false;
        for (T guess : guesses) {
            if (guess.isPlural()) plural = true;
            else single = true;

            if (plural && single)
                return Kleenean.UNKNOWN;
        }
        return Kleenean.get(single);
    }

    /**
     * Helper method for validating instances at runtime
     *
     * @param event The event to grab the instance with
     * @return The instance which has been validated against the reference OR null if it wasn't valid
     */
    public final @Nullable ClassInstance getValidInstance(Event event) {
        ClassInstance newInstance = instanceExpr.getSingle(event);
        return validateInstance(newInstance) ? newInstance : null;
    }
    /**
     * Used for validating instances via expression at parse time
     */
    public boolean validateExpression(Expression<ClassInstance> expr) {
        this.instanceExpr = expr;
        LogEntry error;

        SkriptClass lastGuessedClass = null;
        try (var handler = new SimpleErrorHandler().start()) {
            for (SkriptClass skriptClass : getPossibleClasses(expr)) {
                T product = getProductFromClass(skriptClass);
                if (product == null || !validate(product, contextClass()))
                    continue;
                guesses.add(product);
                lastGuessedClass = skriptClass;
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
        // only one possible product and class
        this.product = guesses.getFirst();
        this.parent = lastGuessedClass;
        return true;
    }

    /**
     * Validates a class against the reference
     * @param skriptClass The class to validate
     * @return true if the class was valid, false if it was not
     */
    public final boolean validateStatic(@NotNull SkriptClass skriptClass) {
        this.product = getProductFromClass(skriptClass);
        this.parent = skriptClass;
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
    public final boolean validateInstance(@Nullable ClassInstance newInstance) {
        if (newInstance == null) {
            error("Target instance was not set");
            return false;
        }
        SkriptClass parent = newInstance.getParent();
        if (parent == null) {
            error("Target instance has no parent class");
            return false;
        }
        if (parent == this.parent) // if the parent is still the same object, assume nothing has changed
            return true;

        this.parent = parent;

        LogEntry error;
        try (var handler = new SimpleErrorHandler().start()) {
            this.product = getProductFromInstance(newInstance);
            if (product != null) {
                if (validate(product, contextClass())) {
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
