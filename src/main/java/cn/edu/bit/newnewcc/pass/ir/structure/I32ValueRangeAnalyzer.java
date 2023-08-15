package cn.edu.bit.newnewcc.pass.ir.structure;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.CompilationProcessCheckFailedException;
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

    /**
     * 当 (i32值数量) * (基本块数量) 不超过以下数值时，启用基于基本块的分析，得到更精确的分析结果
     */
    private static final int BLOCK_BASED_ANALYSIS_THRESHOLD = 100000;

    private static final double NEARLY_HURRY_PERCENTAGE = 0.5;

    // 当迭代次数过多时，自动进入 hurry up 模式
    // 在 hurry up 模式下，只关心变量的正负
    private static final int HURRY_UP_THRESHOLD_INITIAL = 100000;
    private static final int NEARLY_HURRY_THRESHOLD_INITIAL = (int) (HURRY_UP_THRESHOLD_INITIAL * NEARLY_HURRY_PERCENTAGE);

    private static final int HURRY_UP_THRESHOLD_UPDATE = 1000;
    private static final int NEARLY_HURRY_THRESHOLD_UPDATE = (int) (HURRY_UP_THRESHOLD_UPDATE * NEARLY_HURRY_PERCENTAGE);

    public record I32ValueRange(int minValue, int maxValue) {

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

        private static I32ValueRange of(Value value, BasicBlock block, I32ValueRangeAnalyzer helper) {
            if (!(value.getType() == IntegerType.getI32())) {
                throw new IllegalArgumentException();
            }
            if (helper != null && helper.hasValueRangeSolved(value)) {
                return helper.getValueRangeAtBlock(value, block);
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
                I32ValueRange range1 = I32ValueRange.of(integerArithmeticInst.getOperand1(), integerArithmeticInst.getBasicBlock(), helper);
                I32ValueRange range2 = I32ValueRange.of(integerArithmeticInst.getOperand2(), integerArithmeticInst.getBasicBlock(), helper);
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
                for (BasicBlock entryBlock : phiInst.getEntrySet()) {
                    var entryValue = phiInst.getValue(entryBlock);
                    if (helper != null && helper.hasValueRangeSolved(entryValue)) {
                        I32ValueRange entryRange = getEntryRange(helper, phiInst.getBasicBlock(), entryBlock, entryValue);
                        minValue = min(minValue, entryRange.minValue);
                        maxValue = max(maxValue, entryRange.maxValue);
                    } else {
                        minValue = Integer.MIN_VALUE;
                        maxValue = Integer.MAX_VALUE;
                    }
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

        public static I32ValueRange getEntryRange(I32ValueRangeAnalyzer helper, BasicBlock currentBlock, BasicBlock entryBlock, Value entryValue) {
            if (helper.isBlockBasedAnalysisEnable) {
                if (entryBlock.getTerminateInstruction() instanceof BranchInst branchInst) {
                    if (branchInst.getCondition() instanceof IntegerCompareInst integerCompareInst) {
                        if (integerCompareInst.getOperand1() == entryValue) {
                            if (currentBlock == branchInst.getTrueExit()) {
                                return getTrueRange(
                                        integerCompareInst.getCondition(),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand1(), entryBlock),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand2(), entryBlock)
                                );
                            } else if (currentBlock == branchInst.getFalseExit()) {
                                return getFalseRange(
                                        integerCompareInst.getCondition(),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand1(), entryBlock),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand2(), entryBlock)
                                );
                            } else {
                                throw new CompilationProcessCheckFailedException();
                            }
                        } else if (integerCompareInst.getOperand2() == entryValue) {
                            if (currentBlock == branchInst.getTrueExit()) {
                                return getTrueRange(
                                        integerCompareInst.getCondition().swap(),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand2(), entryBlock),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand1(), entryBlock)
                                );
                            } else if (currentBlock == branchInst.getFalseExit()) {
                                return getFalseRange(
                                        integerCompareInst.getCondition().swap(),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand2(), entryBlock),
                                        helper.getValueRangeAtBlock(integerCompareInst.getOperand1(), entryBlock)
                                );
                            } else {
                                throw new CompilationProcessCheckFailedException();
                            }
                        }
                    }
                }
                return helper.getValueRangeAtBlock(entryValue, entryBlock);
            } else {
                return helper.getValueRange(entryValue);
            }
        }

        public static I32ValueRange IMPOSSIBLE = new I32ValueRange(Integer.MAX_VALUE, Integer.MIN_VALUE);
        public static I32ValueRange ANY = new I32ValueRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

        /**
         * 获取两个区间按condition比较时，结果为 true 情况下左侧区间可能的范围
         *
         * @param condition 比较条件
         * @param range1    左侧区间
         * @param range2    右侧区间
         * @return 比较结果为 true 情况下左侧区间可能的范围
         */
        public static I32ValueRange getTrueRange(IntegerCompareInst.Condition condition, I32ValueRange range1, I32ValueRange range2) {
            try {
                return switch (condition) {
                    case EQ -> {
                        yield new I32ValueRange(max(range1.minValue, range2.minValue), min(range1.maxValue, range2.maxValue));
                    }
                    case NE -> {
                        if (range2.minValue == range2.maxValue) {
                            if (range1.minValue == range2.maxValue) {
                                yield new I32ValueRange(addExact(range1.minValue, 1), range1.maxValue);
                            }
                            if (range1.maxValue == range2.maxValue) {
                                yield new I32ValueRange(range1.minValue, subtractExact(range1.maxValue, 1));
                            }
                        }
                        yield range1;
                    }
                    case SLT -> {
                        yield new I32ValueRange(range1.minValue, min(range1.maxValue, subtractExact(range2.maxValue, 1)));
                    }
                    case SLE -> {
                        yield new I32ValueRange(range1.minValue, min(range1.maxValue, range2.maxValue));
                    }
                    case SGT -> {
                        yield new I32ValueRange(max(range1.minValue, addExact(range2.minValue, 1)), range1.maxValue);
                    }
                    case SGE -> {
                        yield new I32ValueRange(max(range1.minValue, range2.minValue), range1.maxValue);
                    }
                };
            } catch (ArithmeticException e) {
                return IMPOSSIBLE;
            }
        }

        /**
         * 获取两个区间按condition比较时，结果为 false 情况下左侧区间可能的范围
         *
         * @param condition 比较条件
         * @param range1    左侧区间
         * @param range2    右侧区间
         * @return 比较结果为 false 情况下左侧区间可能的范围
         */
        public static I32ValueRange getFalseRange(IntegerCompareInst.Condition condition, I32ValueRange range1, I32ValueRange range2) {
            return getTrueRange(condition.not(), range1, range2);
        }

    }

    private static class RangeBuffer extends HashMap<Value, I32ValueRange> {
        @Override
        public I32ValueRange get(Object key) {
            if (key instanceof ConstInt constInt) {
                return new I32ValueRange(constInt.getValue(), constInt.getValue());
            }
            return super.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            if (key instanceof ConstInt) return true;
            return super.containsKey(key);
        }
    }

    private final RangeBuffer rangeBuffer = new RangeBuffer();

    private final boolean isBlockBasedAnalysisEnable;
    private final Map<BasicBlock, RangeBuffer> blockBufferMap = new HashMap<>();


    private I32ValueRangeAnalyzer(boolean isBlockBasedAnalysisEnable) {
        this.isBlockBasedAnalysisEnable = isBlockBasedAnalysisEnable;
    }

    private boolean hasValueRangeSolved(Value value) {
        return rangeBuffer.containsKey(value);
    }

    public I32ValueRange getValueRange(Value value) {
        if (value instanceof ConstInt) {
            return I32ValueRange.of(value);
        }
        if (rangeBuffer.containsKey(value)) {
            return rangeBuffer.get(value);
        } else {
            return I32ValueRange.ANY;
        }
    }

    public I32ValueRange getValueRangeAtBlock(Value value, BasicBlock block) {
        if (!isBlockBasedAnalysisEnable) {
            return getValueRange(value);
        } else {
            if (blockBufferMap.get(block).containsKey(value)) {
                return blockBufferMap.get(block).get(value);
            } else {
                return getValueRange(value);
            }
        }
    }

    private void setValueRange(Value value, I32ValueRange valueRange) {
        rangeBuffer.put(value, valueRange);
        if (isBlockBasedAnalysisEnable) {
            if (value instanceof Instruction instruction) {
                blockBufferMap.get(instruction.getBasicBlock()).put(value, valueRange);
            } else if (value instanceof Function.FormalParameter formalParameter) {
                blockBufferMap.get(formalParameter.getFunction().getEntryBasicBlock()).put(value, valueRange);
            }
        }
    }

    private static boolean judgeBlockBasedAnalysisEnable(Function function) {
        int instructionCount = 0;
        int blockCount = 0;
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            blockCount++;
            for (Instruction instruction : basicBlock.getInstructions()) {
                if (instruction.getType() == IntegerType.getI32()) {
                    instructionCount++;
                }
            }
        }
        try {
            return Math.multiplyExact(instructionCount, blockCount) <= BLOCK_BASED_ANALYSIS_THRESHOLD;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    public static I32ValueRangeAnalyzer analysis(Function function) {
        boolean blockBasedAnalysisEnable = judgeBlockBasedAnalysisEnable(function);
        var analyzer = new I32ValueRangeAnalyzer(blockBasedAnalysisEnable);
        if (blockBasedAnalysisEnable) {
            analysisOnBlock(function, analyzer);
        } else {
            analysisGlobally(function, analyzer);
        }
        return analyzer;
    }

    private static void analysisOnBlock(Function function, I32ValueRangeAnalyzer analyzer) {
        // 为每个基本块添加一个 buffer
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            analyzer.blockBufferMap.put(basicBlock, new RangeBuffer());
        }
        // 使用队列维护待更新范围的指令
        Queue<Instruction> updateQueue = new LinkedList<>() {
            private final Set<Instruction> updateQueueElement = new HashSet<>();

            @Override
            public boolean add(Instruction instruction) {
                if (updateQueueElement.contains(instruction)) return false;
                return super.add(instruction);
            }

            @Override
            public Instruction remove() {
                var result = super.remove();
                updateQueueElement.remove(result);
                return result;
            }

        };
        // 某条指令的范围被更新时，触发的事件
        var onValueUpdated = new Consumer<Instruction>() {
            @Override
            public void accept(Instruction instruction) {
                for (Operand usage : instruction.getUsages()) {
                    if (usage.getInstruction().getType() == IntegerType.getI32()) {
                        updateQueue.add(usage.getInstruction());
                    } else if (usage.getInstruction() instanceof IntegerCompareInst integerCompareInst) {
                        if (integerCompareInst.getOperand1() instanceof Instruction instruction1 && instruction1 != instruction) {
                            updateQueue.add(instruction1);
                        }
                        if (integerCompareInst.getOperand2() instanceof Instruction instruction2 && instruction2 != instruction) {
                            updateQueue.add(instruction2);
                        }
                    }
                }
            }
        };
        // 先全部求解一遍初始范围，之后的 I32ValueRange.of 会直接使用缓存的结果，不会递归分析
        var domTree = DomTree.buildOver(function);
        var dfsDomTree = new Consumer<BasicBlock>() {
            @Override
            public void accept(BasicBlock basicBlock) {
                var currentBuffer = analyzer.blockBufferMap.get(basicBlock);
                if (domTree.getDomFather(basicBlock) != null) {
                    var domFather = domTree.getDomFather(basicBlock);
                    for (Value value : analyzer.blockBufferMap.get(domFather).keySet()) {
                        var minValue = Integer.MAX_VALUE;
                        var maxValue = Integer.MIN_VALUE;
                        for (BasicBlock entryBlock : basicBlock.getEntryBlocks()) {
                            var range = I32ValueRange.getEntryRange(analyzer, basicBlock, entryBlock, value);
                            minValue = min(minValue, range.minValue);
                            maxValue = max(maxValue, range.maxValue);
                        }
                        currentBuffer.put(value, new I32ValueRange(minValue, maxValue));
                    }
                }
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (instruction.getType() == IntegerType.getI32()) {
                        I32ValueRange.of(instruction, analyzer);
                        onValueUpdated.accept(instruction);
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
                    newRange = getHurryUpRange(instruction, analyzer);
                }
            } else {
                newRange = getHurryUpRange(instruction, analyzer);
            }
            if (!Objects.equals(oldRange, newRange)) {
                analyzer.setValueRange(instruction, newRange);
                onValueUpdated.accept(instruction);
                // 此处只需要更新 block value range ，相关的值在 onValueUpdated 中已经被加入队列
                var recalculateValueRange = new Consumer<BasicBlock>() {
                    @Override
                    public void accept(BasicBlock basicBlock) {
                        if (basicBlock != instruction.getBasicBlock()) {
                            var minValue = Integer.MAX_VALUE;
                            var maxValue = Integer.MIN_VALUE;
                            for (BasicBlock entryBlock : basicBlock.getEntryBlocks()) {
                                var range = I32ValueRange.getEntryRange(analyzer, basicBlock, entryBlock, instruction);
                                minValue = min(minValue, range.minValue);
                                maxValue = max(maxValue, range.maxValue);
                            }
                            analyzer.blockBufferMap.get(basicBlock).put(instruction, new I32ValueRange(minValue, maxValue));
                        }
                        var currentBuffer = analyzer.blockBufferMap.get(basicBlock);
                        var range = currentBuffer.get(instruction);
                        if (basicBlock.getTerminateInstruction() instanceof BranchInst branchInst &&
                                branchInst.getCondition() instanceof IntegerCompareInst integerCompareInst) {
                        } else {
                            for (BasicBlock domSon : domTree.getDomSons(basicBlock)) {
                                analyzer.blockBufferMap.get(domSon).put(instruction, range);
                            }
                        }
                        for (BasicBlock domSon : domTree.getDomSons(basicBlock)) {
                            accept(domSon);
                        }
                    }
                };
                recalculateValueRange.accept(instruction.getBasicBlock());
            }
        }
    }

    private static I32ValueRange getSonRangeTrue(IntegerCompareInst integerCompareInst, Value value, RangeBuffer currentBuffer) {
        I32ValueRange sonRangeTrue;
        if (integerCompareInst.getOperand1() == value) {
            sonRangeTrue = I32ValueRange.getTrueRange(
                    integerCompareInst.getCondition(),
                    currentBuffer.get(integerCompareInst.getOperand1()),
                    currentBuffer.get(integerCompareInst.getOperand2())
            );
        } else if (integerCompareInst.getOperand2() == value) {
            sonRangeTrue = I32ValueRange.getTrueRange(
                    integerCompareInst.getCondition().swap(),
                    currentBuffer.get(integerCompareInst.getOperand2()),
                    currentBuffer.get(integerCompareInst.getOperand1())
            );
        } else {
            sonRangeTrue = currentBuffer.get(value);
        }
        return sonRangeTrue;
    }

    private static I32ValueRange getSonRangeFalse(IntegerCompareInst integerCompareInst, Value value, RangeBuffer currentBuffer) {
        I32ValueRange sonRangeFalse;
        if (integerCompareInst.getOperand1() == value) {
            sonRangeFalse = I32ValueRange.getFalseRange(
                    integerCompareInst.getCondition(),
                    currentBuffer.get(integerCompareInst.getOperand1()),
                    currentBuffer.get(integerCompareInst.getOperand2())
            );
        } else if (integerCompareInst.getOperand2() == value) {
            sonRangeFalse = I32ValueRange.getFalseRange(
                    integerCompareInst.getCondition().swap(),
                    currentBuffer.get(integerCompareInst.getOperand2()),
                    currentBuffer.get(integerCompareInst.getOperand1())
            );
        } else {
            sonRangeFalse = currentBuffer.get(value);
        }
        return sonRangeFalse;
    }

    private static void analysisGlobally(Function function, I32ValueRangeAnalyzer analyzer) {
        Queue<Instruction> updateQueue = new LinkedList<>() {
            private final Set<Instruction> updateQueueElement = new HashSet<>();

            @Override
            public boolean add(Instruction instruction) {
                if (updateQueueElement.contains(instruction)) return false;
                return super.add(instruction);
            }

            @Override
            public Instruction remove() {
                var result = super.remove();
                updateQueueElement.remove(result);
                return result;
            }

        };
        // 先全部求解一遍初始范围，之后的 I32ValueRange.of 会直接使用缓存的结果，不会递归分析
        var domTree = DomTree.buildOver(function);
        var onValueUpdated = new Consumer<Instruction>() {
            @Override
            public void accept(Instruction instruction) {
                for (Operand usage : instruction.getUsages()) {
                    if (usage.getInstruction().getType() == IntegerType.getI32()) {
                        updateQueue.add(usage.getInstruction());
                    }
                }
            }
        };
        var dfsDomTree = new Consumer<BasicBlock>() {
            @Override
            public void accept(BasicBlock basicBlock) {
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (instruction.getType() == IntegerType.getI32()) {
                        I32ValueRange.of(instruction, analyzer);
                        onValueUpdated.accept(instruction);
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
                    newRange = getHurryUpRange(instruction, analyzer);
                }
            } else {
                newRange = getHurryUpRange(instruction, analyzer);
            }
            if (!Objects.equals(oldRange, newRange)) {
                analyzer.setValueRange(instruction, newRange);
                onValueUpdated.accept(instruction);
            }
        }
    }

    public void onInstructionUpdated(Instruction updatedInstruction) {
        throw new CompilationProcessCheckFailedException("Method discarded");
//        Queue<Instruction> updateQueue = new LinkedList<>();
//        updateQueue.add(updatedInstruction);
//        // 手动迭代更新未收敛的值
//        // todo: 适配启用了基本块级分析的方法
//        int updateCount = 0;
//        while (!updateQueue.isEmpty()) {
//            var instruction = updateQueue.remove();
//            I32ValueRange oldRange = this.getValueRange(instruction);
//            I32ValueRange newRange;
//            if (updateCount < HURRY_UP_THRESHOLD_UPDATE) {
//                updateCount++;
//                if (updateCount < NEARLY_HURRY_THRESHOLD_UPDATE + (int) (Math.random() * (HURRY_UP_THRESHOLD_UPDATE - NEARLY_HURRY_THRESHOLD_UPDATE))) {
//                    newRange = I32ValueRange.calculateI32ValueRange(instruction, this);
//                } else {
//                    newRange = getHurryUpRange(instruction, oldRange);
//                }
//            } else {
//                newRange = getHurryUpRange(instruction, oldRange);
//            }
//            if (!Objects.equals(oldRange, newRange)) {
//                this.setValueRange(instruction, newRange);
//                for (Operand usage : instruction.getUsages()) {
//                    if (usage.getInstruction().getType() == IntegerType.getI32()) {
//                        updateQueue.add(usage.getInstruction());
//                    }
//                }
//            }
//        }
    }

    private static I32ValueRange getHurryUpRange(Instruction instruction, I32ValueRangeAnalyzer analyzer) {
        I32ValueRange newRange = I32ValueRange.calculateI32ValueRange(instruction, analyzer);
        newRange = new I32ValueRange(
                newRange.minValue == 0 ? 0 : (newRange.minValue > 0 ? 1 : Integer.MIN_VALUE),
                newRange.maxValue == 0 ? 0 : (newRange.maxValue > 0 ? Integer.MAX_VALUE : -1)
        );
        //instruction.setComment(String.format("(Hurry Up)[%d, %d]", newRange.minValue, newRange.maxValue));
        return newRange;
    }

}
