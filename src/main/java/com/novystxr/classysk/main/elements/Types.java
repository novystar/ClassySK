package com.novystxr.classysk.main.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.EmptyStacktraceException;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.Fields.FieldContext;
import com.novystxr.classysk.api.classes.AbstractSkriptClass;
import com.novystxr.classysk.api.classes.AbstractSkriptClass.ClassOption;
import com.novystxr.classysk.api.classes.ClassManager;
import com.novystxr.classysk.api.classes.SkriptClass;

import java.io.StreamCorruptedException;
import java.util.Map.Entry;

public class Types {
    public static void register() {
        Classes.registerClass(new ClassInfo<>(AbstractSkriptClass.class, "abstractclass")
            .user("^abstract class(es)?$")
            .name("Abstract Class")
            .description("Non-instance version of a class, holds static methods and fields, representing the class as a whole.")
            .parser(new Parser<>() {

                @Override
                public boolean canParse(ParseContext context) {
                    return false;
                }

                @Override
                public String toString(AbstractSkriptClass o, int flags) {
                    return "Abstract Class " + o.name;
                }

                @Override
                public String toVariableNameString(AbstractSkriptClass o) {
                    return "Abstract Class " + o.name;
                }
            })
        );

        Classes.registerClass(new ClassInfo<>(SkriptClass.class, "class")
            .user("^class( instance)?(es)?$")
            .name("Class")
            .description("Instance version of a class, holds non-static methods and fields, representing a created instance of a class.")
            .parser(new Parser<>() {

                @Override
                public boolean canParse(ParseContext context) {
                    return false;
                }

                @Override
                public String toString(SkriptClass o, int flags) {
                    return "Class " + o.name;
                }

                @Override
                public String toVariableNameString(SkriptClass o) {
                    return "Class " + o.name + " (" + o.getHashCode() + ")";
                }
            })
            .serializer(new Serializer<>() {
                @Override
                public Fields serialize(SkriptClass o) {
                    // silently reject serialization
                    if (!o.getParent().option(ClassOption.STORABLE)) {
                        throw new EmptyStacktraceException();
                    }
                    Fields fields = new Fields();
                    fields.putObject("name", o.name);

                    for (Entry<String, Object[]> entry : o.getFieldValueMap().entrySet()) {
                        fields.putObject("field:"+entry.getKey(), entry.getValue());
                    }
                    return fields;
                }

                @Override
                protected SkriptClass deserialize(Fields fields) throws StreamCorruptedException {
                    String name = fields.getAndRemoveObject("name", String.class);
                    AbstractSkriptClass parentClass = ClassManager.getClass(name);

                    SkriptClass instance;
                    if (parentClass != null) {
                        instance = parentClass.createInstance();
                    } else {
                        instance = new SkriptClass(name);
                        ClassManager.setAwaitingParent(instance);
                    }
                    for (FieldContext context : fields) {
                        if (!context.getID().startsWith("field:")) continue;

                        String fieldName = context.getID().substring("field:".length());
                        Object[] value = context.getObject(Object[].class);

                        instance.putAwaitingField(fieldName, value);
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
