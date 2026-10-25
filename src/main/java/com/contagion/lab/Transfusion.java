package com.contagion.lab;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import com.contagion.ContagionConfig;
import com.contagion.data.BloodSample;
import com.contagion.data.Infection;
import com.contagion.disease.Disease;
import com.contagion.disease.Vanilla;
import com.contagion.registry.ModAttachments;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * Using a blood sample on someone (while sneaking) transfuses it.
 * <ul>
 *     <li>Infected blood (with enough pathogen) infects the recipient. Useful to keep a test subject, or worse.</li>
 *     <li>Blood from someone who was cured carries antibodies and gives a short remission.</li>
 * </ul>
 * Nobody is told whether it "worked".
 */
public final class Transfusion {
	private Transfusion() {
	}

	public static void transfuse(ServerLevel level, ServerPlayer user, LivingEntity target, BloodSample sample) {
		MinecraftServer server = level.getServer();
		Vanilla.playSound(level, target, "item.bottle.empty", 0.8F, 1.3F);
		Chat.bar(user, Msg.of("contagion.transfuse.done", sample.source(), target.getName().getString()), ChatFormatting.GRAY);

		if (target instanceof ServerPlayer other && other != user) {
			Chat.bar(other, Msg.of("contagion.transfuse.felt", user.getName().getString()), ChatFormatting.GRAY);
		}

		if (!Microscope.isFresh(server, sample.day())) {
			return;
		}

		if (sample.infected() && sample.load() >= 2) {
			Disease.infect(server, target, false);
			return;
		}

		Infection infection = Disease.get(target);

		if (sample.immune() && infection != null && infection.infected()) {
			long remission = (long) (ContagionConfig.dayLengthTicks / 3 * Math.pow(0.7, infection.treatments()));
			target.setAttached(ModAttachments.INFECTION, infection.withTreatment(Math.max(infection.remission(), remission)));
			Chat.send(target, Msg.of("contagion.transfuse.antibodies"), ChatFormatting.GRAY);
		}
	}
}
