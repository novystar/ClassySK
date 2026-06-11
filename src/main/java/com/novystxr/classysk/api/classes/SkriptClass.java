package com.novystxr.classysk.api.classes;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

import ch.njol.skript.ScriptLoader;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.methods.SkriptMethod;
import com.novystxr.classysk.api.util.ClassyStringUtils;
import com.novystxr.classysk.api.util.ConverterUtils;
import com.novystxr.classysk.main.elements.classes.StructClass;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

/** non instance skript class <br>
 holds static fields/methods, instances and signature data **/
public class SkriptClass extends ClassInstance {

    public enum ClassOption {
        STRICT_SIGNATURE_ENFORCEMENT(false),
        STORABLE(true);

        private final boolean defaultValue;

        ClassOption(boolean defaultValue) {
            this.defaultValue = defaultValue;
        }
        public static Map<ClassOption, Boolean> getDefaults() {
            Map<ClassOption, Boolean> result = new HashMap<>();
            for (ClassOption option : ClassOption.class.getEnumConstants()) {
                result.put(option, option.defaultValue);
            }
            return result;
        }
    }

    public final Map<ClassOption, Boolean> options = ClassOption.getDefaults();

    private Map<String, FieldSignature> fieldSignatures = new HashMap<>();
    private final Map<String, SkriptMethod> methods = new HashMap<>();

    final List<WeakReference<ClassInstance>> instances = new ArrayList<>();

    private Script script;

    // whether the abstract instance of this class is accessible in scripts
    // this is set to false when the corresponding structure is unloaded
    public boolean accessible = true;

    public SkriptClass(String name, Script script) {
        super(name);
        this.script = script;
    }

    public void setOption(ClassOption option, String value) {
        if (value == null) return;
        if (value.equals("true")) {
            options.replace(option, true);
        } else if (value.equals("false")) {
            options.replace(option, false);
        }
    }

    public boolean option(ClassOption option) {
        return options.get(option);
    }

    public boolean hasFieldSignature(String name) {
        return fieldSignatures.containsKey(name);
    }

    public FieldSignature getFieldSignature(String key) {
        return fieldSignatures.get(key);
    }

    public boolean hasMethod(String name) {
        return methods.containsKey(name);
    }
    public SkriptMethod getMethod(String key) {
        return methods.get(key);
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

    public ClassInstance createInstance() {
        ClassInstance instance = new ClassInstance(name);
        instances.add(new WeakReference<>(instance));
        return instance;
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
        return ClassyStringUtils.titleCase(name);
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

        ClassManager.removeClass(name);
        return false;
    }

    public void initMethodRegistry() {
        this.methods.clear();
    }

    public void putMethod(String key, SkriptMethod method) {
        methods.put(key, method);
    }

    public void updateFieldSignatureMap(Map<String, FieldSignature> fieldSignatures) {
        this.fieldSignatures = fieldSignatures;

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
            boolean strictSignatureEnforcement =
                    instance.getParent().option(ClassOption.STRICT_SIGNATURE_ENFORCEMENT);

            for (SkriptField field : instance.getFieldMap().values()) {
                FieldSignature signature = fieldSignatures.get(field.signature.name());

                if (ConverterUtils.canConvert(signature.type(), field.getValue())) {
                    // if signature no longer exists or static context changed, ignore and use existing signature
                    if (signature == null || signature.isStatic() != field.signature.isStatic()) {
                        if (strictSignatureEnforcement) instance.removeField(field);
                        continue;
                    }
                    field.signature = signature;
                } else if (strictSignatureEnforcement) {
                    instance.removeField(field);
                }
            }
            return false;
        });
    }

}