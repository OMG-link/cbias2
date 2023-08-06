package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;

import java.util.Set;

public class AsmLoad extends AsmInstruction {
    public enum Opcode {
        LD("ld"),
        LW("lw"),
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
            if (source instanceof StackVar stackVar) {
                if (stackVar.getSize() == 8) opcode = Opcode.FLD;
                else if (stackVar.getSize() == 4) opcode = Opcode.FLW;
                else throw new IllegalArgumentException();
            } else if (source instanceof Register register) {
                if (register.isFloat()) {
                    opcode = Opcode.FMVS;
                } else {
                    throw new IllegalArgumentException();
                }
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
    public String toString() {
        return String.format("AsmLoad(%s, %s, %s)", getOpcode().getName(), getOperand(1), getOperand(2));
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1).emit(), getOperand(2).emit());
    }

    @Override
    public Set<Register> getDef() {
        return Set.of((Register) getOperand(1));
    }

    @Override
    public Set<Integer> getUse() {
        return switch (getOpcode()) {
            case MV, FMVS -> Set.of(2);
            case LD, LW, LUI, LI, LA, FLD, FLW -> Set.of();
        };
    }
}
