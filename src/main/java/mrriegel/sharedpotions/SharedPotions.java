package mrriegel.sharedpotions;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.Sets;

@Mod(modid = SharedPotions.MODID, name = SharedPotions.NAME, version = SharedPotions.VERSION, acceptableRemoteVersions = "*")
@EventBusSubscriber
public class SharedPotions {

	@Instance(SharedPotions.MODID)
	public static SharedPotions INSTANCE;

	public static final String VERSION = "1.0.0";
	public static final String NAME = "Shared Potions";
	public static final String MODID = "sharedpotions";

	//config
	public static Configuration config;
	public static boolean onlyPlayer, onlyPositive, global;
	public static int range;
	public static String type;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		onlyPlayer = config.getBoolean("onlyPlayer", Configuration.CATEGORY_GENERAL, true, "Potions will be shared only with players.");
		onlyPositive = config.getBoolean("onlyPositive", Configuration.CATEGORY_GENERAL, true, "Only positive potions will be shared.");
		global = config.getBoolean("global", Configuration.CATEGORY_GENERAL, false, "Share potions with the whole dimension (only with players, for type auto).");
		range = config.getInt("range", Configuration.CATEGORY_GENERAL, 8, 1, 100, "The range potions will be shared within");
		type = config.getString("type", Configuration.CATEGORY_GENERAL, "auto", "Type of sharing." + Configuration.NEW_LINE + "\"auto\": automatically for nearby entities," + Configuration.NEW_LINE + "\"hit\": you need to hit an entity with any potion (or glass bottle) in your hand", new String[] { "auto", "hit" });
		//		hitItems=config.getStringList("hitItems", Configuration.CATEGORY_GENERAL, new String[]{"potion"}, "Items you need in your hand to share potions (for type \"hit\").");
		if (config.hasChanged())
			config.save();
	}

	private static void sharePotion(EntityPlayer player, EntityLivingBase entity) {
		Iterable<PotionEffect> potions = player.getActivePotionEffects();
		//		if (SharedPotions.onlyPositive)
		//			potions = Iterables.filter(potions, (PotionEffect pe) -> !pe.getPotion().isBadEffect());
		for (PotionEffect pe : potions)
			if (!SharedPotions.onlyPositive || !pe.getPotion().isBadEffect()) {
				PotionEffect p = PotionEffect.readCustomPotionEffectFromNBT(pe.writeCustomPotionEffectToNBT(new NBTTagCompound()));
				entity.addPotionEffect(p);
			}
	}

	@SubscribeEvent
	public static void update(LivingUpdateEvent event) {
		if (!SharedPotions.type.equalsIgnoreCase("auto"))
			return;
		if (!event.getEntityLiving().worldObj.isRemote && event.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
			if (player.ticksExisted % 35 == 0) {
				Set<EntityLivingBase> ents = Sets.newHashSet(player.worldObj.getEntitiesWithinAABB(SharedPotions.onlyPlayer ? EntityPlayer.class : EntityLivingBase.class, new AxisAlignedBB(player.posX + SharedPotions.range, player.posY + SharedPotions.range, player.posZ + SharedPotions.range, player.posX - SharedPotions.range, player.posY - SharedPotions.range, player.posZ - SharedPotions.range)));
				if (SharedPotions.global)
					ents.addAll(player.worldObj.playerEntities);
				ents.remove(player);
				//Lists.<String>newArrayList().stream().filter(s->s.equalsIgnoreCase("nanana")).findFirst().isPresent();
				for (EntityLivingBase entity : ents)
					sharePotion(player, entity);

			}
		}
	}

	@SubscribeEvent
	public static void attack(AttackEntityEvent event) {
		if (!SharedPotions.type.equalsIgnoreCase("hit"))
			return;
		Item[] items = new Item[] { Items.POTIONITEM, Items.SPLASH_POTION, Items.GLASS_BOTTLE, Items.LINGERING_POTION };
		if (!event.getEntityPlayer().worldObj.isRemote && event.getEntityPlayer().getHeldItemMainhand() != null && ArrayUtils.contains(items, event.getEntityPlayer().getHeldItemMainhand().getItem()) && event.getTarget() instanceof EntityLivingBase) {
			EntityLivingBase entity = (EntityLivingBase) event.getTarget();
			if (!SharedPotions.onlyPlayer || entity instanceof EntityPlayer) {
				sharePotion(event.getEntityPlayer(), entity);
				event.getEntityPlayer().addChatComponentMessage(new TextComponentString("Shared potions {" + event.getEntityPlayer().getActivePotionEffects().stream().//
						map(p -> I18n.format(p.getEffectName())).//
						collect(Collectors.joining(", ")) + "} with " + entity.getDisplayName().getFormattedText() + "."));
			}
		}
	}
}
