package com.novystxr.classysk.api.classes;

import java.util.*;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.FieldHolder;
import com.novystxr.classysk.api.event.FieldEvalEvent;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * The single non-instance version of a class
 */
public class SkriptClass implements FieldHolder {

    public final String name;

    public final MethodRegistry methodRegistry = new MethodRegistry();
    public final Map<String, FieldSignature> fieldSignatures = new HashMap<>();

    public SkriptClass(String name) {
        this.name = name;
    }

    public @Nullable Set<ClassInstance> instances() {
        return ClassManager.instances.get(name);
    }

    @Override
    public Map<String, SkriptField> fieldMap() {
        return ClassManager.staticFields.computeIfAbsent(name, key -> new HashMap<>());
    }

    @Override
    public FieldSignature getFieldSignature(String key) {
        return fieldSignatures.get(key);
    }

    void setDefaults(FieldHolder fieldHolder) {
        for (FieldSignature signature : fieldSignatures.values()) {
            if (signature.isStatic() == fieldHolder instanceof ClassInstance) continue;

            String fieldName = signature.name();
            if (fieldHolder.fieldExists(fieldName)) continue;

            Expression<?> defaultExpr = signature.defaultExpr();
            if (defaultExpr == null) continue;

            Object[] value = defaultExpr.getArray(new FieldEvalEvent());
            fieldHolder.setFieldValue(fieldName, value);
        }
    }

    public ClassInstance createInstance() {
        ClassInstance newInstance = new ClassInstance(name);

        ClassManager.instances.computeIfAbsent(name, key ->
                Collections.newSetFromMap(new WeakHashMap<>()))
            .add(newInstance);

        setDefaults(newInstance);
        return newInstance;
    }

    public String getEffectiveName() {
        return StringUtils.titleCase(name);
    }
}