package com.ghostblock.mixin;

import com.ghostblock.DesyncTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @ModifyVariable(
        method = "handleMovePlayer",
        at = @At("HEAD"),
        argsOnly = true
    )
    private ClientboundPlayerPositionPacket onPositionCorrection(
            ClientboundPlayerPositionPacket packet) {

        // Use Minecraft.getInstance() instead of @Shadow to avoid
        // the field living in the parent class issue
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return packet;

        Set<Relative> relatives = packet.relatives();
        PositionMoveRotation change = packet.change();

        // Resolve absolute server position accounting for relative flags
        double absX = relatives.contains(Relative.X)
            ? player.getX() + change.position().x : change.position().x;
        double absY = relatives.contains(Relative.Y)
            ? player.getY() + change.position().y : change.position().y;
        double absZ = relatives.contains(Relative.Z)
            ? player.getZ() + change.position().z : change.position().z;
        float absYaw = relatives.contains(Relative.Y_ROT)
            ? player.getYRot() + change.yRot() : change.yRot();
        float absPitch = relatives.contains(Relative.X_ROT)
            ? player.getXRot() + change.xRot() : change.xRot();

        // Feed resolved absolute position into state machine
        DesyncTracker.onServerCorrection(absX, absY, absZ, absYaw, absPitch);

        // Rebuild packet substituting our current rotation so the view never snaps
        PositionMoveRotation preservedChange = new PositionMoveRotation(
            change.position(),
            change.deltaMovement(),
            player.getYRot(),
            player.getXRot()
        );

        return new ClientboundPlayerPositionPacket(
            packet.id(),
            preservedChange,
            packet.relatives()
        );
    }
}
