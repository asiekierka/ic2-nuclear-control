package shedar.mods.ic2.nuclearcontrol.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class IconButton extends GuiButton
{
    private ResourceLocation textureLocation;
    public int textureLeft;
    public int textureTop;

    public IconButton(int id, int left, int top, int width, int height, ResourceLocation textureLocation, int textureLeft, int textureTop)
    {
        super(id, left, top, width, height, "");
        this.textureLocation = textureLocation;
        this.textureLeft = textureLeft;
        this.textureTop = textureTop;
    }

    @Override
    public void drawButton(Minecraft par1Minecraft, int mouseX, int mouseY)
    {
        if (this.drawButton)
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, par1Minecraft.renderEngine.func_110581_b/*getTexture*/(textureLocation).func_110552_b());
            boolean isHover = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            if (isHover)
            {
                drawGradientRect(xPosition, yPosition, xPosition + width, yPosition + height, 0x80FFFFFF, 0x80FFFFFF);
            }
            drawTexturedModalRect(xPosition, yPosition, textureLeft, textureTop, width, height);
            this.mouseDragged(par1Minecraft, mouseX, mouseY);
        }
    }
}
