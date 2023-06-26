package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.ConstArrayTools;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;
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
    int blockStart, blockEnd;

    public AsmBasicBlock(AsmFunction function, BasicBlock block) {
        this.function = function;
        this.blockTag = new GlobalTag(function.getFunctionName() + "_" + block.getValueName(), false);
        this.irBlock = block;
    }

    /**
     * 生成基本块的汇编代码，向函数中输出指令
     */
    void emitToFunction() {
        blockStart = function.getInstructionListSize();
        translatePhiInstructions(irBlock.getInstructions());
        function.appendInstruction(new AsmTag(blockTag));
        for (Instruction instruction : irBlock.getInstructions()) {
            translate(instruction);
        }
        blockEnd = function.getInstructionListSize() - 1;
    }

    public int getBlockEnd() {
        return blockEnd;
    }
    public int getBlockStart() {
        return blockStart;
    }

    /**
     * 获取一个ir value在汇编中对应的值（函数参数、寄存器、栈上变量、全局变量、地址）
     * @param value ir中的value
     * @return 对应的汇编操作数
     */
    AsmOperand getValue(Value value) {
        if (value instanceof GlobalVariable globalVariable) {
            AsmGlobalVariable asmGlobalVariable = function.getGlobalCode().getGlobalVariable(globalVariable);
            IntRegister reg = function.getRegisterAllocator().allocateInt();
            function.appendInstruction(new AsmLoad(reg, asmGlobalVariable.emitNoSegmentTag()));
            return new AddressContent(0, reg);
        } else if (value instanceof Function.FormalParameter formalParameter) {
            return function.getParameterByFormal(formalParameter);
        } else if (value instanceof Instruction instruction) {
            if (function.getRegisterAllocator().contain(instruction)) {
                return function.getRegisterAllocator().get(instruction);
            } else if (function.getStackAllocator().contain(instruction)) {
                return function.transformStackVar(function.getStackAllocator().get(instruction));
            } else if (function.getAddressAllocator().contain(instruction)) {
                return function.getAddressAllocator().get(instruction);
            } else {
                throw new RuntimeException("Value not found : " + value.getValueNameIR());
            }
        } else if (value instanceof ConstInt constInt) {
            var intValue = constInt.getValue();
            if (ImmediateTools.bitlengthNotInLimit(intValue)) {
                IntRegister tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmLoad(tmp, new Immediate(intValue)));
                return tmp;
            }
            return new Immediate(constInt.getValue());
        } else if (value instanceof ConstFloat constFloat) {
            return function.transConstFloat(constFloat.getValue());
        }
        //浮点立即数留待补充
        throw new RuntimeException("Value type not found : " + value.getValueNameIR());
    }

    AsmOperand getValueByType(Value value, Type type) {
        var result = getValue(value);
        if (type instanceof PointerType && result instanceof Address address) {
            return getAddressToIntRegister(address.getAddressTag());
        } else {
            return result;
        }
    }

    IntRegister getAddressToIntRegister(Address addressTag) {
        if (addressTag.getOffset() == 0) {
            return addressTag.getRegister();
        } else {
            int offset = Math.toIntExact(addressTag.getOffset());
            var tmp = function.getRegisterAllocator().allocateInt();
            if (ImmediateTools.bitlengthNotInLimit(offset)) {
                function.appendInstruction(new AsmLoad(tmp, new Immediate(offset)));
                function.appendInstruction(new AsmAdd(tmp, tmp, addressTag.getRegister()));
            } else {
                function.appendInstruction(new AsmAdd(tmp, addressTag.getRegister(), new Immediate(offset)));
            }
            return tmp;
        }
    }

    IntRegister getOperandToIntRegister(AsmOperand operand) {
        if (operand instanceof IntRegister intRegister) {
            return intRegister;
        } else if (operand instanceof AddressTag addressTag) {
            return getAddressToIntRegister(addressTag);
        }
        IntRegister res = function.getRegisterAllocator().allocateInt();
        function.appendInstruction(new AsmLoad(res, operand));
        return res;
    }

    FloatRegister getOperandToFloatRegister(AsmOperand operand) {
        if (operand instanceof FloatRegister floatRegister) {
            return floatRegister;
        } else {
            FloatRegister tmp = function.getRegisterAllocator().allocateFloat();
            function.appendInstruction(new AsmLoad(tmp, operand));
            return tmp;
        }
    }

    AddressTag transformStackVarToAddressTag(StackVar stackVar) {
        IntRegister tmp = function.getRegisterAllocator().allocateInt();
        Address now = stackVar.getAddress();
        function.appendInstruction(new AsmLoad(tmp, ExStackVarOffset.transform(stackVar, now.getOffset())));
        function.appendInstruction(new AsmAdd(tmp, tmp, now.getRegister()));
        return now.replaceBaseRegister(tmp).setOffset(0).getAddressTag();
    }

    AddressTag getOperandToAddressTag(AsmOperand operand) {
        if (operand instanceof Address address) {
            var offset = address.getOffset();
            if (ImmediateTools.bitlengthNotInLimit(offset)) {
                var tmp = getAddressToIntRegister(address);
                return new AddressTag(0, tmp);
            } else {
                return address.getAddressTag();
            }
        } else if (operand instanceof StackVar stackVar) {
            if (stackVar instanceof ExStackVarContent) {
                return stackVar.getAddress().getAddressTag();
            }
            return transformStackVarToAddressTag(stackVar);
        } else if (operand instanceof IntRegister intRegister) {
            return new AddressTag(0, intRegister);
        } else {
            throw new RuntimeException("address not found!");
        }
    }

    AddressContent getOperandToAddressContent(AsmOperand operand) {
        return getOperandToAddressTag(operand).getAddressContent();
    }

    void translatePhiInstructions(List<Instruction> instructionList) {
        for (var inst : instructionList) {
            if (!(inst instanceof PhiInst)) {
                break;
            }
            // 待完成
        }
    }

    void translateBinaryInstruction(BinaryInstruction binaryInstruction) {
        if (binaryInstruction instanceof IntegerAddInst integerAddInst) {
            int bitLength = integerAddInst.getType().getBitWidth();
            var addx = getOperandToIntRegister(getValue(integerAddInst.getOperand1()));
            var addy = getValue(integerAddInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerAddInst);
            function.appendInstruction(new AsmAdd(register, addx, addy, bitLength));
        } else if (binaryInstruction instanceof IntegerSubInst integerSubInst) {
            int bitLength = integerSubInst.getType().getBitWidth();
            var subx = getOperandToIntRegister(getValue(integerSubInst.getOperand1()));
            var suby = getValue(integerSubInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSubInst);
            function.appendInstruction(new AsmSub(register, subx, suby, bitLength));
        } else if (binaryInstruction instanceof IntegerMultiplyInst integerMultiplyInst) {
            int bitLength = integerMultiplyInst.getType().getBitWidth();
            var mulx = getOperandToIntRegister(getValue(integerMultiplyInst.getOperand1()));
            var muly = getOperandToIntRegister(getValue(integerMultiplyInst.getOperand2()));
            IntRegister register = function.getRegisterAllocator().allocateInt(integerMultiplyInst);
            function.appendInstruction(new AsmMul(register, mulx, muly, bitLength));
        } else if (binaryInstruction instanceof IntegerSignedDivideInst integerSignedDivideInst) {
            var divx = getOperandToIntRegister(getValue(integerSignedDivideInst.getOperand1()));
            var divy = getOperandToIntRegister(getValue(integerSignedDivideInst.getOperand2()));
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
            function.appendInstruction(new AsmSignedIntegerDivide(register, divx, divy));
        } else if (binaryInstruction instanceof IntegerSignedRemainderInst integerSignedRemainderInst) {
            var divx = getOperandToIntRegister(getValue(integerSignedRemainderInst.getOperand1()));
            var divy = getOperandToIntRegister(getValue(integerSignedRemainderInst.getOperand2()));
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSignedRemainderInst);
            function.appendInstruction(new AsmSignedIntegerRemainder(register, divx, divy));
        } else if (binaryInstruction instanceof CompareInst compareInst) {
            translateCompareInst(compareInst);
        } else if (binaryInstruction instanceof FloatAddInst floatAddInst) {
            var rx = getOperandToFloatRegister(getValue(floatAddInst.getOperand1()));
            var ry = getOperandToFloatRegister(getValue(floatAddInst.getOperand2()));
            FloatRegister result = function.getRegisterAllocator().allocateFloat(floatAddInst);
            function.appendInstruction(new AsmAdd(result, rx, ry));
        } else if (binaryInstruction instanceof FloatSubInst floatSubInst) {
            var rx = getOperandToFloatRegister(getValue(floatSubInst.getOperand1()));
            var ry = getOperandToFloatRegister(getValue(floatSubInst.getOperand2()));
            FloatRegister result = function.getRegisterAllocator().allocateFloat(floatSubInst);
            function.appendInstruction(new AsmSub(result, rx, ry));
        } else if (binaryInstruction instanceof FloatMultiplyInst floatMultiplyInst) {
            var rx = getOperandToFloatRegister(getValue(floatMultiplyInst.getOperand1()));
            var ry = getOperandToFloatRegister(getValue(floatMultiplyInst.getOperand2()));
            FloatRegister result = function.getRegisterAllocator().allocateFloat(floatMultiplyInst);
            function.appendInstruction(new AsmMul(result, rx, ry));
        } else if (binaryInstruction instanceof FloatDivideInst floatDivideInst) {
            var rx = getOperandToFloatRegister(getValue(floatDivideInst.getOperand1()));
            var ry = getOperandToFloatRegister(getValue(floatDivideInst.getOperand2()));
            FloatRegister result = function.getRegisterAllocator().allocateFloat(floatDivideInst);
            function.appendInstruction(new AsmFloatDivide(result, rx, ry));
        }
    }

    void translateCompareInst(CompareInst compareInst) {
        if (compareInst instanceof IntegerCompareInst integerCompareInst) {
            translateIntegerCompareInst(integerCompareInst);
        } else if (compareInst instanceof FloatCompareInst floatCompareInst) {
            translateFloatCompareInst(floatCompareInst);
        }
    }

    void translateIntegerCompareInst(IntegerCompareInst integerCompareInst) {
        var rop1 = getOperandToIntRegister(getValue(integerCompareInst.getOperand1()));
        var rop2 = getOperandToIntRegister(getValue(integerCompareInst.getOperand2()));
        var result = function.getRegisterAllocator().allocateInt(integerCompareInst);
        IntRegister tmp;
        int bitLength = Math.toIntExact(integerCompareInst.getOperand1().getType().getSize() * 8);
        switch (integerCompareInst.getCondition()) {
            case EQ -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmSub(tmp, rop1, rop2, bitLength));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
            }
            case NE -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmSub(tmp, rop1, rop2, bitLength));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SNEZ));
            }
            case SLT -> function.appendInstruction(new AsmIntegerCompare(result, rop1, rop2, AsmIntegerCompare.Condition.SLT));
            case SLE -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmIntegerCompare(tmp, rop2, rop1, AsmIntegerCompare.Condition.SLT));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
            }
            case SGT -> function.appendInstruction(new AsmIntegerCompare(result, rop2, rop1, AsmIntegerCompare.Condition.SLT));
            case SGE -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmIntegerCompare(tmp, rop1, rop2, AsmIntegerCompare.Condition.SLT));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
            }
        }
    }

    void translateFloatCompareInst(FloatCompareInst floatCompareInst) {
        var result = function.getRegisterAllocator().allocateInt(floatCompareInst);
        var op1 = getOperandToFloatRegister(getValue(floatCompareInst.getOperand1()));
        var op2 = getOperandToFloatRegister(getValue(floatCompareInst.getOperand2()));
        IntRegister tmp;
        switch (floatCompareInst.getCondition()) {
            case OLE -> function.appendInstruction(new AsmFloatCompare(result, op1, op2, AsmFloatCompare.Condition.OLE));
            case OEQ -> function.appendInstruction(new AsmFloatCompare(result, op1, op2, AsmFloatCompare.Condition.OEQ));
            case OLT -> function.appendInstruction(new AsmFloatCompare(result, op1, op2, AsmFloatCompare.Condition.OLT));
            case ONE -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmFloatCompare(tmp, op1, op2, AsmFloatCompare.Condition.OEQ));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
            }
            case OGE -> function.appendInstruction(new AsmFloatCompare(result, op2, op1, AsmFloatCompare.Condition.OLE));
            case OGT -> function.appendInstruction(new AsmFloatCompare(result, op2, op1, AsmFloatCompare.Condition.OLT));
        }
    }

    void translateBranchInst(BranchInst branchInst) {
        var condition = getOperandToIntRegister(getValue(branchInst.getCondition()));
        var trueTag = function.getBasicBlock(branchInst.getTrueExit()).blockTag;
        var falseTag = function.getBasicBlock(branchInst.getFalseExit()).blockTag;
        function.appendInstruction(new AsmJump(trueTag, AsmJump.JUMPTYPE.NEZ, condition, null));
        function.appendInstruction(new AsmJump(falseTag, AsmJump.JUMPTYPE.NON, null, null));
    }

    void translateStoreInst(StoreInst storeInst) {
        var address = getValue(storeInst.getAddressOperand());
        var valueOperand = storeInst.getValueOperand();
        if (valueOperand instanceof ConstArray constArray) {
            java.util.function.Function<Integer, Address> getAddress = (Integer tmpInt) -> {
                if (address instanceof Address addressTmp) {
                    return addressTmp;
                } else if (address instanceof StackVar stackVar) {
                    return transformStackVarToAddressTag(stackVar);
                } else {
                    throw new RuntimeException("store ConstArray to non pointer");
                }
            };
            final Address addressStore = getAddress.apply(1);
            ConstArrayTools.workOnArray(constArray, 0, (Long offset, Constant item) -> {
                if (item instanceof ConstInt constInt) {
                    Address goal = getOperandToAddressContent(addressStore.addOffset(offset));
                    Immediate source = new Immediate(constInt.getValue());
                    IntRegister tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmLoad(tmp, source));
                    function.appendInstruction(new AsmStore(tmp, goal));
                }
            }, (Long offset, Long length) -> {
                for (long i = 0; i < length; i += 4) {
                    Address goal = getOperandToAddressContent(addressStore.addOffset(offset));
                    function.appendInstruction(new AsmStore(new IntRegister("zero"), goal));
                    offset += 4;
                }
            });
        } else {
            var source = getValue(valueOperand);
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
        }
    }

    void translateCallInst(CallInst callInst) {
        BaseFunction baseFunction = callInst.getCallee();
        AsmFunction asmFunction = function.getGlobalCode().getFunction(baseFunction);
        List<AsmOperand> parameters = new ArrayList<>();
        var typeList = baseFunction.getParameterTypes();
        for (int i = 0; i < asmFunction.getParameterSize(); i++) {
            parameters.add(getValueByType(callInst.getArgumentAt(i), typeList.get(i)));
        }
        Register returnRegister = null;
        if (asmFunction.getReturnRegister() != null) {
            returnRegister = function.getRegisterAllocator().allocate(callInst);
        }
        function.appendAllInstruction(function.call(asmFunction, parameters, returnRegister));
    }

    void translateJumpInst(JumpInst jumpInst) {
        var jumpTag = function.getBasicBlock(jumpInst.getExit()).blockTag;
        function.appendInstruction(new AsmJump(jumpTag, AsmJump.JUMPTYPE.NON, null, null));
    }

    void translateZeroExtensionInst(ZeroExtensionInst zeroExtensionInst) {
        var source = getValue(zeroExtensionInst.getSourceOperand());
        var result = function.getRegisterAllocator().allocateInt(zeroExtensionInst);
        function.appendInstruction(new AsmLoad(result, source));
    }

    void translateLoadInst(LoadInst loadInst) {
        var address = getValue(loadInst.getAddressOperand());
        Register register = function.getRegisterAllocator().allocate(loadInst);
        function.appendInstruction(new AsmLoad(register, address));
    }

    void translateReturnInst(ReturnInst returnInst) {
        var returnValue = returnInst.getReturnValue();
        if (returnValue.getType() instanceof IntegerType) {
            var ret = getValue(returnInst.getReturnValue());
            function.appendInstruction(new AsmLoad(function.getReturnRegister(), ret));
        } else if (returnValue.getType() instanceof FloatType) {
            var ret = getOperandToFloatRegister(getValue(returnInst.getReturnValue()));
            function.appendInstruction(new AsmLoad(function.getReturnRegister(), ret));
        }
        function.appendInstruction(new AsmJump(function.getRetBlockTag(), AsmJump.JUMPTYPE.NON, null, null));
    }

    void translateAllocateInst(AllocateInst allocateInst) {
        var allocatedType = allocateInst.getAllocatedType();
        var sz = (allocatedType instanceof PointerType) ? 8 : allocatedType.getSize();
        sz += (4 - sz % 4) % 4;
        function.getStackAllocator().allocate(allocateInst, Math.toIntExact(sz));
    }

    void translateGetElementPtrInst(GetElementPtrInst getElementPtrInst) {
        AddressTag baseAddress = getOperandToAddressTag(getValue(getElementPtrInst.getRootOperand()));
        long offset = baseAddress.getOffset();
        IntRegister baseRegister = baseAddress.getRegister();
        var rootType = getElementPtrInst.getRootOperand().getType();
        for (int i = 0; i < getElementPtrInst.getIndicesSize(); i++) {
            var index = getValue(getElementPtrInst.getIndexAt(i));
            long baseSize = ((PointerType) GetElementPtrInst.inferDereferencedType(rootType, i + 1)).getBaseType().getSize();
            if (index instanceof Immediate immediate) {
                offset += immediate.getValue() * baseSize;
            }
        }
        IntRegister offsetR = function.getRegisterAllocator().allocateInt();
        function.appendInstruction(new AsmLoad(offsetR, new Immediate(Math.toIntExact(offset))));
        for (int i = 0; i < getElementPtrInst.getIndicesSize(); i++) {
            var index = getValue(getElementPtrInst.getIndexAt(i));
            long baseSize = ((PointerType) GetElementPtrInst.inferDereferencedType(rootType, i + 1)).getBaseType().getSize();
            if (!(index instanceof Immediate)) {
                IntRegister tmp = getOperandToIntRegister(index);
                IntRegister muly = getOperandToIntRegister(new Immediate(Math.toIntExact(baseSize)));
                function.appendInstruction(new AsmMul(tmp, tmp, muly, 64));
                function.appendInstruction(new AsmAdd(offsetR, offsetR, tmp));
            }
        }
        function.appendInstruction(new AsmAdd(offsetR, offsetR, baseRegister));
        function.getAddressAllocator().allocate(getElementPtrInst, new AddressContent(0, offsetR));
    }

    void translateFloatNegateInst(FloatNegateInst floatNegateInst) {
        FloatRegister result = function.getRegisterAllocator().allocateFloat(floatNegateInst);
        FloatRegister source = getOperandToFloatRegister(getValue(floatNegateInst.getOperand1()));
        function.appendInstruction(new AsmFloatNegate(result, source));
    }

    void translateFloatToSignedIntegerInst(FloatToSignedIntegerInst floatToSignedIntegerInst) {
        IntRegister result = function.getRegisterAllocator().allocateInt(floatToSignedIntegerInst);
        FloatRegister source = getOperandToFloatRegister(getValue(floatToSignedIntegerInst.getSourceOperand()));
        function.appendInstruction(new AsmTransFloatInt(result, source));
    }
    void translateSignedIntegerToFloatInst(SignedIntegerToFloatInst signedIntegerToFloatInst) {
        FloatRegister result = function.getRegisterAllocator().allocateFloat(signedIntegerToFloatInst);
        IntRegister source = getOperandToIntRegister(getValue(signedIntegerToFloatInst.getSourceOperand()));
        function.appendInstruction(new AsmTransFloatInt(result, source));
    }

    void translate(Instruction instruction) {
        try {
            if (instruction instanceof ReturnInst returnInst) {
                translateReturnInst(returnInst);
            } else if (instruction instanceof AllocateInst allocateInst) {
                translateAllocateInst(allocateInst);
            } else if (instruction instanceof LoadInst loadInst) {
                translateLoadInst(loadInst);
            } else if (instruction instanceof ZeroExtensionInst zeroExtensionInst) {
                translateZeroExtensionInst(zeroExtensionInst);
            } else if (instruction instanceof BranchInst branchInst) {
                translateBranchInst(branchInst);
            } else if (instruction instanceof JumpInst jumpInst) {
                translateJumpInst(jumpInst);
            } else if (instruction instanceof StoreInst storeInst) {
                translateStoreInst(storeInst);
            } else if (instruction instanceof BinaryInstruction binaryInstruction) {
                translateBinaryInstruction(binaryInstruction);
            } else if (instruction instanceof CallInst callInst) {
                translateCallInst(callInst);
            } else if (instruction instanceof GetElementPtrInst getElementPtrInst) {
                translateGetElementPtrInst(getElementPtrInst);
            } else if (instruction instanceof FloatNegateInst floatNegateInst) {
                translateFloatNegateInst(floatNegateInst);
            } else if (instruction instanceof FloatToSignedIntegerInst floatToSignedIntegerInst) {
                translateFloatToSignedIntegerInst(floatToSignedIntegerInst);
            } else if (instruction instanceof SignedIntegerToFloatInst signedIntegerToFloatInst) {
                translateSignedIntegerToFloatInst(signedIntegerToFloatInst);
            }
        } catch (RuntimeException exception) {
            throw new RuntimeException("get exception at instruction " + instruction + "\n" + "basic block : " + blockTag.emit() + "\n" + exception.getMessage());
        }
    }
}
