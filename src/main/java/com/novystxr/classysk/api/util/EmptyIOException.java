package com.novystxr.classysk.api.util;

import java.io.NotSerializableException;

public class EmptyIOException extends NotSerializableException {

    public EmptyIOException(String cause) {
        super(cause);
        StackTraceElement stackTraceElement = new StackTraceElement("", "", null, -1);
        super.setStackTrace(new StackTraceElement[]{stackTraceElement});
    }

}
