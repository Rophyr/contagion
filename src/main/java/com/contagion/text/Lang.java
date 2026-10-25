package com.contagion.text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.server.level.ServerPlayer;

import com.contagion.ContagionMod;

/**
 * Server-side access to the mod's own language files (assets/contagion/lang/*.json inside the jar), used where
 * the server must know the final text, such as when laying out book pages. Chat uses translatable components instead.
 */
public final class Lang {
	public static final String DEFAULT = "en_us";
	private static final Map<String, Map<String, String>> TABLES = new HashMap<>();

	private Lang() {
	}

	/** The player's game language if the mod ships it, otherwise English. */
	public static String of(ServerPlayer player) {
		String language = player.clientInformation().language();
		language = language == null ? DEFAULT : language.toLowerCase(Locale.ROOT);
		return table(language).isEmpty() ? DEFAULT : language;
	}

	public static String get(String language, String key) {
		String value = table(language).get(key);

		if (value == null) {
			value = table(DEFAULT).get(key);
		}

		return value == null ? key : value;
	}

	/** Minecraft-style formatting: {@code %s}, {@code %1$s} and {@code %%}. */
	public static String format(String language, String key, Object... args) {
		String pattern = get(language, key);
		StringBuilder out = new StringBuilder();
		int next = 0;

		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);

			if (c != '%' || i + 1 >= pattern.length()) {
				out.append(c);
				continue;
			}

			char d = pattern.charAt(i + 1);

			if (d == '%') {
				out.append('%');
				i++;
			} else if (d == 's') {
				out.append(next < args.length ? args[next] : "");
				next++;
				i++;
			} else if (Character.isDigit(d)) {
				int j = i + 1;

				while (j < pattern.length() && Character.isDigit(pattern.charAt(j))) {
					j++;
				}

				if (j + 1 < pattern.length() && pattern.charAt(j) == '$' && pattern.charAt(j + 1) == 's') {
					int index = Integer.parseInt(pattern.substring(i + 1, j)) - 1;
					out.append(index >= 0 && index < args.length ? args[index] : "");
					i = j + 1;
				} else {
					out.append(c);
				}
			} else {
				out.append(c);
			}
		}

		return out.toString();
	}

	private static synchronized Map<String, String> table(String language) {
		return TABLES.computeIfAbsent(language, Lang::load);
	}

	private static Map<String, String> load(String language) {
		Map<String, String> map = new HashMap<>();
		String path = "/assets/" + ContagionMod.MOD_ID + "/lang/" + language + ".json";

		try (InputStream stream = Lang.class.getResourceAsStream(path)) {
			if (stream == null) {
				return map;
			}

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

				for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
					map.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
		} catch (Exception e) {
			ContagionMod.LOGGER.warn("Could not load language file {}", path, e);
		}

		return map;
	}
}
