package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RetainingLogHandler;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

public abstract class AccessValidator<T extends AccessModifiable> implements RuntimeErrorProducer {
    protected ClassInstance instance;
    protected boolean isStatic;

    protected final SkriptClass contextClass;
    private T product;

    protected ErrorSource errorSource;
    private boolean noRuntimeErrors = false;

    /**
     * Extend with extra data that is necessary
     */
    public AccessValidator(ErrorSource errorSource, SkriptClass contextClass) {
        this.errorSource = errorSource;
        this.contextClass = contextClass;
    }

    protected void setProduct(T product) {
        this.product = product;
    }
    public T getProduct() {
        return product;
    }

    /**
     * Validate your signature and set mutable data
     */
    protected abstract boolean validate(@NotNull ClassInstance instance);

    public final boolean validateInstance(@Nullable ClassInstance newInstance, boolean isRuntime) {
        if (this.instance == newInstance) return true;

        if (newInstance == null || !newInstance.isAccessible()) {
            dynamicError("Passed instance is null or non-accessible", isRuntime);
            return false;
        }
        isStatic = !newInstance.isInstance();
        noRuntimeErrors = newInstance.getParent().option(ClassOption.SUPPRESS_RUNTIME_ERRORS);

        try (RetainingLogHandler handler = new RetainingLogHandler().start()) {
            if (validate(newInstance)) {
                this.instance = newInstance;
                return true;
            }
            LogEntry error = handler.getFirstError();
            if (Skript.testing()) handler.getLog(); // njol stinks

            if (error != null) {
                dynamicError(error.getMessage(), isRuntime);
            }
            handler.clear();
        }
        return false;
    }

    @Override
    public @NotNull ErrorSource getErrorSource() {
        return errorSource;
    }

    private void dynamicError(String message, boolean isRuntime) {
        if (isRuntime && !noRuntimeErrors) {
            error(message);
        } else {
            Skript.error(message);
        }
    }
}
