package mrriegel.limelib;

import java.util.Set;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

@Mod(modid = SharedPotions.MODID, name = SharedPotions.NAME, version = SharedPotions.VERSION)
@EventBusSubscriber
public class SharedPotions {

	@Instance(SharedPotions.MODID)
	public static SharedPotions INSTANCE;

	public static final String VERSION = "1.0.0";
	public static final String NAME = "Shared Potions";
	public static final String MODID = "sharedpotions";

	public static final Logger log = LogManager.getLogger(NAME);

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
		global = config.getBoolean("global", Configuration.CATEGORY_GENERAL, false, "Share potions with the whole dimension (only with players).");
		range = config.getInt("range", Configuration.CATEGORY_GENERAL, 5, 1, 100, "The range potions will be shared within");
		type = config.getString("type", Configuration.CATEGORY_GENERAL, "auto", "Type of sharing."+Configuration.NEW_LINE+"\"auto\": automatically for nearby entities,"+Configuration.NEW_LINE+"\"hit\": you need to hit an entity with any potion", new String[] { "auto", "hit" });
		if (config.hasChanged())
			config.save();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
	}

	private static void sharePotion(EntityPlayer player, EntityLivingBase entity) {
		Iterable<PotionEffect> potions = player.getActivePotionEffects();
		//		if (SharedPotions.onlyPositive)
		//			potions = Iterables.filter(potions, (PotionEffect pe) -> !pe.getPotion().isBadEffect());
		for (PotionEffect pe : potions)
			if (!SharedPotions.onlyPositive || !pe.getPotion().isBadEffect())
				entity.addPotionEffect(PotionEffect.readCustomPotionEffectFromNBT(pe.writeCustomPotionEffectToNBT(new NBTTagCompound())));
	}

	@SubscribeEvent
	public static void update(LivingUpdateEvent event) {
		if (!SharedPotions.type.equalsIgnoreCase("auto"))
			return;
		if (!event.getEntityLiving().worldObj.isRemote && event.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
			if (player.ticksExisted % 35 == 0) {
				Vec3d p = new Vec3d(player.posX, player.posY, player.posZ);
				Set<EntityLivingBase> ents = Sets.newHashSet(player.worldObj.getEntitiesWithinAABB(SharedPotions.onlyPlayer ? EntityPlayer.class : EntityLivingBase.class, new AxisAlignedBB(p.addVector(SharedPotions.range, SharedPotions.range, SharedPotions.range), p.addVector(-SharedPotions.range, -SharedPotions.range, -SharedPotions.range))));
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
		if (!event.getEntityPlayer().worldObj.isRemote && event.getEntityPlayer().getHeldItemMainhand() != null && (event.getEntityPlayer().getHeldItemMainhand().getItem() == Items.POTIONITEM || event.getEntityPlayer().getHeldItemMainhand().getItem() == Items.SPLASH_POTION || event.getEntityPlayer().getHeldItemMainhand().getItem() == Items.GLASS_BOTTLE) && event.getTarget() instanceof EntityLivingBase) {
			EntityLivingBase entity = (EntityLivingBase) event.getTarget();
			if (!SharedPotions.onlyPlayer || entity instanceof EntityPlayer) {
				sharePotion(event.getEntityPlayer(), entity);
				event.getEntityPlayer().addChatComponentMessage(new TextComponentString("Shared potions with " + entity.getDisplayName().getFormattedText() + "."));
			}
		}
	}
}
