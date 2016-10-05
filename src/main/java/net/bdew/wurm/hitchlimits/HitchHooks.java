package net.bdew.wurm.hitchlimits;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
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

    public static void setCreatureDeadMaybe(Creature creature) {
        if (creature.isPlayer() || !creature.isHitched())
            Creatures.getInstance().setCreatureDead(creature);
    }

    public static byte getAttitudeHook(Creature from, Creature to, byte original) {
        if (original != 2) return original;
        if (from.target == to.getWurmId() || to.target == from.getWurmId()) return 2;
        if (from.isHitched() && to.isPlayer()) return 0;
        if (from.isPlayer() && to.isHitched()) return 0;
        return 2;
    }

    public static boolean canAgePollUnhitch(Creature creature) {
        return !creature.isDomestic() && !HitchLimitsMod.preventAgeUnhitching &&
                creature.getStatus().getBattleRatingTypeModifier() > HitchLimitsMod.maxHitchableRating;
    }
}
