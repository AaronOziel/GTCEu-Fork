package gregtech.common.terminal.app.configurator;

import gregtech.api.cover.CoverBehavior;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.terminal.app.AbstractApplication;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class ConfiguratorApp extends AbstractApplication {
    public static final String APP_NAME = "configurator";
    public static final String APP_NBT_TAG = APP_NAME + "_config";

    public ConfiguratorApp(){ super(APP_NAME); }

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
        TileEntity te = gui.entityPlayer.world.getTileEntity(TileEntityLocation);
        if (te instanceof IGregTechTileEntity) {
            IGregTechTileEntity IGTTE = (IGregTechTileEntity) te;
            return IGTTE.getMetaTileEntity() != null;
        }
        return false;
    }

    @Override
    public boolean isClientSideApp() { return true; }
}
