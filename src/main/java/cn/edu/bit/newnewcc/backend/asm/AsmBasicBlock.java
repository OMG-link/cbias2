package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.ConstArrayTools;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;
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
     * 获取一个ir value在汇编中对应的值（函数参数、寄存器、栈上变量、全局变量、地址）
     * @param value ir中的value
     * @return 对应的汇编操作数
     */
    AsmOperand getValue(Value value) {
        if (value instanceof GlobalVariable globalVariable) {
            AsmGlobalVariable asmGlobalVariable = function.getGlobalCode().getGlobalVariable(globalVariable);
            IntRegister reg = function.getRegisterAllocator().allocateInt();
            function.appendInstruction(new AsmLoad(reg, asmGlobalVariable.emitTag(true)));
            function.appendInstruction(new AsmAdd(reg, reg, asmGlobalVariable.emitTag(false)));
            return new AddressContent(0, reg);
        } else if (value instanceof Function.FormalParameter formalParameter) {
            return function.getParameterByFormal(formalParameter);
        } else if (value instanceof Instruction instruction) {
            if (function.getRegisterAllocator().contain(instruction)) {
                return function.getRegisterAllocator().get(instruction);
            } else if (function.getStackAllocator().contain(instruction)) {
                return function.getStackAllocator().get(instruction);
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

    AddressTag transformStackVarToAddressTag(StackVar stackVar) {
        IntRegister tmp = function.getRegisterAllocator().allocateInt();
        Address now = stackVar.getAddress();
        if (ImmediateTools.stackOffsetNotInLimit(now.getOffset())) {
            function.appendInstruction(new AsmLoad(tmp, ExStackVarOffset.transform(stackVar, now.getOffset())));
            function.appendInstruction(new AsmAdd(tmp, tmp, now.getRegister()));
        } else {
            function.appendInstruction(new AsmAdd(tmp, now.getRegister(), ExStackVarOffset.transform(stackVar, Math.toIntExact(now.getOffset()))));
        }
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

    void translateBinaryInstruction(BinaryInstruction binaryInstruction) {
        if (binaryInstruction instanceof IntegerAddInst integerAddInst) {
            var addx = getOperandToIntRegister(getValue(integerAddInst.getOperand1()));
            var addy = getValue(integerAddInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerAddInst);
            function.appendInstruction(new AsmAdd(register, addx, addy));
        } else if (binaryInstruction instanceof IntegerSubInst integerSubInst) {
            var subx = getOperandToIntRegister(getValue(integerSubInst.getOperand1()));
            var suby = getValue(integerSubInst.getOperand2());
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSubInst);
            function.appendInstruction(new AsmSub(register, subx, suby));
        } else if (binaryInstruction instanceof IntegerMultiplyInst integerMultiplyInst) {
            var mulx = getOperandToIntRegister(getValue(integerMultiplyInst.getOperand1()));
            var muly = getOperandToIntRegister(getValue(integerMultiplyInst.getOperand2()));
            IntRegister register = function.getRegisterAllocator().allocateInt(integerMultiplyInst);
            function.appendInstruction(new AsmMul(register, mulx, muly));
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
        }
    }

    void translateCompareInst(CompareInst compareInst) {
        if (compareInst instanceof IntegerCompareInst integerCompareInst) {
            var rop1 = getOperandToIntRegister(getValue(integerCompareInst.getOperand1()));
            var rop2 = getOperandToIntRegister(getValue(integerCompareInst.getOperand2()));
            var result = function.getRegisterAllocator().allocateInt(integerCompareInst);
            IntRegister tmp;
            switch (integerCompareInst.getCondition()) {
                case EQ -> {
                    tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmSub(tmp, rop1, rop2));
                    function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
                }
                case NE -> {
                    tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmSub(tmp, rop1, rop2));
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
        if (!(returnValue.getType() instanceof VoidType)) {
            var ret = getValue(returnInst.getReturnValue());
            function.appendInstruction(new AsmLoad(new IntRegister("a0"), ret));
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
        IntRegister offsetR = null;
        for (int i = 0; i < getElementPtrInst.getIndicesSize(); i++) {
            var index = getValue(getElementPtrInst.getIndexAt(i));
            long baseSize = ((PointerType) GetElementPtrInst.inferDereferencedType(rootType, i + 1)).getBaseType().getSize();
            if (index instanceof Immediate immediate) {
                offset += immediate.getValue() * baseSize;
            }
        }
        for (int i = 0; i < getElementPtrInst.getIndicesSize(); i++) {
            var index = getValue(getElementPtrInst.getIndexAt(i));
            long baseSize = ((PointerType) GetElementPtrInst.inferDereferencedType(rootType, i + 1)).getBaseType().getSize();
            if (!(index instanceof Immediate)) {
                IntRegister tmp = getOperandToIntRegister(index);
                IntRegister muly = getOperandToIntRegister(new Immediate(Math.toIntExact(baseSize)));
                function.appendInstruction(new AsmMul(tmp, tmp, muly));
                if (offsetR == null) {
                    offsetR = function.getRegisterAllocator().allocateInt();
                    if (ImmediateTools.bitlengthNotInLimit(offset)) {
                        function.appendInstruction(new AsmLoad(offsetR, new Immediate(Math.toIntExact(offset))));
                        function.appendInstruction(new AsmAdd(offsetR, offsetR, tmp));
                        offset = 0;
                    } else {
                        function.appendInstruction(new AsmLoad(offsetR, tmp));
                    }
                } else {
                    function.appendInstruction(new AsmAdd(offsetR, offsetR, tmp));
                }
            }
        }
        if (offsetR != null) {
            function.appendInstruction(new AsmAdd(offsetR, offsetR, baseRegister));
            baseRegister = offsetR;
        } else if (ImmediateTools.bitlengthNotInLimit(offset)) {
            IntRegister tmp = getOperandToIntRegister(new Immediate(Math.toIntExact(offset)));
            function.appendInstruction(new AsmAdd(tmp, baseRegister, tmp));
            offset = 0;
            baseRegister = tmp;
        }
        function.getAddressAllocator().allocate(getElementPtrInst, new AddressContent(offset, baseRegister));
    }

    void translate(Instruction instruction) {
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
        }
    }
}
