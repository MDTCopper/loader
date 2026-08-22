package copper.launch.builder;

import com.android.tools.r8.*;
import com.android.tools.r8.origin.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * Wraps a d8 run that turns class files into dex, one class per dex file
 * (intermediate mode), and collects the output bytecode into a map.
 *
 * <p>Sources, classpaths and libraries can be added as files or as in-memory
 * {@link D8Resource}s. The output classes are stored per class name so they can
 * be merged into a {@link BaseDexPool} afterwards.</p>
 */
public class DexCompiler {
    /** Id used only for log tags. */
    private String id;
    private final Map<String, byte[]> bytecode;
    /** Extra raw class sources added as bytes. */
    private List<ProgramResource> source;
    private D8Command.Builder builder;

    public DexCompiler() {
        id = "unknown";
        bytecode = new HashMap<>();
        source = new ArrayList<>();
        builder = D8Command.builder(new CompilerLog());
        builder.setMinApiLevel(30);
        builder.setIntermediate(true);
    }

    /** Sets the id used in log tags. */
    public void setId(String id) {
        this.id = id;
    }

    public void addLibrary(File jar) {
        builder.addLibraryFiles(Paths.get(jar.getAbsolutePath()));
    }

    public void addLibrary(D8Resource library) {
        builder.addLibraryResourceProvider(library);
    }

    public void addClassPath(File jar) {
        builder.addClasspathFiles(Paths.get(jar.getAbsolutePath()));
    }

    public void addClassPath(D8Resource library) {
        builder.addClasspathResourceProvider(library);
    }

    public void addDesugaredLibraryConfig(String config) {
        builder.addDesugaredLibraryConfiguration(config);
    }

    public void addSource(D8Resource source) {
        builder.addProgramResourceProvider(source);
    }

    /** Adds a single raw class file as a program source. */
    public void addSource(byte[] code) {
        ProgramResource resource = ProgramResource.fromBytes(
                Origin.unknown(),
                ProgramResource.Kind.CF,
                code,
                null
        );
        source.add(resource);
    }

    /** Runs d8. The resulting dex classes land in {@link #getBytecodes()}. */
    public void compile() {
        try {
            builder.addProgramResourceProvider(new SourceProvider());
            builder.setProgramConsumer(new DexConsumer());
            D8.run(builder.build());
        } catch (Throwable e) {
            throw new RuntimeException("failed to compile dex", e);
        }
    }

    /** Map of class name → dex bytecode produced by the last compile. */
    public Map<String, byte[]> getBytecodes() {
        return bytecode;
    }

    private class SourceProvider implements ProgramResourceProvider {
        @Override
        public Collection<ProgramResource> getProgramResources() throws ResourceException {
            return source;
        }

        @Override
        public void getProgramResources(Consumer<ProgramResource> consumer) throws ResourceException {
            for (var res : getProgramResources())
                consumer.accept(res);
        }
    }

    private class DexConsumer implements DexFilePerClassFileConsumer {
        public void accept(String primaryClassDescriptor, ByteDataView data, Set<String> descriptors, DiagnosticsHandler handler) {
            String name = primaryClassDescriptor;
            if (name.startsWith("L") && name.endsWith(";"))
                name = name.substring(1, name.length() - 1);
            name = name.replace('/', '.');
            // d8 may call back from several threads, guard the shared map
            synchronized (bytecode) {
                bytecode.put(name, data.copyByteData());
            }
        }

        @Override
        public void finished(DiagnosticsHandler diagnosticsHandler) {}
    }

    private class CompilerLog implements DiagnosticsHandler {
        @Override
        public void info(Diagnostic info) {
            Log.info("d8:" + id, info.getDiagnosticMessage());
        }

        @Override
        public void warning(Diagnostic warning) {
            Log.warn("d8:" + id, warning.getDiagnosticMessage());
        }

        @Override
        public void error(Diagnostic error) {
            Log.error("d8:" + id, error.getDiagnosticMessage());
        }
    }
}