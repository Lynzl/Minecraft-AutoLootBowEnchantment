package fr.lnzl.albe;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;

@Mod(AutoLootBowEnchantment.MODID)
@Mod.EventBusSubscriber
public class AutoLootBowEnchantment {

    public static final String MODID = "autolootbowenchantment";

    public static final DeferredRegister<Enchantment> ENCHANTMENTS_REGISTER = new DeferredRegister<>(ForgeRegistries.ENCHANTMENTS, MODID);
    public static final RegistryObject<Enchantment> AUTO_LOOT_ENCHANTMENT = ENCHANTMENTS_REGISTER.register("autoloot", AutoLootEnchantment::new);

    private static final Logger LOGGER = LogManager.getLogger(MODID);

    public AutoLootBowEnchantment() {

        ENCHANTMENTS_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinWorldEvent(final EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ArrowEntity)) return;

        ArrowEntity arrow = (ArrowEntity) entity;

        Entity shooter = arrow.getShooter();
        if (shooter == null) return;

        boolean autoLoot = false;
        Iterable<ItemStack> heldEquipment = shooter.getHeldEquipment();
        for (ItemStack equipped : heldEquipment) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(equipped);
            for (Enchantment enchantment : enchantments.keySet()) {
                if (enchantment instanceof AutoLootEnchantment) {
                    autoLoot = true;
                    break;
                }
            }
        }
        if (!autoLoot) return;

        arrow.getTags().add("autoloot");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(final LivingDeathEvent event) {
        Entity source = event.getSource().getImmediateSource();
        if (!(source instanceof ArrowEntity)) return;

        if (!source.getTags().contains("autoloot")) return;

        LivingEntity killed = event.getEntityLiving();
        killed.getTags().add("autoloot");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDropsEvent(final LivingDropsEvent event) {
        LivingEntity killed = event.getEntityLiving();

        if (!(killed.getTags().contains("autoloot"))) return;

        Entity killer = event.getSource().getTrueSource();
        if (killer == null) return;

        BlockPos killerPos = killer.getPosition();

        Collection<ItemEntity> drops = event.getDrops();

        for (ItemEntity drop : drops) {
            drop.setPosition(killerPos.getX(), killerPos.getY(), killerPos.getZ());
            drop.setMotion(0, 0, 0);
            drop.setOwnerId(killer.getUniqueID());
            drop.setPickupDelay(5);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingExperienceDropEvent(final LivingExperienceDropEvent event) {
        LivingEntity killed = event.getEntityLiving();

        if (!killed.getTags().contains("autoloot")) return;

        PlayerEntity killer = event.getAttackingPlayer();
        if (killer == null) return;

        World world = killer.getEntityWorld();

        BlockPos killerPos = killer.getPosition();

        int experience = event.getDroppedExperience();
        while (experience > 0) {
            int fragmentAmount = ExperienceOrbEntity.getXPSplit(experience);
            experience -= fragmentAmount;
            ExperienceOrbEntity experienceOrbEntity =
                    new ExperienceOrbEntity(world, killerPos.getX(), killerPos.getY(), killerPos.getZ(), fragmentAmount);
            experienceOrbEntity.setMotion(0, 0, 0);
            experienceOrbEntity.delayBeforeCanPickup = 10;
            world.addEntity(experienceOrbEntity);
        }

        event.setCanceled(true);
    }

    public static class AutoLootEnchantment extends Enchantment {

        protected AutoLootEnchantment() {
            super(
                    Rarity.UNCOMMON,
                    EnchantmentType.create(
                            "ALLBOWS",
                            (item) -> (item instanceof BowItem || item instanceof CrossbowItem)
                    ),
                    new EquipmentSlotType[]{
                            EquipmentSlotType.MAINHAND,
                            EquipmentSlotType.OFFHAND
                    }
            );
        }

        @Override
        public int getMinEnchantability(int enchantmentLevel) {
            return 20;
        }

        @Override
        public int getMaxEnchantability(int enchantmentLevel) {
            return 50;
        }
    }
}
