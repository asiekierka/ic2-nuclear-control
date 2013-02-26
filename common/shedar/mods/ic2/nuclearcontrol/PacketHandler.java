package shedar.mods.ic2.nuclearcontrol;

import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler
{
    public static final int PACKET_ALARM = 1;
    public static final int PACKET_SENSOR = 2;
    public static final int PACKET_SENSOR_TITLE = 3;
    public static final int PACKET_CHAT = 4;
    public static final int PACKET_ECOUNTER = 5;
    public static final int PACKET_ACOUNTER = 6;
    
    public static final int PACKET_CLIENT_SOUND = 7;
    public static final int PACKET_CLIENT_REQUEST = 8;
    public static final int PACKET_CLIENT_RANGE_TRIGGER = 11;
    public static final int PACKET_CLIENT_SENSOR = 12;
    public static final int PACKET_CLIENT_COLOR = 13;
    public static final int PACKET_CLIENT_DISPLAY_SETTINGS = 14;
    
    public static final int PACKET_DISP_SETTINGS_ALL = 9;
    public static final int PACKET_DISP_SETTINGS_UPDATE = 10;
    
    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
    {
       IC2NuclearControl.proxy.onPacketData(manager, packet, player);
            
    }

}
