package com.novystxr.classysk.api.classes;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.parser.ParserInstance;
import com.novystxr.classysk.api.event.FieldEvalEvent;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

/**
 * The single non-instance version of a class
 */
public class SkriptClass extends ClassInstance {

    public final MethodRegistry methodRegistry = new MethodRegistry();
    public final Map<String, FieldSignature> fieldSignatures = new HashMap<>();

    final List<WeakReference<ClassInstance>> instances = new ArrayList<>();

    public SkriptClass extendsClass = null;

    SkriptClassWrapper wrapper = null;
    private Script script;

    // whether the class is accessible in scripts
    // this is set to false when the corresponding structure is unloaded
    public boolean accessible = true;

    public SkriptClass(String name, Script script) {
        super(name);
        this.script = script;
    }

    public SkriptClassWrapper getWrapper() {
        if (wrapper == null) {
            wrapper = new SkriptClassWrapper(this);
        }
        return wrapper;
    }

    @Override
    public FieldSignature getFieldSignature(String key) {
        for (SkriptClass target : getInheritanceChain()) {
            FieldSignature signature = target.fieldSignatures.get(key);
            if (signature != null)
                return signature;
        }
        return null;
    }

    /**
     * If this is A and: A extends B, B extends C and C extends D, this method will return A, B, C, D
     * If it doesn't inherit anything it will return a list containing only the class.
     *
     * @return The full inheritance chain from top -> bottom
     */
    public List<SkriptClass> getInheritanceChain() {
        List<SkriptClass> result = new ArrayList<>();
        result.add(this);

        SkriptClass target = this;
        while ((target = target.extendsClass) != null) {
            result.add(target);
        }
        return result;
    }

    public @Nullable Script getValidScript() {
        if (script == null) return null;
        if (script.valid()) {
            return script;
        }
        File file = script.getConfig().getFile();
        if (file == null || !file.isFile()) return null;

        Script newScript = ScriptLoader.getScript(file);
        if (newScript == null) return null;

        this.script = newScript;
        return this.script;
    }

    private void setDefaults(ClassInstance instance) {
        for (FieldSignature signature : fieldSignatures.values()) {
            if (signature.isStatic() == instance.isInstance()) continue;

            String fieldName = signature.name();
            if (instance.fieldExists(fieldName)) continue;

            Expression<?> defaultExpr = signature.defaultExpr();
            if (defaultExpr == null) continue;

            Object[] value = defaultExpr.getArray(new FieldEvalEvent());
            instance.setFieldValue(fieldName, value);
        }
    }

    public ClassInstance createInstance() {
        ClassInstance newInstance = new ClassInstance(name);
        instances.add(new WeakReference<>(newInstance));

        setDefaults(newInstance);
        return newInstance;
    }

    @Override
    public boolean isInstance() {
        return false;
    }

    @Override
    public SkriptClass getParent() {
        return this;
    }

    @Override
    public String getEffectiveName() {
        return StringUtils.titleCase(name);
    }

    /**
     * Determines if the underlying class structure still exists in it's designated script
     */
    public boolean validateStructure() {
        Script script = this.script;
        if (script == null) return false;

        List<Structure> structures = script.getStructures();
        for (Structure structure : structures) {
            if (structure instanceof StructClass classStruct) {
                Script structScript = ParserInstance.get().getCurrentScript();
                if (classStruct.getName().equals(name) && structScript.nameAndPath().equals(script.nameAndPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void revalidateFields() {

        // static field validation
        // attempt to convert, if failed, static context changes or no longer exists, remove field
        fieldMap.values().removeIf(field -> {
            FieldSignature signature = getFieldSignature(field.signature.name());

            // if signature no longer exists or static context changed, ignore and use existing signature
            if (signature == null || signature.isStatic() != field.signature.isStatic()) return true;

            if (signature.canConvert(field.value)) {
                field.signature = signature;
            } else {
                return true;
            }
            return false;
        });

        // init any non-existing static fields with default values
        setDefaults(this);

        instances.removeIf(reference -> {
            ClassInstance instance = reference.get();
            if (instance == null) return true;

            for (SkriptField field : instance.fieldMap.values()) {
                String fieldName = field.signature.name();
                FieldSignature signature = getFieldSignature(fieldName);

                // if signature no longer exists or static context changed, ignore and use existing signature
                if (signature == null || signature.isStatic() != field.signature.isStatic()) {
                    continue;
                }
                // attempt to convert to new signature
                if (signature.canConvert(field.value)) {
                    field.signature = signature;
                }
            }
            return false;
        });
    }

}