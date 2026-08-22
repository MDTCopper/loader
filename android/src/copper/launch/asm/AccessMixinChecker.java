package copper.launch.asm;

import org.objectweb.asm.*;

/**
 * Checks whether a mixin class is an "access mixin".
 *
 * <p>A class is an access mixin when it is annotated {@code @Mixin} and every
 * mixin annotation on its methods is either {@code @Accessor} or {@code @Invoker}.
 * Access mixins only generate accessor code, so unlike normal mixins they are
 * kept as source when the consuming container is compiled.</p>
 */
public class AccessMixinChecker extends ClassVisitor {
    private boolean mixin;
    private boolean accessMixin;

    public AccessMixinChecker() {
        super(Opcodes.ASM9);
        mixin = false;
        accessMixin = true;
    }

    /** Whether the class is an access mixin (mixin + only accessor/invoker methods). */
    public boolean isAccessMixin() {
        return accessMixin && mixin;
    }

    /** Whether the class is a mixin at all. */
    public boolean isMixin() {
        return mixin;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
            mixin = true;
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(this.api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                // any mixin method annotation that is not accessor/invoker makes
                // this a normal (non-access) mixin
                if (descriptor.startsWith("Lorg/spongepowered/asm/mixin/")) {
                    if (!descriptor.endsWith("/Accessor;") && !descriptor.endsWith("/Invoker;"))
                        accessMixin = false;
                }
                return super.visitAnnotation(descriptor, visible);
            }
        };
    }
}