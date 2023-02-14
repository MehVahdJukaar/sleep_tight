package net.mehvahdjukaar.sleep_tight.mixins;

import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(InBedChatScreen.class)
public abstract class InBedChatScreenMixin extends Screen {


    protected InBedChatScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }
}
