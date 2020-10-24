package tshen.fb.entity;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MinecartItem;
import net.minecraft.util.ResourceLocation;
import tshen.fb.setup.FbItems;

public class FbFuelInventory extends FbInventory {

    private static final ResourceLocation FUEL_ID = FbItems.FUEL.getId();

    public FbFuelInventory() {
        super();
    }

    boolean hasFuel() {
        Inventory inventory = getInventory();
        int size = inventory.getSizeInventory();
        for (int i = 0; i < size; ++i) {
            ItemStack itemStack = inventory.getStackInSlot(i);
            Item item = itemStack.getItem();
            if (item.getRegistryName().equals(FUEL_ID) && itemStack.getCount() > 0)
                return true;
        }
        return false;
    }

    /**
     *
     * @param inAmount
     * @return Amount remains after deducted all available
     */
    int deductFuel(int inAmount) {
        Inventory inventory = getInventory();
        int size = inventory.getSizeInventory();
        for (int i = 0; inAmount > 0 && i < size; ++i) {
            ItemStack itemStack = inventory.getStackInSlot(i);
            Item item = itemStack.getItem();
            if (!item.getRegistryName().equals(FUEL_ID))
                continue;
            int count = itemStack.getCount();
            if (count <= inAmount) {
                itemStack.setCount(0);
                inAmount -= count;
            } else {
                itemStack.setCount(count - inAmount);
                inAmount = 0;
            }
        }
        return inAmount;
    }
}
