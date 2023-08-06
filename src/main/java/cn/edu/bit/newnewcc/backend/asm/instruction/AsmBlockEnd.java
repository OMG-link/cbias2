package cn.edu.bit.newnewcc.backend.asm.instruction;

/**
 * 此指令为基本块末尾的占位符，仅用于标示基本块结束
 */
public class AsmBlockEnd extends AsmInstruction {
    public AsmBlockEnd() {
        super("", null, null, null);
    }
}
