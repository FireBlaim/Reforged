package org.silvercatcher.reforged;

import java.util.*;

import org.silvercatcher.reforged.api.IZombieEquippable;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReforgedMonsterArmourer {

	private static final UUID itemModifierUUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
	/** DON'T CHANGE IT HERE! NULLPOINTERS WOULD OCCUR!!! */
	private static Item[] zombieWeapons;

	private Random random = new Random();

	private void equipZombie(EntityZombie zombie) {
		if ((zombie.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) == null
				|| zombie.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) && random.nextInt(10) == 0) {
			Item item = randomFrom(zombieWeapons);
			zombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(item));

			zombie.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
					.applyModifier(new AttributeModifier(itemModifierUUID, "Weapon Damage", 99f, 0));

		}
	}

	@SubscribeEvent
	public void onSpawn(EntityJoinWorldEvent event) {
		if (event.isCanceled() || event.getEntity() == null || event.getWorld().isRemote
				|| !(event.getEntity() instanceof EntityZombie) || zombieWeapons == null)
			return;
		equipZombie((EntityZombie) event.getEntity());
	}

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load e) {
		if (e.isCanceled() || e.getWorld().isRemote)
			return;
		List<Item> list = new ArrayList<Item>();
		for (Item i : ReforgedRegistry.registrationList) {
			if (i instanceof IZombieEquippable) {
				for (int c = 0; c < ((IZombieEquippable) i).zombieSpawnChance(); c++)
					list.add(i);
			}
		}
		if (list.isEmpty())
			zombieWeapons = null;
		else
			zombieWeapons = list.toArray(new Item[list.size()]);
	}

	private Item randomFrom(Item[] selection) {
		return selection[random.nextInt(selection.length)];
	}
}
