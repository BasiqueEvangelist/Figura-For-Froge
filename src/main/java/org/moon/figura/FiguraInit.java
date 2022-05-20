package org.moon.figura;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(value = FiguraMod.MOD_ID)
public class FiguraInit {
    public FiguraInit() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FiguraMod::onInitializeClient);
    }
}
