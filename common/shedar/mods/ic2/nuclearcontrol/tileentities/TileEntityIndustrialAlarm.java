package shedar.mods.ic2.nuclearcontrol.tileentities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import shedar.mods.ic2.nuclearcontrol.IC2NuclearControl;
import shedar.mods.ic2.nuclearcontrol.ITextureHelper;
import shedar.mods.ic2.nuclearcontrol.subblocks.IndustrialAlarm;
import shedar.mods.ic2.nuclearcontrol.utils.Damages;

public class TileEntityIndustrialAlarm extends TileEntityHowlerAlarm implements ITextureHelper
{
    private static final byte[] lightSteps = {0, 7, 15, 7, 0};

    protected byte internalFire;
    public byte lightLevel;

    public TileEntityIndustrialAlarm()
    {
        super();
        internalFire = 0;
        lightLevel = 0;
    }

    @Override
    protected void checkStatus()
    {
        super.checkStatus();
        int light = lightLevel;
        if(!powered){
            lightLevel = 0;
            internalFire = 0;
        }
        else
        {
            internalFire = (byte)((internalFire + 1) % lightSteps.length*2);
            lightLevel = lightSteps[internalFire/2];
        }
        if(lightLevel!=light)
            worldObj.updateAllLightTypes(xCoord, yCoord, zCoord);
    }

    @Override
    public int modifyTextureIndex(int texture)
    {
        if(texture == IndustrialAlarm.I_BACK)
            return texture;
        switch(lightLevel)
        {
            case 7:
                texture +=1;
                break;
            case 15: 
                texture += 2;
                break;
        }
        return texture;
    }
    
    @Override
    public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
    {
        return new ItemStack(IC2NuclearControl.instance.blockNuclearControlMain.blockID, 1, Damages.DAMAGE_INDUSTRIAL_ALARM);
    }
}
