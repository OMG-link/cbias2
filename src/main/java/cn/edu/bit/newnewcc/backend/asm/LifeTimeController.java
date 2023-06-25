package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
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

    public void refreshBlockEndVreg() {
        while (!lifeToBlockEnd.empty()) {
            var x = lifeToBlockEnd.pop();
            setVregLifeTime(x.a, function.getBasicBlock(x.b).getBlockEnd());
        }
    }

    public void setVregLifeTime(Integer index, int t) {
        if (!lifeTime.containsKey(index)) {
            lifeTime.put(index, new Pair<>(t, t));
        }
        var x = lifeTime.get(index);
        lifeTime.put(index, new Pair<>(min(x.a, t), max(x.b, t)));
    }

    public void refreshAllVreg(List<AsmInstruction> instructionList) {
        for (int i = 0; i < instructionList.size(); i++) {
            AsmInstruction instruction = instructionList.get(i);
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
