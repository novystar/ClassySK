package com.novystxr.classysk.api.classes;

import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.EntryValidator.EntryValidatorBuilder;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

import java.util.HashMap;
import java.util.Map;

public enum ClassOption {
    STRICT_SIGNATURE_ENFORCEMENT(false, "strict signature enforcement"),
    STORABLE(true, "storable"),
    EXTERNAL_CREATION(true, "external creation"),
    PRIVATE_ACCESS_ON_CREATE(false, "private access on create"),
    SUPPRESS_RUNTIME_ERRORS(false, "suppress runtime errors");

    private final boolean defaultValue;
    private final String pattern;

    ClassOption(boolean defaultValue, String pattern) {
        this.defaultValue = defaultValue;
        this.pattern = pattern;
    }

    public static Map<ClassOption, Boolean> getDefaults() {
        Map<ClassOption, Boolean> result = new HashMap<>();
        for (ClassOption option : values()) {
            result.put(option, option.defaultValue);
        }
        return result;
    }

    public static void setOptions(SkriptClass skriptClass, @Nullable EntryContainer container) {
        if (container == null) return;
        skriptClass.resetOptions();
        for (ClassOption option : values()) {
            boolean value = container.get(option.pattern, Boolean.class, true);
            if (value == option.defaultValue) continue;

            skriptClass.setOption(option, value);
        }
    }

    public static EntryValidator getValidator() {
        EntryValidatorBuilder builder = EntryValidator.builder();

        for (ClassOption option : values()) {
            builder.addEntryData(new KeyValueEntryData<>(option.pattern, option.defaultValue, true) {
                @Override
                protected @Nullable Boolean getValue(String s) {
                    if (s.equals("true")) return true;
                    if (s.equals("false")) return false;
                    return null;
                }
            });
        }
        return builder.build();
    }
}
