package com.ghostblock.mixin;

import com.ghostblock.DesyncTracker;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ServerboundMovementMixin {

    @ModifyVariable(
        method = "send(Lnet/minecraft/network/protocol/Packet;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Packet<?> lockMovementPacket(Packet<?> packet) {
        // Since we now freeze the client at the server position physically,
        // outgoing position packets are already truthful — no lying needed.
        // We only pass through unchanged. This mixin is kept for future use
        // but does nothing while DESYNCED since client IS at server pos.
        return packet;
    }
}
