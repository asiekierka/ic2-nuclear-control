package shedar.mods.ic2.nuclearcontrol;

import java.util.List;

import shedar.mods.ic2.nuclearcontrol.panel.CardWrapperImpl;

import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.TileEntity;
import net.minecraft.src.ic2.api.Direction;
import net.minecraft.src.ic2.api.ElectricItem;
import net.minecraft.src.ic2.api.EnergyNet;
import net.minecraft.src.ic2.api.IElectricItem;
import net.minecraft.src.ic2.api.IEnergySink;
import net.minecraft.src.ic2.api.IReactor;
import net.minecraft.src.ic2.api.Items;
import net.minecraft.src.ic2.api.NetworkHelper;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;


public class TileEntityRemoteThermo extends TileEntityIC2Thermo implements 
    ISidedInventory, IEnergySink, ISlotItemFilter, IRotation
{
    public static final int SLOT_CHARGER = 0;
    public static final int SLOT_CARD = 1;
    private static final int BASE_PACKET_SIZE = 32;
    private static final int BASE_STORAGE = 600;
    private static final int STORAGE_PER_UPGRADE = 10000;
    private static final int ENERGY_SU_BATTERY = 1000;
    private static final int LOCATION_RANGE = 8;
    
    private int deltaX;
    private int deltaY;
    private int deltaZ;
    private int prevMaxStorage;
    public int maxStorage;
    public int prevMaxPacketSize;
    public int maxPacketSize;
    private int prevTier;
    public int tier;
    public int rotation;
    public int prevRotation;
    public int energy;
    private boolean addedToEnergyNet;
    private ItemStack inventory[];

    public TileEntityRemoteThermo()
    {
        super();
        inventory = new ItemStack[5];//battery + card + 3 overclockers
        addedToEnergyNet = false;
        maxStorage = BASE_STORAGE;
        maxPacketSize = BASE_PACKET_SIZE;
        tier = 1;
        deltaX = 0;
        deltaY = 0;
        deltaZ = 0;
        energy = 0;
        prevRotation = 0;
        rotation = 0;
    }
    
    @Override
    public List<String> getNetworkedFields()
    {
        List<String> list = super.getNetworkedFields();
        list.add("maxStorage");
        list.add("tier");
        list.add("maxPacketSize");
        list.add("rotation");
        
        return list;
    }
    
    @Override
    protected void checkStatus()
    {
        if (!addedToEnergyNet)
        {
            EnergyNet.getForWorld(worldObj).addTileEntity(this);
            addedToEnergyNet = true;
        }
        onInventoryChanged();

        int fire;
        if(energy >= IC2NuclearControl.instance.remoteThermalMonitorEnergyConsumption)
        {
            IReactor reactor = NuclearHelper.getReactorAt(worldObj, xCoord+deltaX, yCoord+deltaY, zCoord+deltaZ);
            if(reactor != null){
                if(tickRate == -1)
                {
                    tickRate = reactor.getTickRate() / 2;
                    if(tickRate == 0)
                        tickRate = 1;
                    updateTicker = tickRate;
                }
                int reactorHeat = reactor.getHeat();
                fire = reactorHeat;
            }
            else
            {
                fire = -1;
            }
        }
        else
        {
            fire = -2;  
        }
        
        if(fire != getOnFire()){
            setOnFire(fire);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, worldObj.getBlockId(xCoord, yCoord, zCoord));
        }
    }
    
    public int getEnergy()
    {
        return energy;
    }
            
    public void setEnergy(int value)
    {
        energy = value;
    }

    public void setTier(int value)
    {
        tier = value;
        if(tier!=prevTier)
        {
            NetworkHelper.updateTileEntityField(this, "tier");
        }
        prevTier = tier;
    }

    public void setRotation(int value)
    {
        rotation = value;
        if(rotation!=prevRotation)
        {
            NetworkHelper.updateTileEntityField(this, "rotation");
        }
        prevRotation = rotation;
    }

    public void setMaxPacketSize(int value)
    {
        maxPacketSize = value;
        if(maxPacketSize!=prevMaxPacketSize)
        {
            NetworkHelper.updateTileEntityField(this, "maxPacketSize");
        }
        prevMaxPacketSize = maxPacketSize;
    }

    public void setMaxStorage(int value)
    {
        maxStorage = value;
        if(maxStorage!=prevMaxStorage)
        {
            NetworkHelper.updateTileEntityField(this, "maxStorage");
        }
        prevMaxStorage = maxStorage;
    }

    @Override
    public void updateEntity()
    {
        if (!worldObj.isRemote)
        {
            int consumption = IC2NuclearControl.instance.remoteThermalMonitorEnergyConsumption; 
            if(energy>=consumption)
            {
                energy-=consumption;
            }
            if(inventory[SLOT_CHARGER]!= null)
            {
                if (energy < maxStorage)
                {
                    if (inventory[SLOT_CHARGER].getItem() instanceof IElectricItem)
                    {
                        IElectricItem ielectricitem = (IElectricItem)inventory[SLOT_CHARGER].getItem();
    
                        if (ielectricitem.canProvideEnergy())
                        {
                            int k = ElectricItem.discharge(inventory[SLOT_CHARGER], maxStorage - energy, tier, false, false);
                            energy += k;
                        }
                    }
                    else if(inventory[SLOT_CHARGER].itemID == Items.getItem("suBattery").itemID)
                    {
                        if ( ENERGY_SU_BATTERY <= maxStorage - energy || energy == 0)
                        {
                            inventory[SLOT_CHARGER].stackSize--;
    
                            if (inventory[SLOT_CHARGER].stackSize <= 0)
                            {
                                inventory[SLOT_CHARGER] = null;
                            }
    
                            energy += ENERGY_SU_BATTERY;
                            if(energy > maxStorage)
                                energy = maxStorage;
                        }
                    }
                }
            }
            setEnergy(energy);
        }
        super.updateEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readFromNBT(nbttagcompound);
        energy = nbttagcompound.getInteger("energy");
        if(nbttagcompound.hasKey("rotation"))
        {
            prevRotation = rotation = nbttagcompound.getInteger("rotation");
        }
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
        super.onNetworkUpdate(field);
        if (field.equals("rotation") && prevRotation != rotation)
        {
            worldObj.markBlockNeedsUpdate(xCoord, yCoord, zCoord);
            prevRotation = rotation;
        }
    }    

    @Override
    public void invalidate()
    {
        if (!worldObj.isRemote && addedToEnergyNet)
        {
            EnergyNet.getForWorld(worldObj).removeTileEntity(this);
            addedToEnergyNet = false;
        }

        super.invalidate();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound)
    {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setInteger("energy", energy);
        nbttagcompound.setInteger("rotation", rotation);

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
                return itemStack;
            }
            
            ItemStack taken = inventory[slotNum].splitStack(amount);
            if (inventory[slotNum].stackSize == 0)
            {
                inventory[slotNum] = null;
            }
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
        int upgradeCountStorage = 0;
        int upgradeCountRange = 0;
        for (int i = 2; i < 5; i++)
        {
            ItemStack itemStack = inventory[i];

            if (itemStack == null)
            {
                continue;
            }

            if (itemStack.isItemEqual(Items.getItem("transformerUpgrade")))
            {
                upgradeCountTransormer += itemStack.stackSize;
            }
            else if (itemStack.isItemEqual(Items.getItem("energyStorageUpgrade")))
            {
                upgradeCountStorage += itemStack.stackSize;
            }
            else if(itemStack.getItem() instanceof ItemRangeUpgrade)
            {
                upgradeCountRange += itemStack.stackSize;
            }
        }
        if(inventory[SLOT_CARD]!=null)
        {
            ChunkCoordinates target = new CardWrapperImpl(inventory[SLOT_CARD]).getTarget();
            if(target != null)
            {
                deltaX = target.posX - xCoord;
                deltaY = target.posY - yCoord;
                deltaZ = target.posZ - zCoord;
                if(upgradeCountRange > 7)
                    upgradeCountRange = 7;
                int range = LOCATION_RANGE * (int)Math.pow(2, upgradeCountRange);
                if(Math.abs(deltaX) > range || 
                    Math.abs(deltaY) > range || 
                    Math.abs(deltaZ) > range)
                {
                    deltaX = deltaY = deltaZ = 0;
                }
            }
            else
            {
                deltaX = 0;
                deltaY = 0;
                deltaZ = 0;
            }
        }
        else
        {
            deltaX = 0;
            deltaY = 0;
            deltaZ = 0;
        }
        upgradeCountTransormer = Math.min(upgradeCountTransormer, 4);
        if(worldObj!=null && !worldObj.isRemote)
        {
            tier = upgradeCountTransormer + 1;
            setTier(tier);
            maxPacketSize = BASE_PACKET_SIZE * (int)Math.pow(4D, upgradeCountTransormer);
            setMaxPacketSize(maxPacketSize);
            maxStorage = BASE_STORAGE + STORAGE_PER_UPGRADE * upgradeCountStorage;
            setMaxStorage(maxStorage);
            if(energy > maxStorage)
                energy = maxStorage;
            setEnergy(energy);
        }
    };

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction)
    {
        return true;
    }

    @Override
    public boolean isAddedToEnergyNet()
    {
        return addedToEnergyNet;
    }

    @Override
    public boolean demandsEnergy()
    {
        return energy <= maxStorage - maxPacketSize || energy == 0;
    }

    @Override
    public int injectEnergy(Direction directionFrom, int amount)
    {
        if (amount > maxPacketSize)
        {
            worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, 0);
            worldObj.createExplosion(null, xCoord, yCoord, zCoord, 0.8F);
            return 0;
        }

        energy += amount;
        int left = 0;

        if (energy > maxStorage)
        {
            left = energy - maxStorage;
            energy = maxStorage;
        }
        setEnergy(energy);
        return left;
    }

    @Override
    public boolean isItemValid(int slotIndex, ItemStack itemstack)
    {
        switch (slotIndex)
        {
            case SLOT_CHARGER:
                if(itemstack.itemID == Items.getItem("suBattery").itemID)
                    return true;
                if(itemstack.getItem() instanceof IElectricItem)
                {
                    IElectricItem item = (IElectricItem)itemstack.getItem();
                    if (item.canProvideEnergy() && item.getTier() <= tier)
                    {
                        return true;
                    }
                }
                return false;
            case SLOT_CARD:
                return itemstack.getItem() instanceof ItemCardReactorSensorLocation;
            default:
                return  itemstack.isItemEqual(Items.getItem("transformerUpgrade")) ||
                        itemstack.isItemEqual(Items.getItem("energyStorageUpgrade")) ||
                        itemstack.getItem() instanceof ItemRangeUpgrade; 
        }
        
    }
    
    @Override
    public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int face) {
        return !entityPlayer.isSneaking() && getFacing() != face;
    };

    @Override
    public boolean wrenchCanRemove(EntityPlayer entityPlayer)
    {
        return !entityPlayer.isSneaking();
    }

    @Override
    public int modifyTextureIndex(int texture)
    {
        return texture;
    }

    @Override
    public int getStartInventorySide(ForgeDirection side)
    {
        if(side == ForgeDirection.UP)
            return 1;
        return 0;    
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side)
    {
        if(side == ForgeDirection.DOWN || side == ForgeDirection.UP)
            return 1;
        return inventory.length;
    }

    @Override
    public void rotate()
    {
        int r;
        switch (rotation)
        {
            case 0:
                r = 1;
                break;
            case 1:
                r = 3;
                break;
            case 3:
                r = 2;
                break;
            case 2:
                r = 0;
                break;
            default:
                r = 0;
                break;
        }
        setRotation(r);
    }

    @Override
    public int getRotation()
    {
        return rotation;
    }
    
}
