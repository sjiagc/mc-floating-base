package tshen.fb.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tshen.fb.FbMod;
import tshen.fb.item.FbItem;
import tshen.fb.setup.FbItems;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FbEntity extends Entity {
    private static final Logger logger = LogManager.getLogger(FbEntity.class);

    private static final String NBT_KEY_ENTITY_TAG = "EntityTag";
    private static final String NBT_KEY_ELECTRICITY = "electricity";
    private static final String NBT_KEY_FUEL_INVENTORY = "fuelInventory";
    private static final String NBT_KEY_INVENTORIES = "inventories";
    private static final String NBT_KEY_INVENTORY_SIZE = "size";
    private static final String NBT_KEY_INVENTORY_PATTERN = "inventory_%2d";
    private static final double MOMENTUM = 0.05F;
    private static final int TURN_SPEED = 3;    // Degree
    private static final int INVENTORY_COUNT = 10;
    private static final int FUEL_CONVERT_BATCH_COUNT = 4;
    private static final int FUEL_ELECTRICITY_CONVERT_RATE = 500;
    private static final int ELECTRICITY_CHARGE_WATER_LEVEL = 100;

    private static final Field FIELD_VEHICLE_FLOATING_TICK_COUNT;

    private static final DataParameter<Float> DATA_ELECTRICITY = EntityDataManager.createKey(FbEntity.class, DataSerializers.FLOAT);

    private int mIntentoryIndex = 0;

    static {
        Field fieldVehicleFloatingTickCount;
        try {
            fieldVehicleFloatingTickCount = ServerPlayNetHandler.class.getDeclaredField("field_184346_E");
            fieldVehicleFloatingTickCount.setAccessible(true);
        } catch (NoSuchFieldException e) {
            fieldVehicleFloatingTickCount = null;
            logger.error("Get field ServerPlayNetHandler.vehicleFloatingTickCount failed: {}", e.getMessage());
        } catch (SecurityException e) {
            fieldVehicleFloatingTickCount = null;
            logger.error("Set field ServerPlayNetHandler.vehicleFloatingTickCount accessible failed: {}", e.getMessage());
        }
        FIELD_VEHICLE_FLOATING_TICK_COUNT = fieldVehicleFloatingTickCount;
    }

    public FbInventory[] mInventories;
    public FbFuelInventory mFuelInventory;
    private boolean mSinking;
    private float mDeltaRotation;
    private int mLerpSteps;
    private double mLerpX;
    private double mLerpY;
    private double mLerpZ;
    private double mLerpYaw;
    private PassengerManager mPassengerManager;

    public FbEntity(EntityType<? extends FbEntity> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);

        this.stepHeight = 0.9999f;
        mFuelInventory = new FbFuelInventory();
        mInventories = new FbInventory[INVENTORY_COUNT];
        for (int i = 0; i < INVENTORY_COUNT; ++i) {
            mInventories[i] = new FbInventory();
        }
        mSinking = false;
        mPassengerManager = new PassengerManager();
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return getItemStack();
    }

    @Override
    protected void registerData() {
        dataManager.register(DATA_ELECTRICITY, 0.0f);
    }

    @Nullable
    public Entity getControllingPassenger() {
        List<Entity> list = this.getPassengers();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void tick() {
        rotationPitch = 0.0f;
        super.tick();
        mPassengerManager.tick(getPassengers());
        this.tickLerp();
        if (this.canPassengerSteer()) {
            updateMotion();
            if (this.world.isRemote) {
                this.drive();
            }
            this.move(MoverType.SELF, this.getMotion());
        } else {
            this.setMotion(Vector3d.ZERO);
        }

        updateElectricity();

        resetDriverFloating();

        this.doBlockCollisions();
        List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getBoundingBox().grow((double)0.2F, (double)-0.01F, (double)0.2F), EntityPredicates.pushableBy(this));
        if (!list.isEmpty()) {
            boolean flag = !this.world.isRemote;

            for(int j = 0; j < list.size(); ++j) {
                Entity entity = list.get(j);
                if (!entity.isPassenger(this)) {
                    if (flag && canFitPassenger(entity) && !entity.isPassenger() && entity.getWidth() < this.getWidth() && entity instanceof LivingEntity && !(entity instanceof WaterMobEntity) && !(entity instanceof PlayerEntity)) {
                        entity.startRiding(this);
                    } else {
                        this.applyEntityCollision(entity);
                    }
                }
            }
        }
    }

    private void tickLerp() {
        if (canPassengerSteer()) {
            mLerpSteps = 0;
            setPacketCoordinates(getPosX(), getPosY(), getPosZ());
        }

        if (mLerpSteps > 0) {
            double d0 = getPosX() + (mLerpX - getPosX()) / (double) mLerpSteps;
            double d1 = getPosY() + (mLerpY - getPosY()) / (double) mLerpSteps;
            double d2 = getPosZ() + (mLerpZ - getPosZ()) / (double) mLerpSteps;
            double d3 = MathHelper.wrapDegrees(mLerpYaw - (double)rotationYaw);
            rotationYaw = (float)((double)rotationYaw + d3 / (double) mLerpSteps);
            --mLerpSteps;
            setPosition(d0, d1, d2);
            setRotation(rotationYaw, rotationPitch);
        }
    }

    private void resetDriverFloating()  {
        if (this.world.isRemote)
            return;
        Entity ctrlPassenger = getControllingPassenger();
        if (ctrlPassenger instanceof ServerPlayerEntity) {
            if (FIELD_VEHICLE_FLOATING_TICK_COUNT == null)
                return;
            ServerPlayerEntity driver = (ServerPlayerEntity) ctrlPassenger;
            try {
                FIELD_VEHICLE_FLOATING_TICK_COUNT.setInt(driver.connection, 0);
            } catch (IllegalAccessException e) {
                logger.error("Set ServerPlayNetHandler.vehicleFloatingTickCount failed: {}", e.getMessage());
            }
        }
    }

    private void drive() {
        if (this.isBeingRidden()) {
            Entity controllingPassenger = getControllingPassenger();
            if (!(controllingPassenger instanceof PlayerEntity))
                return;
            PlayerEntity driver = (PlayerEntity)controllingPassenger;
            float forward = driver.moveForward;
            float upward = 0.0f;
            float deltaRotation = 0.0f;
            if (isSprinting())
                upward = 1.0f;
            else if (mSinking)
                upward = -1.0f;
            if (driver.moveStrafing > 0)
                deltaRotation = -TURN_SPEED;
            else if (driver.moveStrafing < 0)
                deltaRotation = TURN_SPEED;

            if (dataManager.get(DATA_ELECTRICITY) <= 0) {
                forward = 0.0f;
                deltaRotation = 0.0f;
                if (isInWater())
                    upward = Math.max(upward, 0.0f);
                else
                    upward = Math.min(upward, 0.0f);
            }
            if (isAboveWater())
                upward /= 25;

            mDeltaRotation += deltaRotation;
            this.rotationYaw += this.mDeltaRotation;

            setMotion(getMotion().add(MathHelper.sin(-rotationYaw * ((float)Math.PI / 180F)) * forward,
                                      upward,
                                      MathHelper.cos(rotationYaw * ((float)Math.PI / 180F)) * forward));
        }
    }

    /**
     * Update the boat's speed, based on momentum.
     */
    private void updateMotion() {
        double d = 0.0f;
        if (!(this.getControllingPassenger() instanceof PlayerEntity)) {
            d = 0.2f * (isAboveWater() ? 0 : (isInWater() ? 1 : -1));
        } else if (isOnGround()) {
            d = -0.2f;
        }
        Vector3d motion = getMotion();
        double verticalMotion = motion.y * MOMENTUM + d;
        setMotion(preventSmallAbsValue(motion.x * MOMENTUM),
                  preventSmallAbsValue(verticalMotion),
                  preventSmallAbsValue(motion.z * MOMENTUM));
        mDeltaRotation *= MOMENTUM;
    }

    private void updateElectricity() {
        if (world.isRemote())
            return;
        if (dataManager.get(DATA_ELECTRICITY) < ELECTRICITY_CHARGE_WATER_LEVEL) {
            int remaining = mFuelInventory.deductFuel(FUEL_CONVERT_BATCH_COUNT);
            int consumed = FUEL_CONVERT_BATCH_COUNT - remaining;
            if (consumed <= 0)
                return;
            dataManager.set(DATA_ELECTRICITY, dataManager.get(DATA_ELECTRICITY) + consumed * FUEL_ELECTRICITY_CONVERT_RATE);
        }
    }

