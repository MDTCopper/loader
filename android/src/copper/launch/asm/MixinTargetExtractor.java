package copper.launch.asm;

import org.objectweb.asm.*;
import java.util.*;

/**
 * Reads the target classes out of a {@code @Mixin} annotation.
 *
 * <p>Used before mixins are applied to find out which classes of a mod will be
 * transformed. The {@code value}/{@code values} array of the annotation lists
 * the target class names (either as internal names or {@link Type} constants).</p>
 */
public class MixinTargetExtractor extends ClassVisitor {
    private Set<String> targets;

    public MixinTargetExtractor() {
        super(Opcodes.ASM9);
        targets = new HashSet<>();
    }

    /** The collected target class names (dotted). */
    public Set<String> getTargets() {
        return targets;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
            return new MixinAnnotationVisitor();
        return super.visitAnnotation(descriptor, visible);
    }

    private class MixinAnnotationVisitor extends AnnotationVisitor {
        public MixinAnnotationVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            // only the value/values arrays hold the mixin targets
            if (!name.equals("value") && !name.equals("values"))
                return super.visitArray(name);
            return new AnnotationVisitor(api) {
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                    if (value instanceof String str) {
                        // internal name like "La/b/C;" -> a.b.C
                        if (str.startsWith("L") && str.endsWith(";"))
                            str = str.substring(1, str.length() - 1);
                        targets.add(str.replace('/', '.'));
                    } else if (value instanceof Type type) {
                        targets.add(type.getClassName());
                    }
                }
            };
        }
    }
}