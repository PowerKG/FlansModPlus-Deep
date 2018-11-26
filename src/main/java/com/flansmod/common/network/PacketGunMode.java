package com.flansmod.common.network;

import com.flansmod.client.FlansModClient;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.guns.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import com.flansmod.common.FlansMod;

public class PacketGunMode extends PacketBase 
{
	private int handle = 0;

	/** Only server to client */
	public EnumFireMode mode = EnumFireMode.SEMIAUTO;
	public boolean isInSecondary;
	
	public PacketGunMode() {}

	public PacketGunMode(int aHandle)
	{
		handle = aHandle;
	}
	
	public PacketGunMode(EnumFireMode md)
	{
		this.mode = md;
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		data.writeByte(this.mode.ordinal());
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		int i = data.readByte();
		if(i < EnumFireMode.values().length)
		{
			this.mode = EnumFireMode.values()[i];
		}
	}
	
	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) 
	{
		ItemStack itemStack = playerEntity.inventory.getCurrentItem();

		if(handle == 1)
		{
			if(itemStack != null && itemStack.getItem() instanceof ItemGun)
			{
				ItemGun gun = (ItemGun) itemStack.getItem();
				EnumFireMode currentMode = gun.type.getFireMode(itemStack);
				EnumFireMode nextMode = currentMode;
				EnumFireMode[] submode = gun.type.submode;
				for( int i=0; i<submode.length; i++ )
				{
					if(currentMode == submode[i])
					{
						nextMode = submode[ (i + 1) % submode.length ];
						break;
					}
				}

				if(currentMode != nextMode)
				{
//					FlansMod.log("[Server]Switched the gun mode : " + currentMode + " -> " + nextMode);
					gun.type.setFireMode(itemStack, nextMode.ordinal());
					//		playerEntity.worldObj.playSoundEffect(
					//				playerEntity.posX+0.5,
					//				playerEntity.posY+0.5,
					//				playerEntity.posZ+0.5,
					//				"random.click", 2.3F, 1.0F);
					FlansMod.getPacketHandler().sendTo(new PacketGunMode(nextMode), playerEntity);
				}
//					FlansMod.log("[Server]Do not switch the gun mode : " + currentMode);
			}
		}
		else
		{
			//Do not toggle whilst gun is reloading
			PlayerData data = PlayerHandler.getPlayerData(playerEntity);
			if(data.shootTimeLeft <= 0)
			{
				if(itemStack != null && itemStack.getItem() instanceof ItemGun)
				{
					GunType type = ((ItemGun)itemStack.getItem()).type;
					AttachmentType attachment = type.getGrip(itemStack);

					if(attachment != null && attachment.secondaryFire)
					{
						boolean mode = type.getSecondaryFire(itemStack);
						((ItemGun)itemStack.getItem()).type.setSecondaryFire(itemStack, !mode);

						if(attachment.toggleSound != null)
							PacketPlaySound.sendSoundPacket(playerEntity.posX, playerEntity.posY, playerEntity.posZ, type.reloadSoundRange, playerEntity.dimension, attachment.toggleSound, true);
					}
				}
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleClientSide(EntityPlayer clientPlayer) 
	{
		ItemStack itemStack = clientPlayer.inventory.getCurrentItem();

		if(handle == 1)
		{
			if(itemStack != null && itemStack.getItem() instanceof ItemGun)
			{
//				FlansMod.log("[Client]Switched the gun mode : " + this.mode);
				((ItemGun)itemStack.getItem()).type.setFireMode(itemStack, this.mode.ordinal());
			}
		}
		else
		{
			//Do not toggle whilst gun is reloading
			if(FlansModClient.shootTimeLeft <= 0)
			{
				GunType type = ((ItemGun)itemStack.getItem()).type;
				AttachmentType attachment = type.getGrip(itemStack);

				if(attachment != null && attachment.secondaryFire)
				{
					boolean mode = type.getSecondaryFire(itemStack);
					((ItemGun)itemStack.getItem()).type.setSecondaryFire(itemStack, !mode);
				}
			}
		}
	}
}
