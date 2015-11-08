package com.sk89q.craftbook.sponge.mechanics.area;

import com.google.inject.Inject;
import com.me4502.modularframework.module.Module;
import com.me4502.modularframework.module.guice.ModuleConfiguration;
import com.sk89q.craftbook.core.util.ConfigValue;
import com.sk89q.craftbook.core.util.CraftBookException;
import com.sk89q.craftbook.sponge.util.BlockUtil;
import com.sk89q.craftbook.sponge.util.SignUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.Location;

@Module(moduleName = "Bridge", onEnable="onInitialize", onDisable="onDisable")
public class Bridge extends SimpleArea {

    @Inject
    @ModuleConfiguration
    public ConfigurationNode config;

    private ConfigValue<Integer> maximumLength = new ConfigValue<>("maximum-length", "The maximum length the bridge can be.", 16);
    private ConfigValue<Integer> maximumWidth = new ConfigValue<>("maximum-width", "The maximum width each side of the bridge can be. The overall max width is this*2 + 1.", 5);

    @Override
    public void onInitialize() throws CraftBookException {
        maximumLength.load(config);
        maximumWidth.load(config);
    }

    @Override
    public void onDisable() {
        maximumLength.save(config);
        maximumWidth.save(config);
    }

    @Override
    public boolean triggerMechanic(Location block, Sign sign, Human human, Boolean forceState) {

        if (!SignUtil.getTextRaw(sign, 1).equals("[Bridge End]")) {

            Direction back = SignUtil.getBack(block);

            Location baseBlock = block.getRelative(Direction.DOWN);

            Location otherSide = getOtherEnd(block, SignUtil.getBack(block), maximumLength.getValue());
            if (otherSide == null) {
                if (human instanceof CommandSource) ((CommandSource) human).sendMessage(Texts.builder("Missing other end!").build());
                return true;
            }
            Location otherBase = otherSide.getRelative(Direction.DOWN);

            if(!baseBlock.getBlock().equals(otherBase.getBlock())) {
                if (human instanceof CommandSource) ((CommandSource) human).sendMessage(Texts.builder("Both ends must be the same material!").build());
                return true;
            }

            int leftBlocks, rightBlocks;

            Location left = baseBlock.getRelative(SignUtil.getLeft(block));
            Location right = baseBlock.getRelative(SignUtil.getRight(block));

            //Calculate left distance
            Location otherLeft = otherBase.getRelative(SignUtil.getLeft(block));

            leftBlocks = BlockUtil.getMinimumLength(left, otherLeft, baseBlock.getBlock(), SignUtil.getLeft(block), maximumWidth.getValue());

            //Calculate right distance
            Location otherRight = otherBase.getRelative(SignUtil.getRight(block));

            rightBlocks = BlockUtil.getMinimumLength(right, otherRight, baseBlock.getBlock(), SignUtil.getRight(block), maximumWidth.getValue());

            baseBlock = baseBlock.getRelative(back);

            BlockState type = block.getRelative(Direction.DOWN).getBlock();
            if (baseBlock.getBlock().equals(type) && (forceState == null || !forceState)) type = BlockTypes.AIR.getDefaultState();

            while (baseBlock.getBlockX() != otherSide.getBlockX() || baseBlock.getBlockZ() != otherSide.getBlockZ()) {

                baseBlock.setBlock(type);

                left = baseBlock.getRelative(SignUtil.getLeft(block));

                for(int i = 0; i < leftBlocks; i++) {
                    left.setBlock(type);
                    left = left.getRelative(SignUtil.getLeft(block));
                }

                right = baseBlock.getRelative(SignUtil.getRight(block));

                for(int i = 0; i < rightBlocks; i++) {
                    right.setBlock(type);
                    right = right.getRelative(SignUtil.getRight(block));
                }

                baseBlock = baseBlock.getRelative(back);
            }
        } else {
            if (human instanceof CommandSource) ((CommandSource) human).sendMessage(Texts.builder("Bridge not activatable from here!").build());
            return false;
        }

        return true;
    }

    @Override
    public boolean isMechanicSign(Sign sign) {
        return SignUtil.getTextRaw(sign, 1).equalsIgnoreCase("[Bridge]") || SignUtil.getTextRaw(sign, 1).equalsIgnoreCase("[Bridge End]");
    }

    @Override
    public String[] getValidSigns() {
        return new String[]{"[Bridge]", "[Bridge End]"};
    }
}
