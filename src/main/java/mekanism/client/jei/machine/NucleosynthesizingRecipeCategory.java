package mekanism.client.jei.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import mekanism.api.annotations.NonNull;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.math.FloatingLong;
import mekanism.api.recipes.ItemStackToGasRecipe;
import mekanism.api.recipes.NucleosynthesizingRecipe;
import mekanism.api.recipes.inputs.GasStackIngredient;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar.ColorFunction;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.gauge.GuiEnergyGauge.IEnergyInfoHandler;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.lib.Color;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tier.GasTankTier;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.text.TextUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredientGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class NucleosynthesizingRecipeCategory extends BaseRecipeCategory<NucleosynthesizingRecipe> {

    public NucleosynthesizingRecipeCategory(IGuiHelper helper) {
        super(helper, MekanismBlocks.ANTIPROTONIC_NUCLEOSYNTHESIZER, 3, 12, 190, 98);
    }

    @Override
    public Class<? extends NucleosynthesizingRecipe> getRecipeClass() {
        return NucleosynthesizingRecipe.class;
    }

    @Override
    protected void addGuiElements() {
        guiElements.add(new GuiSlot(SlotType.INPUT, this, 25, 39));
        guiElements.add(new GuiSlot(SlotType.POWER, this, 172, 68).with(SlotOverlay.POWER));
        guiElements.add(new GuiSlot(SlotType.EXTRA, this, 5, 68));
        guiElements.add(new GuiSlot(SlotType.OUTPUT, this, 151, 39));
        guiElements.add(new GuiInnerScreen(this, 45, 18, 104, 68));
        guiElements.add(GuiGasGauge.getDummy(GaugeType.SMALL_MED, this, 5, 18));
        guiElements.add(new GuiEnergyGauge(new IEnergyInfoHandler() {
            @Override
            public FloatingLong getEnergy() {
                return FloatingLong.ONE;
            }

            @Override
            public FloatingLong getMaxEnergy() {
                return FloatingLong.ONE;
            }
        }, GaugeType.SMALL_MED, this, 172, 18));
        guiElements.add(new GuiDynamicHorizontalRateBar(this, new IBarInfoHandler() {
            @Override
            public ITextComponent getTooltip() {
                return MekanismLang.PROGRESS.translate(TextUtils.getPercent(timer.getValue() / 20D));
            }

            @Override
            public double getLevel() {
                return timer.getValue() / 20D;
            }
        }, 5, 88, getWidth() - 6, ColorFunction.scale(Color.rgb(60, 45, 74), Color.rgb(100, 30, 170))));
    }

    @Override
    public List<String> getTooltipStrings(NucleosynthesizingRecipe recipe, double mouseX, double mouseY) {
        if (mouseX >= 5 - 3 && mouseX < 5 + getWidth() - 6 - 3 && mouseY >= 88 - 12 && mouseY < 98 - 12) {
            return Arrays.asList(MekanismLang.TICKS_REQUIRED.translate(recipe.getDuration()).getString());
        }
        return Collections.emptyList();
    }

    @Override
    public void setIngredients(NucleosynthesizingRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getItemInput().getRepresentations()));
        List<@NonNull GasStack> gasInputs = recipe.getGasInput().getRepresentations();
        long scale = TileEntityAdvancedElectricMachine.BASE_TICKS_REQUIRED * TileEntityAdvancedElectricMachine.BASE_GAS_PER_TICK;
        List<GasStack> scaledGases = gasInputs.stream().map(gas -> new GasStack(gas, scale)).collect(Collectors.toList());
        ingredients.setInputLists(MekanismJEI.TYPE_GAS, Collections.singletonList(scaledGases));
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutputDefinition()));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, NucleosynthesizingRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        itemStacks.init(0, true, 22, 27);
        itemStacks.init(1, false, 148, 27);
        itemStacks.init(2, false, 2, 56);
        itemStacks.set(0, recipe.getItemInput().getRepresentations());
        itemStacks.set(1, recipe.getOutputDefinition());
        GasStackIngredient gasInput = recipe.getGasInput();
        List<ItemStack> gasItemProviders = new ArrayList<>();
        List<@NonNull GasStack> gasInputs = gasInput.getRepresentations();
        List<GasStack> scaledGases = new ArrayList<>();
        long scale = TileEntityAdvancedElectricMachine.BASE_TICKS_REQUIRED * TileEntityAdvancedElectricMachine.BASE_GAS_PER_TICK;
        for (GasStack gas : gasInputs) {
            gasItemProviders.addAll(getStacksForGas(gas.getType()));
            //While we are already looping the gases ensure we scale it to get the average amount that will get used over all
            scaledGases.add(new GasStack(gas, scale));
        }
        itemStacks.set(2, gasItemProviders);
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initGas(gasStacks, 0, true, 6 - xOffset, 19 - yOffset, 16, 46, scaledGases, true);
    }

    /**
     * Helper method for JEI to get the stacks to display for a specific type of gas
     */
    private static List<ItemStack> getStacksForGas(@Nonnull Gas type) {
        if (type.isEmptyType()) {
            return Collections.emptyList();
        }
        World world = Minecraft.getInstance().world;
        if (world == null) {
            return Collections.emptyList();
        }
        List<ItemStack> stacks = new ArrayList<>();
        //Always include the gas tank of the type
        stacks.add(MekanismUtils.getFullGasTank(GasTankTier.BASIC, type));
        //See if there are any gas to item mappings
        List<ItemStackToGasRecipe> recipes = MekanismRecipeType.GAS_CONVERSION.getRecipes(world);
        for (ItemStackToGasRecipe recipe : recipes) {
            if (recipe.getOutputDefinition().isTypeEqual(type)) {
                stacks.addAll(recipe.getInput().getRepresentations());
            }
        }
        return stacks;
    }
}