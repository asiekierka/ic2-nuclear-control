package shedar.mods.ic2.nuclearcontrol;

import ic2.api.Direction;
import ic2.api.IWrenchable;
import ic2.api.Items;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileSourceEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.api.network.NetworkHelper;

import java.util.List;
import java.util.Vector;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;
import net.minecraftforge.common.MinecraftForge;


public class TileEntityEnergyCounter extends TileEntity implements 
    IEnergySink, IWrenchable, IEnergySource, INetworkClientTileEntityEventListener,
    IInventory, ISlotItemFilter, INetworkDataProvider, INetworkUpdateListener 
{
    private static final int BASE_PACKET_SIZE = 32; 
    
    private boolean init;
    private ItemStack inventory[];

    public long counter;
    private int storage;
    
    private short prevFacing;
    public short facing;
    

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
    }

    private void initData()
    {
        if(worldObj.isRemote){
            NetworkHelper.requestInitialData(this);
        }
        init = true;
    }
    
    @Override
    public short getFacing()
    {
        return (short)Facing.faceToSide[facing];
    }
    
    @Override
    public void setFacing(short f)
    {
        if (addedToEnergyNet)
        {
            EnergyTileUnloadEvent event = new EnergyTileUnloadEvent(this);
            MinecraftForge.EVENT_BUS.post(event);
        }
        addedToEnergyNet = false;
        setSide((short)Facing.faceToSide[f]);
        EnergyTileLoadEvent event = new EnergyTileLoadEvent(this);
        MinecraftForge.EVENT_BUS.post(event);
        addedToEnergyNet = true;
    }
    
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
                addedToEnergyNet = true;
            }
            
            if (storage >= packetSize)
            {
                EnergyTileSourceEvent event = new EnergyTileSourceEvent(this,packetSize);
                MinecraftForge.EVENT_BUS.post(event);
                int sent = packetSize - event.amount;
                storage -= sent;
                counter += sent;
            }
        }
        super.updateEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readFromNBT(nbttagcompound);
        storage = nbttagcompound.getInteger("storage");
        facing = nbttagcompound.getShort("facing");
        counter = nbttagcompound.getLong("counter");
        
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
        nbttagcompound.setInteger("storage", storage);
        nbttagcompound.setShort("facing", facing);
        nbttagcompound.setLong("counter", counter);
        
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
        }
    };

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction)
    {
        return direction.toSideValue() == getFacing();
    }

    @Override
    public boolean emitsEnergyTo(TileEntity emitter, Direction direction)
    {
        return direction.toSideValue()  != getFacing();
    }

    @Override
    public boolean isAddedToEnergyNet()
    {
        return addedToEnergyNet;
    }

    @Override
    public int demandsEnergy()
    {
        return 2*packetSize - storage;
    }

    @Override
    public int injectEnergy(Direction directionFrom, int amount)
    {
        if (amount > packetSize)
        {
            worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, 0);
            worldObj.createExplosion(null, xCoord, yCoord, zCoord, 0.8F, false);
            return 0;
        }

        storage += amount;
        int left = 0;

        if (storage > 2*packetSize)
        {
            left = storage - 2*packetSize;
            storage = 2*packetSize;
        }
        return left;
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
        Vector<String> vector = new Vector<String>(1);
        vector.add("facing");
        return vector;    
    }

    @Override
    public int getMaxEnergyOutput()
    {
        return 4096;
    }

    @Override
    public void onNetworkEvent(EntityPlayer player, int event)
    {
        counter=0;
    }
    
    @Override
    public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
    {
        return new ItemStack(IC2NuclearControl.instance.blockNuclearControlMain.blockID, 1, BlockNuclearControlMain.DAMAGE_ENERGY_COUNTER);
    }

    @Override
    public int getMaxSafeInput()
    {
        return packetSize;
    }
}
