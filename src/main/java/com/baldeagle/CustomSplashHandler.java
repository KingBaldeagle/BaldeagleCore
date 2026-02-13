package com.baldeagle;

import java.lang.reflect.Field;
import java.util.Random;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomSplashHandler {

    private static final String[] SPLASHES = {
        "The Eagle has landed!",
        "Join the Elite",
        "Baldeagle is King!",
        "Yo no se!",
        "Beware of doom!",
    };
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onInitGui(InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            try {
                GuiMainMenu gui = (GuiMainMenu) event.getGui();

                Field splashField;
                try {
                    // Dev environment name
                    splashField = GuiMainMenu.class.getDeclaredField(
                        "splashText"
                    );
                } catch (NoSuchFieldException e) {
                    // Obfuscated runtime name
                    splashField = GuiMainMenu.class.getDeclaredField(
                        "field_73975_c"
                    );
                }

                splashField.setAccessible(true); // THIS IS CRUCIAL

                // Pick a random splash
                String customSplash = SPLASHES[RANDOM.nextInt(SPLASHES.length)];

                splashField.set(gui, customSplash);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
