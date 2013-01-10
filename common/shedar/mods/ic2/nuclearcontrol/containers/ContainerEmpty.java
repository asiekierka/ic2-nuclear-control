package shedar.mods.ic2.nuclearcontrol.containers;

import shedar.mods.ic2.nuclearcontrol.IC2NuclearControl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

public class ContainerEmpty extends Container
{
    private TileEntity entity;
    
    public ContainerEmpty(TileEntity entity)
    {
        super();
        this.entity = entity;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return entity.worldObj.getBlockId(entity.xCoord, entity.yCoord, entity.zCoord) != IC2NuclearControl.instance.blockNuclearControlMain.blockID ? 
                false : player.getDistanceSq(entity.xCoord + 0.5D, entity.yCoord + 0.5D, entity.zCoord + 0.5D) <= 64.0D;
    }
}
