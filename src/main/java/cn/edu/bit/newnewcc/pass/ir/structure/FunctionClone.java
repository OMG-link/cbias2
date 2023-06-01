package cn.edu.bit.newnewcc.pass.ir.structure;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;
import cn.edu.bit.newnewcc.ir.exception.IllegalStateException;
import cn.edu.bit.newnewcc.ir.value.BaseFunction;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.instruction.*;
import cn.edu.bit.newnewcc.pass.ir.util.UtilFunctions;

import java.util.*;

/**
 * 函数的克隆体 <br>
 * 函数的实参、返回地址等均用符号代替，需使用replaceAllUsageTo方法替换 <br>
 */
public class FunctionClone {

    private class Symbol extends Value {
        public Symbol(Type type) {
            super(type);
        }

        private String name;

        @Override
        public String getValueName() {
            if (name == null) {
                name = String.format("%s#%s", this.getClass(), UUID.randomUUID());
            }
            return name;
        }

        @Override
        public String getValueNameIR() {
            return '%' + getValueName();
        }

        @Override
        public void setValueName(String valueName) {
            this.name = name;
        }

    }

    private final Function function;
    private final Map<Value, Value> valueMap = new HashMap<>();
    private final BasicBlock returnBlock;

    /// 需要外部处理的值
    private final List<BasicBlock> basicBlocks = new ArrayList<>();
    private final PhiInst returnValue;
    private final List<AllocateInst> allocateInstructions = new ArrayList<>();

    public FunctionClone(Function function, List<Value> arguments, BasicBlock returnBlock) {
        this.function = function;
        int parameterNum = function.getFormalParameters().size();
        for (int i = 0; i < parameterNum; i++) {
            this.valueMap.put(function.getFormalParameters().get(i), arguments.get(i));
        }
        this.returnBlock = returnBlock;
        this.returnValue = new PhiInst(function.getReturnType());
        doFunctionClone();
    }

