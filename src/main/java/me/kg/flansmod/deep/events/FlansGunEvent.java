package me.kg.flansmod.deep.events;

import java.util.UUID;

import com.flansmod.common.guns.GunType;

public class FlansGunEvent {
	public final UUID from;
	public final GunType gunType;

	public FlansGunEvent(UUID from, GunType gunType) {
		this.from = from;
		this.gunType = gunType;
	}

}
