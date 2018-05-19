package com.universeguard.event.flags;

import com.universeguard.region.enums.EnumRegionFlag;
import com.universeguard.region.enums.RegionEventType;
import com.universeguard.utils.FlagUtils;
import com.universeguard.utils.RegionUtils;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class FlagTrampleListener {

    @Listener
    public void onFarmlandBreak(ChangeBlockEvent.Place event) {
        if(!event.getTransactions().isEmpty()) {
            BlockSnapshot source = event.getTransactions().get(0).getOriginal();
            if(source.getState().getType().equals(BlockTypes.FARMLAND) && source.getLocation().isPresent())
                this.handleEvent(event, source.getLocation().get(), null);
        }
    }

    @Listener
    public void onFieldsBreak(ChangeBlockEvent.Break event, @First Player player) {
        if (!event.getTransactions().isEmpty()) {
            BlockSnapshot block = event.getTransactions().get(0).getFinal();
            if (block.getLocation().isPresent() && FlagUtils.isCrop(block.getState().getType())) {
                Location<World> location = block.getLocation(). get();
                this.handleEvent(event,location, player);
            }
        }
    }

    @Listener
    public void onLeafDrop(SpawnEntityEvent event, @Root BlockSpawnCause cause) {
        BlockType type = cause.getBlockSnapshot().getState().getType();
        if(cause.getType().equals(SpawnTypes.DROPPED_ITEM) && FlagUtils.isCrop(type))
            this.handleEvent(event, cause.getBlockSnapshot().getLocation().get(), null);
    }

    private boolean handleEvent(Cancellable event, Location<World> location, Player player) {
        return RegionUtils.handleEvent(event, EnumRegionFlag.TRAMPLE, location, player, RegionEventType.GLOBAL);
    }
}
