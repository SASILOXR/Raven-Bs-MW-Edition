package keystrokesmod.module.impl.player;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

public class Scaffold extends Module {
    private SliderSetting motion;
    private SliderSetting rotation;
    private SliderSetting fastScaffold;
    private SliderSetting precision;
    private ButtonSetting autoSwap;
    private ButtonSetting fastOnRMB;
    private ButtonSetting highlightBlocks;
    private ButtonSetting multiPlace;
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
    private int add = 0;
    private boolean placedUp;
    private float previousRotation[];
    private int blockSlot = -1;
    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(motion = new SliderSetting("Motion", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(rotation = new SliderSetting("Rotation", rotationModes, 1));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", fastScaffoldModes, 0));
        this.registerSetting(precision = new SliderSetting("Precision", precisionModes, 4));
        this.registerSetting(autoSwap = new ButtonSetting("AutoSwap", true));
        this.registerSetting(delayOnJump = new ButtonSetting("Delay on jump", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", false));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(multiPlace = new ButtonSetting("Multi-place", false));
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
            } else {
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
        else if (!keepYPosition()) {
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
            if (groundDistance() > 0 && mc.thePlayer.fallDistance > 0 && ((!placedUp || isDiagonal()) || fastScaffold.getInput() == 4)) {
                original++;
            }
        }
        Vec3 targetVec3 = getPlacePossibility(0, original);
        if (targetVec3 == null) {
            return;
        }
        BlockPos targetPos = new BlockPos(targetVec3.xCoord, targetVec3.yCoord, targetVec3.zCoord);

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
            mc.thePlayer.inventory.currentItem = blockSlot;
        }
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            blockSlot = -1;
            return;
        }
        MovingObjectPosition rayCasted = null;
        float searchYaw = 25;
        switch ((int) precision.getInput()) {
            case 4:
                searchYaw = 40;
                break;
            case 3:
                searchYaw = 32;
                break;
            case 2:
                break;
            case 1:
                searchYaw = 17;
                break;
            case 0:
                searchYaw = 8;
                break;
        }
        PlaceData placeData = getEnumFacing(targetPos, original);
        if (placeData == null || placeData.blockPos == null || placeData.enumFacing == null) {
            return;
        }
        targetPos = placeData.getBlockPos();
        float[] targetRotation = RotationUtils.getRotations(targetPos);
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
                float fixedYaw = (float) ((playerYaw + checkYaw) + getRandom());
                if (!Utils.overPlaceable(-1)) {
                    continue;
                }
                for (float checkPitch : pitchSearchList) {
                    float fixedPitch = RotationUtils.clampTo90((float) (targetRotation[1] + checkPitch + getRandom()));
                    MovingObjectPosition raycast = RotationUtils.rayCast(mc.playerController.getBlockReachDistance(), fixedYaw, fixedPitch);
                    if (raycast != null) {
                        if (raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            if (raycast.getBlockPos().equals(targetPos) && raycast.sideHit == placeData.getEnumFacing()) {
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
                                        } else {
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
            if (multiPlace.isToggled()) {
                place(placeBlock, true);
            }
            place = false;
            if (placeBlock.sideHit == EnumFacing.UP && keepYPosition()) {
                placedUp = true;
            }
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

    public Vec3 getPlacePossibility(double offsetY, double original) { // rise
        List<Vec3> possibilities = new ArrayList<>();
        int range = 5;
        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= 2; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = blockRelativeToPlayer(x, y, z);
                    if (!block.getMaterial().isReplaceable()) {
                        for (int x2 = -1; x2 <= 1; x2 += 2) {
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));
                        }
                        for (int y2 = -1; y2 <= 1; y2 += 2) {
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));
                        }
                        for (int z2 = -1; z2 <= 1; z2 += 2) {
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                        }
                    }
                }
            }
        }
        if (possibilities.isEmpty()) {
            return null;
        }
        possibilities.sort(Comparator.comparingDouble(vec3 -> {
            final double d0 = (mc.thePlayer.posX) - vec3.xCoord;
            final double d1 = ((keepYPosition() ? original : mc.thePlayer.posY) - 1 + offsetY) - vec3.yCoord;
            final double d2 = (mc.thePlayer.posZ) - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
        }));

        return possibilities.get(0);
    }

    public Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return mc.theWorld.getBlockState(new BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).getBlock();
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

    private boolean isDiagonal() {
        float yaw = ((mc.thePlayer.rotationYaw % 360) + 360) % 360 > 180 ? ((mc.thePlayer.rotationYaw % 360) + 360) % 360 - 360 : ((mc.thePlayer.rotationYaw % 360) + 360) % 360;
        return (yaw >= -170 && yaw <= 170) && !(yaw >= -10 && yaw <= 10) && !(yaw >= 80 && yaw <= 100) && !(yaw >= -100 && yaw <= -80) || Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
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

    private double getRandom() {
        return Utils.randomizeInt(-40, 40) / 100.0;
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

    public PlaceData getEnumFacing(BlockPos blockPos, double yPos) {
        double lastDistance = Double.MAX_VALUE;
        EnumFacing enumFacing = null;
        BlockPos offset = null;
        for (EnumFacing enumFacing2 : EnumFacing.VALUES) {
            if (enumFacing2 != null) {
                BlockPos enumOffset = blockPos.offset(enumFacing2);
                if (!BlockUtils.replaceable(enumOffset)) {
                    double distanceSqToCenter = enumOffset.distanceSqToCenter(mc.thePlayer.posX, (keepYPosition() ? yPos : mc.thePlayer.posY) - 1, mc.thePlayer.posZ);
                    if (enumFacing == null || distanceSqToCenter < lastDistance) {
                        enumFacing = enumFacing2;
                        lastDistance = distanceSqToCenter;
                        offset = enumOffset;
                    }
                }
            }
        }
        return new PlaceData(enumFacing == null ? null : enumFacing.getOpposite(), offset);
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
    }

    private int getSlot() {
        int slot = -1;
        int highestStack = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && InvManager.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
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
            if (stack != null && stack.getItem() instanceof ItemBlock && InvManager.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
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

        BlockPos getBlockPos() {
            return blockPos;
        }
    }
}
