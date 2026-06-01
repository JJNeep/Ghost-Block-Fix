# Ghost Block Fix

Ever stood on a ghost block and watched your screen snap back every time you moved your head? Or tried to look around while riding out a desync only to get yanked back to center? That's what this mod fixes.

## What it does

When the server sends a position correction (the thing that causes the snap), Ghost Block Fix freezes your client at the server's Y level instead of letting you fall through. Since you're now actually at the position the server thinks you're at, everything works normally — you can walk around, place blocks, interact with stuff, and look wherever you want without your view getting yanked back.

When you walk off the ghost block onto real ground, the mod detects that the server has stopped correcting you and releases cleanly. No input required.

**Jumping** while frozen launches you off the ghost block as normal — the server accepts it since it thinks you're standing on solid ground.

**Fall damage** from landing on a ghost block after a long fall is also effectively negated, since the mod keeps `onGround` true while frozen.

## What it doesn't fix

The mod is purely client-side. If you relog while standing on a ghost block, the server will have saved your position at that spot, so you'll spawn there and fall. Nothing a client mod can do about that — the server already committed the save.

## How it works

The server periodically sends `ClientboundPlayerPositionPacket` to correct your position when it disagrees with where you are. Normally this snaps both your position and your camera. Ghost Block Fix intercepts these packets, accepts the position correction (so the server stays happy), but substitutes your current rotation back in so your view never snaps.

It also pins your client's Y coordinate to the server's confirmed position each tick, zeroing out vertical velocity so gravity doesn't accumulate. X and Z are left completely free so your movement looks normal to other players.

Once the server stops sending corrections for ~10 ticks, the mod considers you off the ghost block and releases.

## Compatibility

- Client-side only — works on any server
- Fabric 0.18.1+
- Minecraft 1.21.11

## Notes

This mod is a quality-of-life fix for a vanilla netcode quirk. It doesn't give you any movement capabilities the server wouldn't normally allow — it just stops your client from fighting the server's position corrections unnecessarily.
