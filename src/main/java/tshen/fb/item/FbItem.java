package tshen.fb.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import tshen.fb.entity.FbEntity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class FbItem extends Item {
    public static final String NAME = "fb";

    private static final Predicate<Entity> ENTITY_PREDICATE = EntityPredicates.NOT_SPECTATING.and(Entity::canBeCollidedWith);

    private final Function<World, FbEntity> mBaseSupplier;

    public FbItem(Properties inProperties, Function<World, FbEntity> inBaseSupplier) {
        super(inProperties.maxStackSize(1));
        mBaseSupplier = inBaseSupplier;
    }


    @Override
    public ActionResult<ItemStack> onItemRightClick(World inWorld, PlayerEntity inPlayer, Hand inHand) {
        ItemStack itemstack = inPlayer.getHeldItem(inHand);
        RayTraceResult raytraceresult = rayTrace(inWorld, inPlayer, RayTraceContext.FluidMode.ANY);
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            return ActionResult.resultPass(itemstack);
        } else {
            Vector3d vec3d = inPlayer.getLook(1.0F);
            List<Entity> list
                    = inWorld.getEntitiesInAABBexcluding(inPlayer,
                                                         inPlayer.getBoundingBox().expand(vec3d.scale(5.0D)).grow(1.0D),
                                                         ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                Vector3d vec3d1 = inPlayer.getEyePosition(1.0F);

                for (Entity entity : list) {
                    AxisAlignedBB axisalignedbb = entity.getBoundingBox().grow(entity.getCollisionBorderSize());
                    if (axisalignedbb.contains(vec3d1)) {
                        return ActionResult.resultPass(itemstack);
                    }
                }
            }

            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
                FbEntity fbEntity = mBaseSupplier.apply(inWorld);
                fbEntity.initFromItemStack(itemstack);
                fbEntity.setPosition(raytraceresult.getHitVec().getX(), raytraceresult.getHitVec().getY(), raytraceresult.getHitVec().getZ());
                fbEntity.rotationYaw = inPlayer.rotationYaw;
                fbEntity.prevRotationYaw = inPlayer.prevRotationYaw;
                fbEntity.setCustomName(itemstack.getDisplayName());
                if (!inWorld.hasNoCollisions(fbEntity, fbEntity.getBoundingBox().grow(-0.1D))) {
                    return ActionResult.resultFail(itemstack);
                } else {
                    if (!inWorld.isRemote) {
                        inWorld.addEntity(fbEntity);
                        if (!inPlayer.abilities.isCreativeMode) {
                            itemstack.shrink(1);
                        }
                    }
                    inPlayer.addStat(Stats.ITEM_USED.get(this));
                    return ActionResult.resultSuccess(itemstack);
                }
            } else {
                return ActionResult.resultPass(itemstack);
            }
        }
    }

}
