package com.contagion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import com.contagion.command.ContagionCommands;
import com.contagion.disease.DiseaseTicker;
import com.contagion.disease.Transmission;
import com.contagion.disease.WorldInfection;
import com.contagion.network.ModNetworking;
import com.contagion.registry.ModMenus;
import com.contagion.lab.LabInteractions;
import com.contagion.registry.ModAttachments;
import com.contagion.registry.ModBlocks;
import com.contagion.registry.ModComponents;
import com.contagion.registry.ModItems;

public final class ContagionMod implements ModInitializer {
	public static final String MOD_ID = "contagion";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ContagionConfig.load();

		ModComponents.init();
		ModBlocks.init();
		ModItems.init();
		ModAttachments.init();

		LabInteractions.register();
		DiseaseTicker.register();
		Transmission.register();
		WorldInfection.register();
		ModMenus.init();
		ModNetworking.init();
		ContagionCommands.register();

		LOGGER.info("Contagion loaded. Stay healthy.");
	}
}
