package com.contagion.client;

import net.minecraft.client.gui.screens.MenuScreens;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.contagion.ContagionMod;
import com.contagion.network.JournalPayload;
import com.contagion.network.MicroscopePayload;
import com.contagion.network.ThermometerPayload;
import com.contagion.registry.ModMenus;

public final class ContagionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.MICROSCOPE, MicroscopeScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(MicroscopePayload.TYPE, (payload, context) -> MicroscopeScreen.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(JournalPayload.TYPE, (payload, context) -> context.client().setScreen(new JournalScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ThermometerPayload.TYPE, (payload, context) -> ThermometerHud.receive(payload));

		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ContagionMod.id("thermometer"), ThermometerHud::extract);
	}
}
