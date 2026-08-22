package copper.launch.builder;

import com.android.tools.r8.*;
import com.android.tools.r8.origin.*;
import copper.loader.util.*;
import java.util.*;

/**
 * Merges already compiled dex data back into complete dex files.
 *
 * <p>Used by {@link RuntimeDex} to combine a mod's base dex with its mixin delta
 * into the final {@code classes.dex}/{@code classes2.dex}/... set.</p>
 */
public class DexMerger {
    /** Id used only for log tags. */
    private String id;
    private final List<byte[]> bytecode;
    private D8Command.Builder builder;

    public DexMerger() {
        bytecode = new ArrayList<>();
        builder = D8Command.builder(new MergerLog());
        builder.setMinApiLevel(30);
    }

    /** Sets the id used in log tags. */
    public void setId(String id) {
        this.id = id;
    }

    /** Adds one already compiled dex entry. */
    public void addSource(byte[] code) {
        builder.addDexProgramData(code, Origin.unknown());
    }

    /** Runs the merge. The resulting dex files land in {@link #getBytecodes()}. */
    public void merge() {
        try {
            builder.setProgramConsumer(new DexConsumer());
            D8.run(builder.build());
        } catch (Throwable e) {
            throw new RuntimeException("failed to merge dex", e);
        }
    }

    /** List of merged dex files, in order. */
    public List<byte[]> getBytecodes() {
        return bytecode;
    }

    private class DexConsumer implements DexIndexedConsumer {
        @Override
        public void accept(int fileIndex, ByteDataView data, Set<String> descriptors, DiagnosticsHandler handler) {
            // d8 may call back from several threads, guard the shared list
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