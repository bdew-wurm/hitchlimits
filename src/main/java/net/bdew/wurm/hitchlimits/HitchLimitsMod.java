package net.bdew.wurm.hitchlimits;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HitchLimitsMod implements WurmServerMod, Initable, Configurable {
    private static final Logger logger = Logger.getLogger("HitchLimitsMod");

    static float maxHitchableRating = 1;
    static float hitchingStrengthModifier = 1;
    static float minimumStrengthCap = 1;
    static boolean preventUnhitchingExPets = false;
    static boolean pacifyHitchedCreatures = false;
    static boolean preventAgeUnhitching = false;

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }

    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

    @Override
    public void configure(Properties properties) {
        maxHitchableRating = Float.parseFloat(properties.getProperty("maxHitchableRating"));
        hitchingStrengthModifier = Float.parseFloat(properties.getProperty("hitchingStrengthModifier"));
        minimumStrengthCap = Float.parseFloat(properties.getProperty("minimumStrengthCap"));
        preventUnhitchingExPets = Boolean.parseBoolean(properties.getProperty("preventUnhitchingExPets"));
        pacifyHitchedCreatures = Boolean.parseBoolean(properties.getProperty("pacifyHitchedCreatures"));
        preventAgeUnhitching = Boolean.parseBoolean(properties.getProperty("preventAgeUnhitching"));
        logInfo("maxHitchableRating = " + maxHitchableRating);
        logInfo("hitchingStrengthModifier = " + hitchingStrengthModifier);
        logInfo("minimumStrengthCap = " + minimumStrengthCap);
        logInfo("preventUnhitchingExPets = " + preventUnhitchingExPets);
        logInfo("pacifyHitchedCreatures = " + pacifyHitchedCreatures);
        logInfo("preventAgeUnhitching = " + preventAgeUnhitching);
    }

    @Override
    public void init() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            CtClass ctVehicleBehaviour = classPool.getCtClass("com.wurmonline.server.behaviours.VehicleBehaviour");

            ctVehicleBehaviour.getMethod("actionHitch", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("isDomestic")) {
                        m.replace("$_ = net.bdew.wurm.hitchlimits.HitchHooks.canHitch($0);");
                        logInfo(String.format("Hooked domestic check in %s.%s at %d",
                                m.where().getDeclaringClass().getName(),
                                m.where().getMethodInfo().getName(),
                                m.getLineNumber()));
                    } else if (m.getMethodName().equals("isStrongEnoughToDrag")) {
                        m.replace("$_ = net.bdew.wurm.hitchlimits.HitchHooks.isStrongEnoughToDrag($1,$2);");
                        logInfo(String.format("Hooked strength check in %s.%s at %d",
                                m.where().getDeclaringClass().getName(),
                                m.where().getMethodInfo().getName(),
                                m.getLineNumber()));
                    }
                }
            });

            CtClass ctVehicle = classPool.getCtClass("com.wurmonline.server.behaviours.Vehicle");

            ctVehicle.getMethod("calculateNewVehicleSpeed", "(Z)B").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("getStrengthSkill")) {
                        m.replace("$_ = net.bdew.wurm.hitchlimits.HitchHooks.getStrengthSkill($0);");
                        logInfo(String.format("Hooked getStrengthSkill in %s.%s at %d",
                                m.where().getDeclaringClass().getName(),
                                m.where().getMethodInfo().getName(),
                                m.getLineNumber()));
                    }
                }
            });

            CtClass ctCreature = classPool.getCtClass("com.wurmonline.server.creatures.Creature");

            if (preventUnhitchingExPets) {
                ctCreature.getMethod("setKingdomId", "(BZZZ)Z").instrument(new ExprEditor() {
                    @Override
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getMethodName().equals("setCreatureDead")) {
                            m.replace("net.bdew.wurm.hitchlimits.HitchHooks.setCreatureDeadMaybe($1);");
                            logInfo(String.format("Hooked setCreatureDead in %s.%s at %d",
                                    m.where().getDeclaringClass().getName(),
                                    m.where().getMethodInfo().getName(),
                                    m.getLineNumber()));
                        }
                    }
                });
            }

            if (pacifyHitchedCreatures) {
                ctCreature.getMethod("getAttitude", "(Lcom/wurmonline/server/creatures/Creature;)B")
                        .insertAfter("$_=net.bdew.wurm.hitchlimits.HitchHooks.getAttitudeHook(this,$1,$_);");
            }

            CtClass ctCreatureStatus = classPool.getCtClass("com.wurmonline.server.creatures.CreatureStatus");

            ctCreatureStatus.getMethod("pollAge", "(I)Z").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("isDomestic")) {
                        m.replace("$_ = !net.bdew.wurm.hitchlimits.HitchHooks.canAgePollUnhitch(this.statusHolder);");
                        logInfo(String.format("Hooked age-based unhitch check in %s.%s at %d",
                                m.where().getDeclaringClass().getName(),
                                m.where().getMethodInfo().getName(),
                                m.getLineNumber()));
                    }
                }
            });


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
