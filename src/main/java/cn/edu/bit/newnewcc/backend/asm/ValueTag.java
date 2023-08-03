package cn.edu.bit.newnewcc.backend.asm;

/**
 * 用于标注数据格式，WORD时value存储的为数据值，ZERO则存储数据长度（字节数）
 */
public class ValueTag {
    private final Tag tag;
    private final int value;
    private final long length;
    private final long lvalue;

    ValueTag(Tag tag, int value, long length, long lvalue) {
        this.tag = tag;
        this.value = value;
        this.length = length;
        this.lvalue = lvalue;
    }

    static public ValueTag getZeroValue(long length) {
        return new ValueTag(Tag.ZERO, 0, length, 0);
    }

    public ValueTag(int value) {
        this.tag = Tag.WORD;
        this.length = 0;
        this.value = value;
        this.lvalue = 0;
    }

    public ValueTag(float value) {
        this.tag = Tag.WORD;
        this.length = 0;
        this.value = Float.floatToIntBits(value);
        this.lvalue = 0;
    }

    public ValueTag(long lvalue) {
        this.tag = Tag.DWORD;
        this.length = 0;
        this.value = 0;
        this.lvalue = lvalue;
    }

    public String emit() {
        if (tag == Tag.WORD) {
            return String.format(".word %d", value);
        } else if (tag == Tag.ZERO) {
            return String.format(".zero %d", length);
        } else {
            return String.format(".dword %d", lvalue);
        }
    }

    public enum Tag {
        WORD, ZERO, DWORD
    }
}
