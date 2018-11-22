package me.kg.flansmod.deep.events;

import java.util.UUID;

import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ShootableType;

public class GunFireEvent extends GunEvent implements ICancelable {

	public final ShootableType bulletType;

	private int numBullets;
	private float spread, speed, damage;

	private boolean canceled = false;

	public GunFireEvent(UUID fireFrom, GunType gunType, ShootableType bulletType, int numBullets, float spread,
			float speed, float damage) {
		super(fireFrom, gunType);
		this.bulletType = bulletType;
		this.numBullets = numBullets;
		this.spread = spread;
	}

	public int getNumBullets() {
		return numBullets;
	}

	public void setNumBullets(int amount) {
		this.numBullets = amount;
	}

	public float getSpread() {
		return spread;
	}

	public void setSpread(float spread) {
		this.spread = spread;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public float getDamage() {
		return damage;
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean flag) {
		this.canceled = flag;
	}

}
