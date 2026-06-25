package com.novystxr.classysk.api.classes;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import com.novystxr.classysk.api.event.EmptyEvent;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.MethodRegistry;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.ConverterUtils;
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

    private Script script;

    // whether the abstract instance of this class is accessible in scripts
    // this is set to false when the corresponding structure is unloaded
    public boolean accessible = true;

    public SkriptClass(String name, Script script) {
        super(name);
        this.script = script;
    }

    public void setOption(ClassOption option, boolean value) {
        options.replace(option, value);
    }

    public boolean option(ClassOption option) {
        return options.get(option);
    }
    public void resetOptions() {
        options = ClassOption.getDefaults();
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
        if (file == null) return null;

        Script newScript = ScriptLoader.getScript(file);
        if (newScript == null) return null;

        this.script = newScript;
        return this.script;
    }

    private void evaluateDefaults(ClassInstance instance) {
        instance.fieldDefaults.clear();
        for (var fieldEntry : fieldSignatures.entrySet()) {
            FieldSignature signature = fieldEntry.getValue();

            Expression<?> defaultExpr = signature.defaultExpr();
            if (defaultExpr == null) continue;

            Object[] evaluatedDefault = defaultExpr.getArray(new EmptyEvent());
            if (!signature.canConvert(evaluatedDefault)) continue;

            instance.fieldDefaults.put(fieldEntry.getKey(), evaluatedDefault);
        }
    }

    public ClassInstance createInstance() {
        ClassInstance newInstance = new ClassInstance(name);

        instances.add(new WeakReference<>(newInstance));
        evaluateDefaults(newInstance);

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
        getFieldMap().values().removeIf(field -> {
            FieldSignature signature = fieldSignatures.get(field.signature.name());

            // if signature no longer exists or static context changed, ignore and use existing signature
            if (signature == null || signature.isStatic() != field.signature.isStatic()) return true;

            if (ConverterUtils.canConvert(field.signature.type(), field.getValue())) {
                field.signature = signature;
            } else {
                return true;
            }
            return false;
        });
        instances.removeIf(reference -> {
            ClassInstance instance = reference.get();
            if (instance == null) return true;

            evaluateDefaults(instance);

            boolean strictSignatureEnforcement =
                    instance.getParent().option(ClassOption.STRICT_SIGNATURE_ENFORCEMENT);

            for (SkriptField field : instance.getFieldMap().values()) {
                FieldSignature signature = fieldSignatures.get(field.signature.name());

                // if signature no longer exists or static context changed, ignore and use existing signature
                if (signature == null || signature.isStatic() != field.signature.isStatic()) {
                    if (strictSignatureEnforcement) instance.removeField(field);
                    continue;
                }
                // attempt to convert to new signature
                if (ConverterUtils.canConvert(signature.type(), field.getValue())) {
                    field.signature = signature;
                } else if (strictSignatureEnforcement) {
                    instance.removeField(field);
                }
            }
            return false;
        });
    }

}