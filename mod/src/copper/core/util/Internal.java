package copper.core.util;

import java.lang.annotation.*;

/**
 * Indicates that the annotated class is intended for internal use only.
 * <p>
 * Classes marked with this annotation are not part of the public API.
 * They have no stability guarantees and may be changed, renamed, or
 * removed in future updates without prior notice. External mods or
 * modules should not rely on them.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Internal {
}
