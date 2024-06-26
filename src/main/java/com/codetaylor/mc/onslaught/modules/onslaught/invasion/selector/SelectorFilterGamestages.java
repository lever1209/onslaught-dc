package com.codetaylor.mc.onslaught.modules.onslaught.invasion.selector;

import java.util.Map;
import java.util.function.Predicate;

import com.codetaylor.mc.athenaeum.integration.gamestages.GameStages;
import com.codetaylor.mc.athenaeum.integration.gamestages.Stages;
import com.codetaylor.mc.onslaught.modules.onslaught.template.invasion.InvasionTemplate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

/**
 * Responsible for filtering out invasion templates that do not match the given
 * player's gamestages.
 */
public class SelectorFilterGamestages implements Predicate<Map.Entry<String, InvasionTemplate>> {

	private static final String GAMESTAGES_MOD_ID = "gamestages";

	private final EntityPlayer player;

	public SelectorFilterGamestages(EntityPlayer player) {

		this.player = player;
	}

	@Override
	public boolean test(Map.Entry<String, InvasionTemplate> entry) {

		if (Loader.isModLoaded(GAMESTAGES_MOD_ID)) {
			InvasionTemplate template = entry.getValue();
			Stages gamestages = template.selector.gamestages;
			return GameStages.allowed(this.player, gamestages);
		}

		return true;
	}
}
