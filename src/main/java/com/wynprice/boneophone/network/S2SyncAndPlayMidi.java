package com.wynprice.boneophone.network;

import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class S2SyncAndPlayMidi implements IMessage {

    private int entityID;
    private byte[] abyteIn;
    private MidiStream midi;

    @SuppressWarnings("unused")
    public S2SyncAndPlayMidi() {
    }

    public S2SyncAndPlayMidi(int entityID, byte[] abyteIn) {
        this.entityID = entityID;
        this.abyteIn = abyteIn;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        MidiFileHandler.writeBytes(this.abyteIn, buf);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();
        this.midi = MidiFileHandler.readMidiFile(MidiFileHandler.readBytes(buf));
    }

    public static class Handler extends WorldModificationsMessageHandler<S2SyncAndPlayMidi, IMessage> {

        @Override
        protected void handleMessage(S2SyncAndPlayMidi message, MessageContext ctx, World world, EntityPlayer player) {
            Entity entity = world.getEntityByID(message.entityID);
            if(entity instanceof MusicalSkeleton) {
                ((MusicalSkeleton) entity).currentlyPlaying = message.midi;
                ((MusicalSkeleton) entity).playingTicks = 0;
            }
        }
    }

}
