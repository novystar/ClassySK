package com.novystxr.classysk.main.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.Fields.FieldContext;
import com.novystxr.classysk.Classysk;
import com.novystxr.classysk.api.classes.*;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.fields.SkriptField;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import com.novystxr.classysk.api.util.StringUtils;
import com.novystxr.classysk.api.util.TypedInstanceParser;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

import java.io.StreamCorruptedException;

@SuppressWarnings("UnstableApiUsage")
public class Types {
    public static void register(SkriptAddon addon) {

        Classes.registerClass(new ClassInfo<>(ClassReference.class, "classreference")
            .since("1.0.0")
            .user("class reference(s)?")
            .name("Class Reference")
            .property(Property.NAME, "The name of the class", addon,
                ExpressionPropertyHandler.of(ClassReference::name, String.class))
            .description("Non-instance reference of a class, represents the class as a whole")
            .parser(new Parser<>() {

                @Override
                public boolean canParse(ParseContext context) {
                    return false;
                }

                @Override
                public String toString(ClassReference o, int flags) {
                    return "Class " + StringUtils.titleCase(o.name());
                }

                @Override
                public String toVariableNameString(ClassReference o) {
                    return "Class " + o.name();
                }
            })
        );

        if (Classysk.TYPES_ALLOWED) {
            Classes.registerClass(new ClassInfo<>(TypedInstanceWrapper.class, "typedinstance")
                .since("1.1.0")
                .name("Typed Instance")
                .description("Transitory wrapper that is used by converters to filter instances based on their type.")
                .serializeAs(ClassInstance.class)
                .parser(new TypedInstanceParser<>())
            );
        }

        Classes.registerClass(new ClassInfo<>(ClassInstance.class, "classinstance")
            .since("1.0.0")
            .user("class instances?")
            .name("Class Instance")
            .property(Property.NAME, "The name of the class this instance belongs to", addon,
                ExpressionPropertyHandler.of(instance -> instance.name, String.class))
            .description("Instance version of a class, holds non-static methods and fields, representing a created instance of a class.")
            .parser(new Parser<>() {

                @Override
                public boolean canParse(ParseContext context) {
                    return false;
                }

                @Override
                public String toString(ClassInstance o, int flags) {
                    return "Class Instance " + StringUtils.titleCase(o.name);
                }

                @Override
                public String toVariableNameString(ClassInstance o) {
                    return "Class Instance " + o.name + " (" + o.getHashCode() + ")";
                }
            })
            .serializer(new Serializer<>() {
                @Override
                public Fields serialize(ClassInstance o) {
                    Fields fields = new Fields();
                    fields.putObject("name", o.name);

                    for (SkriptField skriptField : o.fieldMap.values()) {
                        FieldSignature signature = skriptField.signature;
                        SerializableField sField = new SerializableField(skriptField.value, signature.type(), signature.isPlural());

                        if (!sField.canBeSaved()) continue;

                        fields.putObject("field:"+signature.name(), sField);
                    }
                    return fields;
                }

                @Override
                protected ClassInstance deserialize(Fields fields) throws StreamCorruptedException {
                    String name = fields.getAndRemoveObject("name", String.class);
                    name = StringUtils.getLowerCase(name);
                    SkriptClass parentClass = ClassManager.getClass(name);

                    ClassInstance instance;
                    if (parentClass != null) {
                        instance = parentClass.createInstance();
                    } else {
                        instance = new ClassInstance(name);
                        ClassManager.setAwaitingParent(instance);
                    }
                    for (FieldContext context : fields) {
                        if (!context.getID().startsWith("field:")) continue;

                        String fieldName = context.getID().substring("field:".length());
                        SerializableField sField = context.getObject(SerializableField.class);

                        instance.putAwaitingField(fieldName, sField);
                    }
                    return instance;
                }

                @Override
                public boolean mustSyncDeserialization() {
                    return true;
                }

                @Override
                protected boolean canBeInstantiated() {
                    return false;
                }
            })
        );

    }
}
