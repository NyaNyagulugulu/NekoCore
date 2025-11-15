package net.minecraft.server;

import javax.annotation.Nullable;

// Paper start
import com.destroystokyo.paper.antixray.PacketPlayOutMapChunkInfo; // Anti-Xray
// Paper end

public class DataPaletteBlock implements DataPaletteExpandable {

    private static final DataPalette d = new DataPaletteGlobal();
    protected static final IBlockData a = Blocks.AIR.getBlockData(); public static final IBlockData DEFAULT_BLOCK_DATA = DataPaletteBlock.a; // Paper - OBFHELPER
    protected DataBits b; protected DataBits getDataBits() { return this.b; } // Paper - Anti-Xray - OBFHELPER
    protected DataPalette c; protected DataPalette getDataPalette() { return this.c; } // Paper - Anti-Xray - OBFHELPER
    private int e; private int getBitsPerValue() { return this.e; } // Paper - Anti-Xray - OBFHELPER
    private final IBlockData[] predefinedBlockData; // Paper - Anti-Xray - Add predefined block data

    // Paper start - Anti-Xray - Support default constructor
    public DataPaletteBlock() {
        this(null);
    }
    // Paper end

    // Paper start - Anti-Xray - Add predefined block data
    public DataPaletteBlock(IBlockData[] predefinedBlockData) {
        this.predefinedBlockData = predefinedBlockData;

        if (predefinedBlockData == null) {
            // Default constructor
            this.setBitsPerValue(4);
        } else {
            // Count the bits of the maximum array index to initialize a data palette with enough space from the beginning
            // The length of the array is used because air is also added to the data palette from the beginning
            // Start with at least 4
            int maxIndex = predefinedBlockData.length >> 4;
            int bitCount = 4;

            while (maxIndex != 0) {
                maxIndex >>= 1;
                bitCount++;
            }

            // Initialize with at least 15 free indixes
            this.setBitsPerValue((1 << bitCount) - predefinedBlockData.length < 16 ? bitCount + 1 : bitCount);
        }
    }
    // Paper end

    private static int b(int i, int j, int k) {
        return j << 8 | k << 4 | i;
    }

    private void setBitsPerValue(int bitsPerValue) { this.b(bitsPerValue); } // Paper - Anti-Xray - OBFHELPER
    private void b(int i) {
        if (i != this.e) {
            this.e = i;
            if (this.e <= 4) {
                this.e = 4;
                this.c = new DataPaletteLinear(this.e, this);
            } else if (this.e <= 8) {
                this.c = new DataPaletteHash(this.e, this);
            } else {
                this.c = DataPaletteBlock.d;
                this.e = MathHelper.d(Block.REGISTRY_ID.a());
            }

            this.c.a(DataPaletteBlock.a);

            // Paper start - Anti-Xray - Add predefined block data
            if (this.predefinedBlockData != null) {
                for (int j = 0; j < this.predefinedBlockData.length; j++) {
                    this.getDataPalette().getDataBits(this.predefinedBlockData[j]);
                }
            }
            // Paper end

            this.b = new DataBits(this.e, 4096);
        }
    }

    public int a(int i, IBlockData iblockdata) {
        DataBits databits = this.b;
        DataPalette datapalette = this.c;

        this.b(i);

        for (int j = 0; j < databits.b(); ++j) {
            IBlockData iblockdata1 = datapalette.a(databits.a(j));

            if (iblockdata1 != null) {
                this.setBlockIndex(j, iblockdata1);
            }
        }

        return this.c.a(iblockdata);
    }

    public void setBlock(int i, int j, int k, IBlockData iblockdata) {
        this.setBlockIndex(b(i, j, k), iblockdata);
    }

    protected void setBlockIndex(int i, IBlockData iblockdata) {
        int j = this.c.a(iblockdata);

        this.b.a(i, j);
    }

    public IBlockData a(int i, int j, int k) {
        return this.a(b(i, j, k));
    }

