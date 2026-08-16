package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MethodHolder {

    MethodRegistry getRegistry();

    SkriptMethod getExactMethod(MethodIdentifier identifier);

    List<SkriptMethod> getCandidates(MethodReference reference);

    default @Nullable SkriptMethod getExactMethod(MethodSignature signature) {
        if (signature == null) return null;
        SkriptMethod method = getExactMethod(MethodIdentifier.from(signature));
        if (method == null) return null;
        return method.signature.matches(signature) ? method : null;
    }

}
