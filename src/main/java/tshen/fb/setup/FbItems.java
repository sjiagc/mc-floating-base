package tshen.fb.setup;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import tshen.fb.FbMod;
import tshen.fb.entity.FbEntity;
import tshen.fb.item.FbItem;

@Mod.EventBusSubscriber(modid = FbMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FbItems {
    public static final String ITEM_GROUP_NAME = "fb";
    public static final String ITEM_NAME_FUEL = "fuel";
    public static final String ITEM_NAME_SHRINK_RAY = "shrink_ray";

    public static final ItemGroup FLOATING_BASE_ITEM_GROUP;

    public static final DeferredRegister<Item> ITEMS;

    public static final RegistryObject<Item> FB;
    public static final RegistryObject<Item> FUEL;
    public static final RegistryObject<Item> SHRINK_RAY;

    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FbMod.MODID);

        FLOATING_BASE_ITEM_GROUP = new ItemGroup(ITEM_GROUP_NAME) {
            @Override
            public ItemStack createIcon() {
                return new ItemStack(FB.get());
            }
        };

        FB = ITEMS.register(FbItem.NAME,
                            () -> new FbItem(new Item.Properties().group(FLOATING_BASE_ITEM_GROUP),
                                             world -> new FbEntity(FbEntities.BASE.get(),
                                                                   world)));
        FUEL = ITEMS.register(ITEM_NAME_FUEL, () -> new Item(new Item.Properties().group(FLOATING_BASE_ITEM_GROUP)));
        SHRINK_RAY = ITEMS.register(ITEM_NAME_SHRINK_RAY, () -> new Item(new Item.Properties().group(FLOATING_BASE_ITEM_GROUP)));
    }

    public static void init() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

}
