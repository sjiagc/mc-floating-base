package tshen.fb.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.INBTSerializable;
import tshen.fb.FbMod;

import javax.annotation.Nullable;

public class FbInventory implements INamedContainerProvider, INBTSerializable<CompoundNBT> {

    private int mRowCount;
    private Inventory mInventory;

    FbInventory() {
        mRowCount = 6;
        mInventory = new Inventory(9 * mRowCount);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("ui.floatingbase.fb_chest");
    }

    @Nullable
    @Override
    public Container createMenu(int inId, PlayerInventory inPlayerInventory, PlayerEntity inPlayerEntity) {
        return new ChestContainer(ContainerType.GENERIC_9X6, inId, inPlayerInventory, mInventory, mRowCount);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        ListNBT listNbt = mInventory.write();
        nbt.putInt("rowCount", mRowCount);
        nbt.put("items", listNbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT inNbt) {
        ListNBT listNbt = inNbt.getList("items", 10);
        mRowCount = inNbt.getInt("rowCount");
        mInventory.read(listNbt);
    }

    protected Inventory getInventory() {
        return mInventory;
    }
}
