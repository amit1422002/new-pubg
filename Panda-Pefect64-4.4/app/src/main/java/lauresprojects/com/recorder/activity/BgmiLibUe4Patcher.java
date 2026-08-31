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
        return new MemPatchUtil.Patch[0];
    }

    private static MemPatchUtil.Patch patch(long offset, byte[] bytes, String name) {
        return new MemPatchUtil.Patch(offset, MemPatchUtil.PATCH_NOP, bytes, name);
    }

    private BgmiLibUe4Patcher() {
    }
}