    protected IBlockData a(int i) {
        IBlockData iblockdata = this.c.a(this.b.a(i));

        return iblockdata == null ? DataPaletteBlock.a : iblockdata;
    }

    // Paper start - Anti-Xray - Support default method
    public void writeBlocks(PacketDataSerializer packetDataSerializer) { this.b(packetDataSerializer); } // OBFHELPER
    public void b(PacketDataSerializer packetdataserializer) {
        this.b(packetdataserializer, null, 0);
    }
    // Paper end

    public void writeBlocks(PacketDataSerializer packetDataSerializer, PacketPlayOutMapChunkInfo packetPlayOutMapChunkInfo, int chunkSectionIndex) { this.b(packetDataSerializer, packetPlayOutMapChunkInfo, chunkSectionIndex); } // Paper - Anti-Xray - OBFHELPER
    public void b(PacketDataSerializer packetdataserializer, PacketPlayOutMapChunkInfo packetPlayOutMapChunkInfo, int chunkSectionIndex) { // Paper - Anti-Xray - Add chunk packet info
        packetdataserializer.writeByte(this.e);
        this.c.b(packetdataserializer);

        // Paper start - Anti-Xray - Add chunk packet info
        if (packetPlayOutMapChunkInfo != null) {
            packetPlayOutMapChunkInfo.setBitsPerValue(chunkSectionIndex, this.getBitsPerValue());
            packetPlayOutMapChunkInfo.setDataPalette(chunkSectionIndex, this.getDataPalette());
            packetPlayOutMapChunkInfo.setDataBitsIndex(chunkSectionIndex, packetdataserializer.writerIndex() + PacketDataSerializer.countBytes(this.getDataBits().getDataBits().length));
            packetPlayOutMapChunkInfo.setPredefinedBlockData(chunkSectionIndex, this.predefinedBlockData);
        }
        // Paper end

        packetdataserializer.a(this.b.a());
    }

    @Nullable
    public NibbleArray exportData(byte[] abyte, NibbleArray nibblearray) {
        NibbleArray nibblearray1 = null;

        for (int i = 0; i < 4096; ++i) {
            int j = Block.REGISTRY_ID.getId(this.a(i));
            int k = i & 15;
            int l = i >> 8 & 15;
            int i1 = i >> 4 & 15;

            if ((j >> 12 & 15) != 0) {
                if (nibblearray1 == null) {
                    nibblearray1 = new NibbleArray();
                }

                nibblearray1.a(k, l, i1, j >> 12 & 15);
            }

            abyte[i] = (byte) (j >> 4 & 255);
            nibblearray.a(k, l, i1, j & 15);
        }

        return nibblearray1;
    }

    public void a(byte[] abyte, NibbleArray nibblearray, @Nullable NibbleArray nibblearray1) {
        for (int i = 0; i < 4096; ++i) {
            int j = i & 15;
            int k = i >> 8 & 15;
            int l = i >> 4 & 15;
            int i1 = nibblearray1 == null ? 0 : nibblearray1.a(j, k, l);
            int j1 = i1 << 12 | (abyte[i] & 255) << 4 | nibblearray.a(j, k, l);

            // CraftBukkit start - fix blocks with random data values (caused by plugins)
            IBlockData data = Block.REGISTRY_ID.fromId(j1);
            if (data == null) {
                Block block = Block.getById(j1 >> 4);
                if (block != null) {
                    try {
                        data = block.fromLegacyData(j1 & 0xF);
                    } catch (Exception ignored) {
                        data = block.getBlockData();
                    }
                }
            }
            this.setBlockIndex(i, data);
            // this.setBlockIndex(i, (IBlockData) Block.REGISTRY_ID.fromId(j1));
            // CraftBukkit end
        }

    }

    public int a() {
        return 1 + this.c.a() + PacketDataSerializer.a(this.b.b()) + this.b.a().length * 8;
    }
}
