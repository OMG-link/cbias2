package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RegisterControl {
    private final AsmFunction function;
    //每个虚拟寄存器的值当前存储的位置
    Map<Integer, AsmOperand> vregLocation = new HashMap<>();
    //每个寄存器当前存储着哪个虚拟寄存器的内容
    Map<Register, Integer> registerPool = new HashMap<>();
    Map<Register, Integer> registerLevel = new HashMap<>();
    static final int LevelMax = 255;
    //在栈中存储用于临时存储的寄存器内容
    StackPool stackPool;
    List<AsmInstruction> newInstructionList;

    public RegisterControl(AsmFunction function, List<AsmInstruction> newInstructionList, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
        this.newInstructionList = newInstructionList;
        //加入目前可使用的六个寄存器
        for (int i = 0; i <= 31; i++) {
            if ((6 <= i && i <= 7) || (28 <= i)) {
                Register register = new IntRegister(i);
                registerPool.put(register, 0);
                registerLevel.put(register, 0);
                Register fregister = new FloatRegister(i);
                registerPool.put(fregister, 0);
                registerLevel.put(fregister, 0);
            }
        }
    }

    boolean isStoredInRegister(Integer index) {
        if (!vregLocation.containsKey(index)) {
            return false;
        }
        AsmOperand container = vregLocation.get(index);
        return container instanceof Register;
    }

    Register getStoredRegister(Integer index) {
        return (Register) (vregLocation.get(index));
    }

    void setLevel(Register register, int level) {
        registerLevel.put(register, level);
    }

    void freeRegister(Register register) {
        Integer index = registerPool.get(register);
        if (index != 0) {
            StackVar tmp = stackPool.pop();
            newInstructionList.add(new AsmStore(register, tmp));
            vregLocation.put(index, tmp);
            registerPool.put(register, 0);
        }
    }

    Register getRegister(Register.RTYPE rtype) {
        Register ret = null;
        Integer minLevel = LevelMax;
        for (var register : registerPool.keySet()) {
            if (register.getType() == rtype && registerLevel.get(register) < minLevel) {
                minLevel = registerLevel.get(register);
                ret = register;
            }
        }
        if (ret != null) {
            freeRegister(ret);
        }
        return ret;
    }

    public void resetLevel() {
        for (var register : registerLevel.keySet()) {
            if (registerPool.get(register) == 0) {
                registerLevel.put(register, 0);
            } else {
                registerLevel.put(register, 1);
            }
        }
    }

    public void setVregLevelMax(Integer index) {
        if (isStoredInRegister(index)) {
            setLevel(getStoredRegister(index), LevelMax);
        }
    }

    public Register allocateRegister(Register virtualRegister) {
        Register register = null;
        int index = virtualRegister.getIndex();
        if (!vregLocation.containsKey(index)) {
            register = getRegister(virtualRegister.getType());
        } else {
            var container = vregLocation.get(index);
            if (container instanceof Register rtmp) {
                register = rtmp;
            } else if (container instanceof StackVar) {
                register = getRegister(virtualRegister.getType());
                if (register == null) {
                    throw new RuntimeException("register not enough!");
                }
                newInstructionList.add(new AsmLoad(register, container));
                stackPool.push((StackVar) container);
            }
        }
        if (register != null) {
            setLevel(register, LevelMax);
            vregLocation.put(index, register);
            registerPool.put(register, index);
        } else {
            throw new RuntimeException("register allocate null");
        }
        return register;
    }

    public void recycle(Integer index) {
        var container = vregLocation.get(index);
        if (container instanceof Register register) {
            registerPool.put(register, 0);
        } else if (container instanceof StackVar stackVar) {
            stackPool.push(stackVar);
        }
        vregLocation.remove(index);
    }

    public void call(AsmInstruction instruction) {
        List<Pair<Register, StackVar>> pushList = new ArrayList<>();
        for (var register : registerPool.keySet()) {
            Integer index = registerPool.get(register);
            if (index != 0) {
                StackVar tmp = stackPool.pop();
                newInstructionList.add(new AsmStore(register, tmp));
                pushList.add(new Pair<>(register, tmp));
            }
        }
        newInstructionList.add(instruction);
        for (var p : pushList) {
            newInstructionList.add(new AsmLoad(p.a, p.b));
            stackPool.push(p.b);
        }
    }
}
