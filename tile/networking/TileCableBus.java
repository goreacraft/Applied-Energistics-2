package appeng.tile.networking;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import powercrystals.minefactoryreloaded.api.rednet.RedNetConnectionType;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IPart;
import appeng.api.parts.LayerFlags;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.helpers.AEMultiTile;
import appeng.helpers.ICustomCollision;
import appeng.hooks.TickHandler;
import appeng.parts.CableBusContainer;
import appeng.tile.AEBaseTile;
import appeng.tile.events.AETileEventHandler;
import appeng.tile.events.TileEventType;
import appeng.transformer.annotations.integration.Method;
import appeng.util.Platform;

public class TileCableBus extends AEBaseTile implements AEMultiTile, ICustomCollision
{

	public CableBusContainer cb = new CableBusContainer( this );
	private int oldLV = -1; // on re-calculate light when it changes

	class CableBusHandler extends AETileEventHandler
	{

		public CableBusHandler() {
			super( TileEventType.NETWORK, TileEventType.WORLD_NBT );
		}

		@Override
		public void readFromNBT(NBTTagCompound data)
		{
			cb.readFromNBT( data );
		}

		@Override
		public void writeToNBT(NBTTagCompound data)
		{
			cb.writeToNBT( data );
		}

		@Override
		public boolean readFromStream(ByteBuf data) throws IOException
		{
			boolean ret = cb.readFromStream( data );

			int newLV = cb.getLightValue();
			if ( newLV != oldLV )
			{
				oldLV = newLV;
				worldObj.func_147451_t( xCoord, yCoord, zCoord );
				// worldObj.updateAllLightTypes( xCoord, yCoord, zCoord );
			}

			return ret;
		}

		@Override
		public void writeToStream(ByteBuf data) throws IOException
		{
			cb.writeToStream( data );
		}

	};

	@Override
	public void onReady()
	{
		super.onReady();
		if ( cb.isEmpty() )
		{
			if ( worldObj.getTileEntity( xCoord, yCoord, zCoord ) == this )
			{
				worldObj.func_147480_a( xCoord, yCoord, zCoord, true );
				// worldObj.destroyBlock( xCoord, yCoord, zCoord, true );
			}
		}
		else
			cb.addToWorld();
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		cb.removeFromWorld();
	}

	@Override
	public void validate()
	{
		super.validate();
		TickHandler.instance.addInit( this );
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		cb.removeFromWorld();
	}

	@Override
	public boolean canBeRotated()
	{
		return false;
	}

	@Override
	public double getMaxRenderDistanceSquared()
	{
		return 900.0;
	}

	@Override
	public void getDrops(World w, int x, int y, int z, ArrayList drops)
	{
		cb.getDrops( drops );
	}

	public TileCableBus() {
		addNewHandler( new CableBusHandler() );
	}

	@Override
	public IGridNode getGridNode(ForgeDirection dir)
	{
		return cb.getGridNode( dir );
	}

	@Override
	public boolean canAddPart(ItemStack is, ForgeDirection side)
	{
		return cb.canAddPart( is, side );
	}

	@Override
	public ForgeDirection addPart(ItemStack is, ForgeDirection side, EntityPlayer player)
	{
		return cb.addPart( is, side, player );
	}

	@Override
	public void removePart(ForgeDirection side, boolean supressUpdate)
	{
		cb.removePart( side, supressUpdate );
	}

	@Override
	public IPart getPart(ForgeDirection side)
	{
		return cb.getPart( side );
	}

	@Override
	public DimensionalCoord getLocation()
	{
		return new DimensionalCoord( this );
	}

	@Override
	public TileEntity getTile()
	{
		return this;
	}

	@Override
	public Iterable<AxisAlignedBB> getSelectedBoundingBoxsFromPool(World w, int x, int y, int z, Entity e, boolean visual)
	{
		return cb.getSelectedBoundingBoxsFromPool( false, true, e, visual );
	}

	@Override
	public void addCollidingBlockToList(World w, int x, int y, int z, AxisAlignedBB bb, List out, Entity e)
	{
		for (AxisAlignedBB bx : getSelectedBoundingBoxsFromPool( w, x, y, z, e, false ))
			out.add( AxisAlignedBB.getAABBPool().getAABB( bx.minX, bx.minY, bx.minZ, bx.maxX, bx.maxY, bx.maxZ ) );
	}

	@Override
	public AECableType getCableConnectionType(ForgeDirection side)
	{
		return cb.getCableConnectionType( side );
	}

	@Override
	public AEColor getColor()
	{
		return cb.getColor();
	}

	@Override
	public IFacadeContainer getFacadeContainer()
	{
		return cb.getFacadeContainer();
	}

	@Override
	public void clearContainer()
	{
		cb = new CableBusContainer( this );
	}

	@Override
	public boolean isBlocked(ForgeDirection side)
	{
		return !ImmibisMicroblocks_isSideOpen( side.ordinal() );
	}

	@Override
	public void markForUpdate()
	{
		if ( worldObj == null )
			return;

		int newLV = cb.getLightValue();
		if ( newLV != oldLV )
		{
			oldLV = newLV;
			worldObj.func_147451_t( xCoord, yCoord, zCoord );
			// worldObj.updateAllLightTypes( xCoord, yCoord, zCoord );
		}

		super.markForUpdate();
	}

	@Override
	public SelectedPart selectPart(Vec3 pos)
	{
		return cb.selectPart( pos );
	}

	@Override
	public void partChanged()
	{
		if ( worldObj != null )
			worldObj.notifyBlocksOfNeighborChange( xCoord, yCoord, zCoord, Platform.air );
	}

	@Override
	public void markForSave()
	{
		super.markDirty();
	}

	@Override
	public boolean hasRedstone(ForgeDirection side)
	{
		return cb.hasRedstone( side );
	}

	@Override
	@Method(iname = "MFR")
	public RedNetConnectionType getConnectionType(World world, int x, int y, int z, ForgeDirection side)
	{
		return cb.canConnectRedstone( EnumSet.of( side ) ) ? RedNetConnectionType.CableSingle : RedNetConnectionType.None;
	}

	@Override
	@Method(iname = "MFR")
	public int[] getOutputValues(World world, int x, int y, int z, ForgeDirection side)
	{
		// never called!
		return null;
	}

	@Override
	@Method(iname = "MFR")
	public int getOutputValue(World world, int x, int y, int z, ForgeDirection side, int subnet)
	{
		// never called!
		return 0;
	}

	@Override
	@Method(iname = "MFR")
	public void onInputsChanged(World world, int x, int y, int z, ForgeDirection side, int[] inputValues)
	{
		// never called!
	}

	@Override
	@Method(iname = "MFR")
	public void onInputChanged(World world, int x, int y, int z, ForgeDirection side, int inputValue)
	{
		// never called!
	}

	@Override
	public boolean isEmpty()
	{
		return cb.isEmpty();
	}

	@Override
	public boolean requiresTESR()
	{
		return cb.requiresDynamicRender;
	}

	@Override
	public Set<LayerFlags> getLayerFlags()
	{
		return cb.getLayerFlags();
	}

	@Override
	public void cleanup()
	{
		getWorldObj().setBlock( xCoord, yCoord, zCoord, Platform.air );
	}

	/**
	 * Immibis MB Support
	 */

	boolean ImmibisMicroblocks_TransformableTileEntityMarker = true;

	public boolean ImmibisMicroblocks_isSideOpen(int side)
	{
		return true;
	}

	public void ImmibisMicroblocks_onMicroblocksChanged()
	{
		cb.updateConnections();
	}

}
