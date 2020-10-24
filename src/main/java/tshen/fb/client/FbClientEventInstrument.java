package tshen.fb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tshen.fb.FbConfig;
import tshen.fb.entity.FbEntity;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class FbClientEventInstrument {
    @SubscribeEvent
    public static void onInputUpdateEvent(final InputUpdateEvent inInputUpdateEvent) {
        PlayerEntity player = inInputUpdateEvent.getPlayer();
        if (player == null)
            return;
        Entity riddenEntity = player.getLowestRidingEntity();
        if (!(riddenEntity instanceof FbEntity))
            return;
        FbEntity fbEntity = (FbEntity)riddenEntity;
        if (fbEntity.isSafeToDismount())
            return;
        inInputUpdateEvent.getMovementInput().sneaking = false;
    }

    @SubscribeEvent
    public static void onMountEvent(EntityMountEvent inMountEvent) {
        if (!inMountEvent.getWorldObj().isRemote())
            return;
        if (inMountEvent.isDismounting() && inMountEvent.getEntityMounting() == Minecraft.getInstance().player) {
            FbConfig.getInstance().setImmersive(false);
        }
    }
}