package com.contagion.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import com.contagion.ContagionConfig;
import com.contagion.data.BloodSample;
import com.contagion.data.CultureData;
import com.contagion.data.Infection;
import com.contagion.data.Research;
import com.contagion.data.SerumMix;
import com.contagion.data.Strain;
import com.contagion.disease.Disease;
import com.contagion.disease.Form;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Reagent;
import com.contagion.registry.ModComponents;
import com.contagion.registry.ModItems;
import com.contagion.text.Msg;

/**
 * Shared helpers for lab items: building item stacks, research findings, thermometer readings.
 */
public final class Lab {
	private static final String BAR = "▮";

	private Lab() {
	}

	public static long day(MinecraftServer server) {
		return server.overworld().getGameTime() / ContagionConfig.dayLengthTicks + 1;
	}

	public static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	private static Component lore(String key, ChatFormatting color, Object... args) {
		return Msg.of(key, args).styled(color);
	}

	/** Remembers that the players have worked with this substance (in this form), for the journal. */
	public static void noteTried(MinecraftServer server, Ingredient ingredient) {
		Research research = Strains.research(server);
		Research updated = research.withFinding("tried:" + ingredient.reagent().name() + ":" + ingredient.form().name());

		if (updated != research) {
			Strains.setResearch(server, updated);
		}
	}

	// ---- Blood samples ----------------------------------------------------------------------------------------

