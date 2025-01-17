package com.windanesz.wizardrygolems.entity.living;

import com.golems.entity.GolemBase;
import com.windanesz.wizardryutils.entity.ai.EntityAIMinionOwnerHurtByTarget;
import electroblob.wizardry.Wizardry;
import electroblob.wizardry.entity.living.ISummonedCreature;
import electroblob.wizardry.packet.PacketNPCCastSpell;
import electroblob.wizardry.packet.WizardryPacketHandler;
import electroblob.wizardry.registry.Spells;
import electroblob.wizardry.spell.Spell;
import electroblob.wizardry.util.SpellModifiers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIDefendVillage;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.UUID;

public abstract class EntityGolemBaseMinion extends GolemBase implements ISummonedCreature {

	// Minion field implementations
	private int lifetime = -1;
	private UUID casterUUID;

	public EntityGolemBaseMinion(World world) {
		super(world);
	}

	@Override
	protected void initEntityAI() {
		super.initEntityAI();
		this.targetTasks.taskEntries.clear();
		this.targetTasks.addTask(1, new EntityAIMinionOwnerHurtByTarget(this));
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityLivingBase.class,
				0, false, true, this.getTargetSelector()));
		removeTask(EntityAIMoveThroughVillage.class);
		removeTask(EntityAIDefendVillage.class);
	}

	protected void removeTask(Class<? extends EntityAIBase> taskClassToRemove) {
		// Iterate through tasks to find and remove the specified task
		this.tasks.taskEntries.removeIf(entry -> taskClassToRemove.isInstance(entry.action));
	}

	/// Minion related methods ///

	// Setter + getter implementations
	@Override
	public int getLifetime() {return lifetime;}

	@Override
	public void setLifetime(int lifetime) {this.lifetime = lifetime;}

	@Override
	public UUID getOwnerId() {return casterUUID;}

	@Override
	public void setOwnerId(UUID uuid) {this.casterUUID = uuid;}

	// Recommended overrides
	@Override
	protected int getExperiencePoints(EntityPlayer player) {return 0;}

	@Override
	protected boolean canDropLoot() {return false;}

	@Override
	protected Item getDropItem() {return null;}

	@Override
	protected ResourceLocation getLootTable() {return null;}

	@Override
	public boolean canPickUpLoot() {return false;}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		this.writeNBTDelegate(nbttagcompound);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		this.readNBTDelegate(nbttagcompound);
	}

	@Override
	public boolean isPotionApplicable(PotionEffect potioneffectIn) {
		return potioneffectIn.getPotion() != MobEffects.HUNGER && super.isPotionApplicable(potioneffectIn);
	}

	@Override
	public boolean getCanSpawnHere() {
		return this.world.getDifficulty() != EnumDifficulty.PEACEFUL;
	}

	@Override
	public ITextComponent getDisplayName() {
		if (getCaster() != null) {
			return new TextComponentTranslation(NAMEPLATE_TRANSLATION_KEY, getCaster().getName(),
					new TextComponentTranslation("entity." + this.getEntityString() + ".name"));
		} else {
			return super.getDisplayName();
		}
	}

	@Override
	public boolean hasCustomName() {
		// If this returns true, the renderer will show the nameplate when looking directly at the entity
		return Wizardry.settings.summonedCreatureNames && getCaster() != null;
	}

	@Override
	public void onSpawn() {
		this.spawnParticleEffect();
	}

	@Override
	public void onDespawn() {

	}

	private void spawnParticleEffect() {

	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		this.updateDelegate();
	}

	@Override
	public void setRevengeTarget(EntityLivingBase entity) {
		if (this.shouldRevengeTarget(entity)) {super.setRevengeTarget(entity);}
	}

	protected boolean processInteract(EntityPlayer player, EnumHand hand) {
		// In this case, the delegate method determines whether super is called.
		// Rather handily, we can make use of Java's short-circuiting method of evaluating OR statements.
		return this.interactDelegate(player, hand) || super.processInteract(player, hand);
	}

	/// ExtraGolems methods ///

	@Override
	public SoundEvent getGolemSound() {
		return SoundEvents.BLOCK_STONE_STEP;
	}

	@Override
	protected ResourceLocation applyTexture() {
		return null;
	}

	@Override
	public boolean hasParticleEffect() {
		return false;
	}

	@Override
	protected void collideWithEntity(Entity entityIn) {
		entityIn.applyEntityCollision(this);
	}

	protected void castSpellOnTarget(Entity target, Spell spell, SpellModifiers modifiers) {
		if (target instanceof EntityLivingBase) {

			spell.cast(world, this, EnumHand.MAIN_HAND, 0, (EntityLivingBase) target, modifiers);

			if (spell.requiresPacket()) {
				// Sends a packet to all players in dimension to tell them to spawn particles.
				IMessage msg = new PacketNPCCastSpell.Message(this.getEntityId(), target.getEntityId(),
						EnumHand.MAIN_HAND, Spells.chain_lightning, modifiers);
				WizardryPacketHandler.net.sendToDimension(msg, this.world.provider.getDimension());
			}
		}
	}
}
