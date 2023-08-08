package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmCall;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

/**
 * 线性扫描寄存器分配器
 */
public class LinearScanRegisterControl extends RegisterControl{
    protected final IntRegister s1 = IntRegister.S1;
    protected final StackVar s1saved;
    //每个虚拟寄存器的值当前存储的位置
    private final Map<Integer, AsmOperand> vRegLocation = new HashMap<>();
    //每个寄存器当前存储着哪个虚拟寄存器的内容
    private final Map<Register, Integer> registerPool = new HashMap<>();

    public LinearScanRegisterControl(AsmFunction function, StackAllocator allocator) {
        super(function, allocator);
        s1saved = stackPool.pop();
        //加入目前可使用的寄存器
        for (var reg : Registers.USABLE_REGISTERS) {
            registerPool.put(reg, 0);
        }
    }

    private void allocateVReg(Register vReg) {
        var index = vReg.getAbsoluteIndex();
        for (var reg : registerPool.keySet()) {
            if (reg.getClass() == vReg.getClass() && registerPool.get(reg) == 0) {
                registerPool.put(reg, vReg.getAbsoluteIndex());
                vRegLocation.put(vReg.getAbsoluteIndex(), reg);
                updateRegisterPreserve(reg, stackPool.pop());
                return;
            }
        }
        Register spillReg = null;
        int maxR = function.getLifeTimeController().getLifeTimeRange(index).b.getInstID();
        for (var reg : registerPool.keySet()) {
            if (reg.getClass() == vReg.getClass()) {
                int original = registerPool.get(reg);
                int regR = function.getLifeTimeController().getLifeTimeRange(original).b.getInstID();
                if (regR > maxR) {
                    maxR = regR;
                    spillReg = reg;
                }
            }
        }
        if (spillReg != null) {
            int original = registerPool.get(spillReg);
            vRegLocation.remove(original);
            registerPool.put(spillReg, index);
            vRegLocation.put(index, spillReg);
        }
    }

    /**
     * 进行两次线性扫描，第一次分配寄存器并进行spill操作，第二次为spill操作后的寄存器分配栈空间
     */
    public void virtualRegAllocateToPhysics() {
        List<Register> vRegList = new ArrayList<>();
        List<Pair<Integer, Integer>> recycleList = new ArrayList<>();
        for (var index : function.getLifeTimeController().getVRegKeySet()) {
            vRegList.add(function.getRegisterAllocator().get(index));
            recycleList.add(new Pair<>(index, function.getLifeTimeController().getLifeTimeRange(index).b.getInstID()));
        }
        vRegList.sort((a, b) -> {
            var lifeTimeA = function.getLifeTimeController().getLifeTimeRange(a.getAbsoluteIndex());
            var lifeTimeB = function.getLifeTimeController().getLifeTimeRange(b.getAbsoluteIndex());
            return lifeTimeA.compareTo(lifeTimeB);
        });
        recycleList.sort(Comparator.comparingInt(a -> a.b));

        //第一次扫描，分配寄存器本身
        int recycleHead = 0;
        for (var vReg : vRegList) {
            var lifeTime = function.getLifeTimeController().getLifeTimeRange(vReg.getAbsoluteIndex());
            while (recycleHead < recycleList.size() && recycleList.get(recycleHead).b <= lifeTime.a.getInstID()) {
                recycle(recycleList.get(recycleHead).a);
                recycleHead += 1;
            }
            allocateVReg(vReg);
        }

        //第二次扫描，分配被spill的寄存器的栈空间
        recycleHead = 0;
        for (var vReg : vRegList) {
            if (vRegLocation.containsKey(vReg.getAbsoluteIndex())) {
                continue;
            }
            var lifeTime = function.getLifeTimeController().getLifeTimeRange(vReg.getAbsoluteIndex());
            while (recycleHead < recycleList.size() && recycleList.get(recycleHead).b <= lifeTime.a.getInstID()) {
                recycle(recycleList.get(recycleHead).a);
                recycleHead += 1;
            }
            vRegLocation.put(vReg.getAbsoluteIndex(), stackPool.pop());
        }
        stackPool.clear();
    }

    private Map<Register, Register> getUsedRegisters(AsmInstruction inst) {
        Map<Register, Register> used = new HashMap<>();
        for (int j = 1; j <= 3; j++) {
            if (inst.getOperand(j) instanceof RegisterReplaceable registerReplaceable) {
                Register vReg = registerReplaceable.getRegister();
                if (vReg.isVirtual()) {
                    if (vRegLocation.get(vReg.getAbsoluteIndex()) instanceof Register register) {
                        used.put(vReg, register);
                    }
                }
            }
        }
        return used;
    }

