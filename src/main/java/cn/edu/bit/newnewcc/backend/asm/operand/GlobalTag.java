package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 全局标记实际上存储的是地址，通常代表全局变量或浮点变量
 * 读取地址的时候使用%hi(tag), %lo(tag)两个伪指令，分别读取高16位和低16位
 * 使用地址寄存器+地址偏移量的形式读取，例如%lo(tag)(a5)
 */
public class GlobalTag extends AsmOperand {
    private final String tagName;
    private final SEGMENT segment;
    private final IntRegister baseAddress;

    /**
     * 创建一个内存位置的标识符，用于读取数据
     *
     * @param tagName     标识符名字
     * @param segment     取地址的段（分为高16位与低16位）
     * @param baseAddress 取的基地址，若为null则仅返回偏移量
     */
    public GlobalTag(String tagName, SEGMENT segment, IntRegister baseAddress) {
        super(TYPE.GTAG);
        this.tagName = tagName;
        this.segment = segment;
        this.baseAddress = baseAddress;
    }

    /**
     * 创建一个代码位置的标识符，用于支持代码跳转
     * <p>
     * 标识符具有两类，一类是全局变量、函数名等带有globl标签的可链接的名称，
     * <p>
     * 另一类是局部的块标签、数据标签等，无需globl标记，通常在前方加'.'作为区分
     *
     * @param tagName     标识符名称
     * @param isLinkedTag 是否是带有标记的全局类标签
     */
    public GlobalTag(String tagName, boolean isLinkedTag) {
        super(TYPE.GTAG);
        if (isLinkedTag) {
            this.tagName = tagName;
        } else {
            this.tagName = "." + tagName;
        }
        this.segment = null;
        this.baseAddress = null;
    }

    public GlobalTag(String tagName, SEGMENT segment) {
        super(TYPE.GTAG);
        this.tagName = tagName;
        this.segment = segment;
        this.baseAddress = null;
    }

    private String getOffset() {
        if (segment == SEGMENT.HIGH) {
            return String.format("%%hi(%s)", tagName);
        } else if (segment == SEGMENT.LOW) {
            return String.format("%%lo(%s)", tagName);
        } else {
            return tagName;
        }
    }

    public boolean isHighSegment() {
        return segment == SEGMENT.HIGH;
    }

    public String emit() {
        if (baseAddress == null) {
            return getOffset();
        } else {
            return getOffset() + "(" + baseAddress.emit() + ")";
        }
    }

    /**
     * 输出一个带冒号的标签，仅用于标签位置
     *
     * @return 返回标签名称+括号
     */
    public String tagExpress() {
        return tagName + ":";
    }

    public enum SEGMENT {
        HIGH, LOW
    }
}
