package net.minecraft.server;

import javax.annotation.Nullable;

public interface DataPalette {

    default int getDataBits(IBlockData blockData) { return this.a(blockData); } // Paper - Anti-Xray - OBFHELPER
    int a(IBlockData iblockdata);

    @Nullable default IBlockData getBlockData(int dataBits) { return this.a(dataBits); } // Paper - Anti-Xray - OBFHELPER
    @Nullable
    IBlockData a(int i);

    void b(PacketDataSerializer packetdataserializer);

    int a();
}
