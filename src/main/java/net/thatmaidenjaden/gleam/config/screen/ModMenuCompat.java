package net.thatmaidenjaden.gleam.config.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) return YACLConfigScreen::getScreen;
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) return ClothConfigScreen::getScreen;
        return null;
    }
}
