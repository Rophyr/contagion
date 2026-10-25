package com.contagion.registry;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.contagion.ContagionMod;
import com.contagion.data.Infection;
import com.contagion.data.Research;
import com.contagion.data.Strain;

public final class ModAttachments {
	/** Disease state on a living entity. Not copied on death. */
	public static final AttachmentType<Infection> INFECTION = AttachmentRegistry.createPersistent(ContagionMod.id("infection"), Infection.CODEC);

	/** The world's strain, on {@code server.globalAttachments()}. Absent until the first infection. */
	public static final AttachmentType<Strain> STRAIN = AttachmentRegistry.createPersistent(ContagionMod.id("strain"), Strain.CODEC);

	/** Shared research progress, on {@code server.globalAttachments()}. */
	public static final AttachmentType<Research> RESEARCH = AttachmentRegistry.create(ContagionMod.id("research"),
			builder -> builder.persistent(Research.CODEC).initializer(() -> Research.EMPTY));

	private ModAttachments() {
	}

	public static void init() {
		// Loads the class, which registers the attachment types.
	}
}
