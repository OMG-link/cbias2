package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;

/**
 * 汇编部分中的load指令，在本语言中分为
 * <ul>
 *     <li>ld 双字加载，仅当加载64位的栈变量时使用</li>
 *     <li>lw 字加载，在加载任何变量时使用，默认指令均为lw</li>
 *     <li>lui 高位立即数加载，仅当加载全局符号地址中的高位时使用</li>
 *     <li>li 立即数加载</li>
 * </ul>
 */
public class AsmLoad extends AsmInstruction {
    public enum Opcode {
        LW("lw"),
        LD("ld"),
        LUI("lui"),
        LI("li"),
        LA("la"),
        MV("mv"),
        FLD("fld"),
        FLW("flw"),
        FMVS("fmv.s");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmLoad(Register dest, AsmOperand source) {
        super(dest, source, null);

        if (dest.isInt()) {
            if (source instanceof StackVar stackVar) {
                if (stackVar.getSize() == 8) opcode = Opcode.LD;
                else if (stackVar.getSize() == 4) opcode = Opcode.LW;
                else throw new IllegalArgumentException();
            } else if (source instanceof Immediate) {
                opcode = Opcode.LI;
            } else if (source instanceof Label label) {
                if (label.isHighSegment()) {
                    opcode = Opcode.LUI;
                } else if (label.isLowSegment()) {
                    opcode = Opcode.LI;
                } else {
                    opcode = Opcode.LA;
                }
            } else if (source instanceof Register register) {
                if (register.isInt()) {
                    opcode = Opcode.MV;
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            if (source instanceof Register register) {
                if (register.isFloat()) {
                    opcode = Opcode.FMVS;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (source instanceof StackVar stackVar) {
                if (stackVar.getSize() == 8) opcode = Opcode.FLD;
                else if (stackVar.getSize() == 4) opcode = Opcode.FLW;
                else throw new IllegalArgumentException();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public AsmLoad(Register dest, Address source, int bitLength) {
        super(dest, source, null);

        if (bitLength != 32 && bitLength != 64)
            throw new IllegalArgumentException();

        if (dest.isInt()) {
            if (bitLength == 64) opcode = Opcode.LD;
            else opcode = Opcode.LW;
        } else {
            if (bitLength == 64) opcode = Opcode.FLD;
            else opcode = Opcode.FLW;
        }
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2));
    }
}
