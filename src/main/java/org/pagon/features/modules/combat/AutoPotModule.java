package org.pagon.features.modules.combat;

import org.pagon.Pagon;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.inventory.Result;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import static org.pagon.util.inventory.InventoryUtil.FULL_SCOPE;
import static org.pagon.util.inventory.InventoryUtil.HOTBAR_SCOPE;

public class AutoPotModule extends Module {
    private final Setting<Boolean> swapAlternative = bool("SwapAlternative", false);
    private final Setting<PotionType> potionType = mode("Potion", PotionType.STRENGTH);

    public AutoPotModule() {
        super("AutoPot", "Throws pots for you", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (nullCheck() || mc.gameMode == null) {
            return;
        }

        Result result = InventoryUtil.find(stack -> stack.is(Items.SPLASH_POTION) && matchesPotion(stack), swapAlternative.getValue() ? FULL_SCOPE : HOTBAR_SCOPE);
        if (!result.found()) {
            return;
        }

        float yaw = Pagon.rotationManager.hasServerRotation()
                ? Pagon.rotationManager.getServerYaw()
                : mc.player.getYRot();
        float prevYaw = mc.player.getYRot();
        float prevPitch = mc.player.getXRot();

        Pagon.rotationManager.setPlayerRotations(yaw, 90.0f);
        mc.player.setYRot(yaw);
        mc.player.yHeadRot = yaw;
        mc.player.setXRot(90.0f);

        final boolean[] used = {false};
        InventoryUtil.withSwap(result, swapResult -> {
            usePotion(swapResult.hand());
            used[0] = true;
        });

        mc.player.setYRot(prevYaw);
        mc.player.yHeadRot = prevYaw;
        mc.player.setXRot(prevPitch);

        if (used[0]) {
            disable();
        }
    }

    private void usePotion(InteractionHand hand) {
        mc.gameMode.useItem(mc.player, hand);
    }

    private boolean matchesPotion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) {
            return false;
        }
        return switch (potionType.getValue()) {
            case STRENGTH -> matchesAny(contents, Potions.STRENGTH, Potions.LONG_STRENGTH, Potions.STRONG_STRENGTH);
            case TURTLE -> matchesAny(contents, Potions.TURTLE_MASTER, Potions.LONG_TURTLE_MASTER, Potions.STRONG_TURTLE_MASTER);
        };
    }

    private boolean matchesAny(PotionContents contents,
                               net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> normal,
                               net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> longer,
                               net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> stronger) {
        return contents.is(normal) || contents.is(longer) || contents.is(stronger);
    }

    public enum PotionType {
        STRENGTH,
        TURTLE
    }
}


