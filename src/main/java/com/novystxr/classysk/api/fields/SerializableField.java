package com.novystxr.classysk.api.fields;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable.YggdrasilExtendedSerializable;
import com.novystxr.classysk.api.fields.SkriptField.FieldSignature;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.converter.Converters;

import java.io.StreamCorruptedException;

public class SerializableField implements YggdrasilExtendedSerializable {

    public Object[] value;
    public Class<?> signatureType;
    public boolean isPlural;

    public SerializableField(Object[] value, Class<?> signatureType, boolean isPlural) {
        this.value = value;
        this.signatureType = signatureType;
        this.isPlural = isPlural;
    }

    public SerializableField() {}

    @Override
    public Fields serialize() {
        Fields fields = new Fields();

        fields.putObject("value", value);
        fields.putObject("signatureType", signatureType);
        fields.putPrimitive("isPlural", isPlural);

        return fields;
    }

    @Override
    public void deserialize(@NotNull Fields fields) throws StreamCorruptedException {
        value = fields.getObject("value", Object[].class);
        signatureType = fields.getObject("signatureType", Class.class);
        isPlural = fields.getPrimitive("isPlural", boolean.class);
    }

    public boolean canBeSaved() {
        for (Object val : value) {
            ClassInfo<?> classInfo = Classes.getSuperClassInfo(val.getClass());
            Class<?> serializeAs = classInfo.getSerializeAs();
            if (serializeAs != null) {
                classInfo = Classes.getExactClassInfo(serializeAs);
                if (classInfo == null) return false;
                value = Converters.convert(value, serializeAs);
                signatureType = serializeAs;
            }
            if (classInfo.getSerializer() == null) {
                return false;
            }
        }

        return true;
    }

    public FieldSignature mergeSignature(FieldSignature signature) {
        return new FieldSignature(
            signature.name(),
            signatureType,
            null,
            signature.accessType(),
            false,
            isPlural
        );
    }
}
