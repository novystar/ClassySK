package com.novystxr.classysk.api;

import ch.njol.skript.Skript;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

public abstract class AccessValidator<T> implements RuntimeErrorProducer {
    private ClassInstance instance;

    protected boolean dontPrintErrors;
    protected boolean isStatic;

    private final ErrorSource errorSource;
    protected final SkriptClass contextClass;

    protected boolean isRuntime;
    protected T signature;

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
    protected abstract boolean validate(@NotNull ClassInstance instance);

    public final boolean validateInstance(@Nullable ClassInstance newInstance, boolean isRuntime) {
        if (this.instance == newInstance) return true;
        this.isRuntime = isRuntime;

        if (newInstance == null || !newInstance.isAccessible()) {
            error("Passed instance is null or non-accessible");
            return false;
        }
        isStatic = !newInstance.isInstance();

        this.dontPrintErrors = newInstance.getParent().option(ClassOption.SUPPRESS_RUNTIME_ERRORS) && isRuntime;
        if (validate(newInstance)) {
            this.instance = newInstance;
            return true;
        }
        return false;
    }

    public T getSignature() {
        return signature;
    }

    @Override
    public @NotNull ErrorSource getErrorSource() {
        return errorSource;
    }

    @Override
    public final void error(String message) {
        if (dontPrintErrors) return;
        if (isRuntime) {
            RuntimeErrorProducer.super.error(message);
        } else {
            Skript.error(message);
        }
    }

    @Override
    public final void warning(String message) {
        if (dontPrintErrors) return;
        if (isRuntime) {
            RuntimeErrorProducer.super.warning(message);
        } else {
            Skript.warning(message);
        }
    }

}
