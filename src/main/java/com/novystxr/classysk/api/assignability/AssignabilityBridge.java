package com.novystxr.classysk.api.assignability;

import com.novystxr.classysk.api.classes.ClassInstance;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface to satisfy direct type cross-assignability.
 * Every type created with a {@link SubclassManager} implements a unique sub-interface of this
 * so they are all assignable to each other without going through converters.
 *
 */
public interface AssignabilityBridge {
    Map<String, Class<? extends AssignabilityBridge>> interfaces = new HashMap<>();

    static Class<? extends AssignabilityBridge> getSubInterface(String key) {
        return interfaces.computeIfAbsent(key, k ->
            new ByteBuddy()
                .makeInterface(AssignabilityBridge.class)
                .name("com.novystxr.generated.interfaces." + key)
                .make()
                .load(AssignabilityBridge.class.getClassLoader(), Default.WRAPPER)
                .getLoaded()
        );
    }

    ClassInstance unwrap();
}
