package copper.loader.mixin;

import copper.loader.container.*;

public class MixinContainerClassFilter extends ClassFilter {
    public MixinContainerClassFilter() {
        super();
        addRule("exclude copper.loader.mixin.IMixinEngine");
        addRule("exclude copper.loader.mixin.MixinContainerClassFilter");
        addRule("include org.spongepowered.asm.*");
        addRule("include copper.loader.mixin.*");
        addRule("exclude copper.loader.*");
    }
}
