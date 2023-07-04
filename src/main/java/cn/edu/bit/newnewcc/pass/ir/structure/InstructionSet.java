package cn.edu.bit.newnewcc.pass.ir.structure;

import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.util.*;

public class InstructionSet {

    private final Map<List<Object>, Instruction> instructionSet = new HashMap<>();

    public void add(Instruction instruction) {
        instructionSet.put(getFeatures(instruction), instruction);
    }

    public boolean contains(Instruction instruction) {
        return instructionSet.containsKey(getFeatures(instruction));
    }

    public Instruction get(Instruction instruction) {
        return instructionSet.get(getFeatures(instruction));
    }

    private static List<Object> getFeatures(Instruction instruction) {
        List<Object> result = new ArrayList<>();
        result.add(instruction.getClass());
        // 不可合并的指令
        if (instruction instanceof TerminateInst ||
                instruction instanceof CallInst ||
                instruction instanceof MemoryInst) {
            result.add(instruction);
        }
        // 可交换二元算数指令
        else if (instruction instanceof FloatAddInst ||
                instruction instanceof FloatMultiplyInst ||
                instruction instanceof IntegerAddInst ||
                instruction instanceof IntegerMultiplyInst) {
            var binaryInstruction = (BinaryInstruction) instruction;
            var opSet = new HashSet<>();
            opSet.add(binaryInstruction.getOperand1());
            opSet.add(binaryInstruction.getOperand2());
            result.add(opSet);
        }
        // 不可交换二元算数指令
        else if (instruction instanceof FloatDivideInst ||
                instruction instanceof FloatSubInst ||
                instruction instanceof IntegerSignedDivideInst ||
                instruction instanceof IntegerSignedRemainderInst ||
                instruction instanceof IntegerSubInst) {
            var binaryInstruction = (BinaryInstruction) instruction;
            result.add(binaryInstruction.getOperand1());
            result.add(binaryInstruction.getOperand2());
        }
        // 比较指令
        else if (instruction instanceof FloatCompareInst floatCompareInst) {
            switch (floatCompareInst.getCondition()) {
                case OEQ, ONE, OLE, OLT -> {
                    result.add(floatCompareInst.getCondition());
                    result.add(floatCompareInst.getOperand1());
                    result.add(floatCompareInst.getOperand2());
                }
                case OGE, OGT -> {
                    result.add(floatCompareInst.getCondition().swap());
                    result.add(floatCompareInst.getOperand2());
                    result.add(floatCompareInst.getOperand1());
                }
            }
        } else if (instruction instanceof IntegerCompareInst integerCompareInst) {
            switch (integerCompareInst.getCondition()) {
                case EQ, NE, SLT, SLE: {
                    result.add(integerCompareInst.getCondition());
                    result.add(integerCompareInst.getOperand1());
                    result.add(integerCompareInst.getOperand2());
                }
                case SGE, SGT: {
                    result.add(integerCompareInst.getCondition().swap());
                    result.add(integerCompareInst.getOperand2());
                    result.add(integerCompareInst.getOperand1());
                }
            }
        }
        // 特殊指令
        else if (instruction instanceof PhiInst phiInst) {
            var map = new HashMap<BasicBlock, Value>();
            phiInst.forEach(map::put);
            result.add(map);
        } else if (instruction instanceof FloatNegateInst floatNegateInst) {
            result.add(floatNegateInst.getOperand1());
        } else if (instruction instanceof GetElementPtrInst getElementPtrInst) {
            result.add(getElementPtrInst.getRootOperand());
            result.add(getElementPtrInst.getIndexOperands());
        }
        // 类型转换指令
        else if (instruction instanceof FloatToSignedIntegerInst floatToSignedIntegerInst) {
            result.add(floatToSignedIntegerInst.getSourceOperand());
        } else if (instruction instanceof SignedIntegerToFloatInst signedIntegerToFloatInst) {
            result.add(signedIntegerToFloatInst.getSourceOperand());
        } else if (instruction instanceof ZeroExtensionInst zeroExtensionInst) {
            result.add(zeroExtensionInst.getSourceOperand());
        } else if (instruction instanceof BitCastInst bitCastInst) {
            result.add(bitCastInst.getSourceOperand());
        }
        // 未知指令
        else {
            throw new RuntimeException("Unable to extract feature from instruction of class " + instruction.getClass());
        }
        return result;
    }

}
