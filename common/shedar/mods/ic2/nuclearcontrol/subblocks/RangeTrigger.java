package shedar.mods.ic2.nuclearcontrol.subblocks;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import shedar.mods.ic2.nuclearcontrol.containers.ContainerRangeTrigger;
import shedar.mods.ic2.nuclearcontrol.gui.GuiRangeTrigger;
import shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityRangeTrigger;
import shedar.mods.ic2.nuclearcontrol.utils.Damages;

public class RangeTrigger extends Subblock
{
    private static final int DAMAGE = Damages.DAMAGE_RANGE_TRIGGER;
    private static final float[] BOUNDS = {0, 0, 0, 1, 1, 1};

    public static final byte I_BACK = 0;
    public static final byte I_SIDE = 1;
    public static final byte I_FACE_GRAY = 2;
    public static final byte I_FACE_GREEN = 3;
    public static final byte I_FACE_RED = 4;

    private static final byte[][] mapping =
    {
        {I_BACK, I_FACE_GRAY, I_SIDE, I_SIDE, I_SIDE, I_SIDE},
        {I_FACE_GRAY, I_BACK, I_SIDE, I_SIDE, I_SIDE, I_SIDE},
        {I_SIDE, I_SIDE, I_BACK, I_FACE_GRAY, I_SIDE, I_SIDE},
        {I_SIDE, I_SIDE, I_FACE_GRAY, I_BACK, I_SIDE, I_SIDE},
        {I_SIDE, I_SIDE, I_SIDE, I_SIDE, I_BACK, I_FACE_GRAY},
        {I_SIDE, I_SIDE, I_SIDE, I_SIDE, I_FACE_GRAY, I_BACK}
    };
    
    private Icon[] icons = new Icon[5];

    public RangeTrigger()
    {
        super(DAMAGE, "tile.blockRangeTrigger");
    }

    @Override
    public TileEntity getTileEntity()
    {
        return new TileEntityRangeTrigger();
    }

    @Override
    public boolean isSolidBlockRequired()
    {
        return false;
    }

    @Override
    public boolean hasGui()
    {
        return true;
    }

    @Override
    public float[] getBlockBounds(TileEntity tileEntity)
    {
        return BOUNDS;
    }

    @Override
    public Container getServerGuiElement(TileEntity tileEntity, EntityPlayer player)
    {
        return new ContainerRangeTrigger(player, (TileEntityRangeTrigger)tileEntity);
    }

    @Override
    public GuiContainer getClientGuiElement(TileEntity tileEntity, EntityPlayer player)
    {
        ContainerRangeTrigger containerRangeTrigger = new ContainerRangeTrigger(player, (TileEntityRangeTrigger)tileEntity);
        return new GuiRangeTrigger(containerRangeTrigger);
    }

    @Override
    protected Icon getIcon(int index)
    {
        return icons[index];
    }

    @Override
    protected byte[][] getMapping()
    {
        return mapping;
    }

    @Override
    public void registerIcons(IconRegister iconRegister)
    {
        icons[I_BACK] = iconRegister.registerIcon("nuclearControl:rangeTrigger/back");
        icons[I_SIDE] = iconRegister.registerIcon("nuclearControl:rangeTrigger/side");
        icons[I_FACE_GRAY] = iconRegister.registerIcon("nuclearControl:rangeTrigger/faceGray");
        icons[I_FACE_GREEN] = iconRegister.registerIcon("nuclearControl:rangeTrigger/faceGreen");
        icons[I_FACE_RED] = iconRegister.registerIcon("nuclearControl:rangeTrigger/faceRed");
        
    }

}
