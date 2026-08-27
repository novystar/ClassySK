package com.novystxr.classysk.api.fields;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable.YggdrasilExtendedSerializable;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.converter.Converters;

import java.io.StreamCorruptedException;

public class SerializableField implements YggdrasilExtendedSerializable {

    public Object[] value;

    public SerializableField(Object[] value) {
        this.value = value;
    }

    public SerializableField() {}

    @Override
    public Fields serialize() {
        Fields fields = new Fields();

        fields.putObject("value", value);
        return fields;
    }

    @Override
    public void deserialize(@NotNull Fields fields) throws StreamCorruptedException {
        value = fields.getObject("value", Object[].class);
    }

    public boolean canBeSaved() {
        for (Object val : value) {
            ClassInfo<?> classInfo = Classes.getSuperClassInfo(val.getClass());
            Class<?> serializeAs = classInfo.getSerializeAs();
            if (serializeAs != null) {
                classInfo = Classes.getExactClassInfo(serializeAs);
                if (classInfo == null) return false;
                value = Converters.convert(value, serializeAs);
            }
            if (classInfo.getSerializer() == null) {
                return false;
            }
        }

        return true;
    }
}
