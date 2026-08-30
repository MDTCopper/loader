package copper.loader.mixin;

import copper.loader.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.logging.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Mixin service provider that integrates Copper's container system into the
 * SpongePowered Mixin framework.
 *
 * <p>Implements {@link IClassProvider}, {@link IClassBytecodeProvider}, and
 * {@link ITransformerProvider} to serve bytecode from {@link MixinEngine#bytecodeProvider}
 * and mixin configs from in-memory {@code copper://} URIs. Captures the
 * framework's {@link IMixinTransformer} during wiring for later use by containers.</p>
 */
@SuppressWarnings("deprecation")
public class MixinEngineService extends MixinServiceAbstract implements ITransformerProvider, IClassProvider, IClassBytecodeProvider {
    @Override
    public String getName() {
        return "Copper";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    protected ILogger createLogger(String name) {
        return new MixinEngineLogger(name);
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual("Copper");
    }

    /**
     * Serves resources. Mixin configs stored in {@link MixinEngine#config} are accessible
     * via {@code copper://<id>.json} URIs. All other resources are fetched from the classpath.
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        if (name.startsWith("copper://") && name.endsWith(".json")) {
            String id = name.substring(9, name.length() - 5);
            try {
                return new ByteArrayInputStream(MixinEngine.config.get(id).getBytes(StandardCharsets.UTF_8));
            } catch (IndexOutOfBoundsException e) {
                Log.warn(MixinEngine.id, "failed to get config, id: " + id);
            } catch (Throwable ignored) {}
        }
        return MixinEngine.class.getClassLoader().getResourceAsStream(name);
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException {
        byte[] code = MixinEngine.bytecodeProvider.get(name);
        if (code == null)
            throw new ClassNotFoundException(name);
        ClassReader reader = new ClassReader(code);
        ClassNode node = new ClassNode();
        reader.accept(node, readerFlags);
        return node;
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, MixinEngineService.class.getClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, MixinEngine.class.getClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, MixinEngine.class.getClassLoader());
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(String name) {}

    // deprecated
    @Override
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        MixinEngine.phaseConsumer = phaseConsumer;
        super.wire(phase, phaseConsumer);
    }

    /**
     * Captures the {@link IMixinTransformer} when the framework offers its internals.
     */
    @Override
    public void offer(IMixinInternal internal) {
        super.offer(internal);
        if (internal instanceof IMixinTransformerFactory)
            MixinEngine.transformer = getInternal(IMixinTransformerFactory.class).createTransformer();
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_17;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_25;
    }
}
