package com.contagion.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.contagion.ContagionMod;

/**
 * What the microscope screen shows: the job state, what the eyepiece should picture and the result text.
 *
 * @param mode      {@link #IDLE}, {@link #RUNNING}, {@link #READY} (done, not examined yet) or {@link #RESULT}
 * @param kind      {@link #ANALYSIS} or {@link #CULTURE}
 * @param remaining ticks left while running
 * @param total     total ticks of the running job
 * @param load      viral load seen in the result (0-5)
 * @param flags     result flags ({@link #POSITIVE}, {@link #ANTIBODIES}, ...)
 * @param active    culture test: exactly right ingredients (0-3)
 * @param toxic     culture test: tissue damage (0-2)
 * @param lines     result or status text
 */
public record MicroscopePayload(int mode, int kind, int remaining, int total, int load, int flags, int active, int toxic, List<Component> lines)
		implements CustomPacketPayload {
	public static final int IDLE = 0;
	public static final int RUNNING = 1;
	public static final int READY = 2;
	public static final int RESULT = 3;

	public static final int ANALYSIS = 0;
	public static final int CULTURE = 1;

	public static final int POSITIVE = 1;
	public static final int ANTIBODIES = 2;
	public static final int DEGRADED = 4;
	public static final int INSIGHT = 8;
	public static final int TRACE = 16;
	public static final int ERROR = 32;

	public static final MicroscopePayload EMPTY = new MicroscopePayload(IDLE, ANALYSIS, 0, 0, 0, 0, 0, 0, List.of());

	public static final Type<MicroscopePayload> TYPE = new Type<>(ContagionMod.id("microscope"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MicroscopePayload> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeVarInt(p.mode);
				buf.writeVarInt(p.kind);
				buf.writeVarInt(p.remaining);
				buf.writeVarInt(p.total);
				buf.writeVarInt(p.load);
				buf.writeVarInt(p.flags);
				buf.writeVarInt(p.active);
				buf.writeVarInt(p.toxic);
				buf.writeVarInt(p.lines.size());

				for (Component line : p.lines) {
					ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, line);
				}
			},
			buf -> {
				int mode = buf.readVarInt();
				int kind = buf.readVarInt();
				int remaining = buf.readVarInt();
				int total = buf.readVarInt();
				int load = buf.readVarInt();
				int flags = buf.readVarInt();
				int active = buf.readVarInt();
				int toxic = buf.readVarInt();
				int count = buf.readVarInt();
				List<Component> lines = new ArrayList<>(count);

				for (int i = 0; i < count; i++) {
					lines.add(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
				}

				return new MicroscopePayload(mode, kind, remaining, total, load, flags, active, toxic, List.copyOf(lines));
			});

	public boolean has(int flag) {
		return (this.flags & flag) != 0;
	}

	@Override
	public Type<MicroscopePayload> type() {
		return TYPE;
	}
}
