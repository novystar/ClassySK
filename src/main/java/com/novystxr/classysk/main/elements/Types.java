package com.novystxr.classysk.main.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.Fields.FieldContext;
import com.novystxr.classysk.api.classes.*;
import com.novystxr.classysk.api.fields.SerializableField;
import com.novystxr.classysk.api.util.StringUtils;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

import java.io.StreamCorruptedException;

@SuppressWarnings("UnstableApiUsage")
public class Types {
    public static Parser<? extends ClassInstance> classParser = new Parser<>() {

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(ClassInstance o, int flags) {
            return StringUtils.titleCase(o.name) + " instance";
        }

        @Override
        public String toVariableNameString(ClassInstance o) {
            return StringUtils.titleCase(o.name) + " instance" + " (" + o.hashCode() + ")";
        }
    };

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

        Classes.registerClass(new ClassInfo<>(ClassInstance.class, "classinstance")
            .since("1.0.0")
            .user("class instances?")
            .name("Class Instance")
            .property(Property.NAME, "The name of the class this instance belongs to", addon,
                ExpressionPropertyHandler.of(instance -> instance.name, String.class))
            .description("Instance version of a class, holds non-static methods and fields, representing a created instance of a class.")
            .parser(classParser)
            .serializer(new Serializer<>() {
                @Override
                public Fields serialize(ClassInstance o) {
                    Fields fields = new Fields();
                    fields.putObject("name", o.name);

                    for (var entry : o.fieldValueMap().entrySet()) {
                        SerializableField sField = new SerializableField(entry.getValue());

                        if (sField.canBeSaved()) {
                            fields.putObject("field:" + entry.getKey(), sField);
                        }
                    }
                    return fields;
                }

                @Override
                protected ClassInstance deserialize(Fields fields) throws StreamCorruptedException {
                    String name = fields.getAndRemoveObject("name", String.class);
                    name = StringUtils.getLowerCase(name);

                    ClassInstance instance = ClassManager.getNewInstance(name);
                    for (FieldContext context : fields) {
                        if (!context.getID().startsWith("field:")) continue;

                        String fieldName = context.getID().substring("field:".length());
                        SerializableField sField = context.getObject(SerializableField.class);
                        if (sField == null) throw new StreamCorruptedException();

                        instance.fieldValueMap.put(fieldName, sField.value);
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
