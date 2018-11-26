package com.flansmod.client.model;

import com.flansmod.common.guns.*;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.event.RenderHandEvent;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.paintjob.Paintjob;
import com.flansmod.common.vector.Vector3f;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

public class RenderGun implements IItemRenderer
{
	private static TextureManager renderEngine;

	public static float smoothing;
    
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type)
	{
		switch(type)
		{
		case ENTITY : if(!Minecraft.getMinecraft().gameSettings.fancyGraphics) return false;
		case EQUIPPED : case EQUIPPED_FIRST_PERSON :  /*case INVENTORY : */return item != null && item.getItem() instanceof ItemGun && ((ItemGun)item.getItem()).type.model != null;
		default : break;
		}
		return false;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
	{
		return false;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data)
	{
		//Avoid any broken cases by returning
		if(!(item.getItem() instanceof ItemGun))
			return;

		RenderBlocks renderBlocks = (RenderBlocks)data[0];



		GunType gunType = ((ItemGun)item.getItem()).type;
		if(gunType == null)
			return;

		ModelGun model = gunType.model;
		if(model == null)
			return;

		//Render main hand gun
		{
			GunAnimations animations = type == ItemRenderType.ENTITY ? new GunAnimations() : FlansModClient.getGunAnimations((EntityLivingBase)data[1], false);
			renderGun(type, item, gunType, animations, false, data);
		}

		//Render off-hand gun
		if(gunType.oneHanded && type == ItemRenderType.EQUIPPED_FIRST_PERSON)
		{
			EntityLivingBase entity = (EntityLivingBase)data[1];
			if(entity instanceof EntityPlayer)
			{
				EntityPlayer player = (EntityPlayer)entity;
				PlayerData playerData = PlayerHandler.getPlayerData(player, Side.CLIENT);
				if(playerData.offHandGunSlot != 0)
				{
					GunAnimations animations = FlansModClient.gunAnimationsLeft.get(data[1]);
					if(animations == null)
					{
						animations = new GunAnimations();
						FlansModClient.gunAnimationsLeft.put((EntityLivingBase)data[1], animations);
					}
					ItemStack offHandItem = player.inventory.getStackInSlot(playerData.offHandGunSlot - 1);
					if(offHandItem == null || !(offHandItem.getItem() instanceof ItemGun))
						return;
					GunType offHandGunType = ((ItemGun)offHandItem.getItem()).type;
					if(!offHandGunType.oneHanded)
						return;

					renderGun(type, offHandItem, offHandGunType, animations, true, data);
				}

			}
		}
	}

	//Render off-hand gun in 3rd person
	public void renderOffHandGun(EntityPlayer player, ItemStack offHandItemStack)
	{
		GunAnimations animations = FlansModClient.gunAnimationsLeft.get(player);
		if(animations == null)
		{
			animations = new GunAnimations();
			FlansModClient.gunAnimationsLeft.put(player, animations);
		}
		GunType offHandGunType = ((ItemGun)offHandItemStack.getItem()).type;
		if(!offHandGunType.oneHanded)
			return;

		renderGun(ItemRenderType.EQUIPPED, offHandItemStack, offHandGunType, animations, true, player);
	}

	private void renderGun(ItemRenderType type, ItemStack item, GunType gunType, GunAnimations animations, boolean offHand, Object... data)
	{
		//The model scale
		float f = 1F / 16F;
		ModelGun model = gunType.model;

		int flip = offHand ? -1 : 1;

		GL11.glPushMatrix();
		{
			//Get the reload animation rotation
			float reloadRotate = 0F;

			//Setup transforms based on gun position
			switch(type)
			{
				case ENTITY :
				{
					EntityItem entity = (EntityItem)data[1];
					GL11.glRotatef(entity.age + (entity.age == 0 ? 0 : smoothing), 0F, 1F, 0F);
					GL11.glTranslatef(-0.2F + model.itemFrameOffset.x, 0.2F + model.itemFrameOffset.y, 0.1F + model.itemFrameOffset.z);
					break;
				}
				case EQUIPPED:
				{
					if(offHand)
					{
						GL11.glRotatef(-70F, 1F, 0F, 0F);
						GL11.glRotatef(48F, 0F, 0F, 1F);
						GL11.glRotatef(105F, 0F, 1F, 0F);
						GL11.glTranslatef(-0.1F, -0.22F, -0.15F);
					}
					else
					{
						GL11.glRotatef(35F, 0F, 0F, 1F);
						GL11.glRotatef(-5F, 0F, 1F, 0F);
						GL11.glTranslatef(0.75F, -0.22F, -0.08F);
						GL11.glScalef(1F, 1F, -1F);
					}
					GL11.glTranslatef(model.thirdPersonOffset.x, model.thirdPersonOffset.y, model.thirdPersonOffset.z);
					/*
					if(animations.meleeAnimationProgress > 0 && animations.meleeAnimationProgress < gunType.meleePath.size())
					{
						Vector3f meleePos = gunType.meleePath.get(animations.meleeAnimationProgress);
						Vector3f nextMeleePos = animations.meleeAnimationProgress + 1 < gunType.meleePath.size() ? gunType.meleePath.get(animations.meleeAnimationProgress + 1) : new Vector3f();
						GL11.glTranslatef(meleePos.x + (nextMeleePos.x - meleePos.x) * smoothing, meleePos.y + (nextMeleePos.y - meleePos.y) * smoothing, meleePos.z + (nextMeleePos.z - meleePos.z) * smoothing);
					}
					*/
					break;
				}
				case EQUIPPED_FIRST_PERSON:
				{
					IScope scope = gunType.getCurrentScope(item);
					if(FlansModClient.zoomProgress > 0.9F && scope.hasZoomOverlay())
					{
						GL11.glPopMatrix();
						return;
					}
					float adsSwitch = FlansModClient.lastZoomProgress + (FlansModClient.zoomProgress - FlansModClient.lastZoomProgress) * smoothing;//0F;//((float)Math.sin((FlansMod.ticker) / 10F) + 1F) / 2F;
					if(offHand)
					{
						GL11.glTranslatef(0F, 0.03F, -0.76F);
						GL11.glRotatef(23F, 0F, 0F, 1F);
						GL11.glRotatef(-4F, 0F, 1F, 0F);
						GL11.glTranslatef(0.15F, 0.2F, -0.6F);
					}
					else if(FlansModClient.zoomProgress + 0.1F > 0.9F && ItemGun.crouching && !animations.reloading)
					{
						GL11.glRotatef(25F - 5F * adsSwitch, 0F, 0F, 1F);
						GL11.glRotatef(-5F, 0F, 1F, 0F);
						GL11.glTranslatef(0.15F, 0.2F + 0.175F * adsSwitch, -0.6F - 0.405F * adsSwitch);
						if(gunType.hasScopeOverlay)
							GL11.glTranslatef(-0.3F * adsSwitch, 0F, 0F);
						GL11.glRotatef(4.5F * adsSwitch, 0F, 0F, 1F);
						//forward, up, sideways
						GL11.glTranslatef(model.crouchZoom, -0.03F * adsSwitch, 0F);
					}
					else if(FlansModClient.zoomProgress + 0.1F < 0.2F && ItemGun.sprinting && !animations.reloading && !ItemGun.shooting && model.fancyStance)
					{
						GL11.glRotatef(25F - 5F * adsSwitch + model.stanceRotate.z, 0F, 0F, 1F);
						// left/right on length == left/right on height == null == down/up
						GL11.glRotatef(-5F + model.stanceRotate.x, 0F + model.stanceRotate.y, 1F, -0.0F);
						GL11.glTranslatef(0.15F, 0.2F + 0.175F * adsSwitch, -0.6F - 0.405F * adsSwitch);
						if(gunType.hasScopeOverlay)
							GL11.glTranslatef(-0.3F * adsSwitch, 0F, 0F);
						GL11.glRotatef(4.5F * adsSwitch, 0F, 0F, 1F);
						//forward, up, sideways
						GL11.glTranslatef(0.0F + model.stanceTranslate.x, -0.03F * adsSwitch + model.stanceTranslate.y, 0F + model.stanceTranslate.z);
					}
					else
					{
						GL11.glRotatef(25F - 5F * adsSwitch, 0F, 0F, 1F);
						GL11.glRotatef(-5F, 0F, 1F, 0F);
						GL11.glTranslatef(0.15F, 0.2F + 0.175F * adsSwitch, -0.6F - 0.405F * adsSwitch);
						if(gunType.hasScopeOverlay)
							GL11.glTranslatef(-0.3F * adsSwitch, 0F, 0F);
						GL11.glRotatef(4.5F * adsSwitch, 0F, 0F, 1F);
						GL11.glTranslatef(-0.0F, -0.03F * adsSwitch, 0F);
					}

					if(animations.meleeAnimationProgress > 0 && animations.meleeAnimationProgress < gunType.meleePath.size())
					{
						Vector3f meleePos = gunType.meleePath.get(animations.meleeAnimationProgress);
						Vector3f nextMeleePos = animations.meleeAnimationProgress + 1 < gunType.meleePath.size() ? gunType.meleePath.get(animations.meleeAnimationProgress + 1) : new Vector3f();
						GL11.glTranslatef(meleePos.x + (nextMeleePos.x - meleePos.x) * smoothing, meleePos.y + (nextMeleePos.y - meleePos.y) * smoothing, meleePos.z + (nextMeleePos.z - meleePos.z) * smoothing);
						Vector3f meleeAngles = gunType.meleePathAngles.get(animations.meleeAnimationProgress);
						Vector3f nextMeleeAngles = animations.meleeAnimationProgress + 1 < gunType.meleePathAngles.size() ? gunType.meleePathAngles.get(animations.meleeAnimationProgress + 1) : new Vector3f();
						GL11.glRotatef(meleeAngles.y + (nextMeleeAngles.y - meleeAngles.y) * smoothing, 0F, 1F, 0F);
						GL11.glRotatef(meleeAngles.z + (nextMeleeAngles.z - meleeAngles.z) * smoothing, 0F, 0F, 1F);
						GL11.glRotatef(meleeAngles.x + (nextMeleeAngles.x - meleeAngles.x) * smoothing, 1F, 0F, 0F);

					}

					if(model.spinningCocking)
					{
						GL11.glTranslatef(model.spinPoint.x, model.spinPoint.y, model.spinPoint.z);
						float pumped = (animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing);
						GL11.glRotatef(pumped * 180F + 180F, 0F, 0F, 1F);
						GL11.glTranslatef(-model.spinPoint.x, -model.spinPoint.y, -model.spinPoint.z);
					}

					if(animations.reloading)
					{
						EnumAnimationType anim = model.animationType;
						if(gunType.getGrip(item) != null && gunType.getSecondaryFire(item))
							anim = gunType.getGrip(item).model.secondaryAnimType;

						//Calculate the amount of tilt required for the reloading animation
						float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
						reloadRotate = 1F;
						if(effectiveReloadAnimationProgress < model.tiltGunTime)
							reloadRotate = effectiveReloadAnimationProgress / model.tiltGunTime;
						if(effectiveReloadAnimationProgress > model.tiltGunTime + model.unloadClipTime + model.loadClipTime)
							reloadRotate = 1F - (effectiveReloadAnimationProgress - (model.tiltGunTime + model.unloadClipTime + model.loadClipTime)) / model.untiltGunTime;

						//Rotate the gun dependent on the animation type
						switch(anim)
						{
							case BOTTOM_CLIP : case PISTOL_CLIP : case SHOTGUN : case END_LOADED :
							{
								GL11.glRotatef(60F * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(30F * reloadRotate * flip, 1F, 0F, 0F);
								GL11.glTranslatef(0.25F * reloadRotate, 0F, 0F);
								break;
							}
							case CUSTOMBOTTOM_CLIP : case CUSTOMPISTOL_CLIP : case CUSTOMSHOTGUN : case CUSTOMEND_LOADED :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case BACK_LOADED :
							{
								GL11.glRotatef(-75F * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(-30F * reloadRotate * flip, 1F, 0F, 0F);
								GL11.glTranslatef(0.5F * reloadRotate, 0F, 0F);
								break;
							}
							case CUSTOMBACK_LOADED :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case BULLPUP :
							{
								GL11.glRotatef(70F * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(10F * reloadRotate * flip, 1F, 0F, 0F);
								GL11.glTranslatef(0.5F * reloadRotate, -0.2F * reloadRotate, 0F);
								break;
							}
							case CUSTOMBULLPUP :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case RIFLE :
							{
								GL11.glRotatef(30F * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(-30F * reloadRotate * flip, 1F, 0F, 0F);
								GL11.glTranslatef(0.5F * reloadRotate, 0F, -0.5F * reloadRotate);
								break;
							}
							//CUSTOMRIFLE allows you to customize gun tilt & rotation while maintaining the specialized reload
							case CUSTOMRIFLE :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case RIFLE_TOP : case REVOLVER :
							{
								GL11.glRotatef(30F * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(10F * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(-10F * reloadRotate * flip, 1F, 0F, 0F);
								GL11.glTranslatef(0.1F * reloadRotate, -0.2F * reloadRotate, -0.1F * reloadRotate);
								break;
							}
							case CUSTOMRIFLE_TOP : case CUSTOMREVOLVER :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case REVOLVER2 :
							{
								GL11.glRotatef(20F * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(-10F * reloadRotate * flip, 1F, 0F, 0F);
								break;
							}
							case CUSTOMREVOLVER2 :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case ALT_PISTOL_CLIP :
							{
								GL11.glRotatef(60F * reloadRotate * flip, 0F, 1F, 0F);
								GL11.glTranslatef(0.15F * reloadRotate, 0.25F * reloadRotate, 0F);
								break;
							}
							case CUSTOMALT_PISTOL_CLIP :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case STRIKER :
							{
								GL11.glRotatef(-35F * reloadRotate * flip, 1F, 0F, 0F);
								GL11.glTranslatef(0.2F * reloadRotate, 0F, -0.1F * reloadRotate);
								break;
							}
							case CUSTOMSTRIKER :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case GENERIC :
							{
								//Gun reloads partly or completely off-screen.
								GL11.glRotatef(45F * reloadRotate, 0F, 0F, 1F);
								GL11.glTranslatef(-0.2F * reloadRotate, -0.5F * reloadRotate, 0F);
								break;
							}
							case CUSTOMGENERIC :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							case CUSTOM :
							{
								GL11.glRotatef(model.rotateGunVertical * reloadRotate, 0F, 0F, 1F);
								GL11.glRotatef(model.rotateGunHorizontal * reloadRotate, 0F, 1F, 0F);
								GL11.glRotatef(model.tiltGun * reloadRotate, 1F, 0F, 0F);
								GL11.glTranslatef(model.translateGun.x * reloadRotate,  model.translateGun.y * reloadRotate, model.translateGun.z * reloadRotate);
								break;
							}
							default : break;
						}
					}
					break;
				}
				default : break;
			}

			renderGun(item, gunType, f, model, animations, reloadRotate, type);
		}
		GL11.glPopMatrix();
	}

	/** Gun render method, seperated from transforms so that mecha renderer may also call this */
	public void renderGun(ItemStack item, GunType type, float f, ModelGun model, GunAnimations animations, float reloadRotate, ItemRenderType rtype)
	{
		//Make sure we actually have the renderEngine
		if(renderEngine == null)
			renderEngine = Minecraft.getMinecraft().renderEngine;

		//If we have no animation variables, use defaults
		if(animations == null)
			animations = GunAnimations.defaults;

		//Get all the attachments that we may need to render
		AttachmentType scopeAttachment = type.getScope(item);
		AttachmentType barrelAttachment = type.getBarrel(item);
		AttachmentType stockAttachment = type.getStock(item);
		AttachmentType gripAttachment = type.getGrip(item);
		AttachmentType gadgetAttachment = type.getGadget(item);
		AttachmentType slideAttachment = type.getSlide(item);
		AttachmentType pumpAttachment = type.getPump(item);
		AttachmentType accessoryAttachment = type.getAccessory(item);

		ItemStack scopeItemStack = type.getScopeItemStack(item);
		ItemStack barrelItemStack = type.getBarrelItemStack(item);
		ItemStack stockItemStack = type.getStockItemStack(item);
		ItemStack gripItemStack = type.getGripItemStack(item);
		ItemStack gadgetItemStack = type.getGadgetItemStack(item);
		ItemStack slideItemStack = type.getSlideItemStack(item);
		ItemStack pumpItemStack = type.getPumpItemStack(item);
		ItemStack accessoryItemStack = type.getAccessoryItemStack(item);

		GL11.glPushMatrix();
		if(rtype == ItemRenderType.EQUIPPED_FIRST_PERSON)
		{
			GL11.glTranslatef(0F, 0, 0);

			//Gun recoil
            GL11.glTranslatef(-(animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * getRecoilDistance(gripAttachment, type, item), 0F, 0F);
            GL11.glRotatef(-(animations.lastGunRecoil + (animations.gunRecoil - animations.lastGunRecoil) * smoothing) * getRecoilAngle(gripAttachment, type, item), 0F,0F,1F);

            //Do not move gun when there's a pump in the reload
			if(model.animationType == EnumAnimationType.SHOTGUN && !animations.reloading)
			{
				GL11.glRotatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * -5F, 0F, 1F, 0F);
				GL11.glRotatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * 5F, 1F, 0F, 0F);
			}

			if(model.isSingleAction)
			{
				GL11.glRotatef(-(1 - Math.abs(animations.lastGunPullback + (animations.gunPullback - animations.lastGunPullback) * smoothing)) * -5F, 0F,0F,1F);
				GL11.glRotatef(-(1 - Math.abs(animations.lastGunPullback + (animations.gunPullback - animations.lastGunPullback) * smoothing)) * 2.5F, 1F,0F,0F);
			}
		}

		ItemStack[] bulletStacks = new ItemStack[type.getNumAmmoItemsInGun(item)];
		boolean empty = true;
		for(int i = 0; i < type.getNumAmmoItemsInGun(item); i++)
		{
			bulletStacks[i] = ((ItemGun)item.getItem()).getBulletItemStack(item, i);
			if(bulletStacks[i] != null && bulletStacks[i].getItem() instanceof ItemShootable && bulletStacks[i].getItemDamage() < bulletStacks[i].getMaxDamage())
				empty = false;
		}

		//Sanity check for empty guns
		if(model.slideLockOnEmpty)
		{
			if(empty)
				animations.onGunEmpty(true);
			else if(!empty && !animations.reloading)
				animations.onGunEmpty(false);
		}

		//Load texture
		if(rtype == ItemRenderType.EQUIPPED_FIRST_PERSON && model.hasArms)
		{
			Minecraft mc = Minecraft.getMinecraft();
			renderFirstPersonArm(mc.thePlayer, model, animations);
		}
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(type.getPaintjob(item.getItemDamage())));

		//This allows you to offset your gun with a sight attached to properly align the aiming reticle
		//Can be adjusted per scope and per gun
		if(scopeAttachment != null && model.gunOffset != 0)
			GL11.glTranslatef(0F, -scopeAttachment.model.renderOffset + model.gunOffset / 16F, 0F);

		//Render the gun and default attachment models
		GL11.glPushMatrix();
		{
			GL11.glScalef(type.modelScale, type.modelScale, type.modelScale);

			model.renderGun(f);
			//Render any default attachments
			if(scopeAttachment == null && !model.scopeIsOnSlide && !model.scopeIsOnBreakAction)
				model.renderDefaultScope(f);
			if(barrelAttachment == null)
				model.renderDefaultBarrel(f);
			if(stockAttachment == null)
				model.renderDefaultStock(f);
			if(gripAttachment == null && !model.gripIsOnPump)
				model.renderDefaultGrip(f);
			if(gadgetAttachment == null && !model.gadgetIsOnPump)
				model.renderDefaultGadget(f);
			
			
			//Option to offset flash location with a barrel attachment (location + offset = new location)
			if(animations.muzzleFlashTime > 0 && !type.getSecondaryFire(item))
			{
				GL11.glPushMatrix();
				if(barrelAttachment != null)
					GL11.glTranslatef(model.muzzleFlashPoint.x + model.attachmentFlashOffset.x, model.muzzleFlashPoint.y + model.attachmentFlashOffset.y, model.muzzleFlashPoint.z + model.attachmentFlashOffset.z);
				else
					GL11.glTranslatef(model.muzzleFlashPoint.x, model.muzzleFlashPoint.y, model.muzzleFlashPoint.z);
				model.renderFlash(f, animations.flashInt);
				GL11.glPopMatrix();
			}

			//Render various shoot / reload animated parts
			//Render the slide and charge action
			if(slideAttachment == null && !type.getSecondaryFire(item))
			{
				GL11.glPushMatrix();
				{
					GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
					GL11.glTranslatef(-(1 - Math.abs(animations.lastCharged + (animations.charged - animations.lastCharged) * smoothing)) * model.chargeHandleDistance, 0F, 0F);
					model.renderSlide(f);
					if(scopeAttachment == null && model.scopeIsOnSlide)
						model.renderDefaultScope(f);
				}
				GL11.glPopMatrix();
			}

			//Render the alternate slide
			if(slideAttachment == null && !type.getSecondaryFire(item))
			{
				GL11.glPushMatrix();
				{
					GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.altgunSlideDistance, 0F, 0F);
					model.renderaltSlide(f);
					//if(scopeAttachment == null && model.scopeIsOnSlide)
						//model.renderDefaultScope(f);
				}
				GL11.glPopMatrix();
			}
			
            //Render casing ejection (Willy + Gold Testing)
			//Only render in first person
			if(rtype == ItemRenderType.EQUIPPED_FIRST_PERSON && FlansMod.casingEnable && !type.getSecondaryFire(item))
			{
                GL11.glPushMatrix();
                
                float casingProg = (float) (animations.lastCasingStage + (animations.casingStage - animations.lastCasingStage) * smoothing)/model.casingAnimTime;
                if (casingProg >= 1)
                    casingProg = 0;
                float moveX = model.casingAnimDistance.x + (animations.casingRandom.x * model.casingAnimSpread.x);
                float moveY = model.casingAnimDistance.y + (animations.casingRandom.y * model.casingAnimSpread.y);
                float moveZ = model.casingAnimDistance.z + (animations.casingRandom.z * model.casingAnimSpread.z);
                
                GL11.glTranslatef(casingProg * moveX, casingProg * moveY,  casingProg * moveZ);
                GL11.glRotatef(casingProg*180, model.casingRotateVector.x, model.casingRotateVector.y, model.casingRotateVector.z);
                model.renderCasing(f);

                GL11.glPopMatrix();
            }

			//Render the break action
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
				GL11.glRotatef(reloadRotate * -model.breakAngle, 0F, 0F, 1F);
				GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
				model.renderBreakAction(f);
				if(scopeAttachment == null && model.scopeIsOnBreakAction)
					model.renderDefaultScope(f);
			}
			GL11.glPopMatrix();
			
			//Render the alternate break action
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(model.altbarrelBreakPoint.x, model.altbarrelBreakPoint.y, model.altbarrelBreakPoint.z);
				GL11.glRotatef(reloadRotate * -model.altbreakAngle, 0F, 0F, 1F);
				GL11.glTranslatef(-model.altbarrelBreakPoint.x, -model.altbarrelBreakPoint.y, -model.altbarrelBreakPoint.z);
				model.renderaltBreakAction(f);
				//if(scopeAttachment == null && model.scopeIsOnBreakAction)
					//model.renderDefaultScope(f);
			}
			GL11.glPopMatrix();

			//Render the hammer
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(model.hammerSpinPoint.x, model.hammerSpinPoint.y, model.hammerSpinPoint.z);
				GL11.glRotatef(-animations.hammerRotation, 0F, 0F, 1F);
				GL11.glTranslatef(-model.hammerSpinPoint.x, -model.hammerSpinPoint.y, -model.hammerSpinPoint.z);
				model.renderHammer(f);
			}
			GL11.glPopMatrix();

			//Render the alternate hammer
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(model.althammerSpinPoint.x, model.althammerSpinPoint.y, model.althammerSpinPoint.z);
				GL11.glRotatef(-animations.althammerRotation, 0F, 0F, 1F);
				GL11.glTranslatef(-model.althammerSpinPoint.x, -model.althammerSpinPoint.y, -model.althammerSpinPoint.z);
				model.renderaltHammer(f);
			}
			GL11.glPopMatrix();
			
			//Render the pump-action handle
			if(pumpAttachment == null)
			{
				GL11.glPushMatrix();
				{
					GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
					GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
					model.renderPump(f);
					if(gripAttachment == null && model.gripIsOnPump)
						model.renderDefaultGrip(f);
					if(gadgetAttachment == null && model.gadgetIsOnPump)
						model.renderDefaultGadget(f);
				}
				GL11.glPopMatrix();
			}
			
			//Render the alternate pump-action handle
			if(pumpAttachment == null)
			{
				GL11.glPushMatrix();
				{

					GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
					float pumped = (animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing);
					model.renderaltPump(f);
					if(gripAttachment == null && model.gripIsOnPump)
						model.renderDefaultGrip(f);
					if(gadgetAttachment == null && model.gadgetIsOnPump)
						model.renderDefaultGadget(f);
				}
				GL11.glPopMatrix();
			}
			
			//Render the charge handle
			if(model.chargeHandleDistance != 0F)
			{
				GL11.glPushMatrix();
				{
					GL11.glTranslatef(-(1 - Math.abs(animations.lastCharged + (animations.charged - animations.lastCharged) * smoothing)) * model.chargeHandleDistance, 0F, 0F);
					model.renderCharge(f);
				}
				GL11.glPopMatrix();
			}

			//Render the minigun barrels
			if(type.mode == EnumFireMode.MINIGUN)
			{
				GL11.glPushMatrix();
				GL11.glTranslatef(model.minigunBarrelOrigin.x, model.minigunBarrelOrigin.y, model.minigunBarrelOrigin.z);
				GL11.glRotatef(animations.minigunBarrelRotation, 1F, 0F, 0F);
				GL11.glTranslatef(-model.minigunBarrelOrigin.x, -model.minigunBarrelOrigin.y, -model.minigunBarrelOrigin.z);
				model.renderMinigunBarrel(f);
				GL11.glPopMatrix();
			}

			//Render the cocking handle

			//Render the revolver barrel
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
				GL11.glRotatef(reloadRotate * model.revolverFlipAngle, 1F, 0F, 0F);
				GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
				model.renderRevolverBarrel(f);
			}
			GL11.glPopMatrix();

			//Render the revolver2 barrel
			GL11.glPushMatrix();
			{
				GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
				GL11.glRotatef(reloadRotate * model.revolverFlipAngle, -1F, 0F, 0F);
				GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
				model.renderRevolver2Barrel(f);
			}
			GL11.glPopMatrix();

			//Render the clip
			GL11.glPushMatrix();
			{
				boolean shouldRender = true;

				EnumAnimationType anim = model.animationType;
				if(gripAttachment != null && type.getSecondaryFire(item))
					anim = gripAttachment.model.secondaryAnimType;

				float tiltGunTime = model.tiltGunTime, unloadClipTime = model.unloadClipTime, loadClipTime = model.loadClipTime;
				if(gripAttachment != null && type.getSecondaryFire(item))
				{
					tiltGunTime = gripAttachment.model.tiltGunTime;
					unloadClipTime = gripAttachment.model.unloadClipTime;
					loadClipTime = gripAttachment.model.loadClipTime;
				}

				//Check to see if the ammo should be rendered first
				switch(anim)
				{
					case END_LOADED : case BACK_LOADED :
					{
						if(empty)
							shouldRender = false;
						break;
					}
					default: break;
				}
				//If it should be rendered, do the transformations required
				if(shouldRender && animations.reloading && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0)
				{
					//Calculate the amount of tilt required for the reloading animation
					float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
					float clipPosition = 0F;
					if(effectiveReloadAnimationProgress > tiltGunTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime)
						clipPosition = (effectiveReloadAnimationProgress - tiltGunTime) / unloadClipTime;
					if(effectiveReloadAnimationProgress >= tiltGunTime + unloadClipTime && effectiveReloadAnimationProgress < tiltGunTime + unloadClipTime + loadClipTime)
						clipPosition = 1F - (effectiveReloadAnimationProgress - (tiltGunTime + unloadClipTime)) / loadClipTime;
					float loadOnlyClipPosition = Math.max(0F, Math.min(1F, 1F - ((effectiveReloadAnimationProgress - tiltGunTime) / (unloadClipTime + loadClipTime))));

					//Rotate the gun dependent on the animation type
					switch(anim)
					{
						case BREAK_ACTION :
						{
							GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
							GL11.glRotatef(reloadRotate * -model.breakAngle, 0F, 0F, 1F);
							GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
							GL11.glTranslatef(-1F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						case CUSTOMBREAK_ACTION :
						{
							GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
							GL11.glRotatef(reloadRotate * -model.breakAngle, 0F, 0F, 1F);
							GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
							GL11.glTranslatef(-1F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						case REVOLVER :
						{
							GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
							GL11.glRotatef(reloadRotate * model.revolverFlipAngle, 1F, 0F, 0F);
							GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
							GL11.glTranslatef(-1F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						
						case CUSTOMREVOLVER :
						{
							GL11.glTranslatef(model.revolverFlipPoint.x, model.revolverFlipPoint.y, model.revolverFlipPoint.z);
							GL11.glRotatef(reloadRotate * model.revolverFlipAngle, 1F, 0F, 0F);
							GL11.glTranslatef(-model.revolverFlipPoint.x, -model.revolverFlipPoint.y, -model.revolverFlipPoint.z);
							GL11.glTranslatef(-1F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}

						case REVOLVER2 :
						{
							GL11.glTranslatef(model.revolver2FlipPoint.x, model.revolver2FlipPoint.y, model.revolver2FlipPoint.z);
							GL11.glRotatef(reloadRotate * model.revolver2FlipAngle, -1F, 0F, 0F);
							GL11.glTranslatef(-model.revolver2FlipPoint.x, -model.revolver2FlipPoint.y, -model.revolver2FlipPoint.z);
							GL11.glTranslatef(-1F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						
						case CUSTOMREVOLVER2 :
						{
							GL11.glTranslatef(model.revolver2FlipPoint.x, model.revolver2FlipPoint.y, model.revolver2FlipPoint.z);
							GL11.glRotatef(reloadRotate * model.revolver2FlipAngle, -1F, 0F, 0F);
							GL11.glTranslatef(-model.revolver2FlipPoint.x, -model.revolver2FlipPoint.y, -model.revolver2FlipPoint.z);
							GL11.glTranslatef(-1F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}

						case BOTTOM_CLIP :
						{
							GL11.glRotatef(-180F * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(60F * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(0.5F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						case CUSTOMBOTTOM_CLIP :
						{
							GL11.glRotatef(-180F * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(60F * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(0.5F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						case PISTOL_CLIP :
						{
							GL11.glRotatef(-90F * clipPosition * clipPosition, 0F, 0F, 1F);
							GL11.glTranslatef(0F, -1F * clipPosition * 1/type.modelScale, 0F);
							break;
						}
						case CUSTOMPISTOL_CLIP :
						{
							GL11.glRotatef(-90F * clipPosition * clipPosition, 0F, 0F, 1F);
							GL11.glTranslatef(0F, -1F * clipPosition * 1/type.modelScale, 0F);
							break;
						}
						case ALT_PISTOL_CLIP :
						{
							GL11.glRotatef(5F * clipPosition, 0F, 0F, 1F);
							GL11.glTranslatef(0F, -3F * clipPosition * 1/type.modelScale, 0F);
							break;
						}
						case CUSTOMALT_PISTOL_CLIP :
						{
							GL11.glRotatef(5F * clipPosition, 0F, 0F, 1F);
							GL11.glTranslatef(0F, -3F * clipPosition * 1/type.modelScale, 0F);
							break;
						}
						case SIDE_CLIP :
						{
							GL11.glRotatef(180F * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(60F * clipPosition, 0F, 1F, 0F);
							GL11.glTranslatef(0.5F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						case CUSTOMSIDE_CLIP :
						{
							GL11.glRotatef(180F * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(60F * clipPosition, 0F, 1F, 0F);
							GL11.glTranslatef(0.5F * clipPosition * 1/type.modelScale, 0F, 0F);
							break;
						}
						case BULLPUP :
						{
							GL11.glRotatef(-150F * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(60F * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(1F * clipPosition * 1/type.modelScale, -0.5F * clipPosition * 1/type.modelScale, 0F);
							break;
						}
						case CUSTOMBULLPUP :
						{
							GL11.glRotatef(-150F * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(60F * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(1F * clipPosition * 1/type.modelScale, -0.5F * clipPosition * 1/type.modelScale, 0F);
							break;
						}
						case P90 :
						{
							GL11.glRotatef(-15F * reloadRotate * reloadRotate, 0F, 0F, 1F);
							GL11.glTranslatef(0F, 0.075F * reloadRotate, 0F);
							GL11.glTranslatef(-2F * clipPosition * 1/type.modelScale, -0.3F * clipPosition * 1/type.modelScale, 0.5F * clipPosition * 1/type.modelScale);
							break;
						}
						case CUSTOMP90 :
						{
							GL11.glRotatef(-15F * reloadRotate * reloadRotate, 0F, 0F, 1F);
							GL11.glTranslatef(0F, 0.075F * reloadRotate, 0F);
							GL11.glTranslatef(-2F * clipPosition * 1/type.modelScale, -0.3F * clipPosition * 1/type.modelScale, 0.5F * clipPosition * 1/type.modelScale);
							break;
						}
						case RIFLE :
						{
							float thing = clipPosition * getNumBulletsInReload(gripAttachment, type, item);
							int bulletNum = MathHelper.floor_float(thing);
							float bulletProgress = thing - bulletNum;

							GL11.glRotatef(bulletProgress * 15F, 0F, 1F, 0F);
							GL11.glRotatef(bulletProgress * 15F, 0F, 0F, 1F);
							GL11.glTranslatef(bulletProgress * -1F * 1/type.modelScale, 0F, bulletProgress * 0.5F * 1/type.modelScale);

							break;
						}
						case CUSTOMRIFLE :
						{
							float thing = clipPosition * getNumBulletsInReload(gripAttachment, type, item);
							int bulletNum = MathHelper.floor_float(thing);
							float bulletProgress = thing - bulletNum;

							GL11.glRotatef(bulletProgress * 15F, 0F, 1F, 0F);
							GL11.glRotatef(bulletProgress * 15F, 0F, 0F, 1F);
							GL11.glTranslatef(bulletProgress * -1F * 1/type.modelScale, 0F, bulletProgress * 0.5F * 1/type.modelScale);

							break;
						}
						case RIFLE_TOP :
						{
							float thing = clipPosition * getNumBulletsInReload(gripAttachment, type, item);
							int bulletNum = MathHelper.floor_float(thing);
							float bulletProgress = thing - bulletNum;

							GL11.glRotatef(bulletProgress * 55F, 0F, 1F, 0F);
							GL11.glRotatef(bulletProgress * 95F, 0F, 0F, 1F);
							GL11.glTranslatef(bulletProgress * -0.1F * 1/type.modelScale, bulletProgress * 1F * 1/type.modelScale, bulletProgress * 0.5F * 1/type.modelScale);

							break;
						}
						case CUSTOMRIFLE_TOP :
						{
							float thing = clipPosition * getNumBulletsInReload(gripAttachment, type, item);
							int bulletNum = MathHelper.floor_float(thing);
							float bulletProgress = thing - bulletNum;

							GL11.glRotatef(bulletProgress * 55F, 0F, 1F, 0F);
							GL11.glRotatef(bulletProgress * 95F, 0F, 0F, 1F);
							GL11.glTranslatef(bulletProgress * -0.1F * 1/type.modelScale, bulletProgress * 1F * 1/type.modelScale, bulletProgress * 0.5F * 1/type.modelScale);

							break;
						}
						case SHOTGUN : case STRIKER :
						{
							float thing = clipPosition * getNumBulletsInReload(gripAttachment, type, item);
							int bulletNum = MathHelper.floor_float(thing);
							float bulletProgress = thing - bulletNum;

							GL11.glRotatef(bulletProgress * -30F, 0F, 0F, 1F);
							GL11.glTranslatef(bulletProgress * -0.5F * 1/type.modelScale, bulletProgress * -1F * 1/type.modelScale, 0F);

							break;
						}
						case CUSTOMSHOTGUN : case CUSTOMSTRIKER :
						{
							float thing = clipPosition * getNumBulletsInReload(gripAttachment, type, item);
							int bulletNum = MathHelper.floor_float(thing);
							float bulletProgress = thing - bulletNum;

							GL11.glRotatef(bulletProgress * -30F, 0F, 0F, 1F);
							GL11.glTranslatef(bulletProgress * -0.5F * 1/type.modelScale, bulletProgress * -1F * 1/type.modelScale, 0F);

							break;
						}
						case CUSTOM :
						{
							//Staged reload allows you to change the animation route half way through
							if(effectiveReloadAnimationProgress < 0.5 && model.stagedReload)
							{
							GL11.glRotatef(model.rotateClipVertical * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(model.rotateClipHorizontal * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(model.tiltClip * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(model.translateClip.x * clipPosition * 1/type.modelScale,  model.translateClip.y * clipPosition * 1/type.modelScale, model.translateClip.z * clipPosition * 1/type.modelScale);
							break;
							}
							else if (effectiveReloadAnimationProgress > 0.5 && model.stagedReload)
							{
							GL11.glRotatef(model.stagedrotateClipVertical * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(model.stagedrotateClipHorizontal * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(model.stagedtiltClip * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(model.stagedtranslateClip.x * clipPosition * 1/type.modelScale,  model.stagedtranslateClip.y * clipPosition * 1/type.modelScale, model.stagedtranslateClip.z * clipPosition * 1/type.modelScale);
							break;
							}
							else
							{
							GL11.glRotatef(model.rotateClipVertical * clipPosition, 0F, 0F, 1F);
							GL11.glRotatef(model.rotateClipHorizontal * clipPosition, 0F, 1F, 0F);
							GL11.glRotatef(model.tiltClip * clipPosition, 1F, 0F, 0F);
							GL11.glTranslatef(model.translateClip.x * clipPosition * 1/type.modelScale,  model.translateClip.y * clipPosition * 1/type.modelScale, model.translateClip.z * clipPosition * 1/type.modelScale);
							break;
							}
						}
						case END_LOADED :
						{
							float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);

							GL11.glRotatef(-45F * dYaw, 0F, 0F, 1F);
							GL11.glTranslatef(-getEndLoadedDistance(gripAttachment, type, item) * dYaw, -0.5F * dYaw, 0F);

							float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
							GL11.glTranslatef(getEndLoadedDistance(gripAttachment, type, item) * xDisplacement, 0F, 0F);
							break;
						}
						case CUSTOMEND_LOADED :
						{
							float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);

							GL11.glRotatef(-45F * dYaw, 0F, 0F, 1F);
							GL11.glTranslatef(-getEndLoadedDistance(gripAttachment, type, item) * dYaw, -0.5F * dYaw, 0F);

							float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
							GL11.glTranslatef(getEndLoadedDistance(gripAttachment, type, item) * xDisplacement, 0F, 0F);
							break;
						}
						case BACK_LOADED :
						{
							float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
							//GL11.glRotatef(-45F * dYaw, 0F, 0F, 1F);
							GL11.glTranslatef(getEndLoadedDistance(gripAttachment, type, item) * dYaw, -0.5F * dYaw, 0F);

							float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
							GL11.glTranslatef(-getEndLoadedDistance(gripAttachment, type, item) * xDisplacement, 0F, 0F);
						}
						case CUSTOMBACK_LOADED :
						{
							float dYaw = (loadOnlyClipPosition > 0.5F ? loadOnlyClipPosition * 2F - 1F : 0F);
							//GL11.glRotatef(-45F * dYaw, 0F, 0F, 1F);
							GL11.glTranslatef(getEndLoadedDistance(gripAttachment, type, item) * dYaw, -0.5F * dYaw, 0F);

							float xDisplacement = (loadOnlyClipPosition < 0.5F ? loadOnlyClipPosition * 2F : 1F);
							GL11.glTranslatef(-getEndLoadedDistance(gripAttachment, type, item) * xDisplacement, 0F, 0F);
						}

						default : break;
					}
				}

				if(rtype == ItemRenderType.EQUIPPED_FIRST_PERSON && model.hasArms)
				{
					Minecraft mc = Minecraft.getMinecraft();
					renderAnimArm(mc.thePlayer, model, type, animations);
				}
				renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(type.getPaintjob(item.getItemDamage())));

				if(shouldRender)
				{
					if(gripAttachment != null && type.getSecondaryFire(item))
						renderAttachmentAmmo(f, gripAttachment, gripItemStack, model, type);
					else
						model.renderAmmo(f);
				}
				//Renders fullammo model for 2nd half of reload animation
				float effectiveReloadAnimationProgress = animations.lastReloadAnimationProgress + (animations.reloadAnimationProgress - animations.lastReloadAnimationProgress) * smoothing;
				reloadRotate = 1F;
				if(effectiveReloadAnimationProgress > 0.5)
					model.renderfullAmmo(f);
			}
			GL11.glPopMatrix();

			//Render a static model of the ammo NOT being reloaded
			GL11.glPushMatrix();
			{
				if(type.getSecondaryFire(item))
					model.renderAmmo(f);
				else if (gripAttachment != null && !type.getSecondaryFire(item))
					renderAttachmentAmmo(f, gripAttachment, gripItemStack, model, type);
			}
			GL11.glPopMatrix();
		}
		GL11.glPopMatrix();

		//Render static attachments
		//Scope
		if(scopeAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(scopeAttachment, scopeItemStack, model.scopeAttachPoint, type);
				if(model.scopeIsOnBreakAction)
				{
					GL11.glTranslatef(model.barrelBreakPoint.x, model.barrelBreakPoint.y, model.barrelBreakPoint.z);
					GL11.glRotatef(reloadRotate * -model.breakAngle, 0F, 0F, 1F);
					GL11.glTranslatef(-model.barrelBreakPoint.x, -model.barrelBreakPoint.y, -model.barrelBreakPoint.z);
				}
				if(model.scopeIsOnSlide)
					GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
				postRenderAttachment(scopeAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Grip
		if(gripAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(gripAttachment, gripItemStack, model.gripAttachPoint, type);
				if(model.gripIsOnPump)
					GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				postRenderAttachment(gripAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Barrel
		if(barrelAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(barrelAttachment, barrelItemStack, model.barrelAttachPoint, type);
				postRenderAttachment(barrelAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Stock
		if(stockAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(stockAttachment, stockItemStack, model.stockAttachPoint, type);
				postRenderAttachment(stockAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Slide
		if(slideAttachment != null && !type.getSecondaryFire(item))
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(slideAttachment, slideItemStack, model.slideAttachPoint, type);
				GL11.glTranslatef(-(animations.lastGunSlide + (animations.gunSlide - animations.lastGunSlide) * smoothing) * model.gunSlideDistance, 0F, 0F);
				postRenderAttachment(slideAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Gadget
		if(gadgetAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(gadgetAttachment, gadgetItemStack, model.gadgetAttachPoint, type);
				if(model.gadgetIsOnPump)
					GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				postRenderAttachment(gadgetAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Accessory
		if(accessoryAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(accessoryAttachment, accessoryItemStack, model.accessoryAttachPoint, type);
				postRenderAttachment(accessoryAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Pump
		if(pumpAttachment != null)
		{
			GL11.glPushMatrix();
			{
				preRenderAttachment(pumpAttachment, pumpItemStack, model.pumpAttachPoint, type);
				GL11.glTranslatef(-(1 - Math.abs(animations.lastPumped + (animations.pumped - animations.lastPumped) * smoothing)) * model.pumpHandleDistance, 0F, 0F);
				postRenderAttachment(pumpAttachment, type, f);
			}
			GL11.glPopMatrix();
		}

		//Release
		GL11.glPopMatrix();
	}

	/** Clean up some redundant code */
	private void preRenderAttachment(AttachmentType attachment, ItemStack stack, Vector3f model, GunType type)
	{
		Paintjob paintjob = attachment.getPaintjob(stack.getItemDamage());
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(paintjob));
		GL11.glTranslatef(model.x * type.modelScale, model.y * type.modelScale, model.z * type.modelScale);
		GL11.glScalef(attachment.modelScale, attachment.modelScale, attachment.modelScale);
	}

	private void postRenderAttachment(AttachmentType attachment, GunType type, float f)
	{
		ModelAttachment model = attachment.model;
		if(model != null)
			model.renderAttachment(f);
		renderEngine.bindTexture(FlansModResourceHandler.getTexture(type));
	}

	/** Load the attachment ammo model plus its texture */
	private void renderAttachmentAmmo(float f, AttachmentType grip, ItemStack gripStack, ModelGun model, GunType type)
	{
		Paintjob paintjob = grip.getPaintjob(gripStack.getItemDamage());
		renderEngine.bindTexture(FlansModResourceHandler.getPaintjobTexture(paintjob));
		GL11.glTranslatef(model.gripAttachPoint.x, model.gripAttachPoint.y, model.gripAttachPoint.z);
		grip.model.renderAttachmentAmmo(f);
		renderEngine.bindTexture(FlansModResourceHandler.getTexture(type));
	}

	private void renderFirstPersonArm(EntityPlayer player, ModelGun model,  GunAnimations anim)
    {
    	Minecraft mc = Minecraft.getMinecraft();
    	ModelBiped modelBipedMain = new ModelBiped(0.0F);
        mc.renderEngine.bindTexture(mc.thePlayer.getLocationSkin());

        float f = 1.0F;
        GL11.glColor3f(f, f, f);
        modelBipedMain.onGround = 0.0F;
        GL11.glPushMatrix();
        //right hand pump action animation
        if(!anim.reloading && model.righthandPump)
        {
		GL11.glTranslatef(-(model.rightArmPos.x - Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.pumpModifier), model.rightArmPos.y, model.rightArmPos.z);
	        GL11.glRotatef(model.rightArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmRot.x, 1F, 0F, 0F);
        }  
        //This moves the right hand if leftHandAmmo & handCharge are true (For left hand reload with right hand charge)
        else if(anim.charged < 0.9 && model.leftHandAmmo && model.rightHandCharge && anim.charged != -1.0F)
        {
	        GL11.glRotatef(model.rightArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(-(model.rightArmChargePos.x - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.x), (-(model.rightArmChargePos.y - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.y)), (-(model.rightArmChargePos.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)));
        }
        //This moves the right hand if leftHandAmmo & handBolt are true (For left hand reload with right hand bolt action)
        else if(anim.pumped < 0.9 && model.rightHandBolt && model.leftHandAmmo)
        {
	        GL11.glRotatef(model.rightArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef((model.rightArmChargePos.x + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.x), ((model.rightArmChargePos.y + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.y)), (-(model.rightArmChargePos.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)));
        }
        
        else if(!anim.reloading && !model.righthandPump)
        {
	        GL11.glRotatef(model.rightArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.rightArmPos.x, model.rightArmPos.y, model.rightArmPos.z);
        }
        
        else
		{
			GL11.glRotatef(model.rightArmReloadRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmReloadRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmReloadRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.rightArmReloadPos.x, model.rightArmReloadPos.y, model.rightArmReloadPos.z);
        }
        GL11.glScalef(model.rightArmScale.x,model.rightArmScale.y,model.rightArmScale.z);
        modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
        modelBipedMain.bipedRightArm.offsetY = 0F;
        if(!model.rightHandAmmo)
        {
        	modelBipedMain.bipedRightArm.render(0.0625F);
        }
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        //left hand pump action animation
        if(!anim.reloading && model.lefthandPump)
        {
		GL11.glTranslatef(-(model.leftArmPos.x - Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.pumpModifier), model.leftArmPos.y, model.leftArmPos.z);
	        GL11.glRotatef(model.leftArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmRot.x, 1F, 0F, 0F);	        
        }        
        //This moves the right hand if leftHandAmmo & handCharge are true (For left hand reload with right hand charge)
        else if(anim.charged < 0.9 && model.rightHandCharge && model.leftHandAmmo && anim.charged != -1.0F)
        {
	        GL11.glRotatef(model.leftArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(-(model.leftArmChargePos.x - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.x), (-(model.leftArmChargePos.y - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.y)), model.leftArmChargePos.z);
        }
        //This moves the right hand if leftHandAmmo & handBolt are true (For left hand reload with right hand bolt action)
        else if(anim.pumped < 0.9 && model.rightHandBolt && model.leftHandAmmo)
        {
	        GL11.glRotatef(model.leftArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef((model.leftArmChargePos.x + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.x), ((model.leftArmChargePos.y + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.y)), (-(model.leftArmChargePos.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)));
        }
        else if(!anim.reloading && !model.lefthandPump)
        {
	        GL11.glRotatef(model.leftArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.leftArmPos.x, model.leftArmPos.y, model.leftArmPos.z);
        }       
        else
        {
	        GL11.glRotatef(model.leftArmReloadRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmReloadRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmReloadRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.leftArmReloadPos.x, model.leftArmReloadPos.y, model.leftArmReloadPos.z);
        }
        
        GL11.glScalef(model.leftArmScale.x,model.leftArmScale.y,model.leftArmScale.z);
        modelBipedMain.bipedLeftArm.offsetY = 0F;
        if(!model.leftHandAmmo)
        {
        	modelBipedMain.bipedLeftArm.render(0.0625F);
        }
        GL11.glPopMatrix();
    }

    private void renderAnimArm(EntityPlayer player, ModelGun model, GunType type, GunAnimations anim)
    {
    	Minecraft mc = Minecraft.getMinecraft();
    	ModelBiped modelBipedMain = new ModelBiped(0.0F);
        mc.renderEngine.bindTexture(mc.thePlayer.getLocationSkin());
        GL11.glPushMatrix();
        GL11.glScalef(1/type.modelScale, 1/type.modelScale, 1/type.modelScale);
        float f = 1.0F;
        GL11.glColor3f(f, f, f);
        modelBipedMain.onGround = 0.0F;
        GL11.glPushMatrix();
		float effectiveReloadAnimationProgress = anim.lastReloadAnimationProgress + (anim.reloadAnimationProgress - anim.lastReloadAnimationProgress) * smoothing;

      
        //This moves the right hand if rightHandAmmo & handCharge are true (For slides)
        if(anim.charged < 0.9 && model.rightHandCharge && model.rightHandAmmo && anim.charged != -1.0F)
        {
	        GL11.glRotatef(model.rightArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(-(model.rightArmChargePos.x - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.x), (-(model.rightArmChargePos.y - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.y)), (-(model.rightArmChargePos.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)));
        }
        //This moves the right hand if rightHandAmmo & handBolt are true (For pumps/bolts)
        else if(anim.pumped < 0.9 && model.rightHandBolt && model.rightHandAmmo)
        {
	        GL11.glRotatef(model.rightArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef((model.rightArmChargePos.x + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.x), ((model.rightArmChargePos.y + Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.chargeModifier.y)), (-(model.rightArmChargePos.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)));
        }
        //This is the default state
        else if(!anim.reloading)
		{
	        GL11.glRotatef(model.rightArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.rightArmPos.x, model.rightArmPos.y, model.rightArmPos.z);
        }
        //This is the default reloading state
		else
		{
	        GL11.glRotatef(model.rightArmReloadRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.rightArmReloadRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.rightArmReloadRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.rightArmReloadPos.x, model.rightArmReloadPos.y, model.rightArmReloadPos.z);
        }
        
        GL11.glScalef(model.rightArmScale.x,model.rightArmScale.y,model.rightArmScale.z);
        modelBipedMain.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
        modelBipedMain.bipedRightArm.offsetY = 0F;
        if(model.rightHandAmmo)
        {
        	modelBipedMain.bipedRightArm.render(0.0625F);

        }
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        //System.out.println(anim.charged);
        //This moves the left hand if leftHandAmmo & handCharge are true (For slides)	
        if(anim.charged < 0.9 && model.leftHandCharge && model.leftHandAmmo && anim.charged != -1.0F)
        {
	        GL11.glRotatef(model.leftArmChargeRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmChargeRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmChargeRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(-(model.leftArmChargePos.x - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.x), (-(model.leftArmChargePos.y - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.y)), (-(model.leftArmChargePos.z - Math.abs(anim.lastCharged + (anim.charged - anim.lastCharged) * smoothing) / model.chargeModifier.z)));
        } 

        else if(!anim.reloading && model.lefthandPump)
        {
		GL11.glTranslatef(-(model.leftArmPos.x - Math.abs(anim.lastPumped + (anim.pumped - anim.lastPumped) * smoothing) / model.pumpModifier), model.leftArmPos.y, model.leftArmPos.z);
	        GL11.glRotatef(model.leftArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmRot.x, 1F, 0F, 0F);	        
        }   
	
        //This is the default state
        else if(!anim.reloading)
		{
	        GL11.glRotatef(model.leftArmRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.leftArmPos.x, model.leftArmPos.y, model.leftArmPos.z);
        }
        //This is the default reloading state
		else if(effectiveReloadAnimationProgress < 0.5 && model.stagedleftArmReloadPos.x != 0)
		{
	        GL11.glRotatef(model.leftArmReloadRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmReloadRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmReloadRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.leftArmReloadPos.x, model.leftArmReloadPos.y, model.leftArmReloadPos.z);
        }
		else if(effectiveReloadAnimationProgress > 0.5 && model.stagedleftArmReloadPos.x != 0)
		{
	        GL11.glRotatef(model.stagedleftArmReloadRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.stagedleftArmReloadRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.stagedleftArmReloadRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.stagedleftArmReloadPos.x, model.stagedleftArmReloadPos.y, model.stagedleftArmReloadPos.z);
        }
		else
		{
	        GL11.glRotatef(model.leftArmReloadRot.y, 0F, 1F, 0F);
	        GL11.glRotatef(model.leftArmReloadRot.z, 0F, 0F, 1F);
	        GL11.glRotatef(model.leftArmReloadRot.x, 1F, 0F, 0F);
	        GL11.glTranslatef(model.leftArmReloadPos.x, model.leftArmReloadPos.y, model.leftArmReloadPos.z);
        }
        
        GL11.glScalef(model.leftArmScale.x,model.leftArmScale.y,model.leftArmScale.z);
        modelBipedMain.bipedLeftArm.offsetY = 0F;
        if(model.leftHandAmmo)
        {
        	modelBipedMain.bipedLeftArm.render(0.0625F);
        }
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }
    
	/** Get the end loaded distance, based on ammo type to reload */
	private float getEndLoadedDistance(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if(grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.endLoadedAmmoDistance;
		else
			return gun.model.endLoadedAmmoDistance;
	}

	/** Get the number of bullets to reload in animation, based on ammo type to reload */
	private float getNumBulletsInReload(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if(grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.numBulletsInReloadAnimation;
		else
			return gun.model.numBulletsInReloadAnimation;
	}

	/** Get the recoil distance, based on ammo type to reload */
	private float getRecoilDistance(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if(grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.recoilDistance;
		else
			return gun.model.RecoilSlideDistance;
	}

	/** Get the recoil angle, based on ammo type to reload */
	private float getRecoilAngle(AttachmentType grip, GunType gun, ItemStack gunStack)
	{
		if(grip != null && gun.getSecondaryFire(gunStack))
			return grip.model.recoilAngle;
		else
			return gun.model.RotateSlideDistance;
	}
}
