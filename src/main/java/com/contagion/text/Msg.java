package com.contagion.text;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * A translatable message: a language-file key plus arguments (plain strings or other messages).
 *
 * <p>Sent to chat as a translatable component, so each client shows it in its own language. The research
 * journal (a book) needs exact text to lay out pages, so it resolves messages on the server in the reader's
 * language through {@link Lang}. Messages are also stored in the lab notebook, hence the codec.
 */
public record Msg(String key, List<Either<String, Msg>> args) {
	public static final Codec<Msg> CODEC = Codec.recursive("contagion_msg", self -> RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("key").forGetter(Msg::key),
			Codec.either(Codec.STRING, self).listOf().optionalFieldOf("args", List.of()).forGetter(Msg::args)
	).apply(i, Msg::new)));

	/** Arguments may be {@link Msg}s or anything else (turned into a string). */
	public static Msg of(String key, Object... args) {
		List<Either<String, Msg>> list = new ArrayList<>(args.length);

		for (Object arg : args) {
			list.add(arg instanceof Msg msg ? Either.right(msg) : Either.left(String.valueOf(arg)));
		}

		return new Msg(key, List.copyOf(list));
	}

	public MutableComponent component() {
		Object[] converted = new Object[this.args.size()];

		for (int i = 0; i < converted.length; i++) {
			converted[i] = this.args.get(i).map(string -> string, Msg::component);
		}

		return Component.translatable(this.key, converted);
	}

	public MutableComponent styled(ChatFormatting... style) {
		return component().withStyle(style);
	}

	public String resolve(String language) {
		Object[] converted = new Object[this.args.size()];

		for (int i = 0; i < converted.length; i++) {
			converted[i] = this.args.get(i).map(string -> string, msg -> msg.resolve(language));
		}

		return Lang.format(language, this.key, converted);
	}
}
