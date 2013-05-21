package shedar.mods.ic2.nuclearcontrol.items;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import shedar.mods.ic2.nuclearcontrol.utils.NuclearHelper;
import shedar.mods.ic2.nuclearcontrol.utils.NuclearNetworkHelper;
import shedar.mods.ic2.nuclearcontrol.utils.TextureResolver;

public class ItemToolThermometer extends Item
{

    public ItemToolThermometer(int i)
    {
        super(i);
        setMaxDamage(102);
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabMisc);
    }

    protected boolean canTakeDamage(ItemStack itemstack, int i)
    {
        return true;
    }

    @Override
    public void registerIcons(IconRegister iconRegister)
    {
        itemIcon = iconRegister.registerIcon(TextureResolver.getItemTexture("thermometer"));
    }    

    @Override
    public boolean onItemUseFirst(ItemStack itemstack, EntityPlayer player, World world, int x, int y, int z, int side,  float hitX, float hitY, float hitZ)
    {
        boolean isServer = player instanceof EntityPlayerMP;
        if(!isServer)
            return false;
        if (!canTakeDamage(itemstack, 2))
        {
            return false;
        }
        IReactor reactor = NuclearHelper.getReactorAt(world, x, y, z);
        if (reactor == null)
        {
        	IReactorChamber chamber = NuclearHelper.getReactorChamberAt(world, x, y, z);
        	if(chamber!=null)
        	{
        		reactor = chamber.getReactor();
        	}
        }
        if(reactor != null)
        {
            messagePlayer(player, reactor);
            damage(itemstack, 1, player);
        	return true;
        }
        return false;

    }

    protected void messagePlayer(EntityPlayer entityplayer, IReactor reactor)
    {
        int heat = reactor.getHeat();
    	NuclearNetworkHelper.chatMessage(entityplayer, "Thermo:" + heat);
    }

    protected void damage(ItemStack itemstack, int i, EntityPlayer entityplayer)
    {
        itemstack.damageItem(10, entityplayer);
    }

}
