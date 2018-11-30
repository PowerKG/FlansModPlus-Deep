package me.kg.flansmod.deep.quality;

import java.util.HashMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class QualityManager {
	private static HashMap<String, GunQuality> qualities = new HashMap<String, GunQuality>();

	public static void clearQualities() {
		qualities.clear();
	}

	public static void registerQuality(String name, GunQuality quality) {
		qualities.put(name, quality);
	}

	public static GunQuality findQuality(String qualityName) {
		return qualities.get(qualityName);
	}

}
