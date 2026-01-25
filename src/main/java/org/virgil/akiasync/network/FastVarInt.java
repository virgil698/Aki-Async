package org.virgil.akiasync.network;

import io.netty.buffer.ByteBuf;

public class FastVarInt {

    private static final int MAXIMUM_VARINT_SIZE = 5;
    private static final int[] VAR_INT_LENGTHS = new int[33];

    static {
        for (int i = 0; i <= 32; ++i) {
            VAR_INT_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
        }
        VAR_INT_LENGTHS[32] = 1;
    }

    public static int readVarInt(ByteBuf buf) {
        int readable = buf.readableBytes();
        if (readable == 0) {
            throw new IllegalStateException("Empty buffer for VarInt");
        }

        int k = buf.readByte();
        if ((k & 0x80) != 128) {
            return k;
        }

        int maxRead = Math.min(MAXIMUM_VARINT_SIZE, readable);
        int i = k & 0x7F;
        for (int j = 1; j < maxRead; j++) {
            k = buf.readByte();
            i |= (k & 0x7F) << j * 7;
            if ((k & 0x80) != 128) {
                return i;
            }
        }
        throw new IllegalStateException("VarInt too big");
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.writeByte(value);
            return;
        }

        if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
            return;
        }

        writeVarIntFull(buf, value);
    }

    private static void writeVarIntFull(ByteBuf buf, int value) {
        if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16
                  | ((value >>> 7) & 0x7F | 0x80) << 8
                  | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24
                  | (((value >>> 7) & 0x7F | 0x80) << 16)
                  | ((value >>> 14) & 0x7F | 0x80) << 8
                  | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24
                  | ((value >>> 7) & 0x7F | 0x80) << 16
                  | ((value >>> 14) & 0x7F | 0x80) << 8
                  | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }

    public static int varIntBytes(int value) {
        return VAR_INT_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }

    public static int encode21BitVarInt(int value) {
        return (value & 0x7F | 0x80) << 16
             | ((value >>> 7) & 0x7F | 0x80) << 8
             | (value >>> 14);
    }
}
