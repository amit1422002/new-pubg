package lauresprojects.com.recorder.activity;

public class MemPatchUtil {
    public static final int PATCH_NOP = 1;
    public static final int PATCH_RET = 2;
    
    public static class Patch {
        public final long offset;
        public final int type;
        public final byte[] bytes;
        public final String name;
        
        public Patch(long offset, int type, byte[] bytes, String name) {
            this.offset = offset;
            this.type = type;
            this.bytes = bytes;
            this.name = name;
        }
    }
}