package com.multitera.tinkershield.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.tconstruct.common.TinkerNetwork;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.materials.MaterialTypes;
import slimeknights.tconstruct.library.tinkering.Category;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tools.SwordCore;
import slimeknights.tconstruct.library.tools.ToolNBT;
import slimeknights.tconstruct.library.utils.EntityUtil;
import slimeknights.tconstruct.library.utils.ToolHelper;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.common.network.EntityMovementChangePacket;

import javax.annotation.Nonnull;
import java.util.List;

// BattleShield Ability: Improved Battlesign that adds a shield bash, charge, and stomp.
public class BattleShield extends SwordCore {

    private static PartMaterialType shieldTrimPMT = new PartMaterialType(TinkerTools.largePlate, MaterialTypes.HEAD, MaterialTypes.EXTRA);

    public BattleShield() {
        super(PartMaterialType.handle(TinkerTools.toughToolRod),
                PartMaterialType.head(TinkerTools.signHead),
                shieldTrimPMT);

        addCategory(Category.WEAPON);

        setUnlocalizedName("battleshield").setRegistryName("battleshield");

        this.addPropertyOverride(new ResourceLocation("blocking"), new IItemPropertyGetter() {
            @Override
            @SideOnly(Side.CLIENT)
            public float apply(@Nonnull ItemStack stack, World worldIn, EntityLivingBase entityIn) {
                return entityIn != null && entityIn.isHandActive() && entityIn.getActiveItemStack() == stack ? 1.0F : 0.0F;
            }
        });
    }

    @Override
    public double attackSpeed() {
        return 1.2;
    }

    @Override
    public float damagePotential() {
        return 0.86f;
    }

