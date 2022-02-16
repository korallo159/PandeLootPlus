package net.seyarada.pandeloot.compatibility.mythicmobs;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import io.lumine.xikage.mythicmobs.io.MythicConfig;
import net.seyarada.pandeloot.drops.IDrop;
import net.seyarada.pandeloot.drops.LootDrop;
import net.seyarada.pandeloot.trackers.DamageTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MythicMobsListener implements Listener {

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event)	{
        switch (event.getMechanicName().toLowerCase()) {
            case "pandeloot", "ploot" -> event.register(new MythicMobsMechanic(event.getConfig()));
        }
    }

    @EventHandler
    public void onSpawn(MythicMobSpawnEvent e) {
        MythicConfig config = e.getMobType().getConfig();
        boolean shouldTrack = config.getStringList("Rewards").size()>0;
        if(!shouldTrack) shouldTrack = config.getBoolean("Options.ScoreHologram");
        if(!shouldTrack) shouldTrack = config.getBoolean("Options.ScoreMessage");

        if(shouldTrack) DamageTracker.initTracking(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onDeath(MythicMobDeathEvent e) {
        UUID mob = e.getEntity().getUniqueId();
        if(!DamageTracker.has(mob)) return;

        MythicConfig config = e.getMobType().getConfig();
        boolean scoreMessage = config.getBoolean("Options.ScoreMessage");
        boolean scoreHologram = config.getBoolean("Options.ScoreHologram");

        List<String> strings = e.getMobType().getConfig().getStringList("Rewards");
        ArrayList<IDrop> itemsToDrop = IDrop.getAsDrop(strings);

        DamageTracker.DamageBoard damageBoard = new DamageTracker.DamageBoard((LivingEntity) e.getEntity(), DamageTracker.mobsDamageMap.get(mob));
        damageBoard.lastHit = e.getKiller();

        for(UUID uuid : DamageTracker.mobsDamageMap.get(mob).keySet()) {
            LootDrop lootDrop = new LootDrop(itemsToDrop, Bukkit.getPlayer(uuid), e.getEntity().getLocation())
                    .setDamageBoard(damageBoard)
                    .setSourceEntity(e.getEntity())
                    .build();

            if(scoreHologram) lootDrop.displayScoreHolograms();
            if(scoreMessage) lootDrop.displayScoreMessage();

            lootDrop.drop();
        }

        DamageTracker.remove(mob);
    }

}
