package copper.launch.builder;

import com.android.tools.r8.*;
import com.android.tools.r8.origin.*;
import copper.loader.util.*;
import java.util.*;

public class DexMerger {
    private String id;
    private final List<byte[]> bytecode;
    private D8Command.Builder builder;

    public DexMerger() {
        bytecode = new ArrayList<>();
        builder = D8Command.builder(new MergerLog());
        builder.setMinApiLevel(30);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addSource(byte[] code) {
        builder.addDexProgramData(code, Origin.unknown());
    }

    public void merge() {
        try {
            builder.setProgramConsumer(new DexConsumer());
            D8.run(builder.build());
        } catch (Throwable e) {
            throw new RuntimeException("failed to merge dex", e);
        }
    }

    public List<byte[]> getBytecodes() {
        return bytecode;
    }

    private class DexConsumer implements DexIndexedConsumer {
        @Override
        public void accept(int fileIndex, ByteDataView data, Set<String> descriptors, DiagnosticsHandler handler) {
            synchronized (bytecode) {
                bytecode.add(data.copyByteData());
            }
        }

        @Override
        public void finished(DiagnosticsHandler diagnosticsHandler) {}
    }

    private class MergerLog implements DiagnosticsHandler {
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
