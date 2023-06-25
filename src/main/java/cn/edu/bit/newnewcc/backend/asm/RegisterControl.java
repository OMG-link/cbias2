package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmCall;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

class RegisterControl {
    public enum TYPE {
        PRESERVED, UNPRESERVED
    }
    private final AsmFunction function;
    //每个虚拟寄存器的值当前存储的位置
    private final Map<Integer, AsmOperand> vregLocation = new HashMap<>();
    //每个寄存器当前存储着哪个虚拟寄存器的内容
    private final Map<Register, Integer> registerPool = new HashMap<>();
    //寄存器在调用过程中保留与否，保留的寄存器需要在函数头尾额外保存
    private final Map<Register, TYPE> registerPreservedType = new HashMap<>();
    private final Map<Register, StackVar> preservedRegisterSaved = new HashMap<>();
    //在栈中存储用于临时存储的寄存器内容
    StackPool stackPool;

    public List<AsmInstruction> emitHead() {
        List<AsmInstruction> res = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            res.add(new AsmStore(register, preservedRegisterSaved.get(register)));
        }
        return res;
    }
    public List<AsmInstruction> emitTail() {
        List<AsmInstruction> res = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            res.add(new AsmLoad(register, preservedRegisterSaved.get(register)));
        }
        return res;
    }

    public RegisterControl(AsmFunction function, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
        //加入目前可使用的寄存器
        for (int i = 0; i <= 31; i++) {
            if ((6 <= i && i <= 7) || (28 <= i)) {
                Register register = new IntRegister(i);
                registerPool.put(register, 0);
                registerPreservedType.put(register, TYPE.UNPRESERVED);
            }
            if (i == 9 || (18 <= i && i <= 27)) {
                Register register = new IntRegister(i);
                registerPool.put(register, 0);
                registerPreservedType.put(register, TYPE.PRESERVED);
            }
            if (18 <= i && i <= 27) {
                Register register = new FloatRegister(i);
                registerPool.put(register, 0);
                registerPreservedType.put(register, TYPE.PRESERVED);
            }
            if (i <= 7 || 28 <= i) {
                Register register = new FloatRegister(i);
                registerPool.put(register, 0);
                registerPreservedType.put(register, TYPE.UNPRESERVED);
            }
        }
    }

    void allocateVreg(Register vreg) {
        var index = vreg.getIndex();
        for (var reg : registerPool.keySet()) {
            if (reg.getType() == vreg.getType() && registerPool.get(reg) == 0) {
                registerPool.put(reg, vreg.getIndex());
                vregLocation.put(vreg.getIndex(), reg);
                if (registerPreservedType.get(reg) == TYPE.PRESERVED) {
                    preservedRegisterSaved.put(reg, stackPool.pop());
                }
                return;
            }
        }
        Register spillReg = null;
        int maxR = function.getLifeTimeController().getLifeTime(index).b;
        for (var reg : registerPool.keySet()) {
            if (reg.getType() == vreg.getType()) {
                int original = registerPool.get(reg);
                int regR = function.getLifeTimeController().getLifeTime(original).b;
                if (regR > maxR) {
                    maxR = regR;
                    spillReg = reg;
                }
            }
        }
        if (spillReg != null) {
            int original = registerPool.get(spillReg);
            vregLocation.put(original, stackPool.pop());
            registerPool.put(spillReg, index);
            vregLocation.put(index, spillReg);
            return;
        }
        vregLocation.put(index, stackPool.pop());
    }

    void linearScanRegAllocate(List<AsmInstruction> instructionList) {
        List<Register> vregList = new ArrayList<>();
        List<Pair<Integer, Integer>> recycleList = new ArrayList<>();
        for (var index : function.getLifeTimeController().getKeySet()) {
            vregList.add(function.getRegisterAllocator().get(index));
            recycleList.add(new Pair<>(index, function.getLifeTimeController().getLifeTime(index).b));
        }
        vregList.sort((a, b) -> {
            var lifeTimeA = function.getLifeTimeController().getLifeTime(a.getIndex());
            var lifeTimeB = function.getLifeTimeController().getLifeTime(b.getIndex());
            if (Objects.equals(lifeTimeA.a, lifeTimeB.a)) {
                return lifeTimeA.b - lifeTimeB.b;
            }
            return lifeTimeA.a - lifeTimeB.a;
        });
        recycleList.sort(Comparator.comparingInt(a -> a.b));
        int recycleHead = 0;
        for (var vreg : vregList) {
            var lifeTime = function.getLifeTimeController().getLifeTime(vreg.getIndex());
            while (recycleHead < recycleList.size() && recycleList.get(recycleHead).b < lifeTime.a) {
                recycle(recycleList.get(recycleHead).a);
                recycleHead += 1;
            }
            allocateVreg(vreg);
        }
    }

    Set<Register> getUsedRegisters(AsmInstruction inst) {
        Set<Register> used = new HashSet<>();
        for (int j = 1; j <= 3; j++) {
            if (inst.getOperand(j) instanceof RegisterReplaceable registerReplaceable) {
                Register vreg = registerReplaceable.getRegister();
                if (vreg.isVirtual()) {
                    if (vregLocation.get(vreg.getIndex()) instanceof Register register) {
                        used.add(register);
                    }
                }
            }
        }
        return used;
    }

    Register getExRegister(Set<Register> used, Register vreg) {
        for (var reg : registerPool.keySet()) {
            if (reg.getType() == vreg.getType() && !used.contains(reg)) {
                used.add(reg);
                return reg;
            }
        }
        return null;
    }

    List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList) {
        List<AsmInstruction> newInstList = new ArrayList<>();
        for (var inst : instructionList) {
            Map<Register, StackVar> registerSaveMap = new HashMap<>();
            var used = getUsedRegisters(inst);
            for (int j = 1; j <= 3; j++) {
                if (inst.getOperand(j) instanceof RegisterReplaceable registerReplaceable) {
                    var vreg = registerReplaceable.getRegister();
                    if (vreg.isVirtual()) {
                        Register physicRegister = null;
                        if (vregLocation.get(vreg.getIndex()) instanceof StackVar stackVar) {
                            physicRegister = getExRegister(used, vreg);
                            var tmp = stackPool.pop();
                            registerSaveMap.put(physicRegister, tmp);
                            newInstList.add(new AsmStore(physicRegister, tmp));
                            newInstList.add(new AsmLoad(physicRegister, stackVar));
                        } else if (vregLocation.get(vreg.getIndex()) instanceof Register register) {
                            physicRegister = register;
                        }
                        inst.replaceOperand(j, registerReplaceable.replaceRegister(physicRegister));
                    }
                }
            }
            if (inst instanceof AsmCall) {
                Map<Register, StackVar> callSaved = new HashMap<>();
                for (var reg : registerPool.keySet()) {
                    if (registerPool.get(reg) != 0 && registerPreservedType.get(reg) != TYPE.PRESERVED) {
                        var tmp = stackPool.pop();
                        callSaved.put(reg, tmp);
                        newInstList.add(new AsmStore(reg, tmp));
                    }
                }
                newInstList.add(inst);
                for (var reg : callSaved.keySet()) {
                    var tmp = callSaved.get(reg);
                    newInstList.add(new AsmLoad(reg, tmp));
                    stackPool.push(tmp);
                }
            } else {
                newInstList.add(inst);
            }
            for (var reg : registerSaveMap.keySet()) {
                var tmp = registerSaveMap.get(reg);
                newInstList.add(new AsmLoad(reg, tmp));
                stackPool.push(tmp);
            }
        }
        return newInstList;
    }

    void recycle(Integer index) {
        var container = vregLocation.get(index);
        if (container instanceof Register register) {
            registerPool.put(register, 0);
        } else if (container instanceof StackVar stackVar) {
            stackPool.push(stackVar);
        }
        vregLocation.remove(index);
    }
}
