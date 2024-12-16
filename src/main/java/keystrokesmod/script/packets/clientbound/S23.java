package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Block;
import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S23PacketBlockChange;

public class S23 extends SPacket {
    public Vec3 position;
    public Block block;

    public S23(S23PacketBlockChange packet) {
        super(packet);
        this.position = Vec3.convert(packet.getBlockPosition());
        this.block = new Block(packet.getBlockState().getBlock());
    }
}
