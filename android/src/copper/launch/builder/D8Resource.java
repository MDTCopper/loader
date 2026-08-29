package copper.launch.builder;

import com.android.tools.r8.*;
import com.android.tools.r8.origin.*;
import copper.loader.container.*;
import copper.loader.func.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;

/**
 * An in-memory jar of class files presented to d8 as program/library resources.
 *
 * <p>Classes are keyed by descriptor (e.g. {@code La/b/C;}). The class also
 * implements d8's provider interfaces so it can be passed straight to
 * {@link DexCompiler} as a source, classpath or library.</p>
 */
public class D8Resource implements ClassFileResourceProvider, ProgramResourceProvider {
    /** Map of class descriptor to program resource. */
    private Map<String, ProgramResource> codes;

    public D8Resource() {
        codes = new HashMap<>();
    }

    /** Reads every class of a jar file. */
    public D8Resource(File jar) throws IOException {
        this(jar, null);
    }

    /** Reads the classes of a jar file that pass the filter. */
    public D8Resource(File jar, ClassFilter filter) throws IOException {
        codes = new HashMap<>();
        try (var zip = new ZipFile(jar)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class"))
                    continue;
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/"))
                    name = name.substring(1);
                // remove .class
                name = name.substring(0, name.length() - 6).replace('/', '.');
                if (filter == null || filter.check(name)) {
                    byte[] code = Streams.readAllBytes(zip.getInputStream(entry));
                    putCode(name, code);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read jar", e);
        }
    }

    /** Reads every class of an in-memory jar. */
    public D8Resource(byte[] jar) {
        this(new ByteArrayInputStream(jar), null);
    }

    /** Reads the classes of an in-memory jar that pass the filter. */
    public D8Resource(byte[] jar, ClassFilter filter) {
        this(new ByteArrayInputStream(jar), filter);
    }

    public D8Resource(InputStream jar, ClassFilter filter) {
        codes = new HashMap<>();
        try (var zis = new ZipInputStream(jar)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    zis.closeEntry();
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/"))
                    name = name.substring(1);
                // remove .class
                name = name.substring(0, name.length() - 6).replace('/', '.');
                if (filter == null || filter.check(name)) {
                    byte[] code = Streams.readAllBytes(zis);
                    putCode(name, code);
                }
                zis.closeEntry();
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read jar", e);
        }
    }

    /** Returns a copy that only keeps the classes passing the filter. */
    public D8Resource getFiltered(ClassFilter filter) {
        var filtered = new D8Resource();
        for (var entry : codes.entrySet()) {
            String name = entry.getKey();
            // convert desc to name: La/b/c; -> a.b.c
            name = name.substring(1, name.length() - 1).replace('/', '.');
            if (filter.check(name))
                filtered.codes.put(entry.getKey(), entry.getValue());
        }
        return filtered;
    }

    /** Whether a class is present. */
    public boolean hasCode(String className) {
        className = "L" + className.replace('.', '/') + ";";
        return codes.containsKey(className);
    }

    /** Merges all classes of another resource into this one. */
    public void putAllCode(D8Resource resource) {
        codes.putAll(resource.codes);
    }

    /** Stores class bytecode under its name. */
    public void putCode(String className, byte[] code) {
        String classDesc = "L" + className.replace('.', '/') + ";";
        ProgramResource resource = ProgramResource.fromBytes(
                Origin.unknown(),
                ProgramResource.Kind.CF,
                code,
                null
        );
        this.codes.put(classDesc, resource);
    }

    /** Iterates over every stored class (name → raw bytes). */
    public void eachCode(ThrowableCons2<String, byte[]> cons) {
        try {
            for (var entry : codes.entrySet()) {
                String name = entry.getKey();
                // convert desc to name: La/b/c; -> a.b.c
                name = name.substring(1, name.length() - 1).replace('/', '.');
                cons.get(name, entry.getValue().getBytes());
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ---- d8 provider interfaces ----

    @Override
    public Set<String> getClassDescriptors() {
        return codes.keySet();
    }

    @Override
    public ProgramResource getProgramResource(String s) {
        return codes.get(s);
    }

    @Override
    public Collection<ProgramResource> getProgramResources() {
        return codes.values();
    }

    @Override
    public void getProgramResources(Consumer<ProgramResource> consumer) {
        for (var res : codes.values())
            consumer.accept(res);
    }

    @Override
    public void finished(DiagnosticsHandler handler) {}

    @Override
    public DataResourceProvider getDataResourceProvider() {
        return null;
    }
}