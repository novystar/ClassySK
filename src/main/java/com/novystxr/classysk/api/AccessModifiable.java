package com.novystxr.classysk.api;

import com.novystxr.classysk.api.classes.SkriptClass;
import org.jetbrains.annotations.Nullable;

public interface AccessModifiable {
    boolean checkAccess(@Nullable SkriptClass contextClass);
    boolean checkContext(boolean isStatic);

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
