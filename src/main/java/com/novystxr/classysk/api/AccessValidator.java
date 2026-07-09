package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import com.novystxr.classysk.api.util.SimpleErrorHandler;
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

import java.util.ArrayList;
import java.util.List;

public abstract class AccessValidator<T extends AccessModifiable> implements RuntimeErrorProducer {
    private ClassInstance instance;

    private final SkriptClass contextClass;
    private T product = null;

    private final ErrorSource errorSource;
    private boolean noRuntimeErrors = false;

    private List<T> guesses = null;

    /**
     * Extend with extra data that is necessary
     */
    public AccessValidator(ErrorSource errorSource, SkriptClass contextClass) {
        this.errorSource = errorSource;
        this.contextClass = contextClass;
    }

    /**
     * Validate your signature and set mutable data
     */
    protected abstract boolean validate(T product, boolean isStatic, boolean isSameContext);

    protected abstract @Nullable T getProductFromClass(SkriptClass skriptClass);
    protected abstract @Nullable T getProductFromInstance(ClassInstance instance);

    public abstract String productName();

    public final Class<?> getReturnType() {
        if (product != null) return product.type();
        if (guesses == null || guesses.isEmpty()) return Object.class;

        Class<?>[] possibleTypes = new Class[guesses.size()];

        int i = 0;
        for (T guess : guesses) {
            possibleTypes[i++] = guess.type();
        }
        if (possibleTypes.length == 1) {
            return possibleTypes[0];
        }
        return Utils.highestDenominator(Object.class, possibleTypes);
    }

    public final Kleenean shouldBeSingle() {
        if (product != null) return Kleenean.get(!product.isPlural());
        if (guesses == null || guesses.isEmpty()) return Kleenean.UNKNOWN;

        boolean hasSingle = false;
        boolean hasPlural = false;
        for (T guess : guesses) {
            if (guess.isPlural()) hasPlural = true;
            else hasSingle = true;
        }
        if (hasSingle != hasPlural) return Kleenean.get(hasSingle);
        return Kleenean.UNKNOWN;
    }

    public final Kleenean guessProductAndValidate(boolean isSelf) {
        guesses = evaluateGuesses(isSelf);
        if (guesses.isEmpty()) {
            return Kleenean.FALSE;
        }
        if (guesses.size() != 1) return Kleenean.UNKNOWN;
        T product = guesses.getFirst();

        LogEntry error;
        try (var handler = new SimpleErrorHandler().start()) {
            if (validate(product, false, isSelf)) {
                this.product = product;
                return Kleenean.TRUE;
            }
            error = handler.getLastError();
        }
        if (error != null) {
            dynamicError(error.getMessage(), false);
        }
        return Kleenean.FALSE;
    }

    private List<T> evaluateGuesses(boolean isSelf) {
        LogEntry error;
        List<T> products = new ArrayList<>();

        try (var handler = new SimpleErrorHandler().start()) {
            if (isSelf) {
                T product = getProductFromClass(contextClass);
                if (product != null) products.add(product);
            } else {
                for (SkriptClass skriptClass : ClassManager.getClasses()) {
                    T product = getProductFromClass(skriptClass);
                    if (product != null) products.add(product);
                }
            }
            error = handler.getLastError();
        }
        if (products.isEmpty() && error != null) {
            dynamicError(error.getMessage(), false);
        }
        return products;
    }

    public final boolean validateInstance(@Nullable ClassInstance newInstance, boolean isRuntime) {
        if (this.instance == newInstance) return true;

        if (newInstance == null || !newInstance.isAccessible()) {
            dynamicError("Passed instance is null or non-accessible", isRuntime);
            return false;
        }
        boolean isStatic = !newInstance.isInstance();
        noRuntimeErrors = newInstance.getParent().option(ClassOption.SUPPRESS_RUNTIME_ERRORS);

        if (isRuntime && isStatic) {
            dynamicError("Static "+productName()+"s cannot be accessed at runtime", true);
            return false;
        }
        LogEntry error;
        try (var handler = new SimpleErrorHandler().start()) {
            if (validate(newInstance)) {
                this.instance = newInstance;
                return true;
            }
            error = handler.getLastError();
        }
        if (error != null) {
            dynamicError(error.getMessage(), isRuntime);
        }

        return false;
    }

    @Override
    public @NotNull ErrorSource getErrorSource() {
        return errorSource;
    }

    private void dynamicError(String message, boolean isRuntime) {
        if (isRuntime) {
            if (!noRuntimeErrors) error(message);
        } else {
            Skript.error(message);
        }
    }
}
