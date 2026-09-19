package com.novystxr.classysk.api.assignability;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default;

import java.lang.reflect.Constructor;
import java.util.*;

public class SubclassManager<E, P extends E> {

    private final Class<?>[] argTypes;
    private final String packageID;

    private final boolean canUseKey;

    /**
     *
     * @param packageID A unique package identifier to generate the subclasses in
     * @param argTypes Constructor argument types to fetch the constructor of the generated class
     */
    public SubclassManager(String packageID, Class<?>... argTypes) {
        this.packageID = packageID;
        this.argTypes = argTypes;
        this.canUseKey = argTypes.length == 1 && argTypes[0] == String.class;
    }

    private final Map<String, Constructor<? extends P>> constructors = new HashMap<>();
    private final Map<String, Class<? extends P>> subclasses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Class<? extends P> getSubclass(String key, Class<? extends E> clazz) {
        Class<? extends AssignabilityBridge> bridge = AssignabilityBridge.getSubInterface(key);
        return subclasses.computeIfAbsent(key, k -> (Class<? extends P>) new ByteBuddy()
            .subclass(clazz)
            .implement(bridge)
            .name("com.novystxr.generated."+packageID+"."+key)
            .make()
            .load(bridge.getClassLoader(), Default.WRAPPER)
            .getLoaded()
        );
    }

    public P newInstance(String key, Class<? extends E> clazz, Object... args) {
        if (canUseKey && args.length == 0)
            args = new Object[]{key};
        Constructor<? extends P> constructor = constructors.computeIfAbsent(key, k -> {
            try {
                return getSubclass(key, clazz).getDeclaredConstructor(argTypes);
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
