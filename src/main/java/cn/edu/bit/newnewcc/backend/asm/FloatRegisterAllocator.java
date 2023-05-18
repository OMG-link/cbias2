package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class FloatRegisterAllocator {
    Map<Instruction, FloatRegister> registerMap;
    int total;

    public FloatRegisterAllocator() {
        total = 0;
        registerMap = new HashMap<>();
    }

    public FloatRegister allocate(Instruction instruction) {
        total -= 1;
        FloatRegister reg = new FloatRegister(total);
        registerMap.put(instruction, reg);
        return reg;
    }

    public FloatRegister allocate() {
        total -= 1;
        return new FloatRegister(total);
    }

    public FloatRegister get(Instruction instruction) {
        return registerMap.get(instruction);
    }

    public boolean contain(Instruction instruction) {
        return registerMap.containsKey(instruction);
    }
}
