package com.contagion.disease;

import java.util.Locale;

import com.contagion.text.Msg;

/** How a reagent was prepared before going into a serum. Each active agent of the cure only works in one form. */
public enum Form {
	RAW,
	GROUND,
	EXTRACT;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** "raw" / "ground" / "boiled into an extract" as a phrase. */
	public Msg msg() {
		return Msg.of("contagion.form." + id());
	}

	public static Form byName(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return RAW;
		}
	}
}
