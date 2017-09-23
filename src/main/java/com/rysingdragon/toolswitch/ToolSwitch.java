package com.rysingdragon.toolswitch;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.property.item.EfficiencyProperty;
import org.spongepowered.api.data.property.item.HarvestingProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Plugin(id = "toolswitch", name = "ToolSwitch", authors = {"RysingDragon"})
public class ToolSwitch {

    private Map<UUID, Boolean> autoSwitch;

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        this.autoSwitch = new HashMap<>();
    }

    @Listener
    public void onInit(GameInitializationEvent event) {
        CommandSpec spec = CommandSpec.builder()
                .permission("toolswitch.command.toolswitchtoggle")
                .executor((src, args) -> {
                    if (!(src instanceof Player))
                        throw new CommandException(Text.of("You must be a player to run this command"));
                    Player player = (Player) src;
                    boolean val = !this.autoSwitch.getOrDefault(player.getUniqueId(), true);
                    this.autoSwitch.put(player.getUniqueId(), val);
                    String state = val ? "on" : "off";
                    player.sendMessage(Text.of("You have turned auto tool switch ", state));
                    return CommandResult.success();
                })
                .build();
        Sponge.getCommandManager().register(this, spec, "toolswitchtoggle", "tstoggle");
    }

    @Listener
    public void onStartBreaking(InteractBlockEvent.Primary.MainHand event, @First Player player) {
        boolean val = this.autoSwitch.getOrDefault(player.getUniqueId(), true);
        if (!val) {
            return;
        }

        ItemStack handItem = player.getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.empty());
        if (handItem.getProperty(HarvestingProperty.class).isPresent()) {
            HarvestingProperty harvestingProperty = handItem.getProperty(HarvestingProperty.class).get();
            if (harvestingProperty.getValue() != null && harvestingProperty.getValue().contains(event.getTargetBlock().getState().getType())) {
                return;
            }
        }

        List<Inventory> slots = new ArrayList<>();
        for (Inventory slot : player.getInventory().slots()) {
            if (!slot.peek().isPresent())
                continue;
            ItemStack stack = slot.peek().get();
            if (!stack.getProperty(HarvestingProperty.class).isPresent())
                continue;
            HarvestingProperty property = stack.getProperty(HarvestingProperty.class).get();
            if (property.getValue() != null && property.getValue().contains(event.getTargetBlock().getState().getType())) {
                slots.add(slot);
            }
        }

        Inventory efficientSlot = null;
        for (Inventory slot : slots) {
            ItemStack stack = slot.peek().get();
            if (efficientSlot == null)
                efficientSlot = slot;
            if (!stack.getProperty(EfficiencyProperty.class).isPresent())
                break;
            ItemStack efficientStack = efficientSlot.peek().get();
            EfficiencyProperty efficientStackProperty = efficientStack.getProperty(EfficiencyProperty.class).get();
            EfficiencyProperty property = stack.getProperty(EfficiencyProperty.class).get();
            System.out.println("vl: " + property.getValue());
            if (property.getValue() != null && efficientStackProperty.getValue() != null && property.getValue() > efficientStackProperty.getValue()) {
                efficientSlot = slot;
            }
        }
        if (efficientSlot != null) {
            player.setItemInHand(HandTypes.MAIN_HAND, efficientSlot.peek().get());
            efficientSlot.set(handItem);
        }
    }
}
