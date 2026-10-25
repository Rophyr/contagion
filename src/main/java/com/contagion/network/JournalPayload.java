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
 * Everything the research journal screen shows. Only what the players discovered is sent, never the cure.
 *
 * @param day        current day
 * @param identified whether the pathogen was identified (then name and profile are set)
 * @param findings   discovered fact keys ({@code cat:}, {@code form:}, {@code toxic:}, {@code inert:})
 * @param log        notebook entries, oldest first
 */
public record JournalPayload(int day, boolean identified, Component name, Component profile, List<String> findings, List<Component> log)
		implements CustomPacketPayload {
	public static final Type<JournalPayload> TYPE = new Type<>(ContagionMod.id("journal"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeVarInt(p.day);
				buf.writeBoolean(p.identified);
				ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.name);
				ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, p.profile);
				buf.writeVarInt(p.findings.size());

				for (String finding : p.findings) {
					buf.writeUtf(finding);
				}

				buf.writeVarInt(p.log.size());

				for (Component entry : p.log) {
					ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, entry);
				}
			},
			buf -> {
				int day = buf.readVarInt();
				boolean identified = buf.readBoolean();
				Component name = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
				Component profile = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
				int findingCount = buf.readVarInt();
				List<String> findings = new ArrayList<>(findingCount);

				for (int i = 0; i < findingCount; i++) {
					findings.add(buf.readUtf());
				}

				int logCount = buf.readVarInt();
				List<Component> log = new ArrayList<>(logCount);

				for (int i = 0; i < logCount; i++) {
					log.add(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
				}

				return new JournalPayload(day, identified, name, profile, List.copyOf(findings), List.copyOf(log));
			});

	@Override
	public Type<JournalPayload> type() {
		return TYPE;
	}
}
