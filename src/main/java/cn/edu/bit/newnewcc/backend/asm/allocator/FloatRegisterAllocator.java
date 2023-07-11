package cn.edu.bit.newnewcc.backend.asm.allocator;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class FloatRegisterAllocator {
    Map<Instruction, FloatRegister> registerMap;

    public FloatRegisterAllocator() {
        registerMap = new HashMap<>();
    }

    public FloatRegister allocate(Instruction instruction, int index) {
        FloatRegister reg = new FloatRegister(index);
        registerMap.put(instruction, reg);
        return reg;
    }

    public FloatRegister allocate(int index) {
        return new FloatRegister(index);
    }

    public FloatRegister get(Instruction instruction) {
        return registerMap.get(instruction);
    }

    public boolean contain(Instruction instruction) {
        return registerMap.containsKey(instruction);
    }
}