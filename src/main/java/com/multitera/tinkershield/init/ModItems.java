package com.multitera.tinkershield.init;

import com.multitera.tinkershield.TinkerShield;
import com.multitera.tinkershield.item.BattleShield;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.TinkerRegistryClient;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;

@Mod.EventBusSubscriber(modid = TinkerShield.MODID)
public class ModItems {

    public static BattleShield battleShield;

    public static void preInit(FMLPreInitializationEvent event) {
    }

    public static void init(FMLInitializationEvent initializationEvent) {
        TinkerRegistry.registerToolForgeCrafting(battleShield);

        ToolBuildGuiInfo info = new ToolBuildGuiInfo(battleShield);
        info.addSlotPosition(40 , 40);
        info.addSlotPosition(20 , 30);
        info.addSlotPosition(20 , 50);
        TinkerRegistryClient.addToolBuilding(info);
    }

    public static void postInit(FMLPostInitializationEvent postInitializationEvent) {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {

        battleShield = new BattleShield();
        battleShield.setRegistryName(TinkerShield.MODID,"battleshield");
        event.getRegistry().register(battleShield);
        TinkerShield.proxy.registerToolModel(battleShield);
        MinecraftForge.EVENT_BUS.register(battleShield); // battleshield events

    }
}
