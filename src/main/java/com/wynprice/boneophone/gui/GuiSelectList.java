package com.wynprice.boneophone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;

public class GuiSelectList {

    private static Minecraft mc = Minecraft.getMinecraft();

    public static final float SCROLL_AMOUNT = 0.4F;

    public final int width;
    public final int cellHeight;

    public final int cellMax;

    private final int xPos;
    private final int yPos;

    private boolean open;
    private float scroll;

    private SelectListEntry active;

    private final Supplier<List<SelectListEntry>> listSupplier;

    private int lastYClicked = -1;

    public GuiSelectList(int xPos, int yPos, int width, int cellHeight, int cellMax, Supplier<List<SelectListEntry>> listSupplier) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.cellHeight = cellHeight;
        this.cellMax = cellMax;
        this.listSupplier = listSupplier;
    }

    public void render(int mouseX, int mouseY) {
        List<SelectListEntry> entries = this.listSupplier.get();
        int height = this.cellHeight + (this.open ?  Math.min(entries.size(), this.cellMax) * this.cellHeight : 0);

        int ySize = (entries.size() - this.cellMax) * this.cellHeight;
        int totalHeight = height - this.cellHeight;

        int scrollBarWidth = 6;
        int scrollBarLeft = this.xPos + this.width - scrollBarWidth;


        float scrollLength = MathHelper.clamp(totalHeight * totalHeight / ySize, 32, totalHeight - 8);
        float scrollYStart = this.scroll * this.cellHeight * (totalHeight - scrollLength) / (Math.max((entries.size() -  this.cellMax) * this.cellHeight, 0)) + this.yPos + this.cellHeight;
        if (scrollYStart < this.yPos) {
            scrollYStart = this.yPos;
        }

        if(this.lastYClicked != -1) {
            if(!Mouse.isButtonDown(0)) {
                this.lastYClicked = -1;
            } else {
                float oldScroll = this.scroll;
                float pixelsPerEntry = (totalHeight - scrollLength) / (entries.size() - this.cellMax);
                this.scroll((this.lastYClicked - mouseY) / pixelsPerEntry);
                if(oldScroll != this.scroll) {
                    this.lastYClicked = mouseY;
                }
            }
        }
//        int listedCells = Math.min(entries.size(), this.cellMax);

        if(!Minecraft.getMinecraft().getFramebuffer().isStencilEnabled()) {
            Minecraft.getMinecraft().getFramebuffer().enableStencil();
        }

        glEnable(GL_STENCIL_TEST);
        glColorMask(false, false, false, false);
        glDepthMask(false);
        glStencilFunc(GL_NEVER, 1, 0xFF);
        glStencilOp(GL_REPLACE, GL_KEEP, GL_KEEP);

        glStencilMask(0xFF);
        glClear(GL_STENCIL_BUFFER_BIT);

        Gui.drawRect(this.xPos, this.yPos + this.cellHeight, this.xPos + this.width, this.yPos + height, -1);

        glColorMask(true, true, true, true);
        glDepthMask(true);
        glStencilMask(0x00);

        glStencilFunc(GL_EQUAL, 1, 0xFF);

        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;

        int borderSize = 1;
        int borderColor = -1;
        int insideColor = 0xFF000000;
        int insideSelectionColor = 0xFF303030;
        int highlightColor = 0x2299bbff;
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.enableGUIStandardItemLighting();

        if(this.open) {
            for (int i = 0; i < entries.size(); i++) {
                int yStart = (int) (this.yPos + this.cellHeight * (i + 1) - this.scroll * this.cellHeight);
                Gui.drawRect(this.xPos, yStart, this.xPos + this.width, yStart + this.cellHeight, insideSelectionColor);
                Gui.drawRect(this.xPos, yStart, this.xPos + this.width, yStart + borderSize, borderColor);
                entries.get(i).draw(this.xPos, yStart);
            }
        }

        boolean highlighedScrollbar = false;

        if(relX > 0 && relY > 0) {
            if(relX <= this.width){
                if (relY <= this.cellHeight) {
                    Gui.drawRect(this.xPos, this.yPos, this.xPos + this.width, this.yPos + this.cellHeight, highlightColor);
                } else if(this.open) {
                    if(entries.size() > this.cellMax && mouseX >= scrollBarLeft && mouseX <= scrollBarLeft + scrollBarWidth && mouseY >= scrollYStart && mouseY <= scrollYStart + scrollLength) {
                        highlighedScrollbar = true;
                    } else {
                        for (int i = 0; i < entries.size(); i++) {
                            if(relY <= this.cellHeight * (i + 2) - this.scroll * this.cellHeight) {
                                int yStart = (int) (this.yPos + this.cellHeight * (i + 1) - this.scroll * this.cellHeight);
                                Gui.drawRect(this.xPos, yStart, this.xPos + this.width, yStart + this.cellHeight, highlightColor);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if(this.open) {
            if(entries.size() > this.cellMax) {
                int l = (int) scrollLength;
                int ys = (int) scrollYStart;

                Gui.drawRect(scrollBarLeft, ys, scrollBarLeft + scrollBarWidth, ys + l, insideColor);

                if(highlighedScrollbar) {
                    Gui.drawRect(scrollBarLeft, ys, scrollBarLeft + scrollBarWidth, ys + l, highlightColor);
                }

                Gui.drawRect(scrollBarLeft, ys, scrollBarLeft + scrollBarWidth, ys + borderSize, borderColor);
                Gui.drawRect(scrollBarLeft, ys + l, scrollBarLeft + scrollBarWidth, ys + l - borderSize, borderColor);
                Gui.drawRect(scrollBarLeft, ys, scrollBarLeft + borderSize, ys + l, borderColor);
//                Gui.drawRect(left + w, ys, left + w - borderSize, ys + l, borderColor);
            }
        }

        RenderHelper.disableStandardItemLighting();

        glDisable(GL_STENCIL_TEST);

        GlStateManager.disableDepth();
        Gui.drawRect(this.xPos, this.yPos, this.xPos + this.width, this.yPos + this.cellHeight, insideColor);


        if(this.active != null) {
            this.active.draw(this.xPos, this.yPos);
        }

        Gui.drawRect(this.xPos, this.yPos, this.xPos + this.width, this.yPos + borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos + height, this.xPos + this.width, this.yPos + height - borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos, this.xPos + borderSize, this.yPos + height, borderColor);
        Gui.drawRect(this.xPos + this.width, this.yPos, this.xPos + this.width - borderSize, this.yPos + height, borderColor);
        GlStateManager.enableDepth();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(mouseButton == 0) {
            List<SelectListEntry> entries = this.listSupplier.get();

            int height = this.cellHeight + (this.open ?  Math.min(entries.size(), this.cellMax) * this.cellHeight : 0);
            int ySize = (entries.size() - this.cellMax) * this.cellHeight;
            int totalHeight = height - this.cellHeight;
            int scrollBarWidth = 6;
            int scrollBarLeft = this.xPos + this.width - scrollBarWidth;
            float scrollLength = MathHelper.clamp(totalHeight * totalHeight / ySize, 32, totalHeight - 8);
            float scrollYStart = this.scroll * this.cellHeight * (totalHeight - scrollLength) / (Math.max((this.listSupplier.get().size() -  this.cellMax) * this.cellHeight, 0)) + this.yPos + this.cellHeight;
            if (scrollYStart < this.yPos) {
                scrollYStart = this.yPos;
            }

            if(this.open && entries.size() > this.cellMax && mouseX >= scrollBarLeft && mouseX <= scrollBarLeft + scrollBarWidth && mouseY >= scrollYStart && mouseY <= scrollYStart + scrollLength) {
                this.lastYClicked = mouseY;
                return;
            }

            int relX = mouseX - this.xPos;
            int relY = mouseY - this.yPos;
            if(relX > 0 && relY > 0) {
                if(relX <= this.width) {
                    if(relY <= this.cellHeight) {
                        this.open = !this.open;
                        return;
                    } else if(this.open){
                        for (int i = 0; i < entries.size(); i++) {
                            if(relY <= this.cellHeight * (i + 2) - this.scroll * this.cellHeight) {
                                entries.get(i).onClicked(relX, relY);
                                this.active = entries.get(i);
                                break;
                            }
                        }
                    }
                }
            }
        }
        this.open = false;
    }

    public void handleMouseInput() {
        int mouseInput = Mouse.getEventDWheel();
        if(mouseInput != 0) {
            this.scroll((mouseInput < 0 ? -1 : 1) * SCROLL_AMOUNT);
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;
        if(relX > 0 && relY > 0) {
            if(relX <= this.width) {
                if(relY <= this.cellHeight) {
                    return true;
                } else if(this.open){
                    return relY <= this.cellHeight * (Math.min(this.listSupplier.get().size(), this.cellMax) + 1);
                }
            }
        }
        return false;
    }

    public void scroll(float amount) {
        this.scroll -= amount;
        this.scroll = MathHelper.clamp(this.scroll, 0, Math.max(this.listSupplier.get().size() -  this.cellMax, 0));
    }

    public SelectListEntry getActive() {
        return this.active;
    }

    interface SelectListEntry {
        void draw(int x, int y);

        default void onClicked(int relMouseX, int relMouseY) {

        }
    }
}
