package com.novystxr.classysk.api.assignability;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;

import java.lang.reflect.Constructor;
import java.util.*;

public class SubclassManager<T> {

    private final Class<T> fromType;
    private final String packageID;

    private final Class<?>[] argTypes;
    private final boolean canUseKey;

    /**
     *
     * @param packageID A unique package identifier to generate the subclasses in
     * @param argTypes Constructor argument types to fetch the constructor of the generated class
     */
    public SubclassManager(Class<T> fromType, String packageID, Class<?>... argTypes) {
        this.fromType = fromType;
        this.packageID = packageID;
        this.argTypes = argTypes;
        this.canUseKey = argTypes.length == 1 && argTypes[0] == String.class;
    }

    private final Map<String, Constructor<? extends T>> constructors = new HashMap<>();
    private final Map<String, Class<? extends T>> subclasses = new HashMap<>();

    public Class<? extends T> getSubclass(String key) {
        Class<? extends AssignabilityBridge> bridge = AssignabilityBridge.getSubInterface(key);
        return subclasses.computeIfAbsent(key, k -> new ByteBuddy()
            .subclass(fromType)
            .implement(bridge)
            .name("com.novystxr.generated."+packageID+"."+key)
            .make()
            .load(bridge.getClassLoader(), Default.WRAPPER)
            .getLoaded()
        );
    }

    public T newInstance(String key, Object... args) {
        if (canUseKey && args.length == 0)
            args = new Object[]{key};
        Constructor<? extends T> constructor = constructors.computeIfAbsent(key, k -> {
            try {
                return getSubclass(key).getDeclaredConstructor(argTypes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
