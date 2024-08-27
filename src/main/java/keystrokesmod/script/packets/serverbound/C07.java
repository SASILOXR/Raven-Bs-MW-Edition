package keystrokesmod.script.packets.serverbound;

import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class C07 extends CPacket {
    public Vec3 pos;
    public String status;
    public String facing;

    public C07(Vec3 pos, String status, String facing) {
        super(null);
        this.pos = pos;
        this.status = status;
        this.facing = facing;
    }

    protected C07(C07PacketPlayerDigging packet) {
        super(packet);
        this.pos = Vec3.convert(packet.getPosition());
        this.status = packet.getStatus().name();
        this.facing = packet.getFacing().name();
    }

    @Override
    public C07PacketPlayerDigging convert() {
        return new C07PacketPlayerDigging(Utils.getEnum(C07PacketPlayerDigging.Action.class, this.status), new BlockPos(this.pos.x, this.pos.y, this.pos.z), Utils.getEnum(EnumFacing.class, this.facing));
    }
}
