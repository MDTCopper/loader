package copper.launch.asm;

import org.objectweb.asm.*;
import java.util.*;

public class MixinTargetExtractor extends ClassVisitor {
    private Set<String> target;

    public MixinTargetExtractor() {
        super(Opcodes.ASM9);
        target = new HashSet<>();
    }

    public Set<String> getTarget() {
        return target;
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
            if (!name.equals("value") && !name.equals("values"))
                return super.visitArray(name);
            return new AnnotationVisitor(api) {
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                    if (value instanceof String str) {
                        if (str.startsWith("L") && str.endsWith(";"))
                            str = str.substring(1, str.length() - 1);
                        target.add(str.replace('/', '.'));
                    } else if (value instanceof Type type) {
                        target.add(type.getClassName());
                    }
                }
            };
        }
    }
}
