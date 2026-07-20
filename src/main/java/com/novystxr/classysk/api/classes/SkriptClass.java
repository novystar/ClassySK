package com.novystxr.classysk.api.classes;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
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
    public Map<ClassOption, Boolean> options = ClassOption.getDefaults();

    public final MethodRegistry methodRegistry = new MethodRegistry();
    public final Map<String, FieldSignature> fieldSignatures = new HashMap<>();

    final List<WeakReference<ClassInstance>> instances = new ArrayList<>();

    SkriptClassWrapper wrapper = null;
    private Script script;

    // whether the class is accessible in scripts
    // this is set to false when the corresponding structure is unloaded
    public boolean accessible = true;

    public SkriptClass(String name, Script script) {
        super(name);
        this.script = script;
    }

    /**
     * Helper method that resets the class back to a pre-registration state
     */
    public void initForRegistration() {
        options = ClassOption.getDefaults();
        methodRegistry.init();
        fieldSignatures.clear();
    }

    public void setOption(ClassOption option, boolean value) {
        options.put(option, value);
    }

    public boolean option(ClassOption option) {
        return options.get(option);
    }

    public SkriptClassWrapper getWrapper() {
        if (wrapper == null) {
            wrapper = new SkriptClassWrapper(this);
        }
        return wrapper;
    }

    @Override
    public FieldSignature getFieldSignature(String key) {
        return fieldSignatures.get(key);
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
                Script structScript = classStruct.getParser().getCurrentScript();
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
            FieldSignature signature = fieldSignatures.get(field.signature.name());

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

            boolean strictSignatureEnforcement =
                    instance.getParent().option(ClassOption.STRICT_SIGNATURE_ENFORCEMENT);

            for (SkriptField field : instance.fieldMap.values()) {
                String fieldName = field.signature.name();
                FieldSignature signature = fieldSignatures.get(fieldName);

                // if signature no longer exists or static context changed, ignore and use existing signature
                if (signature == null || signature.isStatic() != field.signature.isStatic()) {
                    if (strictSignatureEnforcement) instance.removeField(fieldName);
                    continue;
                }
                // attempt to convert to new signature
                if (signature.canConvert(field.value)) {
                    field.signature = signature;
                } else if (strictSignatureEnforcement) {
                    instance.removeField(fieldName);
                }
            }
            return false;
        });
    }

}