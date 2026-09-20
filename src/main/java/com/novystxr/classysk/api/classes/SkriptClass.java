package com.novystxr.classysk.api.classes;

import java.util.*;
import java.util.stream.Stream;

import com.novystxr.classysk.api.Modifier;
import com.novystxr.classysk.api.ModifierHolder;
import com.novystxr.classysk.api.fields.FieldHolder;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * The single non-instance version of a class
 */
public class SkriptClass implements FieldHolder, ModifierHolder {

    public final String name;
    public final String extendsName;
    public final Modifier[] modifiers;

    public final MethodRegistry methodRegistry = new MethodRegistry();
    public final Map<String, SkriptField> fields = new HashMap<>();

    public SkriptClass(String name, String extendsName, Modifier[] modifiers) {
        this.name = name;
        this.extendsName = extendsName;
        this.modifiers = modifiers;
    }

    public Set<ClassInstance> instances() {
        return ClassManager.instances.computeIfAbsent(name, key ->
            Collections.newSetFromMap(new WeakHashMap<>()));
    }

    public Stream<SkriptClass> inheritanceStream() {
        return Stream.iterate(this, Objects::nonNull, SkriptClass::getExtends);
    }

    public SkriptClass getExtends() {
        return extendsName != null ? ClassManager.getClass(extendsName) : null;
    }

    public boolean inherits(@Nullable SkriptClass otherClass) {
        if (otherClass == null) return false;
        return inheritanceStream()
            .anyMatch(target -> target == otherClass);
    }
    @Override
    public Map<String, Object[]> fieldValueMap() {
        return ClassManager.staticFieldMaps.computeIfAbsent(name, key -> new HashMap<>());
    }

    @Override
    public SkriptField getField(String key) {
        SkriptField firstField = fields.get(key);
        return (firstField != null) ? firstField : inheritanceStream().skip(1)
            .map(target -> target.fields.get(key))
            .filter(Objects::nonNull)
            .filter(field -> !field.isStatic())
            .findFirst().orElse(null);
    }

    @Override
    public void setDefaults() {
        for (SkriptField field : fields.values()) {
            if (!field.isStatic()) continue;
            if (fieldExists(field.name)) continue;

            resetField(field.name);
        }
    }

    public Class<? extends ClassInstance> getSubclass() {
        return ClassInstance.getSubclass(name);
    }

    public String getEffectiveName() {
        return StringUtils.titleCase(name);
    }

    public SkriptMethod getExactMethod(MethodIdentifier identifier) {
        return inheritanceStream()
            .map(target -> target.methodRegistry.getExactMethod(identifier))
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    public List<SkriptMethod> getCandidates(MethodReference reference) {
        if (reference.isStatic()) return methodRegistry.candidates(reference).values().stream().toList();

        Map<MethodIdentifier, SkriptMethod> result = new HashMap<>();
        for (SkriptClass target : inheritanceStream().toList().reversed()) {
            result.putAll(target.methodRegistry.candidates(reference));
        }
        return result.values().stream().toList();
    }

    public SkriptClass refresh() {
        return ClassManager.getClass(name);
    }

    public ClassInstance createInstance() {
        ClassInstance newInstance = ClassInstance.newInstance(name);
        newInstance.setDefaults();
        return newInstance;
    }

    @Override
    public Modifier[] modifiers() {
        return modifiers;
    }
}