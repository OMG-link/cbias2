package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.ConstArrayTools;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;
import cn.edu.bit.newnewcc.backend.asm.util.Others;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.constant.*;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Math.abs;


/**
 * 汇编基本块
 */
public class AsmBasicBlock {
    private final Label blockLabel;
    private final AsmLabel instBlockLabel;
    private final AsmFunction function;
    private final BasicBlock irBlock;
    private final Map<BasicBlock, List<AsmInstruction>> phiOperation = new HashMap<>();
    private final Map<BasicBlock, Map<Value, List<Register>>> phiValueMap = new HashMap<>();

    public AsmBasicBlock(AsmFunction function, BasicBlock block) {
        this.function = function;
        this.blockLabel = new Label(function.getFunctionName() + "_" + block.getValueName(), false);
        this.irBlock = block;
        this.instBlockLabel = new AsmLabel(blockLabel);
        function.putBlockAsmLabel(this, instBlockLabel);
    }

    /**
     * 生成基本块的汇编代码，向函数中输出指令
     */
    void emitToFunction() {
        function.appendInstruction(instBlockLabel);
        for (Instruction instruction : irBlock.getInstructions()) {
            translate(instruction);
        }
    }

    /**
     * 获取一个ir value在汇编中对应的值（函数参数、寄存器、栈上变量、全局变量、地址）
     *
     * @param value ir中的value
     * @return 对应的汇编操作数
     */
    AsmOperand getValue(Value value) {
        if (value instanceof GlobalVariable globalVariable) {
            AsmGlobalVariable asmGlobalVariable = function.getGlobalCode().getGlobalVariable(globalVariable);
            IntRegister reg = function.getRegisterAllocator().allocateInt();
            function.appendInstruction(new AsmLoad(reg, asmGlobalVariable.emitNoSegmentLabel()));
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
        } else if (value instanceof Constant constant) {
            return getConstantVar(constant, function::appendInstruction);
        }
        throw new RuntimeException("Value type not found : " + value.getValueNameIR());
    }

    AsmOperand getConstInt(int intValue, Consumer<AsmInstruction> appendInstruction) {
        if (ImmediateTools.bitlengthNotInLimit(intValue)) {
            IntRegister tmp = function.getRegisterAllocator().allocateInt();
            appendInstruction.accept(new AsmLoad(tmp, new Immediate(intValue)));
            return tmp;
        }
        return new Immediate(intValue);
    }

    AsmOperand getConstantVar(Constant constant, Consumer<AsmInstruction> appendInstruction) {
        if (constant instanceof ConstInt constInt) {
            var intValue = constInt.getValue();
            return getConstInt(intValue, appendInstruction);
        } else if (constant instanceof ConstFloat constFloat) {
            return function.transConstFloat(constFloat.getValue(), appendInstruction);
        } else if (constant instanceof ConstBool constBool) {
            return new Immediate(constBool.getValue() ? 1 : 0);
        } else if (constant instanceof ConstLong constLong) {
            if (ImmediateTools.isIntValue(constLong.getValue())) {
                return getConstInt(Math.toIntExact(constLong.getValue()), appendInstruction);
            }
            return function.transConstLong(constLong.getValue(), appendInstruction);
        }
        throw new RuntimeException("Constant value error");
    }

    AsmOperand getValueByType(Value value, Type type) {
        var result = getValue(value);
        if (type instanceof PointerType && result instanceof Address address) {
            return getAddressToIntRegister(address.getAddressDirective());
        } else {
            return result;
        }
    }

