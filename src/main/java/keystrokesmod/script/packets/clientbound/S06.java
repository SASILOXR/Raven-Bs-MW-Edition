package keystrokesmod.script.packets.clientbound;

import net.minecraft.network.play.server.S06PacketUpdateHealth;

public class S06 extends SPacket {
    public float health;
    public float saturation;
    public int food;

    public S06(S06PacketUpdateHealth packet) {
        super(packet);
        this.health = packet.getHealth();
        this.saturation = packet.getSaturationLevel();
        this.food = packet.getFoodLevel();
    }

}
