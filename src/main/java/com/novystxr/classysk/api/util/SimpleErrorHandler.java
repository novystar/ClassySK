package com.novystxr.classysk.api.util;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class SimpleErrorHandler extends LogHandler {

    private LogEntry lastError = null;

    @Override
    public LogResult log(LogEntry entry) {
        if (entry.getLevel().intValue() >= Level.SEVERE.intValue()) {
            lastError = entry;
        }
        return LogResult.DO_NOT_LOG;
    }

    public @Nullable String getErrorMessage() {
        if (lastError == null) return null;
        return lastError.getMessage();
    }

    @Override
    public SimpleErrorHandler start() {
        return SkriptLogger.startLogHandler(this);
    }
}
