package tshen.fb.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.vector.Vector3f;
import tshen.fb.FbConfig;
import tshen.fb.entity.FbEntity;

public abstract class FbAbstractRenderer<T extends FbEntity> extends EntityRenderer<T> {

    protected FbAbstractRenderer(EntityRendererManager renderManager) {
        super(renderManager);
    }

    @Override
    public void render(T inEntity, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        if (!FbConfig.getInstance().isImmersive() || !inEntity.isPassenger(Minecraft.getInstance().player)) {
            matrixStackIn.push();
            matrixStackIn.translate(0.0D, 1.5D, 0.0D);
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(180.0f));
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(entityYaw));
            EntityModel<T> planeModel = getModel();
            IVertexBuilder ivertexbuilder = ItemRenderer
                    .getEntityGlintVertexBuilder(bufferIn, planeModel.getRenderType(this.getEntityTexture(inEntity)), false, false);
            planeModel.setRotationAngles(inEntity, partialTicks, 0, 0, 0, 0);
            planeModel.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
        }
        super.render(inEntity, 0, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    protected abstract EntityModel<T> getModel();
}