    private Register getExRegister(Map<Register, Register> used, Register vReg, Map<Register, StackVar> registerSaveMap, List<AsmInstruction> newInstList) {
        if (used.containsKey(vReg)) {
            return used.get(vReg);
        }
        for (var reg : registerPool.keySet()) {
            if (reg.getClass() == vReg.getClass() && !used.containsValue(reg) && registerPool.get(reg) == 0) {
                used.put(vReg, reg);
                return reg;
            }
        }
        for (var reg : registerPool.keySet()) {
            if (reg.getClass() == vReg.getClass() && !used.containsValue(reg)) {
                used.put(vReg, reg);
                var tmp = stackPool.pop();
                registerSaveMap.put(reg, tmp);
                if (saveToStackVar(newInstList, reg, tmp, s1)) {
                    updateRegisterPreserve(s1, s1saved);
                }
                return reg;
            }
        }
        return null;
    }

    public List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList) {
        registerPool.replaceAll((r, v) -> 0);
        List<AsmInstruction> newInstList = new ArrayList<>();
        List<List<Pair<Integer, Integer>>> callSavedRegisters = new ArrayList<>();
        for (int i = 0; i <= instructionList.size() + 1; i++) {
            callSavedRegisters.add(new ArrayList<>());
        }
        for (var index : function.getLifeTimeController().getVRegKeySet()) {
            if (vRegLocation.get(index) instanceof Register) {
                var lifeTime = function.getLifeTimeController().getLifeTimeRange(index);
                callSavedRegisters.get(lifeTime.a.getInstID()).add(new Pair<>(index, 1));
                callSavedRegisters.get(lifeTime.b.getInstID() + 1).add(new Pair<>(index, -1));
            }
        }
        for (int i = 0; i < instructionList.size(); i++) {
            for (var p : callSavedRegisters.get(i)) {
                Register reg = (Register) vRegLocation.get(p.a);
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

            var writeId = AsmInstructions.getWriteVRegId(inst);
            var vRegId = AsmInstructions.getVRegId(inst);
            for (int j : vRegId) {
                RegisterReplaceable registerReplaceable = (RegisterReplaceable) inst.getOperand(j);
                var vReg = registerReplaceable.getRegister();
                Register physicRegister = getExRegister(used, vReg, registerSaveMap, newInstList);
                if (vRegLocation.get(vReg.getAbsoluteIndex()) instanceof StackVar stackVar) {
                    if (writeId.contains(j)) {
                        writeReg = physicRegister;
                        writeStack = stackVar;
                    } else if (!loaded.contains(physicRegister)) {
                        loaded.add(physicRegister);
                        if (loadFromStackVar(newInstList, physicRegister, stackVar, s1)) {
                            updateRegisterPreserve(s1, s1saved);
                        }
                    }
                }
                inst.setOperand(j, registerReplaceable.replaceRegister(physicRegister));
            }

            if (inst instanceof AsmCall) {
                Map<Register, StackVar> callSaved = new HashMap<>();
                for (var reg : registerPool.keySet()) {
                    if (registerPool.get(reg) != 0 && !Registers.isPreservedAcrossCalls(reg)) {
                        var tmp = stackPool.pop();
                        callSaved.put(reg, tmp);
                        if (saveToStackVar(newInstList, reg, tmp, s1)) {
                            updateRegisterPreserve(s1, s1saved);
                        }
                    }
                }
                newInstList.add(inst);
                for (var reg : callSaved.keySet()) {
                    var tmp = callSaved.get(reg);
                    if (loadFromStackVar(newInstList, reg, tmp, s1)) {
                        updateRegisterPreserve(s1, s1saved);
                    }
                    stackPool.push(tmp);
                }
            } else {
                newInstList.add(inst);
            }
            if (writeStack != null) {
                if (saveToStackVar(newInstList, writeReg, writeStack, s1)) {
                    updateRegisterPreserve(s1, s1saved);
                }
            }
            for (var reg : registerSaveMap.keySet()) {
                var tmp = registerSaveMap.get(reg);
                if (loadFromStackVar(newInstList, reg, tmp, s1)) {
                    updateRegisterPreserve(s1, s1saved);
                }
                stackPool.push(tmp);
            }
        }
        return newInstList;
    }

    private void recycle(Integer index) {
        if (!vRegLocation.containsKey(index)) {
            return;
        }
        var container = vRegLocation.get(index);
        if (container instanceof Register register) {
            registerPool.put(register, 0);
        } else if (container instanceof StackVar stackVar) {
            stackPool.push(stackVar);
        }
    }

    @Override
    public List<AsmInstruction> work(List<AsmInstruction> instructionList) {
        virtualRegAllocateToPhysics();
        return spillRegisters(instructionList);
    }
}
