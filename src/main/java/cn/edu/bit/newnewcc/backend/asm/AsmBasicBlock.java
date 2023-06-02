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
        } else if (binaryInstruction instanceof IntegerMultiplyInst integerMultiplyInst) {
            var mulx = getValue(integerMultiplyInst.getOperand1());
            var muly = getValue(integerMultiplyInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerMultiplyInst);
            if (mulx instanceof IntRegister mulrx) {
                if (muly instanceof IntRegister mulry) {
                    function.appendInstruction(new AsmMul(register, mulrx, mulry));
                } else {
                    IntRegister tmpry = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmLoad(tmpry, muly));
                    function.appendInstruction(new AsmMul(register, mulrx, tmpry));
                }
            } else {
                throw new RuntimeException("Error: multiplyInst operand1 is not an int register");
            }
        } else if (binaryInstruction instanceof IntegerSignedDivideInst integerSignedDivideInst) {
            var divx = getValue(integerSignedDivideInst.getOperand1());
            var divy = getValue(integerSignedDivideInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
            if (divx instanceof IntRegister divrx) {
                if (divy instanceof IntRegister divry) {
                    function.appendInstruction(new AsmSignedIntegerDivide(register, divrx, divry));
                } else {
                    IntRegister tmpry = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmLoad(tmpry, divy));
                    function.appendInstruction(new AsmSignedIntegerDivide(register, divrx, tmpry));
                }
            } else {
                throw new RuntimeException("Error: divideInst operand1 is not an int register");
            }
        } else if (binaryInstruction instanceof IntegerSignedRemainderInst integerSignedRemainderInst) {
            var divx = getValue(integerSignedRemainderInst.getOperand1());
            var divy = getValue(integerSignedRemainderInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSignedRemainderInst);
            if (divx instanceof IntRegister divrx) {
                if (divy instanceof IntRegister divry) {
                    function.appendInstruction(new AsmSignedIntegerRemainder(register, divrx, divry));
                } else {
                    IntRegister tmpry = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmLoad(tmpry, divy));
                    function.appendInstruction(new AsmSignedIntegerRemainder(register, divrx, tmpry));
                }
            } else {
                throw new RuntimeException("Error: remainderInst operand1 is not an int register");
            }
        } else if (binaryInstruction instanceof CompareInst compareInst) {
            translateCompareInst(compareInst);
        }
    }

    IntRegister getOperandToIntRegister(AsmOperand operand) {
        if (operand instanceof IntRegister intRegister) {
            return intRegister;
        }
        IntRegister res = function.getRegisterAllocator().allocateInt();
        function.appendInstruction(new AsmLoad(res, operand));
        return res;
    }

    void translateCompareInst(CompareInst compareInst) {
        if (compareInst instanceof IntegerCompareInst integerCompareInst) {
            var rop1 = getOperandToIntRegister(getValue(integerCompareInst.getOperand1()));
            var op2 = getValue(integerCompareInst.getOperand2());
            var result = function.getRegisterAllocator().allocateInt(integerCompareInst);
            IntRegister rop2, tmp;
            switch (integerCompareInst.getCondition()) {
                case EQ -> {
                    tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmSub(tmp, rop1, op2));
                    function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
                }
                case NE -> {
                    tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmSub(tmp, rop1, op2));
                    function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SNEZ));
                }
                case SLT -> function.appendInstruction(new AsmIntegerCompare(result, rop1, op2, AsmIntegerCompare.Condition.SLT));
                case SLE -> {
                    tmp = function.getRegisterAllocator().allocateInt();
                    rop2 = getOperandToIntRegister(op2);
                    function.appendInstruction(new AsmIntegerCompare(tmp, rop2, rop1, AsmIntegerCompare.Condition.SLT));
                    function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
                }
                case SGT -> {
                    rop2 = getOperandToIntRegister(op2);
                    function.appendInstruction(new AsmIntegerCompare(result, rop2, rop1, AsmIntegerCompare.Condition.SLT));
                }
                case SGE -> {
                    tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmIntegerCompare(tmp, rop1, op2, AsmIntegerCompare.Condition.SLT));
                    function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
                }
            }
        }
    }

    void translate(Instruction instruction) {
        if (instruction instanceof ReturnInst returnInst) {
            var ret = getValue(returnInst.getReturnValue());
            function.appendInstruction(new AsmLoad(new IntRegister("a0"), ret));
            function.appendInstruction(new AsmJump(function.getRetBlockTag(), AsmJump.JUMPTYPE.NON, null, null));
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
            Register returnRegister = null;
            if (asmFunction.getReturnRegister() != null) {
                returnRegister = function.getRegisterAllocator().allocate(callInst);
            }
            function.appendAllInstruction(function.call(asmFunction, parameters, returnRegister));
        }
    }
}
