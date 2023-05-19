package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.util.ArrayList;
import java.util.List;


/**
 * 汇编基本块
 */
public class AsmBasicBlock {
    private final GlobalTag blockTag;
    private final AsmFunction function;
    private final BasicBlock irBlock;

    public AsmBasicBlock(AsmFunction function, BasicBlock block) {
        this.function = function;
        this.blockTag = new GlobalTag(function.getFunctionName() + "_" + block.getValueName(), false);
        this.irBlock = block;
    }

    /**
     * 生成基本块的汇编代码，向函数中输出指令
     */
    void emitToFunction() {
        function.appendInstruction(new AsmTag(blockTag));
        for (Instruction instruction : irBlock.getInstructions()) {
            translate(instruction);
        }
    }

    /**
     * 获取一个ir value在汇编中对应的值（函数参数、寄存器、栈上变量、全局变量）
     * @param value ir中的value
     * @return 对应的汇编操作数
     */
    AsmOperand getValue(Value value) {
        if (value instanceof GlobalVariable globalVariable) {
            AsmGlobalVariable asmGlobalVariable = function.getGlobalCode().getGlobalVariable(globalVariable);
            IntRegister reg = function.getRegisterAllocator().allocateInt();
            function.appendInstruction(new AsmLoad(reg, asmGlobalVariable.emitTag(true)));
            function.appendInstruction(new AsmAdd(reg, reg, asmGlobalVariable.emitTag(false)));
            return new Address(0, reg);
        } else if (value instanceof Function.FormalParameter formalParameter) {
            return function.getParameterByFormal(formalParameter);
        } else if (value instanceof Instruction instruction) {
            if (function.getRegisterAllocator().contain(instruction)) {
                return function.getRegisterAllocator().get(instruction);
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

    void translateBinaryInstruction(BinaryInstruction binaryInstruction) {
        if (binaryInstruction instanceof IntegerAddInst integerAddInst) {
            var addx = getValue(integerAddInst.getOperand1());
            var addy = getValue(integerAddInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerAddInst);
            if (addx instanceof IntRegister addrx) {
                function.appendInstruction(new AsmAdd(register, addrx, addy));
            } else {
                throw new RuntimeException("Error: addInst operand1 is not an int register");
            }
        } else if (binaryInstruction instanceof IntegerSubInst integerSubInst) {
            var subx = getValue(integerSubInst.getOperand1());
            var suby = getValue(integerSubInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSubInst);
            if (subx instanceof IntRegister subrx) {
                function.appendInstruction(new AsmSub(register, subrx, suby));
            } else {
                throw new RuntimeException("Error: subInst operand1 is not an int register");
            }
        }
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
                    register = function.getRegisterAllocator().allocateFloat();
                } else {
                    register = function.getRegisterAllocator().allocateInt();
                }
                function.appendInstruction(new AsmLoad(register, source));
                function.appendInstruction(new AsmStore(register, address));
            }
        } else if (instruction instanceof LoadInst loadInst) {
            var address = getValue(loadInst.getAddressOperand());
            Register register = function.getRegisterAllocator().allocate(loadInst);
            function.appendInstruction(new AsmLoad(register, address));
        } else if (instruction instanceof BinaryInstruction binaryInstruction) {
            translateBinaryInstruction(binaryInstruction);
        } else if (instruction instanceof CallInst callInst) {
            BaseFunction baseFunction = callInst.getCallee();
            AsmFunction asmFunction = function.getGlobalCode().getFunction(baseFunction);
            List<AsmOperand> parameters = new ArrayList<>();
            for (int i = 0; i < asmFunction.getParameterSize(); i++) {
                parameters.add(getValue(callInst.getArgumentAt(i)));
            }
            function.appendAllInstruction(function.call(asmFunction, parameters));
            if (function.getReturnRegister() != null) {
                Register register = function.getRegisterAllocator().allocate(callInst);
                function.appendInstruction(new AsmLoad(register, function.getReturnRegister()));
            }
        }
    }
}
