package copper.launch.asm;

import org.objectweb.asm.*;

public class AccessMixinChecker extends ClassVisitor {
    private boolean mixin;
    private boolean accessMixin;

    public AccessMixinChecker() {
        super(Opcodes.ASM9);
        mixin = false;
        accessMixin = true;
    }

    public boolean isAccessMixin() {
        return accessMixin && mixin;
    }

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
                if (descriptor.startsWith("Lorg/spongepowered/asm/mixin/")) {
                    if (!descriptor.endsWith("/Accessor;") && !descriptor.endsWith("/Invoker;"))
                        accessMixin = false;
                }
                return super.visitAnnotation(descriptor, visible);
            }
        };
    }
}