//    @Override
//    public void notifyDataManagerChange(DataParameter<?> inKey) {
//        super.notifyDataManagerChange(inKey);
//        if (DATA_ELECTRICITY.equals(inKey)) {
//            if (world.isRemote) {
//                dataManager.setClean();
//            }
//        }
//    }

    @Override
    protected void addPassenger(Entity inPassenger) {
        super.addPassenger(inPassenger);
        if (this.canPassengerSteer() && this.mLerpSteps > 0) {
            this.mLerpSteps = 0;
            this.setPositionAndRotation(this.mLerpX, this.mLerpY, this.mLerpZ, (float)this.mLerpYaw, 0.0f);
        }
        mPassengerManager.addPassenger(inPassenger);
    }

    @Override
    protected void removePassenger(Entity inPassenger) {
        super.removePassenger(inPassenger);
        mPassengerManager.removePassenger(inPassenger);
    }

    @Override
    protected boolean canFitPassenger(Entity inPassenger) {
        if (inPassenger.getRidingEntity() == this)
            return false;
        return mPassengerManager.canFitPassenger(inPassenger);
    }

    @Override
    public void updatePassenger(Entity inPassenger) {
        if (this.isPassenger(inPassenger)) {
            if (isInWater())
                inPassenger.setAir(inPassenger.getMaxAir());

            Vector3f offset = mPassengerManager.getPassengerMountOffset(inPassenger);
            offset.setY((float)(offset.getY() + inPassenger.getYOffset()));

            Vector3d rotatedOffset = (new Vector3d(offset.getX(), offset.getY(), offset.getZ())).rotateYaw(-this.rotationYaw * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
            inPassenger.setPosition(this.getPosX() + rotatedOffset.x,
                                  this.getPosY() + rotatedOffset.y,
                                  this.getPosZ() + rotatedOffset.z);
            inPassenger.rotationYaw += this.mDeltaRotation;
            inPassenger.setRotationYawHead(inPassenger.getRotationYawHead() + this.mDeltaRotation);
            this.applyYawToEntity(inPassenger);
            if (inPassenger instanceof AnimalEntity && this.getPassengers().size() > 1) {
                int j = inPassenger.getEntityId() % 2 == 0 ? 90 : 270;
                inPassenger.setRenderYawOffset(((AnimalEntity)inPassenger).renderYawOffset + (float)j);
                inPassenger.setRotationYawHead(inPassenger.getRotationYawHead() + (float)j);
            }
        }
    }

    /**
     * Applies this boat's yaw to the given entity. Used to update the orientation of its passenger.
     */
    protected void applyYawToEntity(Entity entityToUpdate) {
        entityToUpdate.setRenderYawOffset(this.rotationYaw);
        float f = MathHelper.wrapDegrees(entityToUpdate.rotationYaw - this.rotationYaw);
        float f1 = MathHelper.clamp(f, -105.0F, 105.0F);
        entityToUpdate.prevRotationYaw += f1 - f;
        entityToUpdate.rotationYaw += f1 - f;
        entityToUpdate.setRotationYawHead(entityToUpdate.rotationYaw);
    }

    @Override
    public void applyEntityCollision(Entity entityIn) {
        if (entityIn instanceof FbEntity) {
            if (entityIn.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.applyEntityCollision(entityIn);
            }
        } else if (entityIn.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.applyEntityCollision(entityIn);
        }
    }

    /**
     * Applies this entity's orientation (pitch/yaw) to another entity. Used to update passenger orientation.
     */
    @OnlyIn(Dist.CLIENT)
    public void applyOrientationToEntity(Entity inEntityToUpdate) {
        this.applyYawToEntity(inEntityToUpdate);
    }

    @Override
    public void setPositionAndRotation(double inX, double inY, double inZ, float inYaw, float inPitch) {
        float prevYaw = rotationYaw;
        super.setPositionAndRotation(inX, inY, inZ, inYaw, 0);
        if (world.isRemote)
            return;
        Vector3d curPos = getPositionVec();
        float yawDiff = MathHelper.degreesDifferenceAbs(prevYaw, rotationYaw);
        double totalDiff = MathHelper.sqrt(curPos.squareDistanceTo(lastTickPosX, lastTickPosY, lastTickPosZ)) + yawDiff / 5;
        if (!isSmallAbsValue(totalDiff)) {
            //logger.info("electricity = {}", dataManager.get(DATA_ELECTRICITY));
            float electricity = dataManager.get(DATA_ELECTRICITY);
            float deduct = (float)(getPassengers().size() * totalDiff);
            if (electricity <= deduct)
                dataManager.set(DATA_ELECTRICITY, 0.0f);
            else
                dataManager.set(DATA_ELECTRICITY, electricity - deduct);
        }
    }

    /**
     * Sets a target for the client to interpolate towards over the next few ticks
     */
    @OnlyIn(Dist.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        mLerpX = x;
        mLerpY = y;
        mLerpZ = z;
        mLerpYaw = yaw;
        mLerpSteps = 10;
    }

    @Override
    public double getMountedYOffset() {
        return 0.5D;
    }

    public void initFromItemStack(ItemStack inItemStack) {
        CompoundNBT entityTag = inItemStack.getChildTag(NBT_KEY_ENTITY_TAG);
        if (entityTag != null) {
            readAdditional(entityTag);
        }
    }

    public void setSinking(boolean inIsSinking) {
        mSinking = inIsSinking;
    }

    @Override
    protected void readAdditional(CompoundNBT inNbt) {
        dataManager.set(DATA_ELECTRICITY, inNbt.getFloat(NBT_KEY_ELECTRICITY));
        CompoundNBT fuelInventoryNbt = inNbt.getCompound(NBT_KEY_FUEL_INVENTORY);
        mFuelInventory.deserializeNBT(fuelInventoryNbt);
        CompoundNBT inventoriesNbt = inNbt.getCompound(NBT_KEY_INVENTORIES);
        int inventoryCount = inventoriesNbt.getInt(NBT_KEY_INVENTORY_SIZE);
        if (INVENTORY_COUNT != inventoryCount)
            logger.error("Inventory count doesn't match: got {}, expected {}", inventoryCount, INVENTORY_COUNT);
        int countToRead = Math.min(INVENTORY_COUNT, inventoryCount);
        for (int i = 0; i < countToRead; ++i) {
            String inventoryKey = String.format(NBT_KEY_INVENTORY_PATTERN, i);
            CompoundNBT inventoryNbt = inventoriesNbt.getCompound(inventoryKey);
            mInventories[i].deserializeNBT(inventoryNbt);
        }
    }

    @Override
    protected void writeAdditional(CompoundNBT inNbt) {
        CompoundNBT fuelInventoryNbt = mFuelInventory.serializeNBT();
        CompoundNBT inventoriesNbt = new CompoundNBT();
        inventoriesNbt.putInt(NBT_KEY_INVENTORY_SIZE, mInventories.length);
        for (int i = 0; i < mInventories.length; ++i) {
            String inventoryKey = String.format(NBT_KEY_INVENTORY_PATTERN, i);
            CompoundNBT inventoryNbt = mInventories[i].serializeNBT();
            inventoriesNbt.put(inventoryKey, inventoryNbt);
        }
        inNbt.putFloat(NBT_KEY_ELECTRICITY, dataManager.get(DATA_ELECTRICITY));
        inNbt.put(NBT_KEY_FUEL_INVENTORY, fuelInventoryNbt);
        inNbt.put(NBT_KEY_INVENTORIES, inventoriesNbt);
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public ActionResultType processInitialInteract(PlayerEntity player, Hand hand) {
        if (player.isSecondaryUseActive()) {
            if (!this.world.isRemote) {
                int inventoryIndex = mIntentoryIndex++ % INVENTORY_COUNT;
                logger.info("Inventory index {}", inventoryIndex);
                player.openContainer(mFuelInventory);
                return ActionResultType.SUCCESS;
            } else {
                return ActionResultType.SUCCESS;
            }
        } else {
            if (!this.world.isRemote) {
                return player.startRiding(this) ? ActionResultType.CONSUME : ActionResultType.FAIL;
            } else {
                return player.getLowestRidingEntity() == this.getLowestRidingEntity() ? ActionResultType.FAIL : ActionResultType.SUCCESS;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean attackEntityFrom(DamageSource inSource, float inAmount) {
        if (isInvulnerableTo(inSource))
            return false;
        if (this.world.isRemote || this.removed)
            return false;
        if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            dropItem();
        }
        this.remove();
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource inSource) {
        Entity sourceEntity = inSource.getTrueSource();
        if (!(sourceEntity instanceof PlayerEntity))
            return true;
        PlayerEntity player = (PlayerEntity)sourceEntity;
        if (player.isRidingSameEntity(this)) {
            return true;
        }
        if (player.getHeldItemMainhand().getItem().getRegistryName().equals(FbItems.SHRINK_RAY.getId()))
            return false;
        return true;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean func_241845_aY() {
        return true;
    }

    @Override
    protected boolean canBeRidden(Entity entityIn) {
        return true;
    }

    @Override
    public boolean canBeRiddenInWater(Entity rider) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean onLivingFall(float distance, float damageMultiplier) {
        return false;
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    // Get dismount drop point
    @Override
    public Vector3d func_230268_c_(LivingEntity livingEntity) {
        AxisAlignedBB bb = this.getBoundingBox();
        return new Vector3d(bb.maxX + 1, bb.minY + 1, livingEntity.getPosZ());
    }

    @SuppressWarnings("rawtypes")
    public ItemStack getItemStack() {
        ItemStack itemStack = getItem().getDefaultInstance();

        return itemStack;
    }

    public boolean isSafeToDismount() {
        return isOnGround() || isAboveWater();
    }

    protected void dropItem() {
        ItemStack itemStack = getItemStack();
        CompoundNBT entityNbt = new CompoundNBT();
        writeAdditional(entityNbt);
        itemStack.setTagInfo(NBT_KEY_ENTITY_TAG, entityNbt);
        entityDropItem(itemStack);
    }

    protected Item getItem() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(FbMod.MODID, FbItem.NAME));
    }

    private boolean isAboveWater() {
        Vector3d pos = getPositionVec();
        boolean aboveWater = world.getBlockState(new BlockPos(pos.add(0, 0.0, 0))).getBlock() == Blocks.WATER
                && world.getBlockState(new BlockPos(pos.add(0, 1.5, 0))).getBlock() == Blocks.AIR;
        return aboveWater;
    }

    private double preventSmallAbsValue(double inValue) {
        if (Math.abs(inValue) <= 1e-6)
            return 0.0;
        return inValue;
    }

    private boolean isSmallAbsValue(double inValue) {
        return Math.abs(inValue) <= 1e-6;
    }

    private static class PassengerManager {
        private static final int PASSENGER_PLAYER_CAPACITY = 4;
        private static final int PASSENGER_OTHER_CAPACITY = 4;
        private static final Vector3f[] PASSENGER_PLAYER_MOUNT_OFFSETS = {
                new Vector3f(1.0f, 0.8f, 0.0f),
                new Vector3f(0.0f, 0.8f, -1.0f),
                new Vector3f(0.0f, 0.8f, 1.0f),
                new Vector3f(-1.0f, 0.8f, 0.0f)
        };
        private static final Vector3f[] PASSENGER_OTHER_MOUNT_OFFSETS = {
                new Vector3f(1.5f, 2.8f, -1.5f),
                new Vector3f(1.5f, 2.8f, 1.5f),
                new Vector3f(-1.5f, 2.8f, -1.5f),
                new Vector3f(-1.5f, 2.8f, 1.5f)
        };
        private static final Vector3f DEFAULT_MOUNT_OFFSET =
                new Vector3f(0.0f, 5f, 0.0f);

        private int mTicksLeftToSync;

        private static class Compound {
            final int capacity;
            final List<Entity> passengers;
            final Map<Entity, Integer> passengerIndices;
            final Vector3f[] mountOffsets;

            Compound(int inCapacity,
                     Vector3f[] inMountOffsets) {
                capacity = inCapacity;
                mountOffsets = inMountOffsets;
                passengers = new ArrayList<>();
                passengerIndices = new HashMap<>();
            }
        }

        private Compound mPlayerCompound;
        private Compound mOtherCompound;

        PassengerManager() {
            mTicksLeftToSync = 0;
            mPlayerCompound = new Compound(PASSENGER_PLAYER_CAPACITY,
                                           PASSENGER_PLAYER_MOUNT_OFFSETS);
            mOtherCompound = new Compound(PASSENGER_OTHER_CAPACITY,
                                          PASSENGER_OTHER_MOUNT_OFFSETS);
        }

        private Compound getCompoundForEntity(Entity inPassenger) {
            if (inPassenger instanceof PlayerEntity)
                return mPlayerCompound;
            else
                return mOtherCompound;
        }

        boolean canFitPassenger(Entity inPassenger) {
            if (inPassenger.world.isRemote) {
                logger.error("PassengerManager.canFitPassenger is called on client");
                return false;
            }
            Compound compound = getCompoundForEntity(inPassenger);
            return compound.passengers.size() < compound.capacity;
        }

        void addPassenger(Entity inPassenger) {
            Compound compound = getCompoundForEntity(inPassenger);
            if (compound.passengers.size() >= compound.capacity) {
                logger.error("PassengerManager.addPassenger: Out of sync");
                mTicksLeftToSync = 0;
                return;
            }
            compound.passengers.add(inPassenger);
            compound.passengerIndices.put(inPassenger, compound.passengers.size() - 1);
        }

        void removePassenger(Entity inPassenger) {
            Compound compound = getCompoundForEntity(inPassenger);
            compound.passengers.remove(inPassenger);
            compound.passengerIndices.remove(inPassenger);
        }

        Vector3f getPassengerMountOffset(Entity inPassenger) {
            Compound compound = getCompoundForEntity(inPassenger);
            Integer index = compound.passengerIndices.get(inPassenger);
            if (index == null) {
                logger.error("PassengerManager.getPassengerMountOffset: Cannot find passenger");
                mTicksLeftToSync = 0;
                return DEFAULT_MOUNT_OFFSET;
            }
            if (compound.mountOffsets.length <= index) {
                logger.error("PassengerManager.getPassengerMountOffset: Passenger index out of bound");
                mTicksLeftToSync = 0;
                return DEFAULT_MOUNT_OFFSET;
            }
            Vector3f offsetVec = compound.mountOffsets[index];
            return new Vector3f(offsetVec.getX(), offsetVec.getY(), offsetVec.getZ());
        }

        void tick(List<Entity> inPassengers) {
            if (mTicksLeftToSync > 0) {
                --mTicksLeftToSync;
                return;
            }
            mTicksLeftToSync = 20;
            Compound playerCompound = new Compound(PASSENGER_PLAYER_CAPACITY, PASSENGER_PLAYER_MOUNT_OFFSETS);
            Compound otherCompound = new Compound(PASSENGER_OTHER_CAPACITY, PASSENGER_OTHER_MOUNT_OFFSETS);

            for (Entity passenger: inPassengers) {
                Compound compound = null;
                if (passenger instanceof PlayerEntity)
                    compound = playerCompound;
                else
                    compound = otherCompound;
                compound.passengers.add(passenger);
                compound.passengerIndices.put(passenger, compound.passengers.size() - 1);
            }

            mPlayerCompound = playerCompound;
            mOtherCompound = otherCompound;
        }
    }
}
