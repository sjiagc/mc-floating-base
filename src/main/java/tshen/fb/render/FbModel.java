package tshen.fb.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import tshen.fb.entity.FbEntity;

public class FbModel extends EntityModel<FbEntity> {
    private final ModelRenderer body;
    private final ModelRenderer deck;
    private final ModelRenderer diag;
    private final ModelRenderer seats;
    private final ModelRenderer legs;

    public FbModel() {
        textureWidth = 512;
        textureHeight = 512;

        body = new ModelRenderer(this);
        body.setRotationPoint(0.0F, 24.0F, 0.0F);
        body.setTextureOffset(192, 16).addBox(-32.0F, -28.0F, -32.0F, 64.0F, 12.0F, 4.0F, 0.0F, false);
        body.setTextureOffset(192, 0).addBox(-32.0F, -28.0F, 28.0F, 64.0F, 12.0F, 4.0F, 0.0F, false);
        body.setTextureOffset(128, 136).addBox(28.0F, -28.0F, -28.0F, 4.0F, 12.0F, 56.0F, 0.0F, false);
        body.setTextureOffset(0, 136).addBox(-32.0F, -28.0F, -28.0F, 4.0F, 12.0F, 56.0F, 0.0F, false);
        body.setTextureOffset(0, 210).addBox(-13.0F, -28.0F, 32.0F, 26.0F, 13.0F, 13.0F, 0.0F, false);

        deck = new ModelRenderer(this);
        deck.setRotationPoint(0.0F, 0.0F, 0.0F);
        body.addChild(deck);
        deck.setTextureOffset(0, 68).addBox(-32.0F, -16.0F, -32.0F, 64.0F, 4.0F, 64.0F, 0.0F, false);

        diag = new ModelRenderer(this);
        diag.setRotationPoint(0.0F, 0.0F, 0.0F);
        deck.addChild(diag);
        setRotationAngle(diag, 0.0F, -0.7854F, 0.0F);
        diag.setTextureOffset(0, 0).addBox(-32.0F, -15.0F, -32.0F, 64.0F, 3.0F, 64.0F, 0.0F, false);

        seats = new ModelRenderer(this);
        seats.setRotationPoint(0.0F, 0.0F, 0.0F);
        body.addChild(seats);
        seats.setTextureOffset(0, 68).addBox(-8.0F, -18.0F, -24.0F, 16.0F, 2.0F, 16.0F, 0.0F, false);
        seats.setTextureOffset(0, 36).addBox(-24.0F, -18.0F, -8.0F, 16.0F, 2.0F, 16.0F, 0.0F, false);
        seats.setTextureOffset(0, 18).addBox(8.0F, -18.0F, -8.0F, 16.0F, 2.0F, 16.0F, 0.0F, false);
        seats.setTextureOffset(0, 0).addBox(-8.0F, -18.0F, 8.0F, 16.0F, 2.0F, 16.0F, 0.0F, false);

        legs = new ModelRenderer(this);
        legs.setRotationPoint(0.0F, 24.0F, 0.0F);
        legs.setTextureOffset(0, 86).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 12.0F, 8.0F, 0.0F, false);
    }

    @Override
    public void setRotationAngles(FbEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
        //previously the render function, render code was moved to a method below
    }

    @Override
    public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
        body.render(matrixStack, buffer, packedLight, packedOverlay);
        legs.render(matrixStack, buffer, packedLight, packedOverlay);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