	public static ItemStack bloodSample(MinecraftServer server, LivingEntity donor) {
		Infection infection = Disease.get(donor);
		BloodSample data = new BloodSample(
				donor.getName().getString(),
				infection != null && infection.infected(),
				Disease.viralLoad(infection),
				infection != null && infection.immune(),
				(int) day(server));

		ItemStack stack = new ItemStack(ModItems.BLOOD_SAMPLE);
		stack.set(ModComponents.BLOOD_SAMPLE, data);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				lore("contagion.lore.sample.donor", ChatFormatting.GRAY, data.source()),
				lore("contagion.lore.sample.day", ChatFormatting.GRAY, data.day()))));
		return stack;
	}

	// ---- Cultures ---------------------------------------------------------------------------------------------

	public static ItemStack culture(CultureData data, long now) {
		ItemStack stack = new ItemStack(ModItems.CULTURE);
		stack.set(ModComponents.CULTURE, data);
		refreshCulture(stack, now);
		return stack;
	}

	/** Updates the look and tooltip of a culture dish to its current state. Cheap if nothing changed. */
	public static void refreshCulture(ItemStack stack, long now) {
		CultureData data = stack.get(ModComponents.CULTURE);

		if (data == null) {
			return;
		}

		int state = data.state(now);
		CustomModelData model = new CustomModelData(List.of((float) state), List.of(), List.of(), List.of());

		if (model.equals(stack.get(DataComponents.CUSTOM_MODEL_DATA))) {
			return;
		}

		stack.set(DataComponents.CUSTOM_MODEL_DATA, model);
		List<Component> lines = new ArrayList<>();
		lines.add(lore("contagion.lore.culture.donor", ChatFormatting.GRAY, data.source(), data.day()));
		lines.add(switch (state) {
			case CultureData.GROWING -> lore("contagion.lore.culture.growing", ChatFormatting.YELLOW);
			case CultureData.COLONIZED -> lore("contagion.lore.culture.colonized", ChatFormatting.GREEN);
			case CultureData.STERILE -> lore("contagion.lore.culture.sterile", ChatFormatting.GRAY);
			default -> lore("contagion.lore.culture.dead", ChatFormatting.DARK_GRAY);
		});

		stack.set(DataComponents.LORE, new ItemLore(lines));
	}

	// ---- Prepared reagents ------------------------------------------------------------------------------------

	public static ItemStack prepared(Ingredient ingredient) {
		ItemStack stack = new ItemStack(ModItems.PREPARED_REAGENT);
		stack.set(ModComponents.INGREDIENT, ingredient);
		stack.set(DataComponents.ITEM_NAME, ingredient.msg().component());
		// Model: flag = extract (vial) vs. powder, colour = reagent colour.
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(ingredient.form() == Form.EXTRACT), List.of(),
				List.of(0xFF000000 | ingredient.reagent().color())));
		return stack;
	}

	// ---- Serums -----------------------------------------------------------------------------------------------

	public static void updateSerumStack(ItemStack stack, SerumMix mix) {
		stack.set(ModComponents.SERUM, mix);
		List<Ingredient> ingredients = Ingredient.parseAll(mix.reagents());

		List<Component> lines = new ArrayList<>();
		lines.add(mix.sealed()
				? lore("contagion.lore.serum.sealed", ChatFormatting.AQUA)
				: lore("contagion.lore.serum.unsealed", ChatFormatting.YELLOW, ingredients.size(), SerumMix.MAX_REAGENTS));

		for (Ingredient ingredient : ingredients) {
			lines.add(Component.literal(" - ").append(ingredient.msg().component()).withStyle(ChatFormatting.GRAY));
		}

		if (mix.sealed()) {
			lines.add(lore("contagion.lore.serum.sealed_use1", ChatFormatting.DARK_GRAY));
		} else {
			lines.add(lore("contagion.lore.serum.unsealed_use1", ChatFormatting.DARK_GRAY));
			lines.add(lore("contagion.lore.serum.unsealed_use2", ChatFormatting.DARK_GRAY));
		}

		stack.set(DataComponents.LORE, new ItemLore(lines));

		// Drives the item model: float = fill level, flag = sealed (cork + label), colour = liquid tint.
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
				List.of((float) Math.max(1, ingredients.size())),
				List.of(mix.sealed()),
				List.of(),
				List.of(serumColor(ingredients))));
	}

	/** Average of the reagents' colours, slightly brightened so mixtures don't all turn brown. */
	public static int serumColor(List<Ingredient> ingredients) {
		if (ingredients.isEmpty()) {
			return 0xFFFFFFFF;
		}

		int r = 0;
		int g = 0;
		int b = 0;

		for (Ingredient ingredient : ingredients) {
			int color = ingredient.reagent().color();
			r += (color >> 16) & 0xFF;
			g += (color >> 8) & 0xFF;
			b += color & 0xFF;
		}

		int n = ingredients.size();
		r = Math.min(255, r / n + 20);
		g = Math.min(255, g / n + 20);
		b = Math.min(255, b / n + 20);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	// ---- Research findings ------------------------------------------------------------------------------------

	/** Picks a not-yet-known fact about the strain, or null if everything is known. */
	public static @Nullable String newFinding(Strain strain, Research research, RandomSource random) {
		List<String> categories = new ArrayList<>();
		List<String> forms = new ArrayList<>();
		List<String> toxic = new ArrayList<>();
		List<String> inert = new ArrayList<>();

		for (Ingredient ingredient : strain.cureIngredients()) {
			String category = ingredient.reagent().category().name();
			add(categories, research, "cat:" + category);
			add(forms, research, "form:" + category + ":" + ingredient.form().name());
		}

		for (String name : strain.toxic()) {
			add(toxic, research, "toxic:" + name);
		}

		for (Reagent reagent : Reagent.values()) {
			if (!strain.isCureReagent(reagent.name()) && !strain.toxic().contains(reagent.name())) {
				add(inert, research, "inert:" + reagent.name());
			}
		}

		int wCat = categories.isEmpty() ? 0 : 3;
		int wForm = forms.isEmpty() ? 0 : 2;
		int wToxic = toxic.isEmpty() ? 0 : 3;
		int wInert = inert.isEmpty() ? 0 : 4;
		int total = wCat + wForm + wToxic + wInert;

		if (total == 0) {
			return null;
		}

		int roll = random.nextInt(total);
		List<String> pool = roll < wCat ? categories : roll < wCat + wForm ? forms : roll < wCat + wForm + wToxic ? toxic : inert;
		return pool.get(random.nextInt(pool.size()));
	}

	private static void add(List<String> list, Research research, String key) {
		if (!research.knows(key) && !list.contains(key)) {
			list.add(key);
		}
	}

	public static @Nullable Msg describeFinding(String key) {
		String[] parts = key.split(":");

		if (parts.length < 2) {
			return null;
		}

		try {
			return switch (parts[0]) {
				case "cat" -> Msg.of("contagion.fact.category", Reagent.Category.valueOf(parts[1]).msg());
				case "form" -> parts.length < 3 ? null : Msg.of("contagion.fact.form", Reagent.Category.valueOf(parts[1]).msg(), Form.valueOf(parts[2]).msg());
				case "toxic" -> Msg.of("contagion.fact.toxic", Reagent.valueOf(parts[1]).msg());
				case "inert" -> Msg.of("contagion.fact.inert", Reagent.valueOf(parts[1]).msg());
				default -> null;
			};
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	// ---- Thermometer & bars -----------------------------------------------------------------------------------

	public static double temperature(LivingEntity entity) {
		RandomSource random = entity.getRandom();
		Infection infection = Disease.get(entity);
		double base = 36.5 + random.nextDouble() * 0.6;

		double temp = switch (Disease.stage(infection)) {
			case INCUBATION -> Disease.isContagious(infection) ? 36.9 + random.nextDouble() * 0.8 : base;
			case MILD -> 37.7 + random.nextDouble() * 0.9;
			case MODERATE -> 38.5 + random.nextDouble() * 1.0;
			case SEVERE -> 39.3 + random.nextDouble() * 1.1;
			case TERMINAL -> 40.2 + random.nextDouble() * 1.3;
			default -> base;
		};

		if (infection != null && infection.remission() > 0) {
			temp = Math.max(base, temp - 1.3);
		}

		return temp;
	}

	/** 0 = normal, 1 = elevated, 2 = fever, 3 = high fever, 4 = dangerous. */
	public static int temperatureLevel(double c) {
		return c < 37.3 ? 0 : c < 38.0 ? 1 : c < 39.0 ? 2 : c < 40.0 ? 3 : 4;
	}

	public static final String[] TEMPERATURE_LABELS = {"normal", "elevated", "fever", "high", "danger"};
	public static final ChatFormatting[] TEMPERATURE_COLORS = {ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.GOLD, ChatFormatting.RED,
			ChatFormatting.DARK_RED};

	public static Component temperatureReading(LivingEntity entity, boolean self, double c) {
		double f = c * 9.0 / 5.0 + 32.0;
		int level = temperatureLevel(c);
		String label = TEMPERATURE_LABELS[level];
		ChatFormatting color = TEMPERATURE_COLORS[level];

		// Gauge from 35.5 to 41.5 °C, ten segments.
		int segments = (int) Math.round(Math.max(0, Math.min(1, (c - 35.5) / 6.0)) * 10);
		MutableComponent text = (self ? Msg.of("contagion.thermometer.you") : Msg.of("contagion.thermometer.other", entity.getName().getString()))
				.styled(ChatFormatting.WHITE);
		text.append(Component.literal(" " + BAR.repeat(segments)).withStyle(color));
		text.append(Component.literal(BAR.repeat(10 - segments)).withStyle(ChatFormatting.DARK_GRAY));
		text.append(Component.literal(String.format(Locale.ROOT, " %.1f°C / %.1f°F ", c, f)).withStyle(ChatFormatting.WHITE));
		text.append(Msg.of("contagion.thermometer." + label).styled(color));
		return text;
	}

	/** Action-bar progress bar, e.g. for microscope jobs. */
	public static Component progressBar(Msg label, double fraction, long secondsLeft) {
		int filled = (int) Math.round(Math.max(0, Math.min(1, fraction)) * 20);
		MutableComponent text = label.styled(ChatFormatting.AQUA);
		text.append(Component.literal(" " + BAR.repeat(filled)).withStyle(ChatFormatting.GREEN));
		text.append(Component.literal(BAR.repeat(20 - filled)).withStyle(ChatFormatting.DARK_GRAY));
		text.append(Component.literal(" ").append(Msg.of("contagion.progress.left", Math.max(1, secondsLeft)).component()).withStyle(ChatFormatting.GRAY));
		return text;
	}
}
