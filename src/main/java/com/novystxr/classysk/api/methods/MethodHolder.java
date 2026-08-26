package com.novystxr.classysk.api.methods;

import com.novystxr.classysk.api.methods.MethodParser.MethodReference;
import com.novystxr.classysk.api.methods.MethodRegistry.MethodIdentifier;

import java.util.List;

public interface MethodHolder {

    MethodRegistry getRegistry();

    SkriptMethod getExactMethod(MethodIdentifier identifier, boolean isSuper);

    List<SkriptMethod> getCandidates(MethodReference reference);

}
