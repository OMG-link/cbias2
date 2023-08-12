package cn.edu.bit.newnewcc.pass.ir.structure;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.util.*;
import java.util.function.Consumer;

import static java.lang.Math.*;

public class I32ValueRangeAnalyzer {

    private static final double NEARLY_HURRY_PERCENTAGE = 0.5;

    // 当迭代次数过多时，自动进入 hurry up 模式
    // 在 hurry up 模式下，只关心变量的正负
    private static final int HURRY_UP_THRESHOLD_INITIAL = 100000;
    private static final int NEARLY_HURRY_THRESHOLD_INITIAL = (int) (HURRY_UP_THRESHOLD_INITIAL * NEARLY_HURRY_PERCENTAGE);

    private static final int HURRY_UP_THRESHOLD_UPDATE = 1000;
    private static final int NEARLY_HURRY_THRESHOLD_UPDATE = (int) (HURRY_UP_THRESHOLD_UPDATE * NEARLY_HURRY_PERCENTAGE);

    public static class I32ValueRange {
        public final int minValue, maxValue;

        private I32ValueRange(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        private static I32ValueRange of(Value value) {
            return I32ValueRange.of(value, null);
        }

        private static I32ValueRange of(Value value, I32ValueRangeAnalyzer helper) {
            if (!(value.getType() == IntegerType.getI32())) {
                throw new IllegalArgumentException();
            }
            if (helper != null && helper.hasValueRangeSolved(value)) {
                return helper.getValueRange(value);
            }
            I32ValueRange result = calculateI32ValueRange(value, helper);
            if (helper != null) {
                helper.setValueRange(value, result);
            }
            return result;
        }

        private static I32ValueRange calculateI32ValueRange(Value value, I32ValueRangeAnalyzer helper) {
            I32ValueRange result;
            if (value instanceof Constant) {
                if (value instanceof ConstInt constInt) {
                    result = new I32ValueRange(constInt.getValue(), constInt.getValue());
                } else {
                    throw new RuntimeException("Unexpected type of i32 constant.");
                }
            } else if (value instanceof IntegerArithmeticInst integerArithmeticInst) {
                I32ValueRange range1 = I32ValueRange.of(integerArithmeticInst.getOperand1(), helper);
                I32ValueRange range2 = I32ValueRange.of(integerArithmeticInst.getOperand2(), helper);
                if (integerArithmeticInst instanceof IntegerAddInst) {
                    int minValue;
                    try {
                        minValue = addExact(range1.minValue, range2.minValue);
                    } catch (ArithmeticException e) {
                        minValue = Integer.MIN_VALUE;
                    }
                    int maxValue;
                    try {
                        maxValue = addExact(range1.maxValue, range2.maxValue);
                    } catch (ArithmeticException e) {
                        maxValue = Integer.MAX_VALUE;
                    }
                    result = new I32ValueRange(minValue, maxValue);
                } else if (integerArithmeticInst instanceof IntegerSubInst) {
                    int minValue;
                    try {
                        minValue = subtractExact(range1.minValue, range2.maxValue);
                    } catch (ArithmeticException e) {
                        minValue = Integer.MIN_VALUE;
                    }
                    int maxValue;
                    try {
                        maxValue = subtractExact(range1.maxValue, range2.minValue);
                    } catch (ArithmeticException e) {
                        maxValue = Integer.MAX_VALUE;
                    }
                    result = new I32ValueRange(minValue, maxValue);
                } else if (integerArithmeticInst instanceof IntegerMultiplyInst) {
                    int minValue, maxValue;
                    try {
                        var endpointValues = new int[4];
                        endpointValues[0] = multiplyExact(range1.minValue, range2.maxValue);
                        endpointValues[1] = multiplyExact(range1.maxValue, range2.minValue);
                        endpointValues[2] = multiplyExact(range1.minValue, range2.minValue);
                        endpointValues[3] = multiplyExact(range1.maxValue, range2.maxValue);
                        minValue = Integer.MAX_VALUE;
                        maxValue = Integer.MIN_VALUE;
                        for (int endpointValue : endpointValues) {
                            minValue = min(minValue, endpointValue);
                            maxValue = max(maxValue, endpointValue);
                        }
                    } catch (ArithmeticException e) {
                        minValue = Integer.MIN_VALUE;
                        maxValue = Integer.MAX_VALUE;
                    }
                    result = new I32ValueRange(minValue, maxValue);
                } else if (integerArithmeticInst instanceof IntegerSignedDivideInst) {
                    int minValue = Integer.MAX_VALUE;
                    int maxValue = Integer.MIN_VALUE;
                    if (range1.maxValue >= 0) {
                        int minPositiveDividend = max(range1.minValue, 0);
                        if (range2.maxValue > 0) {
                            int minPositiveDivisor = max(range2.minValue, 1);
                            maxValue = max(maxValue, range1.maxValue / minPositiveDivisor);
                            minValue = min(minValue, minPositiveDividend / range2.maxValue);
                        }
                        if (range2.minValue < 0) {
                            int maxNegativeDivisor = min(range2.maxValue, -1);
                            maxValue = max(maxValue, minPositiveDividend / range2.minValue);
                            minValue = min(minValue, range1.maxValue / maxNegativeDivisor);
                        }
                    }
                    if (range1.minValue <= 0) {
                        int maxNegativeDividend = min(range1.maxValue, 0);
                        if (range2.maxValue > 0) {
                            int minPositiveDivisor = max(range2.minValue, 1);
                            maxValue = max(maxValue, maxNegativeDividend / range2.maxValue);
                            minValue = min(minValue, range1.minValue / minPositiveDivisor);
                        }
                        if (range2.minValue < 0) {
                            int maxNegativeDivisor = min(range2.maxValue, -1);
                            maxValue = max(maxValue, range1.minValue / maxNegativeDivisor);
                            minValue = min(minValue, maxNegativeDividend / range2.minValue);
                        }
                    }
                    result = new I32ValueRange(minValue, maxValue);
                } else if (integerArithmeticInst instanceof IntegerSignedRemainderInst) {
                    var maxModulus = max(abs(range2.minValue), abs(range2.maxValue));
                    int minValue, maxValue;
                    if (range1.minValue < 0) {
                        minValue = max(-(maxModulus - 1), range1.minValue);
                    } else {
                        minValue = 0;
                    }
                    if (range1.maxValue > 0) {
                        maxValue = min(maxModulus - 1, range1.maxValue);
                    } else {
                        maxValue = 0;
                    }
                    result = new I32ValueRange(minValue, maxValue);
                } else {
                    throw new RuntimeException("Unexpected type of integer arithmetic instruction.");
                }
            } else if (value instanceof SignedExtensionInst || value instanceof ZeroExtensionInst) {
                if (value instanceof SignedExtensionInst signedExtensionInst) {
                    var sourceType = signedExtensionInst.getSourceType();
                    var sourceBitWidth = sourceType.getBitWidth();
                    var temp = 1 << (sourceBitWidth - 1);
                    result = new I32ValueRange(-temp, temp - 1);
                } else {
                    ZeroExtensionInst zeroExtensionInst = (ZeroExtensionInst) value;
                    var sourceType = zeroExtensionInst.getSourceType();
                    var sourceBitWidth = sourceType.getBitWidth();
                    result = new I32ValueRange(0, (1 << sourceBitWidth) - 1);
                }
            } else if (value instanceof BitCastInst || value instanceof LoadInst ||
                    value instanceof CallInst || value instanceof Function.FormalParameter ||
                    value instanceof FloatToSignedIntegerInst) {
                result = new I32ValueRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
            } else if (value instanceof PhiInst phiInst) {
                // 为了避免循环求值，phi指令不会递归求解，当入口值未知时，暂时忽略它
                int minValue = Integer.MAX_VALUE;
                int maxValue = Integer.MIN_VALUE;
                for (BasicBlock basicBlock : phiInst.getEntrySet()) {
                    var entryValue = phiInst.getValue(basicBlock);
                    if (helper != null && helper.hasValueRangeSolved(entryValue)) {
                        var entryRange = helper.getValueRange(entryValue);
                        int minEntryValue, maxEntryValue;
                        minEntryValue = entryRange.minValue;
                        maxEntryValue = entryRange.maxValue;
                        minValue = min(minValue, minEntryValue);
                        maxValue = max(maxValue, maxEntryValue);
                    }
                }
                if (minValue > maxValue) {
                    throw new RuntimeException("No valid entry were found for this phi node.");
                }
                result = new I32ValueRange(minValue, maxValue);
            } else {
                throw new RuntimeException("Unexpected type of i32 value.");
            }
            if (value instanceof Instruction instruction) {
                instruction.setComment(String.format("[%d, %d]", result.minValue, result.maxValue));
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            I32ValueRange that = (I32ValueRange) o;
            return minValue == that.minValue && maxValue == that.maxValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(minValue, maxValue);
        }

    }

    private final Map<Value, I32ValueRange> rangeBuffer = new HashMap<>();

    private I32ValueRangeAnalyzer() {
    }

    private boolean hasValueRangeSolved(Value value) {
        if (value instanceof Constant) {
            rangeBuffer.put(value, I32ValueRange.of(value));
        }
        return rangeBuffer.containsKey(value);
    }

    public I32ValueRange getValueRange(Value value) {
        if (value instanceof Constant) {
            return I32ValueRange.of(value);
        }
        return rangeBuffer.get(value);
    }

    private void setValueRange(Value value, I32ValueRange valueRange) {
        rangeBuffer.put(value, valueRange);
    }

    public static I32ValueRangeAnalyzer analysis(Function function) {
        var analyzer = new I32ValueRangeAnalyzer();
        Queue<Instruction> updateQueue = new LinkedList<>();
        // 先全部求解一遍初始范围，之后的 I32ValueRange.of 会直接使用缓存的结果，不会递归分析
        var domTree = DomTree.buildOver(function);
        var dfsDomTree = new Consumer<BasicBlock>() {
            @Override
            public void accept(BasicBlock basicBlock) {
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (instruction.getType() == IntegerType.getI32()) {
                        I32ValueRange.of(instruction, analyzer);
                        for (Operand usage : instruction.getUsages()) {
                            if (usage.getInstruction().getType() == IntegerType.getI32()) {
                                updateQueue.add(usage.getInstruction());
                            }
                        }
                    }
                }
                for (BasicBlock domSon : domTree.getDomSons(basicBlock)) {
                    accept(domSon);
                }
            }
        };
        for (Function.FormalParameter formalParameter : function.getFormalParameters()) {
            if (formalParameter.getType() == IntegerType.getI32()) {
                I32ValueRange.of(formalParameter, analyzer);
            }
        }
        dfsDomTree.accept(domTree.getDomRoot());
        // 手动迭代更新未收敛的值
        int updateCount = 0;
        while (!updateQueue.isEmpty()) {
            var instruction = updateQueue.remove();
            I32ValueRange oldRange = analyzer.getValueRange(instruction);
            I32ValueRange newRange;
            if (updateCount < HURRY_UP_THRESHOLD_INITIAL) {
                updateCount++;
                if (updateCount < NEARLY_HURRY_THRESHOLD_INITIAL + (int) (Math.random() * (HURRY_UP_THRESHOLD_INITIAL - NEARLY_HURRY_THRESHOLD_INITIAL))) {
                    newRange = I32ValueRange.calculateI32ValueRange(instruction, analyzer);
                } else {
                    newRange = getHurryUpRange(instruction, oldRange);
                }
            } else {
                newRange = getHurryUpRange(instruction, oldRange);
            }
            if (!Objects.equals(oldRange, newRange)) {
                analyzer.setValueRange(instruction, newRange);
                for (Operand usage : instruction.getUsages()) {
                    if (usage.getInstruction().getType() == IntegerType.getI32()) {
                        updateQueue.add(usage.getInstruction());
                    }
                }
            }
        }
        return analyzer;
    }

