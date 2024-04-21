package shedar.mods.ic2.nuclearcontrol.crossmod.buildcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.liquids.ITankContainer;
import shedar.mods.ic2.nuclearcontrol.crossmod.data.EnergyStorageData;
import shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityAverageCounter;
import shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityEnergyCounter;
import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.tools.IToolWrench;
import cpw.mods.fml.common.registry.GameRegistry;

public class CrossBuildcraft
{
    private boolean _isApiAvailable = false;
    
    public boolean isApiAvailable()
    {
        return _isApiAvailable;
    }
    
    @SuppressWarnings("unchecked")
    public void registerTileEntity()
    {
        try
        {
            GameRegistry.registerTileEntity((Class<? extends TileEntity>)Class.forName("shedar.mods.ic2.nuclearcontrol.crossmod.buildcraft.TileEntityAverageCounterBC"), "IC2NCAverageCounterBC");
            GameRegistry.registerTileEntity((Class<? extends TileEntity>)Class.forName("shedar.mods.ic2.nuclearcontrol.crossmod.buildcraft.TileEntityEnergyCounterBC"), "IC2NCEnergyCounterBC");
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    
    public CrossBuildcraft()
    {
        try
        {
            Class.forName("buildcraft.api.power.IPowerReceptor", false, this.getClass().getClassLoader());
            _isApiAvailable = true;
            registerTileEntity();
        } catch (ClassNotFoundException e)
        {
            _isApiAvailable = false;
        }
    }
    
    public void useWrench(ItemStack itemStack, TileEntity target, EntityPlayer player)
    {
        if(_isApiAvailable)
        {
            ((IToolWrench) itemStack.getItem()).wrenchUsed(player, target.xCoord, target.yCoord, target.zCoord);
        }
    }
    
    public boolean isWrench(ItemStack itemStack, TileEntity target, EntityPlayer player)
    {
        return _isApiAvailable && 
                itemStack.getItem() instanceof IToolWrench && 
                ((IToolWrench)itemStack.getItem()).canWrench(player, target.xCoord, target.yCoord, target.zCoord);
    }
    
    public boolean isTankContainer(Object obj)
    {
        return _isApiAvailable && obj instanceof ITankContainer;
    }
    
    public TileEntityAverageCounter getAverageCounter()
    {
        if(_isApiAvailable)
        {
            try
            {
                return (TileEntityAverageCounter)Class.forName("shedar.mods.ic2.nuclearcontrol.crossmod.buildcraft.TileEntityAverageCounterBC").newInstance();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public TileEntityEnergyCounter getEnergyCounter()
    {
        if(_isApiAvailable)
        {
            try
            {
                return (TileEntityEnergyCounter)Class.forName("shedar.mods.ic2.nuclearcontrol.crossmod.buildcraft.TileEntityEnergyCounterBC").newInstance();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public EnergyStorageData getStorageData(TileEntity target)
    {
        if(!_isApiAvailable || target == null)
            return null;
        IPowerProvider provider = null;
        if(target instanceof IPowerProvider)
        {
            provider = (IPowerProvider)provider;
        }
        else if (target instanceof IPowerReceptor)
        {
            provider = ((IPowerReceptor)target).getPowerProvider();
        }
        if(provider == null)
            return null;
        EnergyStorageData result = new EnergyStorageData();
        result.capacity = provider.getMaxEnergyStored();
        result.stored = (int)provider.getEnergyStored();
        result.units = "MJ";
        result.type = EnergyStorageData.TARGET_TYPE_BC;
        return result;
    }

}
