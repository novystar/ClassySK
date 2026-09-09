package com.novystxr.classysk.api.classes;

import java.util.*;

import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.util.StringUtils;

/**
 * The single non-instance version of a class
 */
public class SkriptClass implements FieldHolder {

    public final String name;

    public final MethodRegistry methodRegistry = new MethodRegistry();
    public final Map<String, SkriptField> fields = new HashMap<>();

    public SkriptClass(String name) {
        this.name = name;
    }

    public Set<ClassInstance> instances() {
        return ClassManager.instances.computeIfAbsent(name, key ->
            Collections.newSetFromMap(new WeakHashMap<>()));
    }

    public ClassInstance createInstance() {
        ClassInstance newInstance = ClassManager.getNewInstance(name);
        newInstance.setDefaults();
        return newInstance;
    }

    public Class<? extends ClassInstance> getSubclass() {
        return ClassManager.getSubclass(name);
    }

    public String getEffectiveName() {
        return StringUtils.titleCase(name);
    }

    @Override
    public Map<String, Object[]> fieldValueMap() {
        return ClassManager.staticFieldMaps.computeIfAbsent(name, key -> new HashMap<>());
    }

    @Override
    public void setDefaults() {
        for (SkriptField field : fields.values()) {
            if (!field.isStatic()) continue;
            if (fieldExists(field.name)) continue;

            resetField(field.name);
        }
    }

    @Override
    public SkriptField getField(String key) {
        return fields.get(key);
    }
}