package net.walksanator.hextweaks.items;

import at.petrak.hexcasting.api.item.PigmentItem;
import at.petrak.hexcasting.api.pigment.ColorProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class VirtualPigment extends Item implements PigmentItem {

    public VirtualPigment(Properties properties) {
        super(properties);
    }



    @Override
    public ColorProvider provideColor(ItemStack stack, UUID owner) {
        int rgb = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("rgb");
        return new MyColorProvider(rgb);
    }
    protected static class MyColorProvider extends ColorProvider {
        private final int rgba;
        MyColorProvider(int rgba) {
            this.rgba = rgba;
        }
        @Override
        protected int getRawColor(float time, Vec3 position) {
            return rgba;
        }
    }
}
