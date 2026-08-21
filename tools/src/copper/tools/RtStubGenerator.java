package copper.tools;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

public class RtStubGenerator {
    public static void main(String[] args) {
        File outputJar = new File("java-stub-rt.jar");
        if (outputJar.exists())
            System.exit(0);

        System.out.println("Generating stub java rt jar...");
        long startTime = System.currentTimeMillis();

        Set<String> writtenEntries = new HashSet<>();

        try {
            FileSystem jrtFs = FileSystems.getFileSystem(URI.create("jrt:/"));
            Path modulesRoot = jrtFs.getPath("/modules");

            try (JarOutputStream zos = new JarOutputStream(new FileOutputStream(outputJar))) {
                Files.walk(modulesRoot).forEach(path -> {
                    if (!Files.isRegularFile(path)) return;

                    String fileName = path.getFileName().toString();

                    if (!fileName.endsWith(".class") || fileName.equals("module-info.class")) {
                        return;
                    }

                    // path: /modules/java.base/java/lang/String.class
                    // we need: java/lang/String.class
                    int nameCount = path.getNameCount();
                    if (nameCount < 3)
                        return;

                    StringBuilder entryNameBuilder = new StringBuilder();
                    for (int i = 2; i < nameCount; i++) {
                        entryNameBuilder.append(path.getName(i).toString());
                        if (i < nameCount - 1) {
                            entryNameBuilder.append("/");
                        }
                    }
                    String entryName = entryNameBuilder.toString().replace('\\', '/');

                    if (!writtenEntries.add(entryName))
                        return;
                    if (!entryName.startsWith("java/") && !entryName.startsWith("javax/"))
                        return;

                    try {
                        byte[] classBytes = Files.readAllBytes(path);

                        byte[] stubbedBytes = processClass(classBytes);

                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);
                        zos.write(stubbedBytes);
                        zos.closeEntry();

                    } catch (Exception e) {
                        System.err.println("failed to process: " + path + " - " + e.getMessage());
                    }
                });
            }

            long endTime = System.currentTimeMillis();
            System.out.printf("Done. Cost: %d ms.\n", (endTime - startTime));
            System.out.printf("File: %s (Size: %.2f MB)\n",
                    outputJar.getAbsolutePath(), outputJar.length() / 1024.0 / 1024.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] processClass(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();

        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        cn.fields.removeIf(f -> (f.access & Opcodes.ACC_PRIVATE) != 0);

        cn.methods.removeIf(m -> (m.access & Opcodes.ACC_PRIVATE) != 0);

        for (MethodNode mn : cn.methods) {
            if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                mn.instructions.clear();
                mn.tryCatchBlocks.clear();
                if (mn.localVariables != null) {
                    mn.localVariables.clear();
                }

                // inject：throw new RuntimeException("Stub!");
                InsnList il = new InsnList();
                il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/RuntimeException"));
                il.add(new InsnNode(Opcodes.DUP));
                il.add(new LdcInsnNode("Stub!"));
                il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false));
                il.add(new InsnNode(Opcodes.ATHROW));

                mn.instructions = il;
                mn.maxStack = 3;
                int argsSize = 0;
                for (Type argType : Type.getArgumentTypes(mn.desc)) {
                    argsSize += argType.getSize();
                }
                mn.maxLocals = argsSize + ((mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1);
            }
        }

        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
