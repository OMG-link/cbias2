package cn.edu.bit.newnewcc.backend.asm;

/**
 * 用于标注数据格式，WORD时value存储的为数据值，ZERO则存储数据长度（字节数）
 */
public class ValueTag {
    private final Tag tag;
    private final int value;
    private final long length;

    public ValueTag(Tag tag, long value) {
        this.tag = tag;
        if (tag == Tag.WORD) {
            this.value = (int) value;
            this.length = 0;
        } else {
            this.length = value;
            this.value = 0;
        }
    }

    public ValueTag(int value) {
        this.tag = Tag.WORD;
        this.length = 0;
        this.value = value;
    }

    public ValueTag(float value) {
        this.tag = Tag.WORD;
        this.length = 0;
        this.value = Float.floatToIntBits(value);
    }

    public String emit() {
        if (tag == Tag.WORD) {
            return String.format(".word %d", value);
        } else {
            return String.format(".zero %d", length);
        }
    }

    public enum Tag {
        WORD, ZERO
    }
}