    IntRegister getAddressToIntRegister(Address addressDirective) {
        if (addressDirective.getOffset() == 0) {
            return addressDirective.getRegister();
        } else {
            int offset = Math.toIntExact(addressDirective.getOffset());
            var tmp = function.getRegisterAllocator().allocateInt();
            if (ImmediateTools.bitlengthNotInLimit(offset)) {
                var reg = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmLoad(reg, new Immediate(offset)));
                function.appendInstruction(new AsmAdd(tmp, reg, addressDirective.getRegister()));
            } else {
                function.appendInstruction(new AsmAdd(tmp, addressDirective.getRegister(), new Immediate(offset)));
            }
            return tmp;
        }
    }

    IntRegister getOperandToIntRegister(AsmOperand operand) {
        if (operand instanceof IntRegister intRegister) {
            return intRegister;
        } else if (operand instanceof AddressDirective addressDirective) {
            return getAddressToIntRegister(addressDirective);
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

    Register getValueToRegister(Value value) {
        var op = getValueByType(value, value.getType());
        Register res;
        if (op instanceof Register reg) {
            res = function.getRegisterAllocator().allocate(reg);
            function.appendInstruction(new AsmLoad(res, reg));
        } else {
            if (value.getType() instanceof FloatType) {
                var tmp = function.getRegisterAllocator().allocateFloat();
                function.appendInstruction(new AsmLoad(tmp, op));
                res = tmp;
            } else {
                var tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmLoad(tmp, op));
                res = tmp;
            }
        }
        return res;
    }

    Label getJumpLabel(BasicBlock jumpBlock) {
        AsmBasicBlock block = function.getBasicBlock(jumpBlock);
        return block.blockLabel;
    }

    AddressDirective transformStackVarToAddressTag(StackVar stackVar) {
        IntRegister tmp = function.getRegisterAllocator().allocateInt();
        Address now = stackVar.getAddress();
        IntRegister t2 = function.getRegisterAllocator().allocateInt();
        function.appendInstruction(new AsmLoad(t2, ExStackVarOffset.transform(stackVar, now.getOffset())));
        function.appendInstruction(new AsmAdd(tmp, t2, now.getRegister()));
        return now.replaceBaseRegister(tmp).setOffset(0).getAddressDirective();
    }

    AddressDirective getOperandToAddressDirective(AsmOperand operand) {
        if (operand instanceof Address address) {
            var offset = address.getOffset();
            if (ImmediateTools.bitlengthNotInLimit(offset)) {
                var tmp = getAddressToIntRegister(address);
                return new AddressDirective(0, tmp);
            } else {
                return address.getAddressDirective();
            }
        } else if (operand instanceof StackVar stackVar) {
            if (stackVar instanceof ExStackVarContent) {
                return stackVar.getAddress().getAddressDirective();
            }
            return transformStackVarToAddressTag(stackVar);
        } else if (operand instanceof IntRegister intRegister) {
            return new AddressDirective(0, intRegister);
        } else {
            throw new RuntimeException("address not found!");
        }
    }

    AddressContent getOperandToAddressContent(AsmOperand operand) {
        return getOperandToAddressDirective(operand).getAddressContent();
    }

    public void preTranslatePhiInstructions() {
        List<PhiInst> phiInstList = new ArrayList<>();
        for (var inst : irBlock.getInstructions()) {
            if (inst instanceof PhiInst phiInst) {
                phiInstList.add(phiInst);
            } else {
                break;
            }
        }
        for (var inst : phiInstList) {
            Register tmp = function.getRegisterAllocator().allocate(inst);
            for (var block : inst.getEntrySet()) {
                if (!phiOperation.containsKey(block)) {
                    phiOperation.put(block, new ArrayList<>());
                    phiValueMap.put(block, new HashMap<>());
                }
                Value source = inst.getValue(block);
                if (source instanceof Constant constant) {
                    phiOperation.get(block).add(new AsmLoad(tmp, getConstantVar(constant, (ins) -> phiOperation.get(block).add(ins))));
                } else {
                    if (!phiValueMap.get(block).containsKey(source)) {
                        phiValueMap.get(block).put(source, new ArrayList<>());
                    }
                    phiValueMap.get(block).get(source).add(tmp);
                }
            }
        }
    }

    void sufTranslatePhiInstructions() {
        for (BasicBlock block : irBlock.getExitBlocks()) {
            var next = function.getBasicBlock(block);
            if (next.phiValueMap.containsKey(irBlock)) {
                function.appendAllInstruction(next.phiOperation.get(irBlock));
                for (Value value : next.phiValueMap.get(irBlock).keySet()) {
                    Register reg = getValueToRegister(value);
                    for (var ar : next.phiValueMap.get(irBlock).get(value)) {
                        function.appendInstruction(new AsmLoad(ar, reg));
                    }
                }
            }
        }
    }

    void translatePhiInst(PhiInst phiInst) {
        var tmp = (Register) getValue(phiInst);
        var reg = function.getRegisterAllocator().allocate(phiInst);
        function.appendInstruction(new AsmLoad(reg, tmp));
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
            translateIntegerSignedDivideInst(integerSignedDivideInst);
        } else if (binaryInstruction instanceof IntegerSignedRemainderInst integerSignedRemainderInst) {
            translateIntegerSignedRemainderInst(integerSignedRemainderInst);
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
        } else if (binaryInstruction instanceof ShiftLeftInst shiftLeftInst) {
            int bitLength = shiftLeftInst.getType().getBitWidth();
            var shx = getOperandToIntRegister(getValue(shiftLeftInst.getOperand1()));
            var shy = getValue(shiftLeftInst.getOperand2());
            IntRegister result = function.getRegisterAllocator().allocateInt(shiftLeftInst);
            function.appendInstruction(new AsmShiftLeft(result, shx, shy, bitLength));
        } else {
            throw new RuntimeException("inst type not translated : " + binaryInstruction);
        }
    }

    private void translateIntegerSignedDivideInst(IntegerSignedDivideInst integerSignedDivideInst) {
        if (integerSignedDivideInst.getOperand2() instanceof ConstInt constI32Divisor) {
            int divisor = abs(constI32Divisor.getValue());
            if (Others.isPowerOf2(divisor)) {
                int x = Others.log2(divisor);
                if (x == 0) {
                    var tmp = getValueToRegister(integerSignedDivideInst.getOperand1());
                    var result = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
                    function.appendInstruction(new AsmLoad(result, tmp));
                } else if (x >= 1 && x <= 30) {
                    var src = (IntRegister) getValueToRegister(integerSignedDivideInst.getOperand1());
                    var reg1 = function.getRegisterAllocator().allocateInt();
                    var reg2 = function.getRegisterAllocator().allocateInt();
                    var regAns = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
                    var imm64SubX = getValue(ConstInt.getInstance(64 - x));
                    var immX = getValue(ConstInt.getInstance(x));
                    // %1 = srli %src, #(64-x)
                    // %2 = addw %src, %1
                    // %ans = sraiw %2, #x
                    function.appendInstruction(new AsmShiftRightLogical(reg1, src, imm64SubX, 64));
                    function.appendInstruction(new AsmAdd(reg2, src, reg1, 32));
                    function.appendInstruction(new AsmShiftRightArithmetic(regAns, reg2, immX, 32));
                }
            } else {
                int l = Others.log2(divisor);
                int sh = l;
                var temp = new BigInteger("1");
                long low = temp.shiftLeft(32 + l).divide(BigInteger.valueOf(divisor)).longValue();
                long high = temp.shiftLeft(32 + l).add(temp.shiftLeft(l + 1)).divide(BigInteger.valueOf(divisor)).longValue();
                while (((low / 2) < (high / 2)) && sh > 0) {
                    low /= 2;
                    high /= 2;
                    sh--;
                }
                if (high < (1L << 31)) {
                    var src = (IntRegister) getValueToRegister(integerSignedDivideInst.getOperand1());
                    var reg1 = function.getRegisterAllocator().allocateInt();
                    var reg2 = function.getRegisterAllocator().allocateInt();
                    var reg3 = function.getRegisterAllocator().allocateInt();
                    var regAns = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
                    var immHigh = getValue(ConstLong.getInstance(high));
                    var imm32PlusSh = getValue(ConstInt.getInstance(32 + sh));
                    var imm31 = getValue(ConstInt.getInstance(31));
                    // %1 = mul %src, #high
                    // %2 = srai %1, #(32+sh)
                    // %3 = sraiw %src, #31
                    // %ans = subw %2, %3
                    function.appendInstruction(new AsmMul(reg1, src, getOperandToIntRegister(immHigh), 64));
                    function.appendInstruction(new AsmShiftRightArithmetic(reg2, reg1, imm32PlusSh, 64));
                    function.appendInstruction(new AsmShiftRightArithmetic(reg3, src, imm31, 32));
                    function.appendInstruction(new AsmSub(regAns, reg2, reg3, 32));
                } else {
                    high = high - (1L << 32);
                    var src = (IntRegister) getValueToRegister(integerSignedDivideInst.getOperand1());
                    var reg1 = function.getRegisterAllocator().allocateInt();
                    var reg2 = function.getRegisterAllocator().allocateInt();
                    var reg3 = function.getRegisterAllocator().allocateInt();
                    var reg4 = function.getRegisterAllocator().allocateInt();
                    var reg5 = function.getRegisterAllocator().allocateInt();
                    var regAns = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
                    var immHigh = getValue(ConstLong.getInstance(high));
                    var imm32 = getValue(ConstInt.getInstance(32));
                    var immSh = getValue(ConstInt.getInstance(sh));
                    var imm31 = getValue(ConstInt.getInstance(31));
                    // %1 = mul %src, #high
                    // %2 = srai %1, #32
                    // %3 = addw %2, %src
                    // %4 = sariw %3, #sh
                    // %5 = sariw %src, #31
                    // %ans = subw %4, %5
                    function.appendInstruction(new AsmMul(reg1, src, getOperandToIntRegister(immHigh), 64));
                    function.appendInstruction(new AsmShiftRightArithmetic(reg2, reg1, imm32, 64));
                    function.appendInstruction(new AsmAdd(reg3, reg2, src, 32));
                    function.appendInstruction(new AsmShiftRightArithmetic(reg4, reg3, immSh, 32));
                    function.appendInstruction(new AsmShiftRightArithmetic(reg5, src, imm31, 32));
                    function.appendInstruction(new AsmSub(regAns, reg4, reg5, 32));
                }
            }
        } else {
            int bitLength = integerSignedDivideInst.getType().getBitWidth();
            var divx = getOperandToIntRegister(getValue(integerSignedDivideInst.getOperand1()));
            var divy = getOperandToIntRegister(getValue(integerSignedDivideInst.getOperand2()));
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSignedDivideInst);
            function.appendInstruction(new AsmSignedIntegerDivide(register, divx, divy, bitLength));
        }
    }

    private void translateIntegerSignedRemainderInst(IntegerSignedRemainderInst integerSignedRemainderInst) {
        if (integerSignedRemainderInst.getOperand2() instanceof ConstInt) {
            var a = integerSignedRemainderInst.getOperand1();
            var b = integerSignedRemainderInst.getOperand2();
            var inst1 = new IntegerSignedDivideInst(integerSignedRemainderInst.getType(), a, b);
            var inst2 = new IntegerMultiplyInst(integerSignedRemainderInst.getType(), inst1, b);
            var inst3 = new IntegerSubInst(integerSignedRemainderInst.getType(), a, inst2);
            translateIntegerSignedDivideInst(inst1);
            translateBinaryInstruction(inst2);
            translateBinaryInstruction(inst3);
        } else {
            int bitLength = integerSignedRemainderInst.getType().getBitWidth();
            var divx = getOperandToIntRegister(getValue(integerSignedRemainderInst.getOperand1()));
            var divy = getOperandToIntRegister(getValue(integerSignedRemainderInst.getOperand2()));
            IntRegister register = function.getRegisterAllocator().allocateInt(integerSignedRemainderInst);
            function.appendInstruction(new AsmSignedIntegerRemainder(register, divx, divy, bitLength));
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
            case SLT ->
                    function.appendInstruction(new AsmIntegerCompare(result, rop1, rop2, AsmIntegerCompare.Condition.SLT));
            case SLE -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmIntegerCompare(tmp, rop2, rop1, AsmIntegerCompare.Condition.SLT));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
            }
            case SGT ->
                    function.appendInstruction(new AsmIntegerCompare(result, rop2, rop1, AsmIntegerCompare.Condition.SLT));
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
            case OLE ->
                    function.appendInstruction(new AsmFloatCompare(result, op1, op2, AsmFloatCompare.Condition.OLE));
            case OEQ ->
                    function.appendInstruction(new AsmFloatCompare(result, op1, op2, AsmFloatCompare.Condition.OEQ));
            case OLT ->
                    function.appendInstruction(new AsmFloatCompare(result, op1, op2, AsmFloatCompare.Condition.OLT));
            case ONE -> {
                tmp = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmFloatCompare(tmp, op1, op2, AsmFloatCompare.Condition.OEQ));
                function.appendInstruction(new AsmIntegerCompare(result, tmp, null, AsmIntegerCompare.Condition.SEQZ));
            }
            case OGE ->
                    function.appendInstruction(new AsmFloatCompare(result, op2, op1, AsmFloatCompare.Condition.OLE));
            case OGT ->
                    function.appendInstruction(new AsmFloatCompare(result, op2, op1, AsmFloatCompare.Condition.OLT));
        }
    }

    void translateBranchInst(BranchInst branchInst) {
        var condition = getOperandToIntRegister(getValue(branchInst.getCondition()));
        sufTranslatePhiInstructions();
        var trueLabel = getJumpLabel(branchInst.getTrueExit());
        var falseLabel = getJumpLabel(branchInst.getFalseExit());
        function.appendInstruction(new AsmJump(trueLabel, AsmJump.JUMPTYPE.NEZ, condition, null));
        function.appendInstruction(new AsmJump(falseLabel, AsmJump.JUMPTYPE.UNCONDITIONAL, null, null));
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
                    Address dest = getOperandToAddressContent(addressStore.addOffset(offset));
                    Immediate source = new Immediate(constInt.getValue());
                    IntRegister tmp = function.getRegisterAllocator().allocateInt();
                    function.appendInstruction(new AsmLoad(tmp, source));
                    function.appendInstruction(new AsmStore(tmp, dest));
                }
            }, (Long offset, Long length) -> {
                for (long i = 0; i < length; i += 4) {
                    Address dest = getOperandToAddressContent(addressStore.addOffset(offset));
                    function.appendInstruction(new AsmStore(IntRegister.zero, dest));
                    offset += 4;
                }
            });
        } else {
            var source = getValue(valueOperand);
            int bitLength = 32;
            if (valueOperand.getType() instanceof IntegerType integerType) {
                bitLength = integerType.getBitWidth();
            }
            if (source instanceof Register register) {
                function.appendInstruction(new AsmStore(register, address, bitLength));
            } else {
                Register register;
                if (storeInst.getValueOperand().getType() instanceof FloatType) {
                    register = function.getRegisterAllocator().allocateFloat();
                } else {
                    register = function.getRegisterAllocator().allocateInt();
                }
                function.appendInstruction(new AsmLoad(register, source, bitLength));
                function.appendInstruction(new AsmStore(register, address, bitLength));
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
        sufTranslatePhiInstructions();
        var jumpLabel = getJumpLabel(jumpInst.getExit());
        function.appendInstruction(new AsmJump(jumpLabel, AsmJump.JUMPTYPE.UNCONDITIONAL, null, null));
    }

    void translateZeroExtensionInst(ZeroExtensionInst zeroExtensionInst) {
        var source = getValue(zeroExtensionInst.getSourceOperand());
        var result = function.getRegisterAllocator().allocateInt(zeroExtensionInst);
        function.appendInstruction(new AsmLoad(result, source));
    }

    //寄存器中的值已经是符号扩展过后的64位数，因此无需专门sext
    void translateSignedExtensionInst(SignedExtensionInst signedExtensionInst) {
        Register tmp = getValueToRegister(signedExtensionInst.getSourceOperand());
        Register reg = function.getRegisterAllocator().allocate(signedExtensionInst);
        function.appendInstruction(new AsmLoad(reg, tmp));
    }

    void translateLoadInst(LoadInst loadInst) {
        var address = getValue(loadInst.getAddressOperand());
        Register register = function.getRegisterAllocator().allocate(loadInst);
        Type type = loadInst.getType();
        if (type instanceof IntegerType integerType) {
            function.appendInstruction(new AsmLoad(register, address, integerType.getBitWidth()));
        } else {
            function.appendInstruction(new AsmLoad(register, address));
        }
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
        function.appendInstruction(new AsmJump(function.getRetBlockLabel(), AsmJump.JUMPTYPE.UNCONDITIONAL, null, null));
    }

    void translateAllocateInst(AllocateInst allocateInst) {
        var allocatedType = allocateInst.getAllocatedType();
        var sz = (allocatedType instanceof PointerType) ? 8 : allocatedType.getSize();
        sz += (4 - sz % 4) % 4;
        function.getStackAllocator().allocate(allocateInst, Math.toIntExact(sz));
    }

    void translateGetElementPtrInst(GetElementPtrInst getElementPtrInst) {
        AddressDirective baseAddress = getOperandToAddressDirective(getValue(getElementPtrInst.getRootOperand()));
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
                IntRegister tmp = function.getRegisterAllocator().allocateInt();
                IntRegister t2 = function.getRegisterAllocator().allocateInt();
                IntRegister t3 = function.getRegisterAllocator().allocateInt();
                function.appendInstruction(new AsmLoad(tmp, getOperandToIntRegister(index)));
                IntRegister muly = getOperandToIntRegister(new Immediate(Math.toIntExact(baseSize)));
                function.appendInstruction(new AsmMul(t2, tmp, muly, 64));
                function.appendInstruction(new AsmAdd(t3, offsetR, t2));
                offsetR = t3;
            }
        }
        IntRegister t4 = function.getRegisterAllocator().allocateInt();
        function.appendInstruction(new AsmAdd(t4, offsetR, baseRegister));
        offsetR = t4;
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

    void translateBitCastInst(BitCastInst bitCastInst) {
        var address = getOperandToAddressDirective(getValue(bitCastInst.getSourceOperand())).getAddressContent();
        function.getAddressAllocator().allocate(bitCastInst, address);
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
            } else if (instruction instanceof SignedExtensionInst signedExtensionInst) {
                translateSignedExtensionInst(signedExtensionInst);
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
            } else if (instruction instanceof BitCastInst bitCastInst) {
                translateBitCastInst(bitCastInst);
            } else if (instruction instanceof PhiInst phiInst) {
                translatePhiInst(phiInst);
            } else {
                throw new RuntimeException("inst type not translated" + instruction);
            }
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            throw new RuntimeException("get exception at instruction " + instruction + "\n" + "basic block : " + blockLabel.emit() + "\n" + exception.getMessage());
        }
    }
}
