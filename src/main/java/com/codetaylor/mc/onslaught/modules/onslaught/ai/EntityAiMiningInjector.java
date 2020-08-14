package com.codetaylor.mc.onslaught.modules.onslaught.ai;

import com.codetaylor.mc.onslaught.modules.onslaught.data.Tag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;

public class EntityAiMiningInjector {

  public void inject(EntityLiving entity, NBTTagCompound tag) {

    if (!tag.hasKey(Tag.AI_MINING)) {
      return;
    }

    entity.tasks.addTask(1, new EntityAiMining(entity));
  }
}