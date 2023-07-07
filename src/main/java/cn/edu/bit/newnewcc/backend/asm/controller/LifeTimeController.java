package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LifeTimeController {
    //虚拟寄存器生命周期的设置过程
    private final AsmFunction function;
    private final Map<Integer, Pair<Integer, Integer>> lifeTime = new HashMap<>();

    public Set<Integer> getKeySet() {
        return lifeTime.keySet();
    }

    public LifeTimeController(AsmFunction function) {
        this.function = function;
    }

    public Pair<Integer, Integer> getLifeTime(Integer index) {
        return lifeTime.get(index);
    }

    private void setVregLifeTime(Integer index, int t) {
        if (!lifeTime.containsKey(index)) {
            lifeTime.put(index, new Pair<>(t, t));
        }
        var x = lifeTime.get(index);
        lifeTime.put(index, new Pair<>(min(x.a, t), max(x.b, t)));
    }

    private Set<Register> getWriteVregSet(AsmInstruction inst) {
        if (!(inst instanceof AsmStore) && inst.getOperand(1) instanceof RegisterReplaceable registerReplaceable) {
            return new HashSet<>(Collections.singleton(registerReplaceable.getRegister()));
        } else {
            return new HashSet<>();
        }
    }

    private Set<Register> getReadVregSet(AsmInstruction inst) {
        var res = new HashSet<Register>();
        for (int i = 1; i <= 3; i++) {
            if (inst.getOperand(i) instanceof RegisterReplaceable op) {
                if ((i == 1 && inst instanceof AsmStore) || (i > 1)) {
                    res.add(op.getRegister());
                }
            }
        }
        return res;
    }

    public void refreshAllVreg(List<AsmInstruction> instructionList) {
        boolean isPhiSegment = false;
        int phiSegmentStart = 0;
        for (int i = 0; i < instructionList.size(); i++) {
            AsmInstruction instruction = instructionList.get(i);
            if (instruction instanceof AsmPhiTag) {
                isPhiSegment = true;
                phiSegmentStart = i;
            }
            if (instruction instanceof AsmTag) {
                isPhiSegment = false;
            }
            if (isPhiSegment) {
                if (instruction instanceof AsmLoad) {
                    var ra = (Register)instruction.getOperand(1);
                    var opb = instruction.getOperand(2);
                    setVregLifeTime(ra.getIndex(), i);
                    if (opb instanceof Register rb) {
                        setVregLifeTime(rb.getIndex(), phiSegmentStart);
                    }
                }
            } else {
                for (int j = 1; j <= 3; j++) {
                    AsmOperand op = instruction.getOperand(j);
                    if (op instanceof RegisterReplaceable registerReplaceableOp) {
                        Register register = registerReplaceableOp.getRegister();
                        if (register.isVirtual()) {
                            setVregLifeTime(register.getIndex(), i);
                        }
                    }
                }
            }
        }
    }
}
