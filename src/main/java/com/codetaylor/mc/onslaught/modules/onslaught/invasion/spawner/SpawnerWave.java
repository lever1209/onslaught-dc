package com.codetaylor.mc.onslaught.modules.onslaught.invasion.spawner;

import java.util.List;
import java.util.UUID;

import com.codetaylor.mc.onslaught.ModOnslaught;
import com.codetaylor.mc.onslaught.modules.onslaught.ModuleOnslaughtConfig;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionPlayerData;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionSpawnDataConverterFunction;
import com.codetaylor.mc.onslaught.modules.onslaught.template.invasion.InvasionTemplateWave;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Responsible for attempting to spawn an invasion wave for the given player.
 *
 * <p>
 * Short-circuits if the time spent spawning exceeds the allotted maximum.
 *
 * <p>
 * Attempts in this order: - Try to spawn mob normally - Try to force spawn
 * (magic spawns) - Try to spawn secondary mob normally
 */
public class SpawnerWave {

	private static final int MAX_ALLOWED_SPAWN_TIME_MS = 500;

	private final InvasionSpawnDataConverterFunction invasionSpawnDataConverterFunction;
	private final SpawnerMob spawnerMob;
	private final SpawnerMobForced spawnerMobForced;
	private final ActiveMobCounter activeMobCounter;

	public SpawnerWave(InvasionSpawnDataConverterFunction invasionSpawnDataConverterFunction, SpawnerMob spawnerMob,
			SpawnerMobForced spawnerMobForced, ActiveMobCounter activeMobCounter) {

		this.invasionSpawnDataConverterFunction = invasionSpawnDataConverterFunction;
		this.spawnerMob = spawnerMob;
		this.spawnerMobForced = spawnerMobForced;
		this.activeMobCounter = activeMobCounter;
	}

	/** @return true to stop the spawn loop */
	public boolean attemptSpawnWave(long startTimestamp, EntityPlayerMP player, UUID invasionUuid, int waveIndex,
			InvasionPlayerData.InvasionData.WaveData waveData, InvasionTemplateWave.SecondaryMob secondaryMob) {

		List<InvasionPlayerData.InvasionData.MobData> mobDataList = waveData.getMobDataList();

		for (int mobIndex = 0; mobIndex < mobDataList.size(); mobIndex++) {
			InvasionPlayerData.InvasionData.MobData mobData = mobDataList.get(mobIndex);

			if (mobData.getKilledCount() >= mobData.getTotalCount()) {
				continue;
			}

			World world = player.world;
			BlockPos position = player.getPosition();
			UUID playerUuid = player.getUniqueID();
			InvasionPlayerData.InvasionData.SpawnData spawnData = mobData.getSpawnData();

			int activeMobs = this.activeMobCounter.countActiveMobs(world.loadedEntityList, invasionUuid, playerUuid,
					waveIndex, mobIndex);
			int remainingMobs = mobData.getTotalCount() - mobData.getKilledCount() - activeMobs;

			if (ModuleOnslaughtConfig.DEBUG.INVASION_SPAWNERS && remainingMobs > 0) {
				String message = String.format("Attempting to spawn %d mobs of type %s for player %s in wave %d",
						remainingMobs, mobData.getMobTemplateId(), player.getName(), waveIndex);
				ModOnslaught.LOG.debug(message);
			}

			while (remainingMobs > 0) {

				// Try to spawn mob normally
				// Try to force spawn (magic spawns)
				// Try to spawn secondary mob normally

				if (this.attemptSpawn(invasionUuid, waveIndex, secondaryMob, mobIndex, mobData, world, position,
						playerUuid, spawnData)) {

					remainingMobs -= 1;

					long elapsedTimeMs = System.currentTimeMillis() - startTimestamp;

					if (elapsedTimeMs > MAX_ALLOWED_SPAWN_TIME_MS) {

						if (ModuleOnslaughtConfig.DEBUG.INVASION_SPAWNERS) {
							String message = String.format("Spawning exceeded max allowed time: %d > %d", elapsedTimeMs,
									MAX_ALLOWED_SPAWN_TIME_MS);
							ModOnslaught.LOG.debug(message);
						}

						return true; // Stop spawning
					}

				} else {
					// If we can't find a spawn location for a mob, break here else
					// we loop forevah!
					break;
				}
			}
		}

		return false; // Continue spawning
	}

	private boolean attemptSpawn(UUID invasionUuid, int waveIndex, InvasionTemplateWave.SecondaryMob secondaryMob,
			int mobIndex, InvasionPlayerData.InvasionData.MobData mobData, World world, BlockPos position,
			UUID playerUuid, InvasionPlayerData.InvasionData.SpawnData spawnData) {

		if (this.spawnerMob.attemptSpawnMob(world, position, invasionUuid, playerUuid, waveIndex, mobIndex,
				mobData.getMobTemplateId(), spawnData)) {
			return true;
		}

		if ((mobData.isForceSpawn() && this.spawnerMobForced.attemptSpawnMob(world, position, invasionUuid, playerUuid,
				waveIndex, mobIndex, mobData.getMobTemplateId(), spawnData, secondaryMob))) {
			return true;
		}

		InvasionPlayerData.InvasionData.SpawnData secondarySpawnData = this.invasionSpawnDataConverterFunction
				.apply(secondaryMob.spawn);

		return this.spawnerMob.attemptSpawnMob(world, position, invasionUuid, playerUuid, waveIndex, mobIndex,
				secondaryMob.id, secondarySpawnData);
	}
}
