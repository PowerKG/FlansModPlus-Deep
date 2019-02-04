package me.kg.flansmod.deep.quality;

import com.flansmod.common.guns.ItemGun;
import com.mojang.realmsclient.gui.ChatFormatting;

public class GunQuality {
	public final String name;

	public int damageUp = 0;//
	public int reloadTimeUp = 0;//
	public int shootDelayUp = 0;//
	public int meleeDamageUp = 0;//
	public int meleeTimeUp = 0;
	public int speedUp = 0;//
	public int spreadUp = 0;//
	public int numBulletsUp = 0;//

	public GunQuality(String name) {
		this.name = name;
	}

	public static String genDamage(GunQuality quality) {
		if (quality == null)
			return "";
		return genString(quality.damageUp, false);
	}

	public static String genReloadTime(GunQuality quality) {
		if (quality == null)
			return "";
		return genString(quality.reloadTimeUp, true);
	}

	public static String genMeleeTime(GunQuality quality) {
		if (quality == null)
			return "";
		return genString(quality.meleeTimeUp, true);
	}

	public static String genMeleeDamage(GunQuality quality) {
		if (quality == null)
			return "";
		return genString(quality.meleeDamageUp, false);
	}

	private static String genString(int value, boolean time) {
		if (value == 0)
			return "";

		if (!time)
			return ChatFormatting.YELLOW + (value > 0 ? " +" : " -") + Math.abs(value);
		else
			return ChatFormatting.YELLOW + (value > 0 ? " +" : " -") + ItemGun.roundFloat(Math.abs(value) / 20, 2)
					+ "s";
	}

}
