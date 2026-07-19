package lauresprojects.com.recorder.activity;

/**
 * BGMI libUE4.so — aim/angle safety + CRC bypass stubs.
 *
 * <p><b>Profile:</b> 14 UE4 patches (anogs hooks/ELF off).
 */
public final class BgmiLibUe4Patcher {

    static final long MIN_MAPPED_BYTES = 4 * 1024 * 1024L;

    private static final byte[] NOP = new byte[] {0x1F, 0x20, 0x03, (byte) 0xD5};
    /** MOV X0, #0 ; RET */
    private static final byte[] MOV0_RET = new byte[] {
            0x00, 0x00, (byte) 0x80, (byte) 0xD2, (byte) 0xC0, 0x03, 0x5F, (byte) 0xD6
    };
    /** Two NOPs (8 bytes) — kill CMP/BL/B sites without early-return. */
    private static final byte[] NOP8 = new byte[] {
            0x1F, 0x20, 0x03, (byte) 0xD5, 0x1F, 0x20, 0x03, (byte) 0xD5
    };

    static MemPatchUtil.Patch[] collectRuntimePatches() {
        return new MemPatchUtil.Patch[] {

                // A) NaN / bad coordinates — 6 NOP patches
                patch(0xA158420L, NOP, "UE4_NAN_COORD_1"),
                patch(0xA158430L, NOP, "UE4_NAN_COORD_2"),
                patch(0xA158444L, NOP, "UE4_NAN_COORD_3"),
                patch(0xA158474L, NOP, "UE4_NAN_COORD_4"),
                patch(0xA158484L, NOP, "UE4_NAN_COORD_5"),
                patch(0xA158494L, NOP, "UE4_NAN_COORD_6"),

                // B) Extreme angle discard — 1 NOP
                patch(0xA158E5CL, NOP, "UE4_EXTREME_ANGLE_NOP"),

                // C) Caller safety — 3 branch patches
                patch(0xA1637D8L, new byte[] {0x11, 0x00, 0x00, 0x14}, "UE4_CALLER_SAFE_1"),
                patch(0xA164E94L, new byte[] {0x11, 0x00, 0x00, 0x14}, "UE4_CALLER_SAFE_2"),
                patch(0xA159EBCL, new byte[] {0x0A, 0x00, 0x00, 0x14}, "UE4_CALLER_SAFE_3"),

                // E) CRC / report — 8-byte NOP stubs
                patch(0x7D3036CL, NOP8, "UE4_CRC_CMP_7D3036C"),
                patch(0x7D30370L, NOP8, "UE4_CRC_BEQ_7D30370"),
                patch(0x7D30354L, NOP8, "UE4_CRC_BL_7D30354"),
                patch(0x7D30250L, NOP8, "UE4_CRC_REPORT_7D30250"),

              

        };
    }

    private static MemPatchUtil.Patch patch(long offset, byte[] bytes, String name) {
        return new MemPatchUtil.Patch(offset, MemPatchUtil.PATCH_NOP, bytes, name);
    }

    private BgmiLibUe4Patcher() {
    }
}
