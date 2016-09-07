package net.bdew.wurm.hitchlimits;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class HitchHooks {
    public static boolean canHitch(Creature creature) {
        return creature.isDomestic() || creature.getStatus().getBattleRatingTypeModifier() <= HitchLimitsMod.maxHitchableRating;
    }

    public static boolean isStrongEnoughToDrag(Creature creature, Item vehicle) {
        return creature.getStrengthSkill() * HitchLimitsMod.hitchingStrengthModifier > vehicle.getTemplate().getWeightGrams() / 10000.0f;
    }

    public static double getStrengthSkill(Creature creature) {
        return Math.max(creature.getStrengthSkill(), HitchLimitsMod.minimumStrengthCap);
    }
}
