package com.destroystokyo.paper.antixray;

import net.minecraft.server.BlockPosition;
import net.minecraft.server.Chunk;
import net.minecraft.server.IBlockData;
import net.minecraft.server.PacketPlayOutMapChunk;
import net.minecraft.server.World;

public class ChunkPacketBlockController {

    public static final ChunkPacketBlockController NO_OPERATION_INSTANCE = new ChunkPacketBlockController();

    protected ChunkPacketBlockController() {
        
    }

    public IBlockData[] getPredefinedBlockData(Chunk chunk, int chunkSectionIndex) {
        return null;
    }

    public boolean onChunkPacketCreate(Chunk chunk, int chunkSectionSelector, boolean force) {
        return true;
    }

    public PacketPlayOutMapChunkInfo getPacketPlayOutMapChunkInfo(PacketPlayOutMapChunk packetPlayOutMapChunk, Chunk chunk, int chunkSectionSelector) {
        return null;
    }

    public void modifyBlocks(PacketPlayOutMapChunk packetPlayOutMapChunk, PacketPlayOutMapChunkInfo packetPlayOutMapChunkInfo) {
        packetPlayOutMapChunk.setReady(true);
    }

    public void updateNearbyBlocks(World world, BlockPosition blockPosition) {

    }
}
