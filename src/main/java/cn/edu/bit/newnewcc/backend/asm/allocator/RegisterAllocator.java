package cn.edu.bit.newnewcc.backend.asm.allocator;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

/**
 * 寄存器分配器，内部包含整数分配和浮点数寄存器分配功能，分配出的寄存器均为虚拟寄存器（下标为负数）
 */
public class RegisterAllocator {
    private final FloatRegisterAllocator floatRegisterAllocator = new FloatRegisterAllocator();
    private final IntRegisterAllocator intRegisterAllocator = new IntRegisterAllocator();
    private final Map<Integer, Register> vregMap = new HashMap<>();
    int total;
    public RegisterAllocator() {
        total = 8;
    }
    public IntRegister allocateInt() {
        var reg = intRegisterAllocator.allocate(++total);
        vregMap.put(total, reg);
        return reg;
    }
    public FloatRegister allocateFloat() {
        var reg = floatRegisterAllocator.allocate(++total);
        vregMap.put(total, reg);
        return reg;
    }
    public IntRegister allocateInt(Instruction instruction) {
        var reg = intRegisterAllocator.allocate(instruction, ++total);
        vregMap.put(total, reg);
        return reg;
    }
    public FloatRegister allocateFloat(Instruction instruction) {
        var reg = floatRegisterAllocator.allocate(instruction, ++total);
        vregMap.put(total, reg);
        return reg;
    }
    public Register allocate(Instruction instruction) {
        if (instruction.getType() instanceof FloatType) {
            return allocateFloat(instruction);
        } else {
            return allocateInt(instruction);
        }
    }
    public Register allocate(Register sourceReg) {
        if (sourceReg instanceof IntRegister) {
            return allocateInt();
        } else {
            return allocateFloat();
        }
    }
    public boolean contain(Instruction instruction) {
        return intRegisterAllocator.contain(instruction) || floatRegisterAllocator.contain(instruction);
    }

    public Register get(Instruction instruction) {
        if (intRegisterAllocator.contain(instruction)) {
            return intRegisterAllocator.get(instruction);
        } else if (floatRegisterAllocator.contain(instruction)) {
            return floatRegisterAllocator.get(instruction);
        } else {
            throw new RuntimeException("instruction to register not found");
        }
    }
    public Register get(Integer index) {
        return vregMap.get(index);
    }
}
