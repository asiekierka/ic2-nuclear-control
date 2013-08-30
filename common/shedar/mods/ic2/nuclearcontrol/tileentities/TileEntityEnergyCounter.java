package shedar.mods.ic2.nuclearcontrol.tileentities;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.item.Items;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.api.network.NetworkHelper;
import ic2.api.tile.IWrenchable;

import java.util.List;
import java.util.Vector;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import shedar.mods.ic2.nuclearcontrol.IC2NuclearControl;
import shedar.mods.ic2.nuclearcontrol.ISlotItemFilter;
import shedar.mods.ic2.nuclearcontrol.utils.Damages;
import cpw.mods.fml.common.FMLCommonHandler;


public class TileEntityEnergyCounter extends TileEntity implements 
    IEnergyConductor, IWrenchable,  INetworkClientTileEntityEventListener,
    IInventory, ISlotItemFilter, INetworkDataProvider, INetworkUpdateListener 
{
    private static final int BASE_PACKET_SIZE = 32; 
    
    private boolean init;
    private ItemStack inventory[];

    protected int updateTicker;
    protected int tickRate;

    public long counter;
    private long prevTotal;
    
    private short prevFacing;
    public short facing;
    
    //0 - EU, 1- MJ
    private byte prevPowerType;
    public byte powerType;

    public int packetSize;

    private boolean addedToEnergyNet;

    public TileEntityEnergyCounter()
    {
        super();
        inventory = new ItemStack[1];//transformers upgrade
        addedToEnergyNet = false;
        packetSize = BASE_PACKET_SIZE;
        prevFacing = facing = 0;
        counter = 0;
        tickRate = IC2NuclearControl.instance.screenRefreshPeriod;
        updateTicker = tickRate;
        prevTotal = -1;
    }

    protected void initData()
    {
        init = true;
    }
    
    public void setPowerType(byte p)
    {
        powerType = p;

        if (prevPowerType != p)
        {
            NetworkHelper.updateTileEntityField(this, "powerType");
        }

        prevPowerType = p;
    }
    
    @Override
    public short getFacing()
    {
        return (short)Facing.oppositeSide[facing];
    }
    
    @Override
    public void setFacing(short f)
    {
        if (addedToEnergyNet)
        {
            EnergyTileUnloadEvent event = new EnergyTileUnloadEvent(this);
            MinecraftForge.EVENT_BUS.post(event);
        }
        setSide((short)Facing.oppositeSide[f]);
        addedToEnergyNet = false;
        if(FMLCommonHandler.instance().getEffectiveSide().isServer())
        {
            EnergyTileLoadEvent event = new EnergyTileLoadEvent(this);
            MinecraftForge.EVENT_BUS.post(event);
            addedToEnergyNet = true;
        }    }
    
    private void setSide(short f)
    {
        facing = f;

        if (prevFacing != f)
        {
            NetworkHelper.updateTileEntityField(this, "facing");
        }

        prevFacing = f;
    }
    
    @Override
    public void updateEntity()
    {
        if (!init)
        {
            initData();
            onInventoryChanged();
        }
        if (!worldObj.isRemote)
        {
            if (!addedToEnergyNet)
            {
                EnergyTileLoadEvent event = new EnergyTileLoadEvent(this);
                MinecraftForge.EVENT_BUS.post(event);
                prevTotal = EnergyNet.instance.getTotalEnergyEmitted(this);
                addedToEnergyNet = true;
            }
            if(updateTicker-- == 0)
            {
                updateTicker = tickRate-1;
                long total = EnergyNet.instance.getTotalEnergyEmitted(this);
                if(total > 0)
                {
                    if(prevTotal!=-1)
                    {
                        total = total - prevTotal;
                        prevTotal += total;
                    }
                    else
                    {
                        prevTotal = total;
                    }
                    if(total>0)
                        counter += total;
                    setPowerType(TileEntityAverageCounter.POWER_TYPE_EU);
                    
                }
            }
        }
        super.updateEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readFromNBT(nbttagcompound);
        facing = nbttagcompound.getShort("facing");
        counter = nbttagcompound.getLong("counter");
        powerType = nbttagcompound.getByte("powerType");
        
        NBTTagList nbttaglist = nbttagcompound.getTagList("Items");
        inventory = new ItemStack[getSizeInventory()];
        for (int i = 0; i < nbttaglist.tagCount(); i++)
        {
            NBTTagCompound compound = (NBTTagCompound)nbttaglist.tagAt(i);
            byte slotNum = compound.getByte("Slot");

            if (slotNum >= 0 && slotNum < inventory.length)
            {
                inventory[slotNum] = ItemStack.loadItemStackFromNBT(compound);
            }
        }
        onInventoryChanged();
    }
    
    @Override
    public void onNetworkUpdate(String field)
    {
        if (field.equals("facing") && prevFacing != facing)
        {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            prevFacing = facing;
        }
    }    

    @Override
    public void invalidate()
    {
        if (!worldObj.isRemote && addedToEnergyNet)
        {
            EnergyTileUnloadEvent event = new EnergyTileUnloadEvent(this);
            MinecraftForge.EVENT_BUS.post(event);
            addedToEnergyNet = false;
        }

        super.invalidate();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound)
    {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setShort("facing", facing);
        nbttagcompound.setLong("counter", counter);
        nbttagcompound.setByte("powerType", powerType);
        
        NBTTagList nbttaglist = new NBTTagList();
        for (int i = 0; i < inventory.length; i++)
        {
            if (inventory[i] != null)
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setByte("Slot", (byte)i);
                inventory[i].writeToNBT(compound);
                nbttaglist.appendTag(compound);
            }
        }
        nbttagcompound.setTag("Items", nbttaglist);
    }

    @Override
    public int getSizeInventory()
    {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slotNum)
    {
        return inventory[slotNum];
    }

    @Override
    public ItemStack decrStackSize(int slotNum, int amount)
    {
        if(inventory[slotNum]!=null)
        {
            if (inventory[slotNum].stackSize <= amount)
            {
                ItemStack itemStack = inventory[slotNum];
                inventory[slotNum] = null;
                onInventoryChanged();
                return itemStack;
            }
            
            ItemStack taken = inventory[slotNum].splitStack(amount);
            if (inventory[slotNum].stackSize == 0)
            {
                inventory[slotNum] = null;
            }
            onInventoryChanged();
            return taken;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int var1)
    {
        return null;
    }
    
    @Override
    public void setInventorySlotContents(int slotNum, ItemStack itemStack)
    {
        inventory[slotNum] = itemStack;

        if (itemStack != null && itemStack.stackSize > getInventoryStackLimit())
        {
            itemStack.stackSize = getInventoryStackLimit();
        }
        onInventoryChanged();
    }
    
    @Override
    public String getInvName()
    {
        return "block.RemoteThermo";
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player)
    {
        return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
                player.getDistanceSq((double)xCoord + 0.5D, (double)yCoord + 0.5D, (double)zCoord + 0.5D) <= 64D;
    }

    @Override
    public void openChest()
    {
    }

    @Override
    public void closeChest()
    {
    }
    
    @Override
    public void onInventoryChanged() 
    {
        super.onInventoryChanged();
        int upgradeCountTransormer = 0;
        ItemStack itemStack = inventory[0];
        if (itemStack!=null && itemStack.isItemEqual(Items.getItem("transformerUpgrade")))
        {
            upgradeCountTransormer = itemStack.stackSize;
        }
        upgradeCountTransormer = Math.min(upgradeCountTransormer, 4);
        if(worldObj!=null && !worldObj.isRemote)
        {
            packetSize = BASE_PACKET_SIZE * (int)Math.pow(4D, upgradeCountTransormer);
            
            if (addedToEnergyNet)
            {
                EnergyTileUnloadEvent event = new EnergyTileUnloadEvent(this);
                MinecraftForge.EVENT_BUS.post(event);
            }
            addedToEnergyNet = false;
            EnergyTileLoadEvent event = new EnergyTileLoadEvent(this);
            MinecraftForge.EVENT_BUS.post(event);
            addedToEnergyNet = true;
        }
    };

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction)
    {
        return direction.ordinal() == getFacing();
    }

    @Override
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction)
    {
        return direction.ordinal() != getFacing();
    }

    @Override
    public boolean isItemValid(int slotIndex, ItemStack itemstack)
    {
        return  itemstack.isItemEqual(Items.getItem("transformerUpgrade")); 
    }
    
    @Override
    public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int face) {
        return getFacing() != face;
    };

    @Override
    public boolean wrenchCanRemove(EntityPlayer entityPlayer)
    {
        return true;
    }

    @Override
    public float getWrenchDropRate()
    {
        return 1;
    }

    @Override
    public List<String> getNetworkedFields()
    {
        Vector<String> vector = new Vector<String>(2);
        vector.add("facing");
        vector.add("powerType");
        return vector;    
    }

    @Override
    public void onNetworkEvent(EntityPlayer player, int event)
    {
        counter=0;
    }
    
    @Override
    public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
    {
        return new ItemStack(IC2NuclearControl.instance.blockNuclearControlMain.blockID, 1, Damages.DAMAGE_ENERGY_COUNTER);
    }

    @Override
    public double getConductionLoss()
    {
        return 0.025D;
    }

    @Override
    public int getInsulationEnergyAbsorption()
    {
        return 16384;
    }

    @Override
    public int getInsulationBreakdownEnergy()
    {
        return packetSize+1;
    }

    @Override
    public int getConductorBreakdownEnergy()
    {
        return packetSize+1;
    }

    @Override
    public void removeInsulation()
    {
    }

    @Override
    public void removeConductor()
    {
        worldObj.setBlock(xCoord, yCoord, zCoord, 0, 0, 3);
        worldObj.createExplosion(null, xCoord, yCoord, zCoord, 0.8F, false);
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemstack)
    {
        return isItemValid(slot, itemstack);
    }

}