    public BasicBlock getEntryBlock() {
        return (BasicBlock) valueMap.get(function.getEntryBasicBlock());
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public PhiInst getReturnValue() {
        return returnValue;
    }

    public List<AllocateInst> getAllocateInstructions() {
        return allocateInstructions;
    }

    private Value getReplacedValue(Value value) {
        if (UtilFunctions.isGlobalValue(value)) {
            return value;
        } else {
            if (!valueMap.containsKey(value)) {
                throw new IllegalStateException("No local value were found to replace.");
            } else {
                return valueMap.get(value);
            }
        }
    }

    private void setValueMapKv(Value key, Value value) {
        valueMap.get(key).replaceAllUsageTo(value);
        valueMap.put(key, value);
    }

    private Instruction cloneInstruction(Instruction instruction) {
        if (instruction instanceof AllocateInst allocateInst) {
            return new AllocateInst(allocateInst.getAllocatedType());
        } else if (instruction instanceof BitCastInst bitCastInst) {
            return new BitCastInst(
                    getReplacedValue(bitCastInst.getSourceOperand()),
                    bitCastInst.getTargetType()
            );
        } else if (instruction instanceof BranchInst branchInst) {
            return new BranchInst(
                    getReplacedValue(branchInst.getCondition()),
                    (BasicBlock) getReplacedValue(branchInst.getTrueExit()),
                    (BasicBlock) getReplacedValue(branchInst.getFalseExit())
            );
        } else if (instruction instanceof CallInst callInst) {
            var argumentList = new ArrayList<Value>();
            for (int i = 0; i < callInst.getArgumentSize(); i++) {
                argumentList.add(getReplacedValue(callInst.getArgumentAt(i)));
            }
            return new CallInst(
                    (BaseFunction) getReplacedValue(callInst.getCallee()),
                    argumentList
            );
        } else if (instruction instanceof FloatAddInst floatAddInst) {
            return new FloatAddInst(
                    floatAddInst.getType(),
                    getReplacedValue(floatAddInst.getOperand1()),
                    getReplacedValue(floatAddInst.getOperand2())
            );
        } else if (instruction instanceof FloatCompareInst floatCompareInst) {
            return new FloatCompareInst(
                    floatCompareInst.getComparedType(),
                    floatCompareInst.getCondition(),
                    getReplacedValue(floatCompareInst.getOperand1()),
                    getReplacedValue(floatCompareInst.getOperand2())
            );
        } else if (instruction instanceof FloatDivideInst floatDivideInst) {
            return new FloatDivideInst(
                    floatDivideInst.getType(),
                    getReplacedValue(floatDivideInst.getOperand1()),
                    getReplacedValue(floatDivideInst.getOperand2())
            );
        } else if (instruction instanceof FloatMultiplyInst floatMultiplyInst) {
            return new FloatMultiplyInst(
                    floatMultiplyInst.getType(),
                    getReplacedValue(floatMultiplyInst.getOperand1()),
                    getReplacedValue(floatMultiplyInst.getOperand2())
            );
        } else if (instruction instanceof FloatNegateInst floatNegateInst) {
            return new FloatNegateInst(
                    floatNegateInst.getType(),
                    getReplacedValue(floatNegateInst.getOperand1())
            );
        } else if (instruction instanceof FloatSubInst floatSubInst) {
            return new FloatSubInst(
                    floatSubInst.getType(),
                    getReplacedValue(floatSubInst.getOperand1()),
                    getReplacedValue(floatSubInst.getOperand2())
            );
        } else if (instruction instanceof FloatToSignedIntegerInst floatToSignedIntegerInst) {
            return new FloatToSignedIntegerInst(
                    getReplacedValue(floatToSignedIntegerInst.getSourceOperand()),
                    floatToSignedIntegerInst.getTargetType()
            );
        } else if (instruction instanceof GetElementPtrInst getElementPtrInst) {
            var indices = new ArrayList<Value>();
            for (Value indexOperand : getElementPtrInst.getIndexOperands()) {
                indices.add(getReplacedValue(indexOperand));
            }
            return new GetElementPtrInst(
                    getReplacedValue(getElementPtrInst.getRootOperand()),
                    indices
            );
        } else if (instruction instanceof IntegerAddInst integerAddInst) {
            return new IntegerAddInst(
                    integerAddInst.getType(),
                    getReplacedValue(integerAddInst.getOperand1()),
                    getReplacedValue(integerAddInst.getOperand2())
            );
        } else if (instruction instanceof IntegerCompareInst integerCompareInst) {
            return new IntegerCompareInst(
                    integerCompareInst.getComparedType(),
                    integerCompareInst.getCondition(),
                    getReplacedValue(integerCompareInst.getOperand1()),
                    getReplacedValue(integerCompareInst.getOperand2())
            );
        } else if (instruction instanceof IntegerMultiplyInst integerMultiplyInst) {
            return new IntegerMultiplyInst(
                    integerMultiplyInst.getType(),
                    getReplacedValue(integerMultiplyInst.getOperand1()),
                    getReplacedValue(integerMultiplyInst.getOperand2())
            );
        } else if (instruction instanceof IntegerSignedDivideInst integerSignedDivideInst) {
            return new IntegerSignedDivideInst(
                    integerSignedDivideInst.getType(),
                    getReplacedValue(integerSignedDivideInst.getOperand1()),
                    getReplacedValue(integerSignedDivideInst.getOperand2())
            );
        } else if (instruction instanceof IntegerSignedRemainderInst integerSignedRemainderInst) {
            return new IntegerSignedRemainderInst(
                    integerSignedRemainderInst.getType(),
                    getReplacedValue(integerSignedRemainderInst.getOperand1()),
                    getReplacedValue(integerSignedRemainderInst.getOperand2())
            );
        } else if (instruction instanceof IntegerSubInst integerSubInst) {
            return new IntegerSubInst(
                    integerSubInst.getType(),
                    getReplacedValue(integerSubInst.getOperand1()),
                    getReplacedValue(integerSubInst.getOperand2())
            );
        } else if (instruction instanceof JumpInst jumpInst) {
            return new JumpInst((BasicBlock) getReplacedValue(jumpInst.getExit()));
        } else if (instruction instanceof LoadInst loadInst) {
            return new LoadInst(getReplacedValue(loadInst.getAddressOperand()));
        } else if (instruction instanceof PhiInst phiInst) {
            var clonedPhiInst = new PhiInst(phiInst.getType());
            phiInst.forEach((entryBlock, value) -> clonedPhiInst.addEntry(
                    (BasicBlock) getReplacedValue(entryBlock),
                    getReplacedValue(value)
            ));
            return clonedPhiInst;
        } else if (instruction instanceof ReturnInst) {
            return new JumpInst(returnBlock);
        } else if (instruction instanceof SignedIntegerToFloatInst signedIntegerToFloatInst) {
            return new SignedIntegerToFloatInst(
                    getReplacedValue(signedIntegerToFloatInst.getSourceOperand()),
                    signedIntegerToFloatInst.getTargetType()
            );
        } else if (instruction instanceof StoreInst storeInst) {
            return new StoreInst(
                    getReplacedValue(storeInst.getAddressOperand()),
                    getReplacedValue(storeInst.getValueOperand())
            );
        } else if (instruction instanceof ZeroExtensionInst zeroExtensionInst) {
            return new ZeroExtensionInst(
                    getReplacedValue(zeroExtensionInst.getSourceOperand()),
                    zeroExtensionInst.getTargetType()
            );
        } else {
            throw new IllegalArgumentException("Cannot clone instruction of type " + instruction.getClass());
        }
    }

    private void doFunctionClone() {
        // 构建占位符
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            valueMap.put(basicBlock, new BasicBlock());
            for (Instruction instruction : basicBlock.getInstructions()) {
                valueMap.put(instruction, new Symbol(instruction.getType()));
            }
        }
        // 构建函数体
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            var clonedBlock = (BasicBlock) getReplacedValue(basicBlock);
            setValueMapKv(basicBlock, clonedBlock);
            for (Instruction instruction : basicBlock.getInstructions()) {
                var clonedInstruction = cloneInstruction(instruction);
                setValueMapKv(instruction, clonedInstruction);
                if (instruction instanceof ReturnInst returnInst) {
                    returnValue.addEntry(clonedBlock, getReplacedValue(returnInst.getReturnValue()));
                }
                // 放置语句到合适的位置
                if (instruction instanceof AllocateInst) {
                    allocateInstructions.add((AllocateInst) clonedInstruction);
                } else {
                    clonedBlock.addInstruction(clonedInstruction);
                }
            }
            this.basicBlocks.add(clonedBlock);
        }
    }

}
