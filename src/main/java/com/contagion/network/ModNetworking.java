package com.contagion.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Server-to-client payloads for the custom screens and the HUD. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(ThermometerPayload.TYPE, ThermometerPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MicroscopePayload.TYPE, MicroscopePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(JournalPayload.TYPE, JournalPayload.CODEC);
	}
}
