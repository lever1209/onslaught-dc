package com.codetaylor.mc.onslaught.modules.onslaught.invasion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.codetaylor.mc.onslaught.ModOnslaught;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/** Responsible for persisting all players' invasion data. */
public class InvasionGlobalSavedData extends WorldSavedData implements Function<UUID, InvasionPlayerData> {

	private static final String DATA_NAME = ModOnslaught.MOD_ID + "_InvasionData";
	private final Map<UUID, InvasionPlayerData> playerDataMap;

	public InvasionGlobalSavedData() {

		this(DATA_NAME);
	}

	public InvasionGlobalSavedData(String name) {

		super(name);
		this.playerDataMap = new HashMap<>();
	}

	public static InvasionGlobalSavedData get(World world) {

		MapStorage storage = world.getMapStorage();

		if (storage == null) {
			throw new RuntimeException("Null world storage");
		}

		InvasionGlobalSavedData instance = (InvasionGlobalSavedData) storage
				.getOrLoadData(InvasionGlobalSavedData.class, DATA_NAME);

		if (instance == null) {
			instance = new InvasionGlobalSavedData();
			storage.setData(DATA_NAME, instance);
		}
		return instance;
	}

	@Override
	public InvasionPlayerData apply(UUID uuid) {

		return this.getPlayerData(uuid);
	}

	public InvasionPlayerData getPlayerData(UUID uuid) {

		InvasionPlayerData invasionPlayerData = this.playerDataMap.get(uuid);

		if (invasionPlayerData == null) {
			invasionPlayerData = new InvasionPlayerData();
			this.playerDataMap.put(uuid, invasionPlayerData);
			this.markDirty();
		}

		return invasionPlayerData;
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound tag) {

		NBTTagList tagList = tag.getTagList("PlayerData", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < tagList.tagCount(); i++) {
			NBTTagCompound tagEntry = (NBTTagCompound) tagList.get(i);
			UUID uuid = UUID.fromString(tagEntry.getString("UUID"));
			InvasionPlayerData data = new InvasionPlayerData();
			data.deserializeNBT(tagEntry.getCompoundTag("Data"));
			this.playerDataMap.put(uuid, data);
		}
	}

	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tag) {

		NBTTagList tagList = new NBTTagList();

		for (Map.Entry<UUID, InvasionPlayerData> entry : this.playerDataMap.entrySet()) {
			NBTTagCompound tagEntry = new NBTTagCompound();
			tagEntry.setString("UUID", entry.getKey().toString());
			tagEntry.setTag("Data", entry.getValue().serializeNBT());
			tagList.appendTag(tagEntry);
		}

		tag.setTag("PlayerData", tagList);

		return tag;
	}
}
