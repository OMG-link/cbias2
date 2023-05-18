package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.AllocateInst;
import cn.edu.bit.newnewcc.ir.value.instruction.LoadInst;
import cn.edu.bit.newnewcc.ir.value.instruction.ReturnInst;
import cn.edu.bit.newnewcc.ir.value.instruction.StoreInst;


public class AsmBasicBlock {
    private final GlobalTag blockTag;
    private final AsmFunction function;
    private final BasicBlock irBlock;

    public AsmBasicBlock(AsmFunction function, BasicBlock block) {
        this.function = function;
        this.blockTag = new GlobalTag(function.getFunctionName() + "_" + block.getValueName(), false);
        this.irBlock = block;
    }

    void emitToFunction() {
        function.appendInstruction(new AsmTag(blockTag));
        for (Instruction instruction : irBlock.getInstructions()) {
            translate(instruction);
        }
    }

    AsmOperand getValue(Value value) {
        if (value instanceof GlobalVariable globalVariable) {
            AsmGlobalVariable asmGlobalVariable = function.getGlobalCode().getGlobalVariable(globalVariable);
            IntRegister reg = function.getIntRegisterAllocator().allocate();
            function.appendInstruction(new AsmLoad(reg, asmGlobalVariable.emitTag(true)));
            function.appendInstruction(new AsmAdd(reg, reg, asmGlobalVariable.emitTag(false)));
            return new Address(0, reg);
        } else if (value instanceof Function.FormalParameter formalParameter) {
            return function.getParameterByFormal(formalParameter);
        } else if (value instanceof Instruction instruction) {
            if (function.getIntRegisterAllocator().contain(instruction)) {
                return function.getIntRegisterAllocator().get(instruction);
            } else if (function.getFloatRegisterAllocator().contain(instruction)) {
                return function.getFloatRegisterAllocator().get(instruction);
            } else if (function.getStackAllocator().contain(instruction)) {
                return function.getStackAllocator().get(instruction);
            } else {
                throw new RuntimeException("Value not found : " + value.getValueNameIR());
            }
        } else if (value instanceof ConstInt constInt) {
            return new Immediate(constInt.getValue());
        }
        //浮点立即数留待补充
        throw new RuntimeException("Value type not found : " + value.getValueNameIR());
    }


    void translate(Instruction instruction) {
        if (instruction instanceof ReturnInst returnInst) {
            var ret = returnInst.getReturnValue();
            if (ret instanceof ConstInt retInt) {
                function.appendInstruction(new AsmLoad(new IntRegister("a0"), new Immediate(retInt.getValue())));
                function.appendInstruction(new AsmJump(function.getRetBlockTag(), AsmJump.JUMPTYPE.NON, null, null));
            }
        } else if (instruction instanceof AllocateInst allocateInst) {
            var sz = allocateInst.getAllocatedType().getSize();
            function.getStackAllocator().allocate(instruction, Math.toIntExact(sz));
        } else if (instruction instanceof StoreInst storeInst) {
            var address = getValue(storeInst.getAddressOperand());
            var source = getValue(storeInst.getValueOperand());
            if (source instanceof Register register) {
                function.appendInstruction(new AsmStore(register, address));
            } else {
                Register register;
                if (storeInst.getValueOperand().getType() instanceof FloatType) {
                    register = function.getFloatRegisterAllocator().allocate();
                } else {
                    register = function.getIntRegisterAllocator().allocate();
                }
                function.appendInstruction(new AsmLoad(register, source));
                function.appendInstruction(new AsmStore(register, address));
            }
        }
    }
}
