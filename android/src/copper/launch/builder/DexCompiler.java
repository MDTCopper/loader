package copper.launch.builder;

import com.android.tools.r8.*;
import com.android.tools.r8.origin.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class DexCompiler {
    private String id;
    private Map<String, byte[]> bytecode;
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

    public void addSource(byte[] code) {
        ProgramResource resource = ProgramResource.fromBytes(
                Origin.unknown(),
                ProgramResource.Kind.CF,
                code,
                null
        );
        source.add(resource);
    }

    public void compile() {
        try {
            builder.addProgramResourceProvider(new SourceProvider());
            builder.setProgramConsumer(new DexConsumer());
            D8.run(builder.build());
        } catch (Throwable e) {
            throw new RuntimeException("failed to compile dex", e);
        }
    }

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
            bytecode.put(name, data.copyByteData());
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
