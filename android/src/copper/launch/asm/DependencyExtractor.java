package copper.launch.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.signature.*;
import java.util.*;

public class DependencyExtractor extends ClassVisitor {
    private static final Set<String> primitives = new HashSet<>(Arrays.asList(
            "void", "boolean", "char", "byte", "short", "int", "float", "long", "double"
    ));

    private Set<String> dependencies;
    private String currentClassName;

    public DependencyExtractor() {
        super(Opcodes.ASM9);
        dependencies = new HashSet<>();
    }

    public Set<String> getDependencies() {
        dependencies.remove(currentClassName);
        dependencies.removeAll(primitives);
        return dependencies;
    }

    private void addInternalName(String internalName) {
        if (internalName == null) return;
        if (internalName.startsWith("[")) {
            addDescriptor(internalName);
        } else {
            addType(Type.getObjectType(internalName));
        }
    }

    private void addDescriptor(String descriptor) {
        if (descriptor == null) return;
        addType(Type.getType(descriptor));
    }

    private void addType(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                addType(type.getElementType());
                break;
            case Type.OBJECT:
                dependencies.add(type.getClassName());
                break;
            case Type.METHOD:
                for (Type argType : type.getArgumentTypes()) {
                    addType(argType);
                }
                addType(type.getReturnType());
                break;
        }
    }

    private void addSignature(String signature) {
        if (signature == null) return;
        SignatureReader sr = new SignatureReader(signature);
        sr.accept(new DependencyExtractor.DependencySignatureVisitor());
    }

    private AnnotationVisitor getAnnotationVisitor() {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if (value instanceof Type) {
                    addType((Type) value);
                }
            }
            @Override
            public void visitEnum(String name, String descriptor, String value) {
                addDescriptor(descriptor);
            }
            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                addDescriptor(descriptor);
                return this;
            }
            @Override
            public AnnotationVisitor visitArray(String name) {
                return this;
            }
        };
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.currentClassName = Type.getObjectType(name).getClassName();
        addInternalName(superName);
        if (interfaces != null) {
            for (String iface : interfaces) {
                addInternalName(iface);
            }
        }
        addSignature(signature); // (e.g. class MyList<T extends BaseEntity>)
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return getAnnotationVisitor();
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return getAnnotationVisitor();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        addDescriptor(descriptor);
        addSignature(signature); // (e.g. List<MyEntity> list;)

        return new FieldVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                addDescriptor(desc);
                return getAnnotationVisitor();
            }
            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                addDescriptor(desc);
                return getAnnotationVisitor();
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        addDescriptor(descriptor);
        addSignature(signature);
        if (exceptions != null) {
            for (String ex : exceptions) {
                addInternalName(ex);
            }
        }

        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                addDescriptor(desc);
                return getAnnotationVisitor();
            }
            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                addDescriptor(desc);
                return getAnnotationVisitor();
            }
            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                addDescriptor(desc);
                return getAnnotationVisitor();
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                addInternalName(type); // NEW, CHECKCAST, INSTANCEOF
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                addInternalName(owner);
                addDescriptor(descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                addInternalName(owner);
                addDescriptor(descriptor);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) {
                // Lambda hides here
                addDescriptor(descriptor);
                addHandle(bsm);
                for (Object arg : bsmArgs) {
                    if (arg instanceof Type) {
                        addType((Type) arg);
                    } else if (arg instanceof Handle) {
                        addHandle((Handle) arg);
                    }
                }
            }

            private void addHandle(Handle handle) {
                if (handle != null) {
                    addInternalName(handle.getOwner());
                    addDescriptor(handle.getDesc());
                }
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    addType((Type) value); // Class.forName XXX.class
                }
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                addDescriptor(descriptor);
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                addInternalName(type);
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                addDescriptor(descriptor);
                addSignature(signature);
            }
        };
    }

    private class DependencySignatureVisitor extends SignatureVisitor {
        private String currentSignatureClassName;

        public DependencySignatureVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitClassType(String name) {
            currentSignatureClassName = name;
            addInternalName(name);
        }

        @Override
        public void visitInnerClassType(String name) {
            currentSignatureClassName = currentSignatureClassName + "$" + name;
            addInternalName(currentSignatureClassName);
        }
    }
}