package com.novystxr.classysk.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.SkriptField.FieldSignature;
import com.novystxr.classysk.api.SkriptMethod.MethodSignature;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.jetbrains.annotations.Nullable;

public class SkriptClass {

    public final String name;

    private final Map<String, SkriptField> fieldMap = new HashMap<>();

    SkriptClass(String name) {
        this.name = name;
    }

    public void createField(SkriptField field) {
        if (fieldExists(field.signature.name())) return;

        fieldMap.put(field.signature.name(), field);

    }

    public void removeField(String name) {
        fieldMap.remove(name);
    }

    public Object[] getFieldValue(String name) {
        SkriptField field = fieldMap.get(name);
        AbstractSkriptClass parent = getParent();

        Object[] value = null;

        if (field != null) {
            value = field.getValue();
        } else if (parent.hasFieldSignature(name)) {
            value = parent.getFieldSignature(name).getDefaultValueArray();
        }

        return value;
    }

    public boolean checkFieldAccess(String fieldName) {

        AbstractSkriptClass parent = getParent();
        FieldSignature signature = parent.getFieldSignature(fieldName);

        ParserInstance parserInstance = ParserInstance.get();

        if (signature == null) return false;

        if (signature.isStatic() == isInstance()) return false;

        if (signature.accessType() == AccessType.PRIVATE) {
            if (parserInstance.getCurrentStructure() instanceof StructClass structClass) {
                return structClass.getName().equals(name);
            }
            return false;
        }

        return true;
    }

    // if field does not yet exist, instantiates one
    public SkriptField getField(String name) {

        SkriptField field = fieldMap.get(name);

        if (field != null) return field;

        AbstractSkriptClass parent = getParent();

        SkriptField.FieldSignature signature = parent.getFieldSignature(name);
        field = new SkriptField(signature);
        createField(field);

        return field;
    }

    public @Nullable MethodSignature getAccessibleMethod(String name) {
        AbstractSkriptClass parent = getParent();

        if (parent == null) return null;
        if (!parent.hasMethodSignature(name)) return null;

        MethodSignature signature = parent.getMethodSignature(name);
        if (signature.isStatic() == isInstance()) return null;

        return signature;
    }

    public String getEffectiveName() {
        return "%"+name+"%";
    }

    Map<String, SkriptField> getFieldMap() {
        return fieldMap;
    }

    public AbstractSkriptClass getParent() {
        return ClassManager.getClass(name);
    }

    public boolean fieldExists(String name) {
        return fieldMap.containsKey(name);
    }

    public int getHashCode() {
        return Objects.hash(name, fieldMap);
    }

    public boolean isInstance() {
        return true;
    }


}
