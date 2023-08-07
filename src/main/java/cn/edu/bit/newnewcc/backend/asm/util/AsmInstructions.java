package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AsmInstructions {
    private AsmInstructions() {
    }

    public static Set<Integer> getWriteRegId(AsmInstruction instr) {
        if (instr instanceof AsmStore) {
            if (instr.getOperand(2) instanceof Register) {
                return Set.of(2);
            }
            return Set.of();
        }
        if (instr instanceof AsmJump || instr instanceof AsmIndirectJump) {
            return new HashSet<>();
        }
        if (instr.getOperand(1) instanceof RegisterReplaceable) {
            return Set.of(1);
        }
        return Set.of();
    }

    public static Set<Integer> getWriteVRegId(AsmInstruction instr) {
        var result = new HashSet<Integer>();
        var regId = getWriteRegId(instr);
        for (var x : regId) {
            var reg = ((RegisterReplaceable)instr.getOperand(x)).getRegister();
            if (reg.isVirtual()) {
                result.add(x);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<Integer> getReadRegId(AsmInstruction instr) {
        var result = new HashSet<Integer>();
        for (int i = 1; i <= 3; i++) {
            if (instr.getOperand(i) instanceof RegisterReplaceable) {
                boolean flag = (instr instanceof AsmStore) ? (i == 1 || (i == 2 && !(instr.getOperand(i) instanceof Register))) :
                    (instr instanceof AsmJump || instr instanceof AsmIndirectJump || (i > 1));
                if (flag) {
                    result.add(i);
                }
            }
        }
        return result;
    }

    public static Set<Integer> getReadVRegId(AsmInstruction instr) {
        var result = new HashSet<Integer>();
        var regId = getReadRegId(instr);
        for (var x : regId) {
            var reg = ((RegisterReplaceable) instr.getOperand(x)).getRegister();
            if (reg.isVirtual()) {
                result.add(x);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<Integer> getVRegId(AsmInstruction instr) {
        Set<Integer> result = new HashSet<>();
        result.addAll(getReadVRegId(instr));
        result.addAll(getWriteVRegId(instr));
        return Collections.unmodifiableSet(result);
    }

    public static Set<Register> getWriteRegSet(AsmInstruction instr) {
        Set<Register> result = new HashSet<>();
        for (int i : getWriteRegId(instr)) {
            RegisterReplaceable op = (RegisterReplaceable) instr.getOperand(i);
            result.add(op.getRegister());
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<Integer> getWriteVRegSet(AsmInstruction instr) {
        Set<Integer> result = new HashSet<>();
        for (int i : getWriteVRegId(instr)) {
            RegisterReplaceable op = (RegisterReplaceable) instr.getOperand(i);
            result.add(op.getRegister().getAbsoluteIndex());
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<Register> getReadRegSet(AsmInstruction instr) {
        Set<Register> result = new HashSet<>();
        for (int i : getReadRegId(instr)) {
            RegisterReplaceable op = (RegisterReplaceable) instr.getOperand(i);
            result.add(op.getRegister());
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<Integer> getReadVRegSet(AsmInstruction instr) {
        Set<Integer> result = new HashSet<>();
        for (int i : getReadVRegId(instr)) {
            RegisterReplaceable op = (RegisterReplaceable) instr.getOperand(i);
            result.add(op.getRegister().getAbsoluteIndex());
        }
        return Collections.unmodifiableSet(result);
    }

    public static boolean isMoveVToV(AsmInstruction instr) {
        return instr instanceof AsmMove && ((Register) instr.getOperand(1)).isVirtual() && ((Register) instr.getOperand(2)).isVirtual();
    }

    public static Pair<Integer, Integer> getMoveVReg(AsmInstruction instr) {
        if (!isMoveVToV(instr)) {
            throw new IllegalArgumentException();
        }
        var writeSet = AsmInstructions.getWriteVRegSet(instr);
        var readSet = AsmInstructions.getReadVRegSet(instr);
        return new Pair<>((Integer) writeSet.toArray()[0], (Integer) readSet.toArray()[0]);
    }

    public static int getRegIndexInInst(AsmInstruction instr, Register reg) {
        for (int i = 1; i <= 3; i++) {
            if (instr.getOperand(i) instanceof RegisterReplaceable registerReplaceable) {
                if (registerReplaceable.getRegister().equals(reg)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
