package com.novystxr.classysk.api.classes;

import java.util.*;
import java.util.stream.Stream;

import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.event.FieldEvalEvent;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.MethodHolder;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * The single non-instance version of a class
 */
public class SkriptClass implements FieldHolder, MethodHolder {

    public final String name;
    public final String extendsName;
    public final boolean isFinal;

    public final MethodRegistry methodRegistry = new MethodRegistry();
    public final Map<String, FieldSignature> fieldSignatures = new HashMap<>();

    public SkriptClass(String name, String extendsName, boolean isFinal) {
        this.name = name;
        this.extendsName = extendsName;
        this.isFinal = isFinal;
    }

    public @Nullable Set<ClassInstance> instances() {
        return ClassManager.instances.get(name);
    }

    public Stream<SkriptClass> inheritanceStream() {
        return Stream.iterate(this, Objects::nonNull, SkriptClass::getExtends);
    }

    public SkriptClass getExtends() {
        return extendsName != null ? ClassManager.getClass(extendsName) : null;
    }

    public boolean inherits(SkriptClass otherClass) {
        return inheritanceStream()
            .anyMatch(target -> target == otherClass);
    }

    @Override
    public Map<String, SkriptField> fieldMap() {
        return ClassManager.staticFields.computeIfAbsent(name, key -> new HashMap<>());
    }

    @Override
    public FieldSignature getFieldSignature(String key) {
        return inheritanceStream()
            .map(target -> target.fieldSignatures.get(key))
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
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

    public void setupInstance(ClassInstance instance) {
        ClassManager.instances.computeIfAbsent(name, key ->
                Collections.newSetFromMap(new WeakHashMap<>()))
            .add(instance);

        setDefaults(instance);
    }

    public ClassInstance createInstance() {
        ClassInstance newInstance = new ClassInstance(name);
        setupInstance(newInstance);
        return newInstance;
    }

    public String getEffectiveName() {
        return StringUtils.titleCase(name);
    }

    @Override
    public MethodRegistry getRegistry() {
        return methodRegistry;
    }

    @Override
    public SkriptMethod getExactMethod(MethodIdentifier identifier, boolean isSuper) {
        return inheritanceStream().skip(isSuper ? 1 : 0)
            .map(target -> target.getRegistry().getExactMethod(identifier))
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    @Override
    public List<SkriptMethod> getCandidates(MethodReference reference) {
        Map<MethodIdentifier, SkriptMethod> result = new HashMap<>();
        for (SkriptClass target : inheritanceStream().toList().reversed()) {
            result.putAll(target.methodRegistry.candidates(reference));
        }
        return result.values().stream().toList();
    }
}