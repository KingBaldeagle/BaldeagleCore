package com.baldeagle.chunkmap.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class ChunkMapKeybinds {

    public static final KeyBinding OPEN_MAP = new KeyBinding(
        "key.baldeaglecore.chunkmap.open",
        Keyboard.KEY_M,
        "key.categories.misc"
    );

    private ChunkMapKeybinds() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_MAP);
    }
}