    @Nonnull
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BLOCK;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
        ItemStack itemStackIn = playerIn.getHeldItem(hand);
        if(!ToolHelper.isBroken(itemStackIn)) {
            playerIn.setActiveHand(hand);

            // Make player charge
            if (playerIn.isSprinting()) {
                playerIn.getEntityData().setBoolean("tinkershield.battleshield.charging", true);
            }

            return new ActionResult<>(EnumActionResult.SUCCESS, itemStackIn);
        }
        return new ActionResult<>(EnumActionResult.FAIL, itemStackIn);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack item, World world, EntityLivingBase user, int timeLeft) {

        if (user.getEntityData().getBoolean("tinkershield.battleshield.charging")) {
            user.getEntityData().setBoolean("tinkershield.battleshield.charging", false);
        }
        //Shield Bash
        if (user.isSneaking()) {
             if (user.getHeldItemMainhand() == item) {
                 user.swingArm(EnumHand.MAIN_HAND);
             } else {
                 user.swingArm(EnumHand.OFF_HAND);
             }

            // is the player currently looking at an entity?
            float range = 3.2f;
            Vec3d eye = new Vec3d(user.posX, user.posY + user.getEyeHeight(), user.posZ); // Entity.getPositionEyes
            Vec3d look = user.getLook(1.0f);
            RayTraceResult mop = EntityUtil.raytraceEntity(user, eye, look, range, true);

            // nothing hit :(
            if(mop == null) {
                return;
            }

            // we hit something. let it FLYYYYYYYYY
            if(mop.typeOfHit == RayTraceResult.Type.ENTITY) {
                Entity target = mop.entityHit;
                float progress = Math.min(1f, (getMaxItemUseDuration(item) - timeLeft) / 30f);

                bonkEntity(item, world, user, look, target, progress);
            }
        }
    }

    private void bonkEntity(ItemStack stack, World world, EntityLivingBase player, Vec3d look, Entity entity, float progress) {
        float strength = 2.6f * progress * progress;
        double x = look.x * strength;
        double y = look.y / 3f * strength + 0.1f + 0.4f * progress;
        double z = look.z * strength;
        ToolHelper.attackEntity(stack, this, player, entity);
        entity.addVelocity(x, y, z);
        if(entity instanceof EntityPlayerMP) {
            TinkerNetwork.sendPacket(entity, new SPacketEntityVelocity(entity));
        }
    }

    @Override
    public void onUsingTick(ItemStack item, EntityLivingBase user, int count) {

        //Sneaking cancels Shield Charge
        if (user.isSneaking()) user.getEntityData().setBoolean("tinkershield.battleshield.charging", false);

        //Shield Charge
        if (user.getEntityData().getBoolean("tinkershield.battleshield.charging")) {
            float bonusSpeed = (!user.onGround || user.isInWater()) ? 0.2f : 1.3f;
            double speed = user.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * bonusSpeed;

                Vec3d dir = user.getLookVec();
                dir = new Vec3d(dir.x, 0, dir.z).normalize();
                user.motionX += dir.x * speed;
                user.motionZ += dir.z * speed;
            if(Math.sqrt(user.motionX*user.motionX + user.motionZ*user.motionZ) > 0.2D) {
                collisionHandler(item, user);
            }
            if(user instanceof EntityPlayer) {
                ((EntityPlayer) user).getFoodStats().addExhaustion(0.15F);
            }
        }

        //Goomba Stomp
        if (user.fallDistance >= 4) {
            boolean bounce = collisionHandler(item, user);
            if (bounce) bounce(user);
        }
        super.onUsingTick(item, user, count);
    }

    private boolean collisionHandler(ItemStack item, EntityLivingBase user) {
        boolean collided = false;
        Vec3d dir;
        Vec3d moveDir = new Vec3d(user.motionX, user.motionY, user.motionZ).normalize();
        World world = user.world;
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, user.getEntityBoundingBox().grow(1), e -> e != user);
        for(EntityLivingBase target : targets) {
            dir = target.getPositionVector().subtract(user.getPositionVector()).normalize();

            //45° angle range
            if(target.canBeCollidedWith() && Math.toDegrees(Math.acos(moveDir.dotProduct(dir))) < 45) {
                this.bonkEntity(item, world, user, moveDir, target, 0.5f);
                if (!collided) collided = true;
            }
        }
        return collided;
    }

    private void bounce(EntityLivingBase user) {
        user.fallDistance = 0;
        user.motionY = 0.42F;

        if (user.isPotionActive(MobEffects.JUMP_BOOST))
        {
            user.motionY += (float)(user.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
        }

        if (user.isSprinting())
        {
            float f = user.rotationYaw * 0.017453292F;
            user.motionX -= MathHelper.sin(f) * 0.2F;
            user.motionZ += MathHelper.cos(f) * 0.2F;
        }

        user.isAirBorne = true;
        net.minecraftforge.common.ForgeHooks.onLivingJump(user);
    }

    // Extra damage reduction when blocking with a battlesign
    @SubscribeEvent(priority = EventPriority.LOW) // lower priority so we get called later since we change tool NBT
    public void reducedDamageBlocked(LivingHurtEvent event) {
        // don't affect unblockable or magic damage or explosion damage
        // projectiles are handled in LivingAttackEvent
        if(event.getSource().isUnblockable() ||
                event.getSource().isMagicDamage() ||
                event.getSource().isExplosion() ||
                event.getSource().isProjectile() ||
                event.isCanceled()) {
            return;
        }
        if(!shouldBlockDamage(event.getEntityLiving())) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack battlesign = player.getActiveItemStack();

        // got hit by something: reduce damage
        int damage = event.getAmount() < 2f ? 1 : Math.round(event.getAmount() / 2f);
        // reduce damage. After this event the damage will be halved again because we're blocking so we have to factor this in
        event.setAmount(event.getAmount() * 0.7f);

        // reflect damage
        if(event.getSource().getTrueSource() != null) {
            event.getSource().getTrueSource().attackEntityFrom(DamageSource.causeThornsDamage(player), event.getAmount() / 2f);
            damage = damage * 3 / 2;
        }
        ToolHelper.damageTool(battlesign, damage, player);
    }

    @SubscribeEvent
    public void reflectProjectiles(LivingAttackEvent event) {
        // only blockable projectile damage
        if(event.getSource().isUnblockable() || !event.getSource().isProjectile() || event.getSource().getImmediateSource() == null) {
            return;
        }
        if(!shouldBlockDamage(event.getEntityLiving())) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack battleshield = player.getActiveItemStack();

        // ensure the player is looking at the projectile (aka not getting shot into the back)
        Entity projectile = event.getSource().getImmediateSource();
        Vec3d motion = new Vec3d(projectile.motionX, projectile.motionY, projectile.motionZ);
        Vec3d look = player.getLookVec();

        // this gives a factor of how much we're looking at the incoming arrow
        double strength = -look.dotProduct(motion.normalize());
        // we're looking away. oh no.
        if(strength < 0.1) {
            return;
        }

        // caught that bastard! block it!
        event.setCanceled(true);

        // and return it to the sender
        // calc speed of the projectile
        double speed = projectile.motionX * projectile.motionX + projectile.motionY * projectile.motionY + projectile.motionZ * projectile.motionZ;
        speed = Math.sqrt(speed);
        speed += 0.2f; // we add a bit speed

        // and redirect it to where the player is looking
        projectile.motionX = look.x * speed;
        projectile.motionY = look.y * speed;
        projectile.motionZ = look.z * speed;

        projectile.rotationYaw = (float) (Math.atan2(projectile.motionX, projectile.motionZ) * 180.0D / Math.PI);
        projectile.rotationPitch = (float) (Math.atan2(projectile.motionY, speed) * 180.0D / Math.PI);

        // notify clients from change, otherwise people will get veeeery confused
        TinkerNetwork.sendToAll(new EntityMovementChangePacket(projectile));

        // special treatement for arrows
        if(projectile instanceof EntityArrow) {
            ((EntityArrow) projectile).shootingEntity = player;

            // the inverse is done when the event is cancelled in arrows etc.
            // we reverse it so it has no effect. yay
            projectile.motionX /= -0.10000000149011612D;
            projectile.motionY /= -0.10000000149011612D;
            projectile.motionZ /= -0.10000000149011612D;
        }

        // use durability equal to the damage prevented
        ToolHelper.damageTool(battleshield, (int) event.getAmount(), player);
    }

    protected boolean shouldBlockDamage(Entity entity) {
        // hit entity is a player?
        if(!(entity instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entity;
        // needs to be blocking with a battleshield
        if(!player.isActiveItemStackBlocking() || player.getActiveItemStack().getItem() != this) {
            return false;
        }

        // broken battleshield.
        return !ToolHelper.isBroken(player.getActiveItemStack());

    }

    @Override
    public ToolNBT buildTagData(List<Material> materials) {
        return buildDefaultTag(materials);
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }
}