    public void onInstructionUpdated(Instruction updatedInstruction) {
        Queue<Instruction> updateQueue = new LinkedList<>();
        updateQueue.add(updatedInstruction);
        // 手动迭代更新未收敛的值
        int updateCount = 0;
        while (!updateQueue.isEmpty()) {
            var instruction = updateQueue.remove();
            I32ValueRange oldRange = this.getValueRange(instruction);
            I32ValueRange newRange;
            if (updateCount < HURRY_UP_THRESHOLD_UPDATE) {
                updateCount++;
                if (updateCount < NEARLY_HURRY_THRESHOLD_UPDATE + (int) (Math.random() * (HURRY_UP_THRESHOLD_UPDATE - NEARLY_HURRY_THRESHOLD_UPDATE))) {
                    newRange = I32ValueRange.calculateI32ValueRange(instruction, this);
                } else {
                    newRange = getHurryUpRange(instruction, oldRange);
                }
            } else {
                newRange = getHurryUpRange(instruction, oldRange);
            }
            if (!Objects.equals(oldRange, newRange)) {
                this.setValueRange(instruction, newRange);
                for (Operand usage : instruction.getUsages()) {
                    if (usage.getInstruction().getType() == IntegerType.getI32()) {
                        updateQueue.add(usage.getInstruction());
                    }
                }
            }
        }
    }

    private static I32ValueRange getHurryUpRange(Instruction instruction, I32ValueRange oldRange) {
        I32ValueRange newRange;
        newRange = new I32ValueRange(
                oldRange.minValue == 0 ? 0 : (oldRange.minValue > 0 ? 1 : Integer.MIN_VALUE),
                oldRange.maxValue == 0 ? 0 : (oldRange.maxValue > 0 ? Integer.MAX_VALUE : -1)
        );
        //instruction.setComment(String.format("(Hurry Up)[%d, %d]", newRange.minValue, newRange.maxValue));
        return newRange;
    }

}
