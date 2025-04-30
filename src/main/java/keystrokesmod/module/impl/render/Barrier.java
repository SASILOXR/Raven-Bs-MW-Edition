package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;

public class Barrier extends Module {
    public Barrier() {
        super("Barrier", category.render);
        this.registerSetting(new DescriptionSetting("Show Barrier"));
    }

    public void onEnable() {
        mc.renderGlobal.loadRenderers();
    }

    public void onDisable() {
        mc.renderGlobal.loadRenderers();
    }

}
