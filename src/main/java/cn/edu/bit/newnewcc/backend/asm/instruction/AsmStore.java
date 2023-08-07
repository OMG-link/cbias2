package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;

import java.util.Set;

public class AsmStore extends AsmInstruction {
    public enum Opcode {
        SD("sd"),
        SW("sw"),
        FSD("fsd"),
        FSW("fsw");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmStore(Register source, StackVar dest) {
        super(source, dest, null);

        if (source instanceof IntRegister) {
            if (dest.getSize() == 8) opcode = Opcode.SD;
            else if (dest.getSize() == 4) opcode = Opcode.SW;
            else throw new IllegalArgumentException();
        } else {
            if (dest.getSize() == 8) opcode = Opcode.FSD;
            else if (dest.getSize() == 4) opcode = Opcode.FSW;
            else throw new IllegalArgumentException();
        }
    }

    public AsmStore(Register source, Address dest, int bitLength) {
        super(source, dest, null);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (source instanceof IntRegister) {
            if (bitLength == 64) opcode = Opcode.SD;
            else opcode = Opcode.SW;
        } else {
            if (bitLength == 64) opcode = Opcode.FSD;
            else opcode = Opcode.FSW;
        }
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format("AsmStore(%s, %s, %s)", getOpcode().getName(), getOperand(1), getOperand(2));
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1).emit(), getOperand(2).emit());
    }

    @Override
    public Set<Register> getDef() {
        return Set.of();
    }

    @Override
    public Set<Integer> getUse() {
        return Set.of(1);
    }

    @Override
    public boolean willReturn() {
        return true;
    }

    @Override
    public boolean mayWriteToMemory() {
        return true;
    }
}
