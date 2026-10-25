package com.contagion.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.contagion.ContagionMod;

/** A finished thermometer reading, shown on the HUD gauge. */
public record ThermometerPayload(float celsius, Component subject) implements CustomPacketPayload {
	public static final Type<ThermometerPayload> TYPE = new Type<>(ContagionMod.id("thermometer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ThermometerPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeFloat(payload.celsius);
				ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, payload.subject);
			},
			buf -> new ThermometerPayload(buf.readFloat(), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf)));

	@Override
	public Type<ThermometerPayload> type() {
		return TYPE;
	}
}
