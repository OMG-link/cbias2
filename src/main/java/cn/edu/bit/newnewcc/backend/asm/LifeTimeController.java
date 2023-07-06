package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmPhiTag;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmTag;
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
    private final Stack<Pair<Integer, BasicBlock>> lifeToBlockEnd = new Stack<>();

    public Set<Integer> getKeySet() {
        return lifeTime.keySet();
    }

    public LifeTimeController(AsmFunction function) {
        this.function = function;
    }

    public void setVregLifeTimeBlockEnd(Integer index, BasicBlock block) {
        lifeToBlockEnd.push(new Pair<>(index, block));
    }

    public Pair<Integer, Integer> getLifeTime(Integer index) {
        return lifeTime.get(index);
    }

    public void refreshBlockEndVreg(List<AsmInstruction> instructionList) {
        Map<AsmTag, Integer> endTime = new HashMap<>();
        AsmTag nowTag = null;
        for (int i = 0; i < instructionList.size(); i++) {
            var inst = instructionList.get(i);
            if (inst instanceof AsmTag tag) {
                nowTag = tag;
            } else if (inst instanceof AsmPhiTag phiTag) {
                nowTag = phiTag.getSourceBlockTag();
            }
            endTime.put(nowTag, i);
        }
        while (!lifeToBlockEnd.empty()) {
            var p = lifeToBlockEnd.pop();
            setVregLifeTime(p.a, endTime.get(function.getBlockAsmTag(function.getBasicBlock(p.b))));
        }
    }

    private void setVregLifeTime(Integer index, int t) {
        if (!lifeTime.containsKey(index)) {
            lifeTime.put(index, new Pair<>(t, t));
        }
        var x = lifeTime.get(index);
        lifeTime.put(index, new Pair<>(min(x.a, t), max(x.b, t)));
    }

    public void refreshAllVreg(List<AsmInstruction> instructionList) {
        refreshBlockEndVreg(instructionList);
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
