package com.novystxr.classysk.api;

public interface AccessModifiable {
    boolean isStatic();
    boolean isPlural();
    AccessType accessType();
    Class<?> type();

    enum AccessType {
        PUBLIC,
        PRIVATE;

        public boolean isPrivate() {
            return this == AccessType.PRIVATE;
        }
        public boolean isPublic() {
            return this == AccessType.PUBLIC;
        }
    }
}
