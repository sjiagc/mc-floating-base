package tshen.fb.render;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tshen.fb.FbMod;
import tshen.fb.entity.FbEntity;

@OnlyIn(Dist.CLIENT)
public class FbRenderer extends FbAbstractRenderer<FbEntity> {
    public FbRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        shadowSize = 0.6f;
    }

    protected final FbModel mModel = new FbModel();

    @Override
    protected EntityModel<FbEntity> getModel() {
        return mModel;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ResourceLocation getEntityTexture(FbEntity entity) {
        return new ResourceLocation(FbMod.MODID,
                                    "textures/entity/base/fb.png");
    }
}
