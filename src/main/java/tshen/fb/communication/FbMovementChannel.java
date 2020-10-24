package tshen.fb.communication;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import tshen.fb.FbMod;
import tshen.fb.entity.FbEntity;

import java.util.function.Supplier;

public class FbMovementChannel {
    private static final String PROTOCOL_VERSION = "1";
    private static final String CHANNEL = "movement";

    private static final int MSG_RISE = 1;
    private static final int MSG_SINK = 1;

    private final SimpleChannel mChannel;

    FbMovementChannel() {
        mChannel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(FbMod.MODID, CHANNEL),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        // MSG_RISE
        mChannel.registerMessage(
                MSG_RISE, // index
                MsgRise.class, // messageType
                (msg, buff) -> MsgRise.SERIALIZER.write(buff, msg), // encoder
                MsgRise.SERIALIZER::read, // decoder
                FbMovementChannel::handleRise // messageConsumer
        );
    }

    private static class MsgRise {
        boolean isRising;

        MsgRise(boolean inIsRising) {
            isRising = inIsRising;
        }

        public static final IDataSerializer<MsgRise> SERIALIZER = new IDataSerializer<MsgRise>() {
            @Override
            public void write(PacketBuffer buf, MsgRise inMsg) {
                buf.writeBoolean(inMsg.isRising);
            }

            @Override
            public MsgRise read(PacketBuffer buf) {
                try {
                    return new MsgRise(buf.readBoolean());
                } catch (IndexOutOfBoundsException e) {
                    throw new RuntimeException("Read MsgRise failed: ", e);
                }
            }

            @Override
            public MsgRise copyValue(MsgRise inMsg) {
                return new MsgRise(inMsg.isRising);
            }
        };
    }

    public void sendRise(boolean inIsRising) {
        mChannel.sendToServer(new MsgRise(inIsRising));
    }

    private static void handleRise(MsgRise inMsg, Supplier<NetworkEvent.Context> inContext) {
        inContext.get().enqueueWork(() -> {
            // Work that needs to be threadsafe (most work)
            PlayerEntity svrPlayerEntity = inContext.get().getSender(); // the client that sent this packet
            if (svrPlayerEntity != null && svrPlayerEntity.getRidingEntity() instanceof FbEntity) {
                FbEntity fbEntity = (FbEntity) svrPlayerEntity.getRidingEntity();
                if (fbEntity.getControllingPassenger() == svrPlayerEntity)
                    fbEntity.setSprinting(inMsg.isRising);
            }
        });
        inContext.get().setPacketHandled(true);
    }
}
