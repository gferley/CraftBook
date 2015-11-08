package com.sk89q.craftbook.sponge.mechanics.ics;

import com.me4502.modularframework.module.Module;
import com.sk89q.craftbook.sponge.mechanics.ics.pinsets.PinSet;
import com.sk89q.craftbook.sponge.mechanics.ics.pinsets.Pins3ISO;
import com.sk89q.craftbook.sponge.mechanics.ics.pinsets.PinsSISO;
import com.sk89q.craftbook.sponge.mechanics.types.SpongeBlockMechanic;
import com.sk89q.craftbook.sponge.st.SelfTriggerManager;
import com.sk89q.craftbook.sponge.st.SelfTriggeringMechanic;
import com.sk89q.craftbook.sponge.util.BlockUtil;
import com.sk89q.craftbook.sponge.util.SignUtil;
import com.sk89q.craftbook.sponge.util.SpongeMechanicData;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.property.block.PoweredProperty;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Module(moduleName = "ICSocket", onEnable="onInitialize", onDisable="onDisable")
public class ICSocket extends SpongeBlockMechanic implements SelfTriggeringMechanic {

    public static final HashMap<String, PinSet> PINSETS = new HashMap<>();

    static {
        PINSETS.put("SISO", new PinsSISO());
        PINSETS.put("3ISO", new Pins3ISO());
    }

    /**
     * Gets the IC that is in use by this IC Socket.
     * 
     * @return The IC
     */
    public IC getIC(Location block) {
        return createICData(block).ic;
    }

    @Override
    public String getName() {
        return "IC";
    }

    @Listener
    public void onBlockUpdate(NotifyNeighborBlockEvent event) {

        BlockSnapshot source;
        if(event.getCause().first(BlockSnapshot.class).isPresent())
            source = event.getCause().first(BlockSnapshot.class).get();
        else
            return;

        if(source.getState().getType() != BlockTypes.REDSTONE_WIRE) return;

        for (Location block : event.getNeighbors().entrySet().stream().map(
                (Function<Map.Entry<Direction, BlockState>, Location>) directionBlockStateEntry -> source.getLocation().get().getRelative(directionBlockStateEntry.getKey())).collect(Collectors.toList())) {

            BaseICData data = createICData(block);
            if (data == null) continue;

            Direction facing = BlockUtil.getFacing(block, source.getLocation().get());
            if(facing == null) return; //Something is wrong here.

            boolean powered = block.getRelative(facing).getProperty(PoweredProperty.class).isPresent();//block.getRelative(facing).isFacePowered
            // (facing
            // .getOpposite());

            if (powered != data.ic.getPinSet().getInput(data.ic.getPinSet().getPinForLocation(data.ic, source.getLocation().get()), data.ic)) {
                data.ic.getPinSet().setInput(data.ic.getPinSet().getPinForLocation(data.ic, source.getLocation().get()), powered, data.ic);
                data.ic.trigger();
            }
        }
    }

    @Override
    public void onThink(Location block) {

        BaseICData data = createICData(block);
        if (data == null) return;
        if (!(data.ic instanceof SelfTriggeringIC)) return;
        ((SelfTriggeringIC) data.ic).think();
    }

    @Override
    public boolean isValid(Location location) {
        return createICData(location) != null;
    }

    public BaseICData createICData(Location block) {

        if (block.getBlockType() == BlockTypes.WALL_SIGN) {
            if(block.getExtent() instanceof Chunk)
                block = ((Chunk) block.getExtent()).getWorld().getLocation(block.getX(), block.getY(), block.getZ());
            Sign sign = ((Sign) block.getTileEntity().get());
            ICType<? extends IC> icType = ICManager.getICType(SignUtil.getTextRaw(sign, 1));
            if (icType == null) return null;

            BaseICData data = getData(BaseICData.class, block);

            if (data.ic == null) {
                // Initialize new IC.
                data.ic = icType.buildIC(block);
                if(data.ic instanceof SelfTriggeringIC && SignUtil.getTextRaw(sign, 1).endsWith("S") ||  SignUtil.getTextRaw(sign, 1).endsWith(" ST"))
                    ((SelfTriggeringIC)data.ic).selfTriggering = true;
            } else if(data.ic.block == null) {
                data.ic.block = block;
                data.ic.type = icType;
            }

            if (data.ic instanceof SelfTriggeringIC && (((SelfTriggeringIC) data.ic).canThink())) SelfTriggerManager.register(this, block);

            return data;
        }

        return null;
    }

    public static class BaseICData extends SpongeMechanicData {
        public IC ic;
    }
}
