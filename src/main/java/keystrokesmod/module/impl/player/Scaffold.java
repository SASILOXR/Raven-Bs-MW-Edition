package keystrokesmod.module.impl.player;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.*;

public class Scaffold extends Module {
    private SliderSetting motion;
    private SliderSetting rotation;
    private SliderSetting fastScaffold;
    private SliderSetting precision;
    private SliderSetting multiPlace;
    private ButtonSetting autoSwap;
    private ButtonSetting fastOnRMB;
    private ButtonSetting highlightBlocks;
    public ButtonSetting safeWalk;
    private ButtonSetting showBlockCount;
    private ButtonSetting delayOnJump;
    private ButtonSetting silentSwing;
    public ButtonSetting tower;
    private MovingObjectPosition placeBlock;
    private int lastSlot;
    private String[] rotationModes = new String[]{"None", "Simple", "Strict", "Precise"};
    private String[] fastScaffoldModes = new String[]{"Disabled", "Sprint", "Edge", "Jump A", "Jump B", "Jump C", "Float"};
    private String[] precisionModes = new String[]{"Very low", "Low", "Moderate", "High", "Very high"};
    private String[] multiPlaceModes = new String[]{"Disabled", "1 extra", "2 extra"};
    public float placeYaw;
    public float placePitch;
    public int at;
    public int index;
    public boolean rmbDown;
    private double startPos = -1;
    private Map<BlockPos, Timer> highlight = new HashMap<>();
    private boolean forceStrict;
    private boolean down;
    private boolean delay;
    private boolean place;
    private int add;
    private boolean placedUp;
    private float previousRotation[];
    private int blockSlot = -1;
    public int blocksPlaced;
    public BlockPos previousBlock;
    private EnumFacing[] facings = { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP };
    private BlockPos[] offsets = { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0) };

    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(motion = new SliderSetting("Motion", 1.0, 0.5, 1.2, 0.01, "x"));
        this.registerSetting(rotation = new SliderSetting("Rotation", rotationModes, 1));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", fastScaffoldModes, 0));
        this.registerSetting(precision = new SliderSetting("Precision", precisionModes, 4));
        this.registerSetting(multiPlace = new SliderSetting("Multi-place", multiPlaceModes, 0));
        this.registerSetting(autoSwap = new ButtonSetting("AutoSwap", true));
        this.registerSetting(delayOnJump = new ButtonSetting("Delay on jump", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", false));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        this.registerSetting(tower = new ButtonSetting("Tower", false));
    }

    public void onDisable() {
        placeBlock = null;
        if (lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot;
            lastSlot = -1;
        }
        delay = false;
        highlight.clear();
        add = 0;
        at = index = 0;
        startPos = -1;
        forceStrict = false;
        down = false;
        place = false;
        placedUp = false;
        blockSlot = -1;
        blocksPlaced = 0;
    }

    public void onEnable() {
        lastSlot = -1;
        startPos = mc.thePlayer.posY;
        placePitch = 85;
        previousRotation = null;
        placeYaw = 2000;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (rotation.getInput() > 0) {
            if (((rotation.getInput() == 2 && forceStrict) || rotation.getInput() == 3) && placeYaw != 2000) {
                event.setYaw(placeYaw);
                event.setPitch(placePitch);
            }
            else {
                event.setYaw(getYaw());
                event.setPitch(85);
            }
        }
        place = true;
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        delay = true;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (delay && delayOnJump.isToggled()) {
            delay = false;
            return;
        }
        final ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
                return;
            }
        }
        if (keepYPosition() && !down) {
            startPos = Math.floor(mc.thePlayer.posY);
            down = true;
        }
        else if (!keepYPosition() || Math.floor(mc.thePlayer.posY) < startPos) {
            down = false;
            placedUp = false;
        }
        if (keepYPosition() && (fastScaffold.getInput() == 3 || fastScaffold.getInput() == 4 || fastScaffold.getInput() == 5) && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            add = 0;
            if (Math.floor(mc.thePlayer.posY) == Math.floor(startPos) && fastScaffold.getInput() == 5) {
                placedUp = false;
            }
        }
        double original = startPos;
        if (fastScaffold.getInput() == 3) {
            if (groundDistance() >= 2 && add == 0) {
                original++;
                add++;
            }
        }
        else if (fastScaffold.getInput() == 4 || fastScaffold.getInput() == 5) {
            if (groundDistance() > 0 && mc.thePlayer.posY - startPos < 1.5 && mc.thePlayer.fallDistance > 0 && ((!placedUp || Utils.isDiagonal()) || fastScaffold.getInput() == 4)) {
                original = mc.thePlayer.posY;
            }
        }

        if (mc.thePlayer.onGround && Utils.isMoving() && motion.getInput() != 1.0) {
            Utils.setSpeed(Utils.getHorizontalSpeed() * motion.getInput());
        }
        int slot = getSlot();
        if (slot == -1) {
            return;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot == -1) {
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = ModuleManager.autoSwap.swapToGreaterStack.isToggled() ? slot : blockSlot;
        }
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return;
        }
        MovingObjectPosition rayCasted = null;
        float searchYaw = 35;
        switch ((int) precision.getInput()) {
            case 4:
                searchYaw = 90;
                break;
            case 3:
                searchYaw = 65;
                break;
            case 2:
                break;
            case 1:
                searchYaw = 20;
                break;
            case 0:
                searchYaw = 6;
                break;
        }

        PlaceData placeData = getBlockData(new BlockPos(mc.thePlayer.posX, keepYPosition() ? original - 1 : mc.thePlayer.posY - 1, mc.thePlayer.posZ));

        if (placeData == null || placeData.blockPos == null || placeData.enumFacing == null) {
            return;
        }

        float[] targetRotation = RotationUtils.getRotations(placeData.blockPos);
        float searchPitch[] = new float[]{78, 12};
        double closestCombinedDistance = Double.MAX_VALUE;
        double offsetWeight = 0.2D;
        for (int i = 0; i < 2; i++) {
            if (i == 1 && rayCasted == null && Utils.overPlaceable(-1)) {
                searchYaw = 180;
                searchPitch = new float[]{65, 25};
            }
            else if (i == 1) {
                break;
            }
            float[] yawSearchList = generateSearchSequence(searchYaw);
            float[] pitchSearchList = generateSearchSequence(searchPitch[1]);
            for (float checkYaw : yawSearchList) {
                float playerYaw = getYaw();
                float fixedYaw = (float) (playerYaw + checkYaw + getRandom());
                if (!Utils.overPlaceable(-1)) {
                    continue;
                }
                for (float checkPitch : pitchSearchList) {
                    float fixedPitch = RotationUtils.clampTo90((float) (targetRotation[1] + checkPitch + getRandom()));
                    MovingObjectPosition raycast = RotationUtils.rayCast(mc.playerController.getBlockReachDistance(), fixedYaw, fixedPitch);
                    if (raycast != null) {
                        if (raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            if (raycast.getBlockPos().equals(placeData.blockPos) && raycast.sideHit == placeData.getEnumFacing()) {
                                if (((ItemBlock) heldItem.getItem()).canPlaceBlockOnSide(mc.theWorld, raycast.getBlockPos(), raycast.sideHit, mc.thePlayer, heldItem)) {
                                    double offSetX = raycast.hitVec.xCoord - raycast.getBlockPos().getX();
                                    double offSetY = raycast.hitVec.yCoord - raycast.getBlockPos().getY();
                                    double offSetZ = raycast.hitVec.zCoord - raycast.getBlockPos().getZ();

                                    double distanceToCenter = Math.abs(offSetX - 0.5f) + Math.abs(offSetY - 0.5f) + Math.abs(offSetZ - 0.5f);
                                    double distanceToPreviousRotation = previousRotation != null ? Math.abs(fixedYaw - previousRotation[0]) : 0;
                                    double combinedDistance = offsetWeight * distanceToCenter + distanceToPreviousRotation / 360;

                                    if (rayCasted == null || combinedDistance < closestCombinedDistance) {
                                        closestCombinedDistance = combinedDistance;
                                        rayCasted = raycast;
                                        placeYaw = fixedYaw;
                                        placePitch = fixedPitch;

                                        if ((forceStrict(checkYaw)) && i == 1) {
                                            forceStrict = true;
                                        }
                                        else {
                                            forceStrict = false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (rayCasted != null) {
                break;
            }
        }
        if (rayCasted != null && (place || rotation.getInput() == 0)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            placeBlock = rayCasted;
            place(placeBlock, false);
            int input = (int) multiPlace.getInput();
            for (int i = 0; i < input; i++) {
                place(placeBlock, true);
            }
            place = false;
            if (placeBlock.sideHit == EnumFacing.UP && keepYPosition()) {
                placedUp = true;
            }
            previousBlock = placeData.blockPos.offset(placeData.getEnumFacing());
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !showBlockCount.isToggled()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            int blocks = totalBlocks();
            String color = "§";
            if (blocks <= 5) {
                color += "c";
            }
            else if (blocks <= 15) {
                color += "6";
            }
            else if (blocks <= 25) {
                color += "e";
            }
            else {
                color = "";
            }
            mc.fontRendererObj.drawStringWithShadow(color + blocks + " §rblock" + (blocks == 1 ? "" : "s"), scaledResolution.getScaledWidth()/2 + 8, scaledResolution.getScaledHeight()/2 + 4, -1);
        }
    }

    @Override
    public String getInfo() {
        return fastScaffoldModes[(int) fastScaffold.getInput()];
    }

    public float[] generateSearchSequence(float value) {
        int length = (int) value * 2;
        float[] sequence = new float[length + 1];

        int index = 0;
        sequence[index++] = 0;

        for (int i = 1; i <= value; i++) {
            sequence[index++] = i;
            sequence[index++] = -i;
        }

        return sequence;
    }

    public PlaceData getBlockData(BlockPos pos) {
        for (int lastCheck = 0; lastCheck < 2; lastCheck++) {
            for (int i = 0; i < offsets.length; i++) {
                BlockPos newPos = pos.add(offsets[i]);
                Block block = BlockUtils.getBlock(newPos);
                if (newPos.equals(previousBlock)) {
                    return new PlaceData(facings[i], newPos);
                }
                if (lastCheck == 0) {
                    continue;
                }
                if (!block.getMaterial().isReplaceable() && !BlockUtils.isInteractable(block)) {
                    return new PlaceData(facings[i], newPos);
                }
            }
        }
        BlockPos[] additionalOffsets = { // adjust these for perfect placement
                pos.add(-1, 0, 0),
                pos.add(1, 0, 0),
                pos.add(0, 0, 1),
                pos.add(0, 0, -1),
                pos.add(0, -1, 0),
        };
        for (int lastCheck = 0; lastCheck < 2; lastCheck++) {
            for (BlockPos additionalPos : additionalOffsets) {
                for (int i = 0; i < offsets.length; i++) {
                    BlockPos newPos = additionalPos.add(offsets[i]);
                    Block block = BlockUtils.getBlock(newPos);
                    if (newPos.equals(previousBlock)) {
                        return new PlaceData(facings[i], newPos);
                    }
                    if (lastCheck == 0) {
                        continue;
                    }
                    if (!block.getMaterial().isReplaceable() && !BlockUtils.isInteractable(block) || newPos.equals(previousBlock)) {
                        return new PlaceData(facings[i], newPos);
                    }
                }
            }
        }
        BlockPos[] additionalOffsets2 = { // adjust these for perfect placement
                new BlockPos(-1, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, -1),
                new BlockPos(0, -1, 0),
        };
        for (int lastCheck = 0; lastCheck < 2; lastCheck++) {
            for (BlockPos additionalPos2 : additionalOffsets2) {
                for (BlockPos additionalPos : additionalOffsets) {
                    for (int i = 0; i < offsets.length; i++) {
                        BlockPos newPos = additionalPos2.add(additionalPos.add(offsets[i]));
                        Block block = BlockUtils.getBlock(newPos);
                        if (newPos.equals(previousBlock)) {
                            return new PlaceData(facings[i], newPos);
                        }
                        if (lastCheck == 0) {
                            continue;
                        }
                        if (!block.getMaterial().isReplaceable() && !BlockUtils.isInteractable(block) || newPos.equals(previousBlock)) {
                            return new PlaceData(facings[i], newPos);
                        }
                    }
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1) {
            rmbDown = mouseEvent.buttonstate;
            if (placeBlock != null && rmbDown) {
                mouseEvent.setCanceled(true);
            }
        }
    }

    public boolean stopFastPlace() {
        return this.isEnabled() && placeBlock != null;
    }

    public double groundDistance() {
        for (int i = 1; i <= 20; i++) {
            if (!mc.thePlayer.onGround && !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (i / 10), mc.thePlayer.posZ)) instanceof BlockAir)) {
                return (i / 10);
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !highlightBlocks.isToggled() || highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.merge(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }

    public boolean sprint() {
        if (this.isEnabled() && fastScaffold.getInput() > 0 && placeBlock != null && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1))) {
            switch ((int) fastScaffold.getInput()) {
                case 1:
                    return true;
                case 2:
                    return Utils.onEdge();
                case 3:
                case 4:
                case 5:
                case 6:
                    return keepYPosition();
            }
        }
        return false;
    }

    private boolean forceStrict(float value) {
        return (inBetween(-170, -105, value) || inBetween(-80, 80, value) || inBetween(98, 170, value)) && !inBetween(-10, 10, value);
    }

    private boolean keepYPosition() {
        return this.isEnabled() && Utils.keysDown() && (fastScaffold.getInput() == 4 || fastScaffold.getInput() == 3 || fastScaffold.getInput() == 5 || fastScaffold.getInput() == 6) && (!Utils.jumpDown() || fastScaffold.getInput() == 6) && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1));
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled() && (!keepYPosition() || fastScaffold.getInput() == 3 || totalBlocks() == 0) ;
    }

    public boolean stopRotation() {
        return this.isEnabled() && (rotation.getInput() <= 1 || (rotation.getInput() == 2 && placeBlock != null));
    }

    private boolean inBetween(float min, float max, float value) {
        return value >= min && value <= max;
    }

    public float getYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.movementInput.moveForward;
        double moveStrafe = mc.thePlayer.movementInput.moveStrafe;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        }
        else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        }
        else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) + yaw;
    }

    private void place(MovingObjectPosition block, boolean extra) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return;
        }
        if (!extra && mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.getBlockPos(), block.sideHit, block.hitVec)) {
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
            highlight.put(block.getBlockPos().offset(block.sideHit), null);
            previousRotation = new float[]{placeYaw, placePitch};
            if (heldItem.stackSize == 0) {
                blockSlot = -1;
            }
        }
        else if (extra) {
            float f = (float)(block.hitVec.xCoord - (double)block.getBlockPos().getX());
            float f1 = (float)(block.hitVec.yCoord - (double)block.getBlockPos().getY());
            float f2 = (float)(block.hitVec.zCoord - (double)block.getBlockPos().getZ());
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(block.getBlockPos(), block.sideHit.getIndex(), heldItem, f, f1, f2));
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
        }
        blocksPlaced++;
    }

    private int getSlot() {
        int slot = -1;
        int highestStack = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (itemStack != null && heldItem != null && (heldItem.getItem() instanceof ItemBlock) && Utils.canBePlaced((ItemBlock) heldItem.getItem()) && ModuleManager.autoSwap.sameType.isToggled() && !(itemStack.getItem().getClass().equals(heldItem.getItem().getClass()))) {
                    continue;
                }
                if (mc.thePlayer.inventory.mainInventory[i].stackSize > highestStack) {
                    highestStack = mc.thePlayer.inventory.mainInventory[i].stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    private double getRandom() {
        return Utils.randomizeInt(-40, 40) / 100.0;
    }

    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;

        PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }

        EnumFacing getEnumFacing() {
            return enumFacing;
        }
    }
}