package com.example.textureeditor.client;

import com.example.textureeditor.TexturePixelEditor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TexturePixelEditor.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            int x = event.getScreen().width / 2 - 100;
            int y = event.getScreen().height / 4 + 96;
            event.addListener(Button.builder(
                    net.minecraft.network.chat.Component.literal("Редактор текстур"),
                    button -> net.minecraft.client.Minecraft.getInstance().setScreen(new TextureEditorScreen())
            ).bounds(x, y, 200, 20).build());
        }
    }
}
