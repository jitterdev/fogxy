package dev.fogxy.mixin;

//? if <1.21.11 {
import dev.fogxy.config.FogxyConfigScreenPages;
import dev.fogxy.fog.VoxyCompat;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
//?}
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI", priority = 500)
public class MixinSodiumOptionsGUI {
    //? if <1.21.11 {
    @Shadow(remap = false)
    @Final
    private List<OptionPage> pages;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fogxy$addFogPage(Screen previousScreen, CallbackInfo ci) {
        if (!VoxyCompat.isLoaded()) {
            return;
        }

        OptionPage page = FogxyConfigScreenPages.page();
        String voxyTitle = Component.translatable("voxy.config.title").getString();

        int insertAt = this.pages.size();
        for (int i = 0; i < this.pages.size(); i++) {
            if (voxyTitle.equals(this.pages.get(i).getName().getString())) {
                insertAt = i + 1;
                break;
            }
        }
        this.pages.add(insertAt, page);
    }
    //?}
}
