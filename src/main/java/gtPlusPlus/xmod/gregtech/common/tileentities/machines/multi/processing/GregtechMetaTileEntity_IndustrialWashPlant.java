package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.GT_HatchElement.Energy;
import static gregtech.api.enums.GT_HatchElement.InputBus;
import static gregtech.api.enums.GT_HatchElement.InputHatch;
import static gregtech.api.enums.GT_HatchElement.Maintenance;
import static gregtech.api.enums.GT_HatchElement.Muffler;
import static gregtech.api.enums.GT_HatchElement.OutputBus;
import static gregtech.api.enums.GT_HatchElement.OutputHatch;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;

import gregtech.api.enums.TAE;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTPP_Recipe.GTPP_Recipe_Map;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.util.minecraft.FluidUtils;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;

public class GregtechMetaTileEntity_IndustrialWashPlant extends
        GregtechMeta_MultiBlockBase<GregtechMetaTileEntity_IndustrialWashPlant> implements ISurvivalConstructable {

    private int mMode = 0;
    private int mCasing;
    private static IStructureDefinition<GregtechMetaTileEntity_IndustrialWashPlant> STRUCTURE_DEFINITION = null;

    public GregtechMetaTileEntity_IndustrialWashPlant(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GregtechMetaTileEntity_IndustrialWashPlant(final String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
        return new GregtechMetaTileEntity_IndustrialWashPlant(this.mName);
    }

    @Override
    public String getMachineType() {
        return "Ore Washer, Simple Washer, Chemical Bath";
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType(getMachineType()).addInfo("Controller Block for the Industrial Wash Plant")
                .addInfo("Can be configured with a screwdriver to also do Simple Washer and process Chemical Bathing")
                .addInfo("400% faster than using single block machines of the same voltage")
                .addInfo("Processes four item per voltage tier")
                .addInfo("Always requires an Input Hatch full of water to refill structure")
                .addInfo("Need to be filled with water.").addInfo("Will automatically fill water from input hatch.")
                .addPollutionAmount(getPollutionPerSecond(null)).addSeparator().beginStructureBlock(5, 3, 7, true)
                .addController("Front Center").addCasingInfoMin("Wash Plant Casings", 40, false)
                .addInputBus("Any Casing", 1).addOutputBus("Any Casing", 1).addInputHatch("Any Casing", 1)
                .addOutputHatch("Any Casing", 1).addEnergyHatch("Any Casing", 1).addMaintenanceHatch("Any Casing", 1)
                .addMufflerHatch("Any Casing", 1).toolTipFinisher(CORE.GT_Tooltip_Builder.get());
        return tt;
    }

    @Override
    public IStructureDefinition<GregtechMetaTileEntity_IndustrialWashPlant> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<GregtechMetaTileEntity_IndustrialWashPlant>builder()
                    .addShape(
                            mName,
                            transpose(
                                    new String[][] { { "CCCCC", "CwwwC", "CwwwC", "CwwwC", "CwwwC", "CwwwC", "CCCCC" },
                                            { "CC~CC", "CwwwC", "CwwwC", "CwwwC", "CwwwC", "CwwwC", "CCCCC" },
                                            { "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC" }, }))
                    .addElement(
                            'C',
                            buildHatchAdder(GregtechMetaTileEntity_IndustrialWashPlant.class)
                                    .atLeast(InputBus, InputHatch, OutputHatch, OutputBus, Maintenance, Energy, Muffler)
                                    .casingIndex(getCasingTextureIndex()).dot(1).buildAndChain(
                                            onElementPass(
                                                    x -> ++x.mCasing,
                                                    ofBlock(getCasingBlock(), getCasingMeta()))))
                    .addElement(
                            'w',
                            ofChain(
                                    isAir(),
                                    ofBlockAnyMeta(Blocks.water),
                                    ofBlockAnyMeta(Blocks.flowing_water),
                                    ofBlockAnyMeta(BlocksItems.getFluidBlock(InternalName.fluidDistilledWater))))
                    .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 2, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivialBuildPiece(mName, stackSize, 2, 1, 0, elementBudget, env, false, true);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasing = 0;
        return checkPiece(mName, 2, 1, 0) && mCasing >= 40 && checkHatch();
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> !f.isVerticallyFliped();
    }

    @Override
    protected IIconContainer getActiveOverlay() {
        return TexturesGtBlock.Overlay_Machine_Controller_Default_Active;
    }

    @Override
    protected IIconContainer getInactiveOverlay() {
        return TexturesGtBlock.Overlay_Machine_Controller_Default;
    }

    @Override
    protected int getCasingTextureId() {
        return getCasingTextureIndex();
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return mMode == 0 ? GT_Recipe.GT_Recipe_Map.sOreWasherRecipes
                : mMode == 1 ? GTPP_Recipe_Map.sSimpleWasherRecipes : GT_Recipe.GT_Recipe_Map.sChemicalBathRecipes;
    }

    @Override
    public boolean checkRecipe(final ItemStack aStack) {
        if (checkForWater()) {
            return checkRecipeGeneric((4 * GT_Utility.getTier(this.getMaxInputVoltage())), 100, 400);
        }
        return false;
    }

    @Override
    public int getMaxParallelRecipes() {
        return (4 * GT_Utility.getTier(this.getMaxInputVoltage()));
    }

    @Override
    public int getEuDiscountForParallelism() {
        return 100;
    }

    @Override
    public int getMaxEfficiency(final ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerSecond(final ItemStack aStack) {
        if (mMode == 2) return CORE.ConfigSwitches.pollutionPerSecondMultiIndustrialWashPlant_ModeChemBath;
        return CORE.ConfigSwitches.pollutionPerSecondMultiIndustrialWashPlant_ModeWasher;
    }

    @Override
    public int getAmountOfOutputs() {
        return 1;
    }

    @Override
    public boolean explodesOnComponentBreak(final ItemStack aStack) {
        return false;
    }

    public Block getCasingBlock() {
        return ModBlocks.blockCasings2Misc;
    }

    public byte getCasingMeta() {
        return 4;
    }

    public byte getCasingTextureIndex() {
        return (byte) TAE.GTPP_INDEX(11);
    }

    public boolean checkForWater() {

        // Get Facing direction
        IGregTechTileEntity aBaseMetaTileEntity = this.getBaseMetaTileEntity();
        ExtendedFacing f = getExtendedFacing();
        Vec3Impl nearCornerOffset = f.getWorldOffset(new Vec3Impl(1, 0, 1));
        Vec3Impl farCornerOffset = f.getWorldOffset(new Vec3Impl(-1, -1, 5));
        int mOffsetX_Lower = Math.min(nearCornerOffset.get0(), farCornerOffset.get0());
        int mOffsetY_Lower = Math.min(nearCornerOffset.get1(), farCornerOffset.get1());
        int mOffsetZ_Lower = Math.min(nearCornerOffset.get2(), farCornerOffset.get2());
        int mOffsetX_Upper = Math.max(nearCornerOffset.get0(), farCornerOffset.get0());
        int mOffsetY_Upper = Math.max(nearCornerOffset.get1(), farCornerOffset.get1());
        int mOffsetZ_Upper = Math.max(nearCornerOffset.get2(), farCornerOffset.get2());

        int tAmount = 0;
        for (int i = mOffsetX_Lower; i <= mOffsetX_Upper; ++i) {
            for (int j = mOffsetZ_Lower; j <= mOffsetZ_Upper; ++j) {
                for (int h = mOffsetY_Lower; h <= mOffsetY_Upper; ++h) {
                    Block tBlock = aBaseMetaTileEntity.getBlockOffset(i, h, j);
                    byte tMeta = aBaseMetaTileEntity.getMetaIDOffset(i, h, j);
                    if (tBlock == Blocks.air || tBlock == Blocks.flowing_water || tBlock == Blocks.water) {
                        if (this.getStoredFluids() != null) {
                            for (FluidStack stored : this.getStoredFluids()) {
                                if (stored.isFluidEqual(FluidUtils.getFluidStack("water", 1))) {
                                    if (stored.amount >= 1000) {
                                        Logger.WARNING("Going to try swap an air block for water from input bus.");
                                        stored.amount -= 1000;
                                        Block fluidUsed = null;
                                        if (tBlock == Blocks.air || tBlock == Blocks.flowing_water) {
                                            fluidUsed = Blocks.water;
                                        }
                                        if (tBlock == Blocks.water) {
                                            fluidUsed = BlocksItems.getFluidBlock(InternalName.fluidDistilledWater);
                                        }
                                        aBaseMetaTileEntity.getWorld().setBlock(
                                                aBaseMetaTileEntity.getXCoord() + i,
                                                aBaseMetaTileEntity.getYCoord() + h,
                                                aBaseMetaTileEntity.getZCoord() + j,
                                                fluidUsed);
                                    }
                                }
                            }
                        }
                    }
                    if (tBlock == Blocks.water) {
                        ++tAmount;
                        // Utils.LOG_WARNING("Found Water");
                    } else if (tBlock == BlocksItems.getFluidBlock(InternalName.fluidDistilledWater)) {
                        ++tAmount;
                        ++tAmount;
                        // Utils.LOG_WARNING("Found Distilled Water");
                    }
                }
            }
        }

        boolean isValidWater = tAmount >= 45;
        if (isValidWater) {
            Logger.WARNING("Filled structure.");
        } else {
            Logger.WARNING("Did not fill structure.");
        }
        return isValidWater;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("mMode", mMode);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        if (aNBT.hasKey("mChemicalMode")) {
            boolean aTempMode = aNBT.getBoolean("mChemicalMode");
            if (aTempMode) {
                mMode = 2;
            } else {
                mMode = 0;
            }
            aNBT.removeTag("mChemicalMode");
        }
        if (aNBT.hasKey("mMode")) {
            mMode = aNBT.getInteger("mMode");
        }
        super.loadNBTData(aNBT);
    }

    @Override
    public void onModeChangeByScrewdriver(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        mMode++;
        if (mMode > 2) {
            mMode = 0;
        }
        if (mMode == 0) {
            PlayerUtils.messagePlayer(aPlayer, "Wash Plant is now running in Ore Washer Mode.");
        } else if (mMode == 1) {
            PlayerUtils.messagePlayer(aPlayer, "Wash Plant is now running in Simple Washer Mode.");
        } else {
            PlayerUtils.messagePlayer(aPlayer, "Wash Plant is now running in Chemical Bath Mode.");
        }
        mLastRecipe = null;
    }
}
