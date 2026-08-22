package copper.launch.builder;

import copper.loader.container.*;
import copper.loader.func.*;
import copper.loader.util.*;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.classfmt.*;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Compiles java sources in memory with the ECJ compiler.
 *
 * <p>Used to build the android components (game android sources + arc backend +
 * {@code LoaderActivity}) without a full android toolchain. Classpaths can be
 * jar files or raw jar bytes, optionally filtered. The compiled classes are
 * collected into a map by class name.</p>
 */
public class JvmCompiler {
    private Map<String, byte[]> bytecode;
    private Map<String, String> setting;
    private ArrayList<SourceFile> source;
    private NameEnv env;
    private ArrayList<String> error;

    public JvmCompiler() {
        bytecode = new HashMap<>();
        setting = new HashMap<>();
        source = new ArrayList<>();
        env = new NameEnv();
        error = new ArrayList<>();
        setting.put(CompilerOptions.OPTION_Encoding, "UTF-8");
    }

    /** Adds a source file by its full class name. */
    public void addSource(String fullClassName, String code) {
        source.add(new SourceFile(fullClassName, code));
    }

    /** Adds a source file, treating {@code path} as relative to {@code basePath}. */
    public void addSource(String basePath, String path, String code) {
        if (path.endsWith(".java"))
            path = path.substring(0, path.length() - 5);
        if (!basePath.endsWith("/"))
            basePath += "/";
        addSource(path.substring(basePath.length()), code);
    }

    /** Sets the source/compliance/target java version (e.g. "17"). */
    public void setVersion(String source, String bytecode) {
        setting.put(CompilerOptions.OPTION_Source, source);
        setting.put(CompilerOptions.OPTION_TargetPlatform, bytecode);
        setting.put(CompilerOptions.OPTION_Compliance, source);
    }

    public void addClassPath(byte[] jar) {
        env.addClassPath(jar, null);
    }

    public void addClassPath(byte[] jar, ClassFilter filter) {
        env.addClassPath(jar, filter);
    }

    public void addClassPath(File jar) {
        env.addClassPath(jar, null);
    }

    public void addClassPath(File jar, ClassFilter filter) {
        env.addClassPath(jar, filter);
    }

    /** Runs the compiler. Errors are collected and reported via {@link #getErrors()}. */
    public void compile() {
        CompilerOptions options = new CompilerOptions(setting);
        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.getDefault());
        ICompilerRequestor requestor = new CompilerRequestor();
        Compiler compiler = new Compiler(env, policy, options, requestor, problemFactory);
        compiler.compile(source.toArray(ICompilationUnit[]::new));
    }

    /** Whether the last compile produced any errors. */
    public boolean hasErrors() {
        return !error.isEmpty();
    }

    /** The compile error messages. */
    public List<String> getErrors() {
        return error;
    }

    /** Map of class name → compiled bytecode. */
    public Map<String, byte[]> getBytecodes() {
        return bytecode;
    }

    /** Collects the compiled classes (or the error messages) of each unit. */
    private class CompilerRequestor implements ICompilerRequestor {
        @Override
        public void acceptResult(CompilationResult result) {
            if (result.hasErrors()) {
                for (var p : result.getProblems())
                    error.add(p.toString());
                return;
            }
            for (ClassFile classFile : result.getClassFiles()) {
                char[][] compoundName = classFile.getCompoundName();
                StringBuilder fullClassName = new StringBuilder();
                for (int i = 0; i < compoundName.length; i++) {
                    if (i > 0)
                        fullClassName.append('.');
                    fullClassName.append(compoundName[i]);
                }
                bytecode.put(fullClassName.toString(), classFile.getBytes());
            }
        }
    }

    /** One in-memory compilation unit, with its class name as the path. */
    private static class SourceFile implements ICompilationUnit {
        private final Path classPath;
        private final String content;

        public SourceFile(String fullClassName, String content) {
            this.classPath = Paths.get(fullClassName.replace('.', '/'));
            this.content = content;
        }

        @Override
        public char[] getContents() {
            return content.toCharArray();
        }

        @Override
        public char[] getMainTypeName() {
            return classPath.getFileName().toString().toCharArray();
        }

        @Override
        public char[][] getPackageName() {
            Path packagePath = classPath.getParent();
            if (packagePath != null) {
                char[][] arr = new char[packagePath.getNameCount()][];
                for (int i = 0; i < arr.length; i++)
                    arr[i] = packagePath.getName(i).toString().toCharArray();
                return arr;
            }
            return new char[0][];
        }

        @Override
        public char[] getFileName() {
            return (classPath.toString() + ".java").toCharArray();
        }

        @Override
        public ModuleBinding module(LookupEnvironment environment) {
            return environment.getModule(ModuleBinding.UNNAMED);
        }
    }

    /**
     * The compiler's name environment: resolves types and packages from the
     * classpath jars added to this compiler.
     */
    private static class NameEnv implements INameEnvironment {
        private Set<String> packageName;
        private Map<String, byte[]> code;

        public NameEnv() {
            packageName = new HashSet<>();
            code = new HashMap<>();
        }

        /** Registers one classpath entry (class name → bytes) if it passes the filter. */
        private void processClassPathFile(ZipEntry entry, ClassFilter filter, ThrowableProv<byte[]> code) throws Throwable {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace('\\', '/');
                if (className.startsWith("/"))
                    className = className.substring(1);
                // remove .class
                className = className.substring(0, className.length() - 6);
                className = className.replace('/', '.');

                if (filter != null && !filter.check(className))
                    return;

                // record every package segment so isPackage() can answer
                int i = 0;
                while (true) {
                    int j = className.indexOf('.', i);
                    if (j == -1)
                        break;
                    packageName.add(className.substring(0, j));
                    i = j + 1;
                }

                this.code.put(className, code.get());
            }
        }

        public void addClassPath(File jar, ClassFilter filter) {
            try {
                ZipFile zip = new ZipFile(jar);
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    processClassPathFile(entry, filter, () -> {
                        try (var is = zip.getInputStream(entry)) {
                            return Streams.readAllBytes(is);
                        }
                    });
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public void addClassPath(byte[] jar, ClassFilter filter) {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jar))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    processClassPathFile(entry, filter, () -> Streams.readAllBytes(zis));
                    zis.closeEntry();
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        /** Answers a type lookup by reading the stored class bytes. */
        @Override
        public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < compoundTypeName.length; i++) {
                if (i > 0) result.append('.');
                result.append(compoundTypeName[i]);
            }

            String name = result.toString();
            byte[] code = this.code.get(name);
            if (code != null) {
                try {
                    ClassFileReader classFileReader = new ClassFileReader(code, name.toCharArray(), true);
                    return new NameEnvironmentAnswer(classFileReader, null);
                } catch (ClassFormatException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            char[][] compoundName = new char[packageName.length + 1][];
            System.arraycopy(packageName, 0, compoundName, 0, packageName.length);
            compoundName[packageName.length] = typeName;
            return findType(compoundName);
        }

        /** Answers whether a package name is known from the classpath. */
        @Override
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            StringBuilder sb = new StringBuilder();
            if (parentPackageName != null) {
                for (char[] p : parentPackageName) {
                    sb.append(new String(p)).append('.');
                }
            }
            sb.append(new String(packageName));
            String name = sb.toString();
            return this.packageName.contains(name);
        }

        @Override
        public void cleanup() {
            packageName.clear();
            code.clear();
        }
    }
}