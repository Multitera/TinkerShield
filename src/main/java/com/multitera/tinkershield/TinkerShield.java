package com.multitera.tinkershield;

import com.multitera.tinkershield.init.ModItems;
import com.multitera.tinkershield.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = TinkerShield.MODID, name = TinkerShield.NAME, version = TinkerShield.VERSION,
        dependencies = "required-after:forge@[14.23.5.2768,);"
                + "required-after:mantle@[1.12-1.3.3.55,);"
                + "required-after:tconstruct@[1.12.2-2.13.0.183,);")

public class TinkerShield {

    @Mod.Instance
    public static TinkerShield instance;
    public static final String MODID = "tinkershield";
    public static final String NAME = "Tinker Shield";
    public static final String VERSION = "0.1";

    public static Logger logger;

    @SidedProxy (serverSide = "com.multitera.tinkershield.proxy.CommonProxy", clientSide = "com.multitera.tinkershield.proxy.ClientProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {

        logger = event.getModLog();
        proxy.preInit(event);
        ModItems.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        ModItems.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postInit(event);
        ModItems.postInit(event);
    }
}
