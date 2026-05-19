package com.novystxr.classysk.main.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import com.novystxr.classysk.api.AbstractSkriptClass;
import com.novystxr.classysk.api.SkriptClass;

public class Types {
    public static void register() {
        Classes.registerClass(
                new ClassInfo<>(AbstractSkriptClass.class, "abstractclass")
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

        Classes.registerClass(
                new ClassInfo<>(SkriptClass.class, "class")
                        .user("^class (instance)?(es)?$")
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
        );

    }
}
