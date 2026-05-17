package com.novystxr.classysk.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    public static List<String> getNames() {
        return classMap.keySet().stream().toList();
    }

}
