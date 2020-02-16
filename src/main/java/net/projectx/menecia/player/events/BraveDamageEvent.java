package net.projectx.menecia.player.events;

import net.projectx.menecia.DataManager;
import net.projectx.menecia.Menecia;
import net.projectx.menecia.mobs.Mob;
import net.projectx.menecia.mobs.MobUtil;
import net.projectx.menecia.player.Brave;
import net.projectx.menecia.resources.Icons;
import net.projectx.menecia.utilities.Hologram;
import net.projectx.menecia.utilities.Utils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class BraveDamageEvent implements Listener {

    private Menecia plugin;
    private DataManager dataManager;
    private static final int damageIndicatorShowTick = 20;
    private static final int healthBarScale = 1;
    private Map<LivingEntity, Map<UUID, Double>> damageMap = new HashMap<>();
    private Map<LivingEntity, BossBar> healthBarMap = new HashMap<>();
    private Map<UUID, BossBar> healthBarCache = new HashMap<>();

    public BraveDamageEvent(Menecia plugin) {
        this.plugin = plugin;
        dataManager = plugin.getDataManager();
    }

    @EventHandler
    private void onEvent(EntityDamageByEntityEvent event) {
        if ((event.getDamager() instanceof Player) && (MobUtil.isMob(event.getEntity()))) {
            Player braveEntity = (Player) event.getDamager();
            Brave brave = dataManager.getBrave(braveEntity);
            Location braveLocation = braveEntity.getLocation();
            LivingEntity mobEntity = (LivingEntity) event.getEntity();
            Mob mob = MobUtil.getMobInstance(mobEntity);
            Location mobLocation = mobEntity.getLocation();

            //TODO: Attack Range
            if (checkDistance(braveEntity, mobEntity) > 2) return;

            double damage = calculateDamage(braveEntity);
            updateDamage(braveEntity, mobEntity, damage);
            displayDamageEffect(mobEntity);
            displayDamageIndicator(damage, mobLocation);
            takeKnockback(mobEntity, braveEntity);

            double mobHealth = mobEntity.getHealth();
            mobHealth -= damage;
            if (mobHealth <= 0) {
                int totalDamage = (int) getTotalDamage(mobEntity, braveEntity);
                braveEntity.sendActionBar(Utils.color("&4You have took " + totalDamage  + " damage point!"));
                killMob(mobEntity);
            } else {
                mobEntity.setHealth(mobHealth);
                showHealthBar(braveEntity, mobEntity);
            }
        }
    }

    private double calculateDamage(Player damager) {
        if (damager.getInventory().getItemInMainHand().getType() == Material.WOODEN_SWORD) {
            return 1;
        }
        return 0;
    }

    private double getTotalDamage(LivingEntity mobEntity, Player braveEntity) {
        return damageMap.get(mobEntity).get(braveEntity.getUniqueId());
    }

    private void updateDamage(Player braveEntity, LivingEntity mobEntity, double damage) {
        damageMap.putIfAbsent(mobEntity, new HashMap<UUID, Double>() {{ put(braveEntity.getUniqueId(), 0D); }});
        Map<UUID, Double> braveDamagingMap = damageMap.get(mobEntity);
        braveDamagingMap.merge(braveEntity.getUniqueId(), damage, Double::sum);
    }

    private double checkDistance(Player player, Entity entity) {
        return player.getLocation().distance(entity.getLocation());
    }

    private void showHealthBar(Player braveEntity, LivingEntity mobEntity) {
        healthBarMap.putIfAbsent(mobEntity, createHealthBar(mobEntity));
        BossBar healthBar = healthBarMap.get(mobEntity);
        cacheHealthBarForPlayer(braveEntity, healthBar);
        if (!healthBar.getPlayers().contains(braveEntity)) healthBar.addPlayer(braveEntity);
        updateHealthBar(mobEntity);
    }

    private void cacheHealthBarForPlayer(Player braveEntity, BossBar newHealthBar) {
        if (healthBarCache.get(braveEntity.getUniqueId()) != null) {
            if (healthBarCache.get(braveEntity.getUniqueId()) != newHealthBar) {
                healthBarCache.get(braveEntity.getUniqueId()).removePlayer(braveEntity);
            }
        }
        healthBarCache.put(braveEntity.getUniqueId(), newHealthBar);
    }

    private void updateHealthBar(LivingEntity mobEntity) {
        BossBar healthBar = healthBarMap.get(mobEntity);
        double mobMaxHealth = MobUtil.getMobInstance(mobEntity).getMaxHealth();
        double mobHealth = mobEntity.getHealth();
        double healthBarValue = (mobHealth / mobMaxHealth) * healthBarScale;
        if (mobHealth <= 0) healthBarValue = 0;
        healthBar.setProgress(healthBarValue);
        String mobDisplayName = MobUtil.getDisplayNameWithLevel(MobUtil.getMobInstance(mobEntity));
        healthBar.setTitle(Utils.color(mobDisplayName + " &f- &c" + (int) mobHealth + " &4" + Icons.RED_HEART));
    }

    private BossBar createHealthBar(LivingEntity mobEntity) {
        String mobDisplayName = MobUtil.getDisplayNameWithLevel(MobUtil.getMobInstance(mobEntity));
        int health = (int) mobEntity.getHealth();
        String display = Utils.color(mobDisplayName + " &f- &c" + health + " &4" + Icons.RED_HEART);
        return Bukkit.createBossBar(display, BarColor.RED, BarStyle.SEGMENTED_20);
    }

    private void displayDamageIndicator(double damage, Location location) {
        double x = ThreadLocalRandom.current().nextDouble(-1.25, 1.25);
        double y = ThreadLocalRandom.current().nextDouble(0, 1.5);
        double z = ThreadLocalRandom.current().nextDouble(-1.25, 1.25);
        Entity damageIndicator = Hologram.draw("&c-" + (int) damage + " &4" + Icons.RED_HEART,
                location.toCenterLocation().add(x, y, z));
        plugin.runTaskLater(damageIndicator::remove, damageIndicatorShowTick);
    }

    private void killMob(LivingEntity mobEntity) {
        mobEntity.setHealth(0);
        damageMap.remove(mobEntity);
        if (healthBarMap.containsKey(mobEntity)) healthBarMap.get(mobEntity).removeAll();
        healthBarMap.remove(mobEntity);
    }

    private void displayDamageEffect(Entity entity) {
        entity.playEffect(EntityEffect.HURT);
        entity.getLocation().getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                entity.getLocation().toCenterLocation(), 4, 0.0, 0.0, 0.0, 0.3);
    }

    private void takeKnockback(Entity victim, Entity damager) {
        Location knowbackLocation = damager.getLocation().clone();
        knowbackLocation.setPitch(0);
        victim.setVelocity(knowbackLocation.getDirection().normalize().multiply(0.05));
        if (victim.isOnGround()) {
            victim.setVelocity(victim.getVelocity().setY(0.1));
        }
    }

}