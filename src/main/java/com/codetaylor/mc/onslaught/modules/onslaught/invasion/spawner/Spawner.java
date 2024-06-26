package com.codetaylor.mc.onslaught.modules.onslaught.invasion.spawner;

import java.util.List;
import java.util.function.Function;

import com.codetaylor.mc.onslaught.modules.onslaught.event.handler.InvasionUpdateEventHandler;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionGlobalSavedData;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionPlayerData;
import com.codetaylor.mc.onslaught.modules.onslaught.template.invasion.InvasionTemplate;
import com.codetaylor.mc.onslaught.modules.onslaught.template.invasion.InvasionTemplateWave;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;

/** Responsible for triggering spawn attempts for active invasions. */
public class Spawner implements InvasionUpdateEventHandler.IInvasionUpdateComponent {

	private final Function<String, InvasionTemplate> invasionTemplateFunction;
	private final SpawnerWave spawnerWave;

	public Spawner(Function<String, InvasionTemplate> invasionTemplateFunction, SpawnerWave spawnerWave) {

		this.invasionTemplateFunction = invasionTemplateFunction;
		this.spawnerWave = spawnerWave;
	}

	@Override
	public void update(int updateIntervalTicks, InvasionGlobalSavedData invasionGlobalSavedData, PlayerList playerList,
			long worldTime) {

		long start = System.currentTimeMillis();

		for (EntityPlayerMP player : playerList.getPlayers()) {
			InvasionPlayerData data = invasionGlobalSavedData.getPlayerData(player.getUniqueID());

			if (data.getInvasionState() != InvasionPlayerData.EnumInvasionState.Active) {
				continue;
			}

			InvasionPlayerData.InvasionData invasionData = data.getInvasionData();

			// If the invasion state is active, the invasion data should never be null.
			if (invasionData == null) {
				continue;
			}

			InvasionTemplate invasionTemplate = this.invasionTemplateFunction
					.apply(invasionData.getInvasionTemplateId());

			if (invasionTemplate == null) {
				// If this happens, the invasion id has changed since the invasion data was
				// created.
				// Nullify the invasion to flag it as complete.
				// The active to waiting state change processor will clean this up.
				data.setInvasionData(null);
				continue;
			}

			List<InvasionPlayerData.InvasionData.WaveData> waveDataList = invasionData.getWaveDataList();

			for (int waveIndex = 0; waveIndex < waveDataList.size(); waveIndex++) {
				InvasionPlayerData.InvasionData.WaveData waveData = waveDataList.get(waveIndex);
				InvasionTemplateWave templateWave = invasionTemplate.waves[waveIndex];

				if (waveData.getDelayTicks() <= 0) {

					if (this.spawnerWave.attemptSpawnWave(start, player, invasionData.getInvasionUuid(), waveIndex,
							waveData, templateWave.secondaryMob)) {
						return;
					}
				}
			}
		}
	}
}
