package tshen.fb.setup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import tshen.fb.FbConfig;
import tshen.fb.communication.FbChannelManager;
import tshen.fb.entity.FbEntity;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class FbKeyBindings {
    private static final String CATEGORY = "fb";

    @OnlyIn(Dist.CLIENT)
    public static List<KeyAction> mKeyActions;

    public static void init() {
        mKeyActions = new ArrayList<>();

        mKeyActions.add(new KeyAction(new KeyBinding("fb.key.rise",
                                                     KeyConflictContext.IN_GAME,
                                                     InputMappings.Type.KEYSYM,
                                                     GLFW.GLFW_KEY_SPACE,
                                                     CATEGORY),
                                      (isDown) -> FbChannelManager.getInstance().getMovementChannel().sendRise(isDown)));
        mKeyActions.add(new KeyAction(new KeyBinding("fb.key.sink",
                                                     KeyConflictContext.IN_GAME,
                                                     InputMappings.Type.KEYSYM,
                                                     GLFW.GLFW_KEY_C,
                                                     CATEGORY),
                                      new KeySinkHandler()));
        mKeyActions.add(new KeyAction(new KeyBinding("fb.key.immersive",
                                                     KeyConflictContext.IN_GAME,
                                                     KeyModifier.CONTROL,
                                                     InputMappings.Type.KEYSYM,
                                                     GLFW.GLFW_KEY_I,
                                                     CATEGORY),
                                      new KeyImmersiveHandler()));

        mKeyActions.forEach(e -> ClientRegistry.registerKeyBinding(e.getKeyBinding()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.phase == TickEvent.Phase.END || mc.currentScreen != null || mc.world == null)
            return;

        mKeyActions.forEach(keyAction -> {
            keyAction.run();
        });
    }

    private static final class KeyAction {
        interface KeyStateChangedHandler {
            void onChanged(boolean inIsDown);
        }

        private KeyBinding mKeyBinding;
        private boolean mIsKeyDown;
        private KeyStateChangedHandler mOnKeyStateChangedHandler;

        KeyAction(KeyBinding inKeyBinding, KeyStateChangedHandler inOnKeyStateChangeHandler) {
            mKeyBinding = inKeyBinding;
            mIsKeyDown = false;
            mOnKeyStateChangedHandler = inOnKeyStateChangeHandler;
        }

        KeyBinding getKeyBinding() {
            return mKeyBinding;
        }

        void run() {
            boolean isKeyDown = mKeyBinding.isKeyDown();
            if (isKeyDown != mIsKeyDown) {
                mIsKeyDown = isKeyDown;
                mOnKeyStateChangedHandler.onChanged(mIsKeyDown);
            }
        }
    }

    private static final class KeySinkHandler implements KeyAction.KeyStateChangedHandler {
        @Override
        public void onChanged(boolean inIsDown) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null)
                return;
            Entity ridingEntity = player.getRidingEntity();
            if (ridingEntity == null || !(ridingEntity instanceof FbEntity))
                return;
            ((FbEntity) ridingEntity).setSinking(inIsDown);
        }
    }

    private static final class KeyImmersiveHandler implements KeyAction.KeyStateChangedHandler {
        @Override
        public void onChanged(boolean inIsDown) {
            if (!inIsDown)
                return;
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null)
                return;
            Entity ridingEntity = player.getRidingEntity();
            if (ridingEntity == null || !(ridingEntity instanceof FbEntity))
                return;
            FbConfig.getInstance().setImmersive(!FbConfig.getInstance().isImmersive());
        }
    }
}
