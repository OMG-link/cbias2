package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmCall;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

/**
 * 线性扫描寄存器分配器
 */
class LinearScanRegisterControl extends RegisterControl{
    public enum TYPE {
        PRESERVED, UNPRESERVED
    }
    //每个虚拟寄存器的值当前存储的位置
    private final Map<Integer, AsmOperand> vregLocation = new HashMap<>();
    //每个寄存器当前存储着哪个虚拟寄存器的内容
    private final Map<Register, Integer> registerPool = new HashMap<>();
    //寄存器在调用过程中保留与否，保留的寄存器需要在函数头尾额外保存
    private final Map<Register, TYPE> registerPreservedType = new HashMap<>();
    private final Map<Register, StackVar> preservedRegisterSaved = new HashMap<>();

    @Override
    public List<AsmInstruction> emitHead() {
        List<AsmInstruction> res = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            res.add(new AsmStore(register, preservedRegisterSaved.get(register)));
        }
        return res;
    }

    @Override
    public List<AsmInstruction> emitTail() {
        List<AsmInstruction> res = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            res.add(new AsmLoad(register, preservedRegisterSaved.get(register)));
        }
        return res;
    }

    public LinearScanRegisterControl(AsmFunction function, StackAllocator allocator) {
        super(function, allocator);
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
                if (!preservedRegisterSaved.containsKey(reg) && registerPreservedType.get(reg) == TYPE.PRESERVED) {
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
            vregLocation.remove(original);
            registerPool.put(spillReg, index);
            vregLocation.put(index, spillReg);
        }
    }

    /**
     * 进行两次线性扫描，第一次分配寄存器并进行spill操作，第二次为spill操作后的寄存器分配栈空间
     */
    @Override
    public void VritualRegAllocateToPhysics() {
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

        //第一次扫描，分配寄存器本身
        int recycleHead = 0;
        for (var vreg : vregList) {
            var lifeTime = function.getLifeTimeController().getLifeTime(vreg.getIndex());
            while (recycleHead < recycleList.size() && recycleList.get(recycleHead).b < lifeTime.a) {
                recycle(recycleList.get(recycleHead).a);
                recycleHead += 1;
            }
            allocateVreg(vreg);
        }

        //第二次扫描，分配被spill的寄存器的栈空间
        recycleHead = 0;
        for (var vreg : vregList) {
            var lifeTime = function.getLifeTimeController().getLifeTime(vreg.getIndex());
            while (recycleHead < recycleList.size() && recycleList.get(recycleHead).b < lifeTime.a) {
                recycle(recycleList.get(recycleHead).a);
                recycleHead += 1;
            }
            vregLocation.put(vreg.getIndex(), stackPool.pop());
        }
        stackPool.clear();
    }

    Map<Register, Register> getUsedRegisters(AsmInstruction inst) {
        Map<Register, Register> used = new HashMap<>();
        for (int j = 1; j <= 3; j++) {
            if (inst.getOperand(j) instanceof RegisterReplaceable registerReplaceable) {
                Register vreg = registerReplaceable.getRegister();
                if (vreg.isVirtual()) {
                    if (vregLocation.get(vreg.getIndex()) instanceof Register register) {
                        used.put(register, register);
                    }
                }
            }
        }
        return used;
    }

    Register getExRegister(Map<Register, Register> used, Register vreg, Map<Register, StackVar> registerSaveMap, List<AsmInstruction> newInstList) {
        if (used.containsKey(vreg)) {
            return used.get(vreg);
        }
        for (var reg : registerPool.keySet()) {
            if (reg.getType() == vreg.getType() && !used.containsValue(reg) && registerPool.get(reg) == 0) {
                used.put(vreg, reg);
                return reg;
            }
        }
        for (var reg : registerPool.keySet()) {
            if (reg.getType() == vreg.getType() && !used.containsValue(reg)) {
                used.put(vreg, reg);
                var tmp = stackPool.pop();
                registerSaveMap.put(reg, tmp);
                newInstList.add(new AsmStore(reg, tmp));
                return reg;
            }
        }
        return null;
    }

    @Override
    public List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList) {
        registerPool.replaceAll((r, v) -> 0);
        List<AsmInstruction> newInstList = new ArrayList<>();
        List<List<Pair<Integer, Integer>>> callSavedRegisters = new ArrayList<>();
        for (int i = 0; i <= instructionList.size(); i++) {
            callSavedRegisters.add(new ArrayList<>());
        }
        for (var index : function.getLifeTimeController().getKeySet()) {
            if (vregLocation.get(index) instanceof Register) {
                var lifeTime = function.getLifeTimeController().getLifeTime(index);
                callSavedRegisters.get(lifeTime.a).add(new Pair<>(index, 1));
                callSavedRegisters.get(lifeTime.b + 1).add(new Pair<>(index, -1));
            }
        }
        for (int i = 0; i < instructionList.size(); i++) {
            for (var p : callSavedRegisters.get(i)) {
                Register reg = (Register) vregLocation.get(p.a);
                if (p.b == 1) {
                    registerPool.put(reg, p.a);
                } else {
                    if (Objects.equals(registerPool.get(reg), p.a)) {
                        registerPool.put(reg, 0);
                    }
                }
            }
            var inst = instructionList.get(i);
            Map<Register, StackVar> registerSaveMap = new HashMap<>();
            Register writeReg = null;
            StackVar writeStack = null;
            var used = getUsedRegisters(inst);
            Set<Register> loaded = new HashSet<>();
            for (int j = 1; j <= 3; j++) {
                if (inst.getOperand(j) instanceof RegisterReplaceable registerReplaceable) {
                    var vreg = registerReplaceable.getRegister();
                    if (vreg.isVirtual()) {
                        Register physicRegister = null;
                        if (vregLocation.get(vreg.getIndex()) instanceof StackVar stackVar) {
                            physicRegister = getExRegister(used, vreg, registerSaveMap, newInstList);
                            if (j == 1 && !(inst instanceof AsmStore)) {
                                writeReg = physicRegister;
                                writeStack = stackVar;
                            } else if (!loaded.contains(physicRegister)) {
                                loaded.add(physicRegister);
                                newInstList.add(new AsmLoad(physicRegister, stackVar));
                            }
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
            if (writeStack != null) {
                newInstList.add(new AsmStore(writeReg, writeStack));
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
        if (!vregLocation.containsKey(index)) {
            return;
        }
        var container = vregLocation.get(index);
        if (container instanceof Register register) {
            registerPool.put(register, 0);
        } else if (container instanceof StackVar stackVar) {
            stackPool.push(stackVar);
        }
    }
}
