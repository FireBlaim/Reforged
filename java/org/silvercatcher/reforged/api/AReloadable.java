package org.silvercatcher.reforged.api;

import java.util.List;

import org.silvercatcher.reforged.ReforgedMod;
import org.silvercatcher.reforged.util.Helpers;

import com.google.common.collect.Multimap;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

public abstract class AReloadable extends ItemBow implements ItemExtension {
	
	private Item ammo;
	private String shootsound;
	
	public AReloadable(String name, String shootsound) {
		setMaxStackSize(1);
		setMaxDamage(100);
		setUnlocalizedName(name);
		setCreativeTab(ReforgedMod.tabReforged);
		this.shootsound = shootsound;
	}
	
	public static final byte empty		= 0;
	public static final byte loading	= 1;
	public static final byte loaded		= 2;
	
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
		if(giveCompound(stack).getBoolean(CompoundTags.STARTED) && giveCompound(stack).getByte(CompoundTags.AMMUNITION) == loading) {
			giveCompound(stack).setInteger(CompoundTags.TIME, getReloadTime(stack) + 1);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		
		byte loadState = giveCompound(stack).getByte(CompoundTags.AMMUNITION);
		
		tooltip.add(I18n.format("item.musket.loadstate") + ": " + (loadState == empty ? 
				I18n.format("item.musket.loadstate.empty")
				: (loadState == loaded ? I18n.format("item.musket.loadstate.loaded") : 
					I18n.format("item.musket.loadstate.loading"))));
	}
	
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return true;
	}
	
	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		
		byte loadState = giveCompound(stack).getByte(CompoundTags.AMMUNITION);
		
		if(loadState == loading) {
			if(ReforgedMod.battlegearDetected) return EnumAction.BOW;
			else return EnumAction.BLOCK;
		}
		if(loadState == loaded) return EnumAction.BOW;
		return EnumAction.NONE;
	}
	
	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		
		byte loadState = giveCompound(stack).getByte(CompoundTags.AMMUNITION);
		
		if(loadState == loading) return getReloadTotal();

		return super.getMaxItemUseDuration(stack);
	}
	
	protected void setAmmo(Item ammo) {
		this.ammo = ammo;
	}
	
	private Item getAmmo() {
		return ammo;
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
		if(hand == EnumHand.MAIN_HAND) {
			NBTTagCompound compound = giveCompound(playerIn.getHeldItemMainhand());
			
			byte loadState = compound.getByte(CompoundTags.AMMUNITION);
			
			if(loadState == empty) {
				if(playerIn.capabilities.isCreativeMode || Helpers.consumeInventoryItem(playerIn, getAmmo())) {
					
					loadState = loading;
					if(compound.getByte(CompoundTags.AMMUNITION) == empty) {
						compound.setBoolean(CompoundTags.STARTED, true);
						compound.setInteger(CompoundTags.TIME, 0);
					}
				} else {
					Helpers.playSound(worldIn, playerIn, "shotgun_reload", 1.0f, 0.7f);
				}
			}
			
			compound.setByte(CompoundTags.AMMUNITION, loadState);
			
			playerIn.setActiveHand(hand);
			
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, playerIn.getHeldItemMainhand());
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, playerIn.getHeldItemOffhand());
	}
	
	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase playerInl, int timeLeft) {
		if(!worldIn.isRemote && playerInl instanceof EntityPlayer) {
			EntityPlayer playerIn = (EntityPlayer) playerInl;
			
			NBTTagCompound compound = giveCompound(stack);
			
			byte loadState = compound.getByte(CompoundTags.AMMUNITION);
			
			if(loadState == loaded) {
				
				Helpers.playSound(worldIn, playerIn, shootsound, 1f, 1f);
				
				shoot(worldIn, playerIn, stack);
				if(playerIn instanceof EntityPlayer && 
						(!playerIn.capabilities.isCreativeMode && stack.getItem().isDamageable() && stack.attemptDamageItem(5, itemRand))) {
					playerIn.renderBrokenItemStack(stack);
					Helpers.destroyCurrentEquippedItem(playerIn);
				}
				
				compound.setByte(CompoundTags.AMMUNITION, empty);
				compound.setBoolean(CompoundTags.STARTED, false);
				compound.setInteger(CompoundTags.TIME, -1);
			}
		}
		if(worldIn.isRemote && playerInl instanceof EntityPlayer) {
			Helpers.playSound(worldIn, playerInl, shootsound, 1f, 1f);
		}
	}
	
	public int getReloadTime(ItemStack stack) {
		return giveCompound(stack).getInteger(CompoundTags.TIME);
	}
	
	public abstract void shoot(World worldIn, EntityLivingBase playerIn, ItemStack stack);
	
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase playerIn) {
		
		byte loadState = giveCompound(stack).getByte(CompoundTags.AMMUNITION);
		
		if(loadState == loading) {
			loadState = loaded;
		}
		giveCompound(stack).setByte(CompoundTags.AMMUNITION, loadState);
		return stack;
	}
	
	public abstract int getReloadTotal();
	
	public NBTTagCompound giveCompound(ItemStack stack) {
		
		NBTTagCompound compound = CompoundTags.giveCompound(stack);
		
		if(!compound.hasKey(CompoundTags.AMMUNITION)) {
			
			compound.setByte(CompoundTags.AMMUNITION, empty);
		}
		return compound;
	}
	
	public int getReloadStarted(ItemStack stack) {
		return giveCompound(stack).getInteger(CompoundTags.STARTED);
	}
	
	public int getReloadLeft(ItemStack stack, EntityPlayer player) {
		return (getReloadStarted(stack) - player.ticksExisted);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Multimap getAttributeModifiers(ItemStack stack) {
		return ItemExtension.super.getAttributeModifiers(stack);
	}
	
	@Override
	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot) {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if(equipmentSlot == EntityEquipmentSlot.MAINHAND && getHitDamage() > 0) {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", getHitDamage(), 0));
        }
        return multimap;
    }
	
}