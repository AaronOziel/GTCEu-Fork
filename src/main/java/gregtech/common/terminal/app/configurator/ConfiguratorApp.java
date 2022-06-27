package gregtech.common.terminal.app.configurator;

import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.terminal.TerminalRegistry;
import gregtech.api.terminal.app.AbstractApplication;
import gregtech.api.util.GTLog;
//import gregtech.common.terminal.app.configurator.widget.ConfigMenuWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;

public class ConfiguratorApp extends AbstractApplication {
    public static final String APP_NAME = "configutrator";
    public static final String APP_NBT_TAG = APP_NAME + "_config";
    private final String PATH_FILE_NAME = "\\" + APP_NAME + ".nbt";

    public ConfiguratorApp(){ super(APP_NAME); }

    public MachineConfiguration CurrentConfiguration;

    private static class MachineConfiguration {
        EnumFacing outputFacingItems;
        EnumFacing outputFacingFluids;
        boolean autoOutputItems;
        boolean autoOutputFluids;
        boolean allowInputFromOutputSideItems;
        boolean allowInputFromOutputSideFluids;
        EnumFacing frontFace;
        boolean isMuffled;
        boolean isWorkingEnabled;
        // Painting?
        CoverBehavior[] coverBehaviors = new CoverBehavior[6];
    }

    @Override
    public ConfiguratorApp initApp() {
        // Choose to apply, copy, or create a machine configuration
        if (os.clickPos != null && IsValidTileEntity(os.clickPos)) {
            MetaTileEntity entity = ((IGregTechTileEntity)gui.entityPlayer.world.getTileEntity(os.clickPos)).getMetaTileEntity();
            // 1. Apply Config
            if (!os.tabletNBT.getCompoundTag(APP_NBT_TAG).isEmpty()) {
                applyMachineConfiguration(entity);
            }
            // 2. Copy Config
            else {
                writeNBT(entity);
            }
            os.shutdown(isClient);
        } else {
            // 3. Open the app to create a configuration
            LaunchApp();
        }

        return this;
    }

    private void applyMachineConfiguration(MetaTileEntity entity) {
        if (isClient) {
            entity.scheduleRenderUpdate();
            return;
        }
        MetaTileEntity templateEntity = entity.createMetaTileEntity(entity.getHolder());
        templateEntity.readFromNBT(os.tabletNBT.getCompoundTag(APP_NBT_TAG));
        applyMachineConfiguration(templateEntity, entity);
    }

    private void applyMachineConfiguration(MetaTileEntity templateEntity, MetaTileEntity existingTileEntity) {
        // Meta Tile Entity
        existingTileEntity.setFrontFacing(templateEntity.getFrontFacing());
        if (existingTileEntity.isMuffled() != templateEntity.isMuffled())
            existingTileEntity.toggleMuffled();

        // Simple Machine Tile Entity
        if (templateEntity instanceof SimpleMachineMetaTileEntity && existingTileEntity instanceof SimpleMachineMetaTileEntity) {
            SimpleMachineMetaTileEntity templateSMTE= (SimpleMachineMetaTileEntity)templateEntity;
            SimpleMachineMetaTileEntity existingSMTE = (SimpleMachineMetaTileEntity)existingTileEntity;
            existingSMTE.setOutputFacingItems(templateSMTE.getOutputFacingItems());
            existingSMTE.setOutputFacingFluids(templateSMTE.getOutputFacingFluids());
            existingSMTE.setAutoOutputItems(templateSMTE.isAutoOutputItems());
            existingSMTE.setAutoOutputFluids(templateSMTE.isAutoOutputFluids());
            existingSMTE.setAllowInputFromOutputSideItems(templateSMTE.isAllowInputFromOutputSideItems());
            existingSMTE.setAllowInputFromOutputSideFluids(templateSMTE.isAllowInputFromOutputSideFluids());
        }

        // Covers
        for (EnumFacing side : EnumFacing.VALUES) {
            if (templateEntity.getCoverAtSide(side) != null) {
                // Retrieve cover and cover's ItemStack
                CoverBehavior cover = templateEntity.getCoverAtSide(side);
                ItemStack coverItemStack = cover.getPickItem();
                if (existingTileEntity.getCoverAtSide(side) != null) {
                    existingTileEntity.removeCover(side);
                }
                existingTileEntity.placeCoverOnSide(side, coverItemStack, cover.getCoverDefinition(), gui.entityPlayer);
            }
        }

        existingTileEntity.scheduleRenderUpdate();
    }

    private void writeNBT(MetaTileEntity entity) {
        if (isClient) { return; }
        NBTTagCompound data = new NBTTagCompound();
        if (entity != null) {
            os.tabletNBT.setTag(APP_NBT_TAG, entity.writeToNBT(data));
        } else {
            os.tabletNBT.removeTag(APP_NBT_TAG);
        }
    }

    private void LaunchApp() {
        if (isClient) {
            //this.addWidget(new ConfigMenuWidget(323, 212));
        } else {
            writeNBT(null);
        }
    }

    private boolean IsValidTileEntity(BlockPos TileEntityLocation) {
        TileEntity te = gui.entityPlayer.world.getTileEntity(os.clickPos);
        if (te instanceof IGregTechTileEntity) {
            IGregTechTileEntity IGTTE = (IGregTechTileEntity) te;
            return IGTTE.getMetaTileEntity() != null;
        }
        return false;
    }

    private MetaTileEntity IsValidTileEntity(TileEntity tileEntity) {
        SimpleMachineMetaTileEntity simple_te = null;
        TileEntity te = gui.entityPlayer.world.getTileEntity(os.clickPos);
        if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity)te).getMetaTileEntity() instanceof SimpleMachineMetaTileEntity) {
            IGregTechTileEntity te_gt = (IGregTechTileEntity)te;
            simple_te = (SimpleMachineMetaTileEntity)te_gt.getMetaTileEntity();
        }
        return simple_te;
    }

    private MachineConfiguration copyMachineConfiguration(SimpleMachineMetaTileEntity machine) {
        MachineConfiguration config = new MachineConfiguration();
        config.outputFacingItems = machine.getOutputFacingItems();
        config.outputFacingFluids = machine.getOutputFacingFluids();
        config.autoOutputItems = machine.isAutoOutputItems();
        config.autoOutputFluids = machine.isAutoOutputFluids();
        config.allowInputFromOutputSideItems = machine.isAllowInputFromOutputSideItems();
        config.allowInputFromOutputSideFluids = machine.isAllowInputFromOutputSideFluids();
        config.frontFace = machine.getFrontFacing();
        config.isMuffled = machine.isMuffled();

        IControllable controllable = machine.getCapability(GregtechTileCapabilities.CAPABILITY_CONTROLLABLE, machine.getFrontFacing());
        if (controllable != null) {
            config.isWorkingEnabled = controllable.isWorkingEnabled();
        }

        for ( EnumFacing side : EnumFacing.VALUES ) {
            config.coverBehaviors[side.getIndex()] = machine.getCoverAtSide(side);
        }

        return config;
    }

    private void applyMachineConfiguration(SimpleMachineMetaTileEntity machine) {
        machine.scheduleRenderUpdate();
        machine.setAutoOutputItems(CurrentConfiguration.autoOutputItems);
        machine.setAutoOutputFluids(CurrentConfiguration.autoOutputFluids);
        machine.setOutputFacingItems(CurrentConfiguration.outputFacingItems);
        machine.setOutputFacingFluids(CurrentConfiguration.outputFacingFluids);
        machine.setAllowInputFromOutputSideItems(CurrentConfiguration.allowInputFromOutputSideItems);
        machine.setAllowInputFromOutputSideFluids(CurrentConfiguration.allowInputFromOutputSideFluids);
        machine.setFrontFacing(CurrentConfiguration.frontFace);
        if (machine.isMuffled() != CurrentConfiguration.isMuffled) {
            machine.toggleMuffled();
        }
        IControllable controllable = machine.getCapability(GregtechTileCapabilities.CAPABILITY_CONTROLLABLE, machine.getFrontFacing());
        if (controllable != null) {
            controllable.setWorkingEnabled(CurrentConfiguration.isWorkingEnabled);
        }
    }

    private void oldWriteNBT(MachineConfiguration config) {
        NBTTagCompound data = new NBTTagCompound();
        if (config != null) {
            data.setInteger("OutputFacing", config.outputFacingItems.getIndex());
            data.setInteger("OutputFacingF", config.outputFacingFluids.getIndex());
            data.setBoolean("AutoOutputItems", config.autoOutputItems);
            data.setBoolean("AutoOutputFluids", config.autoOutputFluids);
            data.setBoolean("AllowInputFromOutputSide", config.allowInputFromOutputSideItems);
            data.setBoolean("AllowInputFromOutputSideF", config.allowInputFromOutputSideFluids);
            data.setInteger("FrontFace", config.frontFace.getIndex());
            data.setBoolean("WorkingAllowed", config.isWorkingEnabled);
            for (CoverBehavior coverBehavior : config.coverBehaviors) {
                if (coverBehavior != null) {
                    NBTTagCompound cover = new NBTTagCompound();
                    coverBehavior.writeToNBT(cover);
                    data.setTag(coverBehavior.attachedSide.getName() + " Cover", cover);
                }
            }
        }

        try {
            CompressedStreamTools.safeWrite(data, new File(TerminalRegistry.TERMINAL_PATH, PATH_FILE_NAME));
        } catch (IOException e) {
            GTLog.logger.error("error while saving local nbt for the configurator", e);
        }
    }

    private MachineConfiguration readNBT() {
        NBTTagCompound data;
        try {
            data = CompressedStreamTools.read(new File(TerminalRegistry.TERMINAL_PATH, PATH_FILE_NAME));
            if (data == null || data.isEmpty()){
                return null;
            }
            MachineConfiguration config = new MachineConfiguration();
            config.outputFacingItems = EnumFacing.VALUES[data.getInteger("OutputFacing")];
            config.outputFacingFluids = EnumFacing.VALUES[data.getInteger("OutputFacingF")];
            config.autoOutputItems = data.getBoolean("AutoOutputItems");
            config.autoOutputFluids = data.getBoolean("AutoOutputFluids");
            config.allowInputFromOutputSideItems = data.getBoolean("AllowInputFromOutputSide");
            config.allowInputFromOutputSideFluids = data.getBoolean("AllowInputFromOutputSideF");
            config.frontFace = EnumFacing.VALUES[data.getInteger("FrontFace")];
            config.isWorkingEnabled = data.getBoolean("WorkingAllowed");
            return config;
        } catch (IOException e) {
            GTLog.logger.error("error while loading local nbt for the configurator", e);
        }
        return null;
    }

    @Override
    public boolean isClientSideApp() { return true; }
}
