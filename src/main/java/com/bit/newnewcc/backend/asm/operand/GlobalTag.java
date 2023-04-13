package com.bit.newnewcc.backend.asm.operand;

//全局标记实际上存储的是地址，通常代表全局变量或浮点变量
//读取地址的时候使用%hi(tag), %lo(tag)两个伪指令，分别读取高16位和低16位
//使用地址寄存器+地址偏移量的形式读取，例如%lo(tag)(a5)

public class GlobalTag extends AsmOperand {
    String tagName;
    SEGMENT segment;
    Register baseAddress;

    public GlobalTag(String tagName, SEGMENT segment, Register baseAddress) {
        super(TYPE.GTAG);
        this.tagName = tagName;
        this.segment = segment;
        this.baseAddress = baseAddress;
    }

    public String getOffset() {
        if (segment == SEGMENT.HIGH) {
            return String.format("%%hi(%s)", tagName);
        } else {
            return String.format("%%lo(%s)", tagName);
        }
    }

    public String emit() {
        if (baseAddress == null) {
            return getOffset();
        } else {
            return getOffset() + "(" + baseAddress.emit() + ")";
        }
    }

    enum SEGMENT {
        HIGH, LOW
    }
}
