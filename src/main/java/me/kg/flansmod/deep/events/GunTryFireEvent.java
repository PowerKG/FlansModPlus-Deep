package me.kg.flansmod.deep.events;

import java.util.UUID;

import com.flansmod.common.guns.GunType;

public class GunTryFireEvent extends FlansGunEvent implements ICancelable {

	private boolean canceled = false;

	public GunTryFireEvent(UUID from, GunType gunType) {
		super(from, gunType);
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean flag) {
		this.canceled = flag;
	}

	public static class Reload extends GunTryFireEvent {

		public Reload(UUID from, GunType gunType) {
			super(from, gunType);
		}

	}

	public static class Fire extends GunTryFireEvent {

		public Fire(UUID from, GunType gunType) {
			super(from, gunType);
		}

	}
}
