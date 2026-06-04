package com.novystxr.classysk.api.classes;

import java.util.*;

public class ClassManager {

    private static final Map<String, AbstractSkriptClass> classMap = new HashMap<>();

    public static void createClass(AbstractSkriptClass skriptClass) {
        classMap.put(skriptClass.name, skriptClass);
    }

    public static void removeClass(String name) {
        classMap.remove(name);
    }

    public static AbstractSkriptClass getClass(String name) {
        return classMap.get(name);
    }

    public static boolean classExists(String name) {
        return classMap.containsKey(name);
    }

    public static boolean isAccessible(String name) {
        if (!classExists(name)) return false;
        return getClass(name).accessible;
    }

    public static Collection<AbstractSkriptClass> getClasses() {
        return classMap.values();
    }

    public static List<String> getNames() {
        return classMap.keySet().stream().toList();
    }

}
