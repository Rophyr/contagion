package com.contagion;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Simple properties-file config at config/contagion.properties.
 * Missing keys are filled with defaults and written back on startup.
 */
public final class ContagionConfig {
	/** Length of one Minecraft day in ticks. */
	public static long dayLengthTicks = 24000;
	/** Hidden incubation period before the first symptoms (default: 1 Minecraft day). */
	public static long incubationTicks = 24000;
	/** Days from first symptoms until the disease becomes terminal (default: 21). */
	public static int symptomaticDays = 21;
	/** In the terminal stage, max health drops by one heart every this many ticks. */
	public static long terminalStepTicks = 8000;

	/** Permission level needed for /virus infect and /virus outbreak (0 = everyone, needed for hardcore without cheats). */
	public static int infectPermissionLevel = 0;
	/** Permission level for admin/debug commands (status, cure, strain, ...). */
	public static int adminPermissionLevel = 2;

	public static double airborneMultiplier = 1.0;
	public static double contactMultiplier = 1.0;
	public static boolean villagersCanCatch = true;

	/** The pathogen lives in the world: some animals and villagers carry it when they are first loaded. */
	public static boolean worldInfection = true;
	/** Chance that a farm animal (cow, pig, sheep, chicken, rabbit, goat) is a carrier when first loaded. */
	public static double animalCarrierChance = 0.04;
	/** Chance that a villager is already infected when first loaded. */
	public static double villagerCarrierChance = 0.01;
	/** Farm animals can catch it from each other and from players. */
	public static boolean animalsCanCatch = true;
	/** Sick animals are ill for this many days after incubation, then recover (immune) or die. */
	public static int animalIllnessDays = 3;
	/** Chance that a sick animal dies at the end of its illness instead of recovering. */
	public static double animalDeathChance = 0.3;

	/** Sleeping through the night also advances the disease, because time really passes. */
	public static boolean sleepAdvancesDisease = true;
	public static long sleepSkipTicks = 10000;

	/** How long the microscope takes to analyze a blood sample. */
	public static long analysisTicks = 3600;
	/** How long a culture test (serum vs. grown culture) takes in the microscope. */
	public static long cultureTicks = 4800;
	/** How long an inoculated petri dish takes to grow (default: half a Minecraft day). */
	public static long cultureGrowTicks = 12000;
	/** How long a grown culture stays alive afterwards (default: 2 Minecraft days). */
	public static long cultureLifeTicks = 48000;
	/** How long a beaker takes to boil a reagent into an extract. */
	public static long boilTicks = 2400;
	/** A donor's veins need this long to recover between blood draws (default: ~3 draws per day). */
	public static long bloodDrawCooldownTicks = 8000;

	private ContagionConfig() {
	}

	public static long symptomaticTicks() {
		return symptomaticDays * dayLengthTicks;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("contagion.properties");
		Properties props = new Properties();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				props.load(reader);
			} catch (IOException e) {
				ContagionMod.LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}

		dayLengthTicks = getLong(props, "day_length_ticks", dayLengthTicks);
		incubationTicks = getLong(props, "incubation_ticks", incubationTicks);
		symptomaticDays = (int) getLong(props, "symptomatic_days", symptomaticDays);
		terminalStepTicks = Math.max(200, getLong(props, "terminal_step_ticks", terminalStepTicks));
		infectPermissionLevel = (int) getLong(props, "infect_permission_level", infectPermissionLevel);
		adminPermissionLevel = (int) getLong(props, "admin_permission_level", adminPermissionLevel);
		airborneMultiplier = getDouble(props, "airborne_multiplier", airborneMultiplier);
		contactMultiplier = getDouble(props, "contact_multiplier", contactMultiplier);
		villagersCanCatch = getBool(props, "villagers_can_catch", villagersCanCatch);
		worldInfection = getBool(props, "world_infection", worldInfection);
		animalCarrierChance = getDouble(props, "animal_carrier_chance", animalCarrierChance);
		villagerCarrierChance = getDouble(props, "villager_carrier_chance", villagerCarrierChance);
		animalsCanCatch = getBool(props, "animals_can_catch", animalsCanCatch);
		animalIllnessDays = (int) Math.max(1, getLong(props, "animal_illness_days", animalIllnessDays));
		animalDeathChance = getDouble(props, "animal_death_chance", animalDeathChance);
		sleepAdvancesDisease = getBool(props, "sleep_advances_disease", sleepAdvancesDisease);
		sleepSkipTicks = getLong(props, "sleep_skip_ticks", sleepSkipTicks);
		analysisTicks = getLong(props, "analysis_ticks", analysisTicks);
		cultureTicks = getLong(props, "culture_ticks", cultureTicks);
		cultureGrowTicks = getLong(props, "culture_grow_ticks", cultureGrowTicks);
		cultureLifeTicks = getLong(props, "culture_life_ticks", cultureLifeTicks);
		boilTicks = getLong(props, "boil_ticks", boilTicks);
		bloodDrawCooldownTicks = getLong(props, "blood_draw_cooldown_ticks", bloodDrawCooldownTicks);

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path)) {
				writer.write("""
						# Contagion config. Times are in ticks (20 ticks = 1 second, 24000 ticks = 1 Minecraft day).
						# Restart the game/server after editing.
						#
						# infect_permission_level: who can use /virus infect and /virus outbreak.
						#   0 = everyone (works in hardcore singleplayer without cheats), 2 = operators only.
						# admin_permission_level: who can use /virus status, cure, strain, setday, newstrain.
						""");
				writer.write("day_length_ticks=" + dayLengthTicks + "\n");
				writer.write("incubation_ticks=" + incubationTicks + "\n");
				writer.write("symptomatic_days=" + symptomaticDays + "\n");
				writer.write("terminal_step_ticks=" + terminalStepTicks + "\n");
				writer.write("infect_permission_level=" + infectPermissionLevel + "\n");
				writer.write("admin_permission_level=" + adminPermissionLevel + "\n");
				writer.write("airborne_multiplier=" + airborneMultiplier + "\n");
				writer.write("contact_multiplier=" + contactMultiplier + "\n");
				writer.write("villagers_can_catch=" + villagersCanCatch + "\n");
				writer.write("sleep_advances_disease=" + sleepAdvancesDisease + "\n");
				writer.write("sleep_skip_ticks=" + sleepSkipTicks + "\n");
				writer.write("analysis_ticks=" + analysisTicks + "\n");
				writer.write("culture_ticks=" + cultureTicks + "\n");
				writer.write("culture_grow_ticks=" + cultureGrowTicks + "\n");
				writer.write("culture_life_ticks=" + cultureLifeTicks + "\n");
				writer.write("boil_ticks=" + boilTicks + "\n");
				writer.write("blood_draw_cooldown_ticks=" + bloodDrawCooldownTicks + "\n");
			}
		} catch (IOException e) {
			ContagionMod.LOGGER.warn("Could not write {}", path, e);
		}
	}

	private static long getLong(Properties props, String key, long def) {
		try {
			return Long.parseLong(props.getProperty(key, Long.toString(def)).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static double getDouble(Properties props, String key, double def) {
		try {
			return Double.parseDouble(props.getProperty(key, Double.toString(def)).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static boolean getBool(Properties props, String key, boolean def) {
		return Boolean.parseBoolean(props.getProperty(key, Boolean.toString(def)).trim());
	}
}
