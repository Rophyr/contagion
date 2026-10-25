package com.contagion.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.contagion.ContagionConfig;
import com.contagion.data.Infection;
import com.contagion.data.Strain;
import com.contagion.disease.Disease;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Strains;
import com.contagion.disease.Symptoms;
import com.contagion.registry.ModAttachments;
import com.contagion.text.Msg;

/**
 * /virus infect &lt;targets&gt;  - secretly infect (nobody is told)
 * /virus outbreak           - secretly infect one random survivor
 * /virus status [targets]   - admin: show disease state
 * /virus cure &lt;targets&gt;     - admin: cure (no immunity)
 * /virus immunize &lt;targets&gt; - admin: cure and make immune
 * /virus setday &lt;targets&gt; &lt;day&gt; - admin: jump to a symptomatic day (-1 = just infected)
 * /virus strain             - admin: show the strain INCLUDING THE CURE (spoilers)
 * /virus newstrain          - admin: replace the strain and wipe research
 */
public final class ContagionCommands {
	private ContagionCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		Predicate<CommandSourceStack> infectPerm = permission(ContagionConfig.infectPermissionLevel);
		Predicate<CommandSourceStack> adminPerm = permission(ContagionConfig.adminPermissionLevel);

		dispatcher.register(literal("virus")
				.then(literal("infect").requires(infectPerm)
						.then(argument("targets", EntityArgument.entities()).executes(ContagionCommands::infect)))
				.then(literal("outbreak").requires(infectPerm).executes(ContagionCommands::outbreak))
				.then(literal("status").requires(adminPerm)
						.executes(ctx -> status(ctx, null))
						.then(argument("targets", EntityArgument.entities()).executes(ctx -> status(ctx, EntityArgument.getEntities(ctx, "targets")))))
				.then(literal("cure").requires(adminPerm)
						.then(argument("targets", EntityArgument.entities()).executes(ctx -> cure(ctx, false))))
				.then(literal("immunize").requires(adminPerm)
						.then(argument("targets", EntityArgument.entities()).executes(ctx -> cure(ctx, true))))
				.then(literal("setday").requires(adminPerm)
						.then(argument("targets", EntityArgument.entities())
								.then(argument("day", IntegerArgumentType.integer(-1, 100)).executes(ContagionCommands::setDay))))
				.then(literal("strain").requires(adminPerm).executes(ContagionCommands::strain))
				.then(literal("newstrain").requires(adminPerm).executes(ContagionCommands::newStrain)));
	}

	private static Predicate<CommandSourceStack> permission(int level) {
		if (level <= 0) {
			return source -> true;
		}

		Permission permission = switch (level) {
			case 1 -> Permissions.COMMANDS_MODERATOR;
			case 2 -> Permissions.COMMANDS_GAMEMASTER;
			case 3 -> Permissions.COMMANDS_ADMIN;
			default -> Permissions.COMMANDS_OWNER;
		};

		return source -> source.permissions().hasPermission(permission);
	}

	private static int infect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		MinecraftServer server = ctx.getSource().getServer();
		int count = 0;

		for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
			if (entity instanceof LivingEntity living && !(living instanceof ServerPlayer player && player.isSpectator())
					&& Disease.infect(server, living, true)) {
				count++;
			}
		}

		int infected = count;
		// Deliberately vague: the victims are not told anything.
		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.released", infected).styled(ChatFormatting.DARK_GREEN), false);
		ctx.getSource().sendSuccess(ContagionCommands::tip, false);
		return infected;
	}

	private static int outbreak(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		List<ServerPlayer> candidates = new ArrayList<>();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSpectator() && !Disease.isInfected(player) && !Disease.isImmune(player)) {
				candidates.add(player);
			}
		}

		if (candidates.isEmpty()) {
			ctx.getSource().sendFailure(Msg.of("contagion.command.nobody").component());
			return 0;
		}

		ServerPlayer patientZero = candidates.get(server.overworld().getRandom().nextInt(candidates.size()));
		Disease.infect(server, patientZero, true);
		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.outbreak").styled(ChatFormatting.DARK_GREEN), false);
		ctx.getSource().sendSuccess(ContagionCommands::tip, false);
		return 1;
	}

	private static Component tip() {
		return Msg.of("contagion.command.tip").styled(ChatFormatting.GRAY, ChatFormatting.ITALIC);
	}

	private static int status(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		Collection<? extends Entity> list = targets != null ? targets : List.of(source.getEntityOrException());

		for (Entity entity : list) {
			Infection infection = Disease.get(entity);
			Disease.Stage stage = Disease.stage(infection);
			String text;

			if (infection == null || (!infection.infected() && !infection.immune())) {
				text = "healthy";
			} else if (infection.immune()) {
				text = "immune";
			} else {
				text = String.format(Locale.ROOT, "%s, symptomatic day %.2f, viral load %s, remission %ds, treatments %d, health penalty %d",
						stage.name().toLowerCase(Locale.ROOT), Disease.symptomaticDay(infection), Disease.viralLoad(infection) + "/5",
						infection.remission() / 20, infection.treatments(), Symptoms.healthPenalty(infection));
			}

			String name = entity.getName().getString();
			source.sendSuccess(() -> Component.literal(name + ": " + text), false);
		}

		return list.size();
	}

	private static int cure(CommandContext<CommandSourceStack> ctx, boolean immunize) throws CommandSyntaxException {
		int count = 0;

		for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
			if (entity instanceof LivingEntity living) {
				Disease.cure(living, immunize);
				count++;
			}
		}

		int cured = count;
		ctx.getSource().sendSuccess(() -> Msg.of(immunize ? "contagion.command.immunized" : "contagion.command.cured", cured).component(), true);
		return cured;
	}

	private static int setDay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		MinecraftServer server = ctx.getSource().getServer();
		int day = IntegerArgumentType.getInteger(ctx, "day");
		long progress = day < 0 ? 0 : ContagionConfig.incubationTicks + day * ContagionConfig.dayLengthTicks;
		int count = 0;

		for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
			if (entity instanceof LivingEntity living) {
				Strains.getOrCreate(server);
				Infection current = Disease.get(living);
				int lastStage = current != null && current.infected() ? current.lastStage() : 0;
				living.setAttached(ModAttachments.INFECTION, new Infection(true, false, progress, 0, 0, Math.min(lastStage, Disease.stage(Infection.fresh().withProgress(progress, 0)).ordinal())));
				count++;
			}
		}

		int changed = count;
		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.setday", changed, day).component(), true);
		return changed;
	}

	private static int strain(CommandContext<CommandSourceStack> ctx) {
		Strain strain = Strains.get(ctx.getSource().getServer());

		if (strain == null) {
			ctx.getSource().sendFailure(Msg.of("contagion.command.no_strain").component());
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.strain", strain.nameMsg(), strain.profileMsg()).styled(ChatFormatting.RED), false);
		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.spoiler_cure", Ingredient.listMsg(strain.cureIngredients())).styled(ChatFormatting.GREEN), false);
		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.spoiler_toxic", Ingredient.listMsg(Ingredient.parseAll(strain.toxic()))).styled(ChatFormatting.GOLD), false);
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Airborne x%.2f, contact x%.2f", strain.airborne(), strain.contact())), false);
		return 1;
	}

	private static int newStrain(CommandContext<CommandSourceStack> ctx) {
		Strain strain = Strains.regenerate(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Msg.of("contagion.command.mutated", strain.nameMsg()).component(), true);
		return 1;
	}
}
