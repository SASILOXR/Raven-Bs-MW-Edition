package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S29PacketSoundEffect;

public class S29 extends SPacket {
    public String sound;
    public Vec3 position;
    public float volume;
    public float pitch;

    public S29(S29PacketSoundEffect packet) {
        super(packet);
        this.sound = packet.getSoundName();
        this.position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        this.volume = packet.getVolume();
        this.pitch = packet.getPitch();
    }
}
