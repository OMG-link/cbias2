package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PeepholeOptimizer {
    private PeepholeOptimizer() {
    }

    private static final IntRegister X0 = IntRegister.getPhysical(0);

    private static final Set<Register> CALLER_SAVED_REGISTERS = Set.of(
        IntRegister.getPhysical(1),
        IntRegister.getPhysical(5),
        IntRegister.getPhysical(6),
        IntRegister.getPhysical(7),
        IntRegister.getPhysical(10),
        IntRegister.getPhysical(11),
        IntRegister.getPhysical(12),
        IntRegister.getPhysical(13),
        IntRegister.getPhysical(14),
        IntRegister.getPhysical(15),
        IntRegister.getPhysical(16),
        IntRegister.getPhysical(17),
        IntRegister.getPhysical(28),
        IntRegister.getPhysical(29),
        IntRegister.getPhysical(30),
        IntRegister.getPhysical(31),
        FloatRegister.getPhysical(0),
        FloatRegister.getPhysical(1),
        FloatRegister.getPhysical(2),
        FloatRegister.getPhysical(3),
        FloatRegister.getPhysical(4),
        FloatRegister.getPhysical(5),
        FloatRegister.getPhysical(6),
        FloatRegister.getPhysical(7),
        FloatRegister.getPhysical(10),
        FloatRegister.getPhysical(11),
        FloatRegister.getPhysical(12),
        FloatRegister.getPhysical(13),
        FloatRegister.getPhysical(14),
        FloatRegister.getPhysical(15),
        FloatRegister.getPhysical(16),
        FloatRegister.getPhysical(17),
        FloatRegister.getPhysical(28),
        FloatRegister.getPhysical(29),
        FloatRegister.getPhysical(30),
        FloatRegister.getPhysical(31)
    );

    private static Set<Integer> getSourceRegIndices(AsmInstruction instr) {
        if (instr instanceof AsmLabel) {
            throw new UnsupportedOperationException();
        } else if (instr instanceof AsmCall) {
            return Set.of();
        } else if (instr instanceof AsmJump) {
            if (instr.getOperand(1) instanceof Register && instr.getOperand(2) instanceof Register) {
                return Set.of(1, 2);
            } else if (instr.getOperand(1) instanceof Register) {
                return Set.of(1);
            } else {
                return Set.of();
            }
        } else if (instr instanceof AsmIndirectJump) {
            return Set.of(1);
        } else if (instr instanceof AsmLoad) {
            if (instr.getOperand(2) instanceof Register) {
                return Set.of(2);
            } else {
                return Set.of();
            }
        } else if (instr instanceof AsmStore) {
            return Set.of(1);
        } else if (instr instanceof AsmAdd || instr instanceof AsmSub) {
            if (instr.getOperand(3) instanceof Register) {
                return Set.of(2, 3);
            } else {
                return Set.of(2);
            }
        } else if (instr instanceof AsmIntegerCompare) {
            if (instr.getOperand(3) instanceof Register) {
                return Set.of(2, 3);
            } else {
                return Set.of(2);
            }
        } else if (instr instanceof AsmShiftLeft) {
            if (instr.getOperand(3) instanceof Register) {
                return Set.of(2, 3);
            } else {
                return Set.of(2);
            }
        } else if (instr instanceof AsmConvertFloatInt) {
            return Set.of(2);
        } else if (instr instanceof AsmFloatNegate) {
            return Set.of(2);
        } else if (instr instanceof AsmShiftRightArithmetic || instr instanceof AsmShiftRightLogical) {
            if (instr.getOperand(3) instanceof Register) {
                return Set.of(2, 3);
            } else {
                return Set.of(2);
            }
        } else if (instr instanceof AsmBlockEnd) {
            return Set.of();
        } else {
            return Set.of(2, 3);
        }
    }

    private static Set<Register> getModifiedRegs(AsmInstruction instr) {
        if (instr instanceof AsmLabel) {
            throw new UnsupportedOperationException();
        } else if (instr instanceof AsmJump) {
            return Set.of();
        } else if (instr instanceof AsmIndirectJump) {
            return Set.of();
        } else if (instr instanceof AsmStore) {
            if (instr.getOperand(2) instanceof Register) {
                return Set.of((Register) instr.getOperand(2));
            } else {
                return Set.of();
            }
        } else if (instr instanceof AsmCall) {
            return CALLER_SAVED_REGISTERS;
        } else if (instr instanceof AsmBlockEnd) {
            return Set.of();
        } else {
            return Set.of((Register) instr.getOperand(1));
        }
    }

    private static boolean lI0ToX0(List<AsmInstruction> instrList) {
        boolean madeChange = false;
        Set<Register> zeroRegs = new HashSet<>();

        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmLabel) {
                zeroRegs.clear();
            } else if (instr instanceof AsmLoad && instr.getOperand(2) instanceof Immediate && ((Immediate) instr.getOperand(2)).getValue() == 0) {
                zeroRegs.add((Register) instr.getOperand(1));
            } else {
                for (int i : getSourceRegIndices(instr)) {
                    if (zeroRegs.contains((Register) instr.getOperand(i))) {
                        instr.replaceOperand(i, X0);
                        madeChange = true;
                    }
                }
                for (Register reg : getModifiedRegs(instr)) {
                    zeroRegs.remove(reg);
                }
            }
        }

        return madeChange;
    }

    public static void runBeforeAllocation(List<AsmInstruction> instrList) {
        lI0ToX0(instrList);
    }

    public static void runAfterAllocation(List<AsmInstruction> instrList) {

    }
}
