package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.SkriptMethod.MethodSignature;

import java.util.List;

public interface MethodHolder {

    MethodRegistry getRegistry();

    default SkriptMethod getExactMethod(MethodSignature signature) {
        return getRegistry().getExactMethod(signature);
    }

    default List<SkriptMethod> getCandidates(MethodReference reference) {
        return getRegistry().candidates(reference).values()
            .stream().toList();
    }

}
