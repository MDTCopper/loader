package copper.loader.mixin;

import copper.loader.func.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;

/**
 * Instrumentation handed to the framework's hot swap agent: a registered transformer is replaced by
 * the wrapper of the container, which decides whether the agent may transform a redefined class.
 * Removal goes through the wrapper, the instance the JVM knows.
 */
public class MixinInstrumentation implements Instrumentation {
    private final Instrumentation target;
    private final Func<ClassFileTransformer, ClassFileTransformer> wrapper;
    private final Map<ClassFileTransformer, ClassFileTransformer> transformerWrapperMap;

    public MixinInstrumentation(Instrumentation instrumentation, Func<ClassFileTransformer, ClassFileTransformer> wrapper) {
        target = instrumentation;
        this.wrapper = wrapper;
        transformerWrapperMap = new HashMap<>();
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        var wrapped = wrapper.get(transformer);
        transformerWrapperMap.put(transformer, wrapped);
        target.addTransformer(wrapped, canRetransform);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        addTransformer(transformer, false);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        ClassFileTransformer wrapper = transformerWrapperMap.get(transformer);
        return wrapper != null && target.removeTransformer(wrapper);
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return target.isRetransformClassesSupported();
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        target.retransformClasses(classes);
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return target.isRedefineClassesSupported();
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {
        target.redefineClasses(definitions);
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return target.isModifiableClass(theClass);
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return target.getAllLoadedClasses();
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return target.getInitiatedClasses(loader);
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return target.getObjectSize(objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        target.appendToBootstrapClassLoaderSearch(jarfile);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        target.appendToSystemClassLoaderSearch(jarfile);
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return target.isNativeMethodPrefixSupported();
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        target.setNativeMethodPrefix(transformer, prefix);
    }

    @Override
    public void redefineModule(Module module, Set<Module> extraReads, Map<String, Set<Module>> extraExports, Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses, Map<Class<?>, List<Class<?>>> extraProvides) {
        target.redefineModule(module, extraReads, extraExports, extraOpens, extraUses, extraProvides);
    }

    @Override
    public boolean isModifiableModule(Module module) {
        return target.isModifiableModule(module);
    }
}
