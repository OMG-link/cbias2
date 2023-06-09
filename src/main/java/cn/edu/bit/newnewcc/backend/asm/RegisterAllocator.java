package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.value.Instruction;

/**
 * 寄存器分配器，内部包含整数分配和浮点数寄存器分配功能，分配出的寄存器均为虚拟寄存器（下标为负数）
 */
public class RegisterAllocator {
    private final FloatRegisterAllocator floatRegisterAllocator = new FloatRegisterAllocator();
    private final IntRegisterAllocator intRegisterAllocator = new IntRegisterAllocator();
    int total;
    public RegisterAllocator() {
        total = 0;
    }
    public IntRegister allocateInt() {
        return intRegisterAllocator.allocate(--total);
    }
    public FloatRegister allocateFloat() {
        return floatRegisterAllocator.allocate(--total);
    }
    public IntRegister allocateInt(Instruction instruction) {
        return intRegisterAllocator.allocate(instruction, --total);
    }
    public FloatRegister allocateFloat(Instruction instruction) {
        return floatRegisterAllocator.allocate(instruction, --total);
    }
    public Register allocate(Instruction instruction) {
        if (instruction.getType() instanceof FloatType) {
            return allocateFloat(instruction);
        } else {
            return allocateInt(instruction);
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
}
