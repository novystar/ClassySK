package com.novystxr.classysk.api;

import ch.njol.skript.config.Node;
import com.novystxr.classysk.api.classes.ClassInstance;
import com.novystxr.classysk.api.classes.ClassOption;
import com.novystxr.classysk.api.classes.SkriptClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

public abstract class AccessValidator<T> implements SyntaxRuntimeErrorProducer {
    private ClassInstance instance;

    protected boolean printErrors = true;
    protected boolean isStatic;

    protected SkriptClass contextClass;
    private final Node node;

    protected T signature;

    /**
     * Extend with extra data that is necessary
     */
    public AccessValidator(Node node, SkriptClass contextClass) {
        this.node = node;
        this.contextClass = contextClass;
    }

    /**
     * Validate your signature and set mutable data
     */
    protected abstract boolean validate(@NotNull ClassInstance instance);

    public final boolean validateInstance(@Nullable ClassInstance newInstance) {
        if (this.instance == newInstance) return true;

        if (newInstance == null || !newInstance.isAccessible()) {
            warning("Passed instance is null or non-accessible");
            return false;
        }
        isStatic = !newInstance.isInstance();

        this.printErrors = newInstance.getParent().option(ClassOption.SUPPRESS_RUNTIME_ERRORS) && !isStatic;
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
    public final Node getNode() {
        return node;
    }

    @Override
    public final void error(String message) {
        if (!printErrors) {
            SyntaxRuntimeErrorProducer.super.error(message);
        }
    }

    @Override
    public final void warning(String message) {
        if (!printErrors) {
            SyntaxRuntimeErrorProducer.super.warning(message);
        }
    }

}
