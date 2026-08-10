package com.novystxr.classysk.api;

public interface TypeWrappable<W, T> {
    W wrap();
    T unwrap();

    Class<? extends W> getSubclass();

}
