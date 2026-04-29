package com.kingmihailp.customtab;

import com.kingmihailp.customtab.command.TabCommand;
import com.kingmihailp.customtab.config.TabConfig;
import com.kingmihailp.customtab.handler.TabListHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CustomTabMod.MODID)
public class CustomTabMod {

    public static final String MODID = "customtab";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public CustomTabMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, TabConfig.SPEC, "customtab-server.toml");

        modEventBus.addListener(this::commonSetup);

        TabListHandler handler = new TabListHandler();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.register(handler);
        TabCommand.setHandlerRef(handler);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("CustomTab mod initialized.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TabCommand.register(event.getDispatcher());
    }
}
