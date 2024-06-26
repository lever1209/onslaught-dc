package com.codetaylor.mc.onslaught.modules.onslaught.invasion.spawner;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.codetaylor.mc.onslaught.ModOnslaught;
import com.codetaylor.mc.onslaught.modules.onslaught.ModuleOnslaughtConfig;
import com.codetaylor.mc.onslaught.modules.onslaught.entity.factory.MobTemplateEntityFactory;
import com.codetaylor.mc.onslaught.modules.onslaught.event.handler.InvasionUpdateEventHandler;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionGlobalSavedData;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionPlayerData;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.InvasionSpawnDataConverterFunction;
import com.codetaylor.mc.onslaught.modules.onslaught.invasion.sampler.predicate.SpawnPredicateFactory;
import com.codetaylor.mc.onslaught.modules.onslaught.template.invasion.InvasionTemplateWave;
import com.codetaylor.mc.onslaught.modules.onslaught.template.mob.MobTemplate;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Responsible for spawning a deferred mob when its delay timer is expired. */
public class DeferredSpawner implements InvasionUpdateEventHandler.IInvasionUpdateComponent {

	private final EntityInvasionDataInjector entityInvasionDataInjector;
	private final SpawnPredicateFactory spawnPredicateFactory;
	private final InvasionSpawnDataConverterFunction invasionSpawnDataConverterFunction;
	private final Function<String, MobTemplate> mobTemplateFunction;
	private final MobTemplateEntityFactory mobTemplateEntityFactory;
	private final List<DeferredSpawnData> deferredSpawnDataList;

	public DeferredSpawner(EntityInvasionDataInjector entityInvasionDataInjector,
			SpawnPredicateFactory spawnPredicateFactory,
			InvasionSpawnDataConverterFunction invasionSpawnDataConverterFunction,
			Function<String, MobTemplate> mobTemplateFunction, MobTemplateEntityFactory mobTemplateEntityFactory,
			List<DeferredSpawnData> deferredSpawnDataList) {

		this.entityInvasionDataInjector = entityInvasionDataInjector;
		this.spawnPredicateFactory = spawnPredicateFactory;
		this.invasionSpawnDataConverterFunction = invasionSpawnDataConverterFunction;
		this.mobTemplateFunction = mobTemplateFunction;
		this.mobTemplateEntityFactory = mobTemplateEntityFactory;
		this.deferredSpawnDataList = deferredSpawnDataList;
	}

	@Override
	public void update(int updateIntervalTicks, InvasionGlobalSavedData invasionGlobalSavedData, PlayerList playerList,
			long worldTime) {

		if (this.deferredSpawnDataList.isEmpty()) {
			return;
		}

		for (int i = this.deferredSpawnDataList.size() - 1; i >= 0; i--) {
			DeferredSpawnData deferredSpawnData = this.deferredSpawnDataList.get(i);
			deferredSpawnData.setTicksRemaining(deferredSpawnData.getTicksRemaining() - updateIntervalTicks);

			if (deferredSpawnData.getTicksRemaining() <= 0) {
				EntityPlayerMP player = playerList.getPlayerByUUID(deferredSpawnData.getPlayerUuid());

				if (player != null && !player.isDead
						&& player.world.provider.getDimension() == deferredSpawnData.getDimensionId()) {
					this.attemptSpawn(player.world, deferredSpawnData);
				}

				this.deferredSpawnDataList.remove(i);
			}
		}
	}

	private void attemptSpawn(World world, DeferredSpawnData deferredSpawnData) {

		if (deferredSpawnData.getSpawnType() == InvasionTemplateWave.EnumSpawnType.ground
				&& !world.isSideSolid(deferredSpawnData.getPos().down(), EnumFacing.UP)) {

			InvasionTemplateWave.SecondaryMob secondaryMob = deferredSpawnData.getSecondaryMob();

			if (ModuleOnslaughtConfig.DEBUG.INVASION_SPAWNERS) {
				ModOnslaught.LOG.debug("Solid ground missing, attempting to spawn secondary mob "+
						secondaryMob.id);
			}

			MobTemplate mobTemplate = this.mobTemplateFunction.apply(secondaryMob.id);

			if (mobTemplate == null) {
				ModOnslaught.LOG.error("Unknown mob template id: " + secondaryMob.id);
				return;
			}

			EntityLiving entity = this.mobTemplateEntityFactory.create(mobTemplate, world);

			if (entity == null) {
				ModOnslaught.LOG.error("Unknown entity id: " + mobTemplate.id);
				return;
			}

			EntityLiving deferredSpawnDataEntity = deferredSpawnData.getEntityLiving();
			entity.setPosition(deferredSpawnDataEntity.posX, deferredSpawnDataEntity.posY,
					deferredSpawnDataEntity.posZ);

			InvasionPlayerData.InvasionData.SpawnData spawnData = this.invasionSpawnDataConverterFunction
					.apply(secondaryMob.spawn);
			Predicate<EntityLiving> predicate = this.spawnPredicateFactory.create(spawnData);

			if (!predicate.test(entity)) {

				if (ModuleOnslaughtConfig.DEBUG.INVASION_SPAWNERS) {
					ModOnslaught.LOG.debug("Spawn predicate test failed, spawnData=" + spawnData);
				}

				return;
			}

			// apply player target, chase long distance, and invasion data tags
			this.entityInvasionDataInjector.inject(entity, deferredSpawnData.getInvasionUuid(),
					deferredSpawnData.getPlayerUuid(), deferredSpawnData.getWaveIndex(),
					deferredSpawnData.getMobIndex());

			if (world.spawnEntity(entity)) {
				entity.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);

			} else {
				ModOnslaught.LOG.error("Unable to spawn entity: " + entity);
			}

		} else {
			EntityLiving entity = deferredSpawnData.getEntityLiving();

			// Check and clear collisions
			this.checkAndClearCollisions(world, entity);

			// apply player target, chase long distance, and invasion data tags
			this.entityInvasionDataInjector.inject(entity, deferredSpawnData.getInvasionUuid(),
					deferredSpawnData.getPlayerUuid(), deferredSpawnData.getWaveIndex(),
					deferredSpawnData.getMobIndex());

			if (world.spawnEntity(entity)) {
				entity.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);

			} else {
				ModOnslaught.LOG.error("Unable to spawn entity: " + entity);
			}
		}
	}

	private void checkAndClearCollisions(World world, EntityLiving entity) {

		List<AxisAlignedBB> collisionBoxes = world.getCollisionBoxes(entity, entity.getEntityBoundingBox());

		if (!collisionBoxes.isEmpty()) {

			for (AxisAlignedBB collisionBox : collisionBoxes) {
				BlockPos blockPos = new BlockPos(collisionBox.getCenter());
				world.setBlockToAir(blockPos);
			}

			world.newExplosion(null, entity.posX, entity.posY, entity.posZ, 7, false, false);

			if (ModuleOnslaughtConfig.DEBUG.INVASION_SPAWNERS) {
				ModOnslaught.LOG.debug("Clearing deferred spawn collisions: " + collisionBoxes);
			}
		}
	}
}
