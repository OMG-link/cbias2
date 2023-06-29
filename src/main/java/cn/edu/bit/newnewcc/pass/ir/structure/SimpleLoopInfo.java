package cn.edu.bit.newnewcc.pass.ir.structure;

import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IllegalStateException;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 简单循环结构，仅支持整数的加法和减法作为循环变量的循环
 *
 * @param condition    循环的条件语句
 * @param stepInst     循环的步进语句
 * @param initialValue 循环变量的初值
 */
public record SimpleLoopInfo(IntegerCompareInst condition, IntegerArithmeticInst stepInst, Value initialValue) {
    private static class Builder {

        private static class BuildFailedException extends Exception {
        }

        private final BasicBlock loopHeadBlock;
        private final Set<BasicBlock> inLoopBlocks = new HashSet<>();
        private final Set<Instruction> inLoopInstructions = new HashSet<>();
        private final SimpleLoopInfo simpleLoopInfo;

        public Builder(Loop loop) {
            this.loopHeadBlock = loop.getHeaderBasicBlock();
            searchLoop(loop);
            simpleLoopInfo = tryCreateSimpleLoopInfo();
        }

        public SimpleLoopInfo getSimpleLoopInfo() {
            return simpleLoopInfo;
        }

        private void searchLoop(Loop loop) {
            inLoopBlocks.addAll(loop.getBasicBlocks());
            for (BasicBlock basicBlock : loop.getBasicBlocks()) {
                inLoopInstructions.addAll(basicBlock.getInstructions());
            }
            for (Loop subLoop : loop.getSubLoops()) {
                searchLoop(subLoop);
            }
        }

        private Value initialValue, exitLoopValue;

        private SimpleLoopInfo tryCreateSimpleLoopInfo() {
            try {
                IntegerCompareInst exitCondition = getExitCondition();
                if (!(exitCondition.getOperand1() instanceof PhiInst phiInst)) throw new BuildFailedException();
                analysisPhi(phiInst);
                IntegerArithmeticInst stepInstruction = getStepInstruction(exitLoopValue);
                if (stepInstruction.getOperand1() != phiInst) throw new BuildFailedException();
                return new SimpleLoopInfo(exitCondition, stepInstruction, initialValue);
            } catch (BuildFailedException e) {
                return null;
            }
        }

        private void analysisPhi(PhiInst phiInst) throws BuildFailedException {
            if (phiInst.getBasicBlock() != loopHeadBlock) throw new BuildFailedException();
            Value initialValue = null;
            Value exitLoopValue = null;
            for (BasicBlock block : phiInst.getEntrySet()) {
                var value = phiInst.getValue(block);
                // 对于来自循环内的入边，取值应当为步进后的值；否则，应当为初值
                if (inLoopBlocks.contains(block)) {
                    if (!(value instanceof Instruction && inLoopInstructions.contains(value))) {
                        throw new BuildFailedException();
                    }
                    if (exitLoopValue == null) {
                        exitLoopValue = value;
                    } else {
                        if (value != exitLoopValue) throw new BuildFailedException();
                    }
                } else {
                    if (initialValue == null) {
                        initialValue = value;
                    } else {
                        if (value != initialValue) throw new BuildFailedException();
                    }
                }
            }
            if (initialValue == null) throw new BuildFailedException();
            if (exitLoopValue == null) throw new BuildFailedException();
            this.initialValue = initialValue;
            this.exitLoopValue = exitLoopValue;
        }

        /**
         * 获取循环的退出条件语句 <br>
         * 如果有多个可能的退出条件，则返回null <br>
         *
         * @return 循环的唯一退出条件语句，或是null
         */
        private IntegerCompareInst getExitCondition() throws BuildFailedException {
            // 获取循环退出的条件值
            Value conditionValue = null;
            for (BasicBlock inLoopBlock : inLoopBlocks) {
                var terminateInst = inLoopBlock.getTerminateInstruction();
                if (terminateInst instanceof BranchInst branchInst) {
                    for (BasicBlock exit : branchInst.getExits()) {
                        if (inLoopBlocks.contains(exit)) continue;
                        if (conditionValue == null) {
                            conditionValue = branchInst.getCondition();
                        } else {
                            // 多出口，不是简单循环
                            throw new BuildFailedException();
                        }
                    }
                } else if (terminateInst instanceof JumpInst jumpInst) {
                    assert inLoopBlocks.contains(jumpInst.getExit());
                } else {
                    // 未处理的结束语句
                    // 例如 ret, unreachable 等，他们都不该出现在循环中
                    throw new IllegalStateException();
                }
            }
            // 仅支持整数加法或减法的循环
            if (!(conditionValue instanceof IntegerCompareInst integerCompareInst)) {
                throw new BuildFailedException();
            }

            // 标准化比较语句，动态变量应当放置在左侧
            var op1 = integerCompareInst.getOperand1();
            var op2 = integerCompareInst.getOperand2();
            boolean isOp1Dynamic = op1 instanceof Instruction && inLoopInstructions.contains((Instruction) op1);
            boolean isOp2Dynamic = op2 instanceof Instruction && inLoopInstructions.contains((Instruction) op2);
            // 两侧都是常量或都是动态量，不属于简单循环
            if (isOp1Dynamic == isOp2Dynamic) {
                throw new BuildFailedException();
            }
            // 标准化的比较语句要求左侧是动态量，右侧是常量
            // 只需要检测op1是动态的，因为op1与op2不同
            if (isOp1Dynamic) {
                return integerCompareInst;
            } else {
                // 如果放反了，新建一个指令替换之
                var newCmpInstruction = new IntegerCompareInst(
                        integerCompareInst.getComparedType(),
                        integerCompareInst.getCondition().swap(),
                        op2, op1
                );
                integerCompareInst.replaceInstructionTo(newCmpInstruction);
                return newCmpInstruction;
            }

        }

        private IntegerArithmeticInst getStepInstruction(Value exitLoopValue) throws BuildFailedException {
            // 仅支持整数加或减的循环
            if (!(exitLoopValue instanceof IntegerAddInst || exitLoopValue instanceof IntegerSubInst)) {
                throw new BuildFailedException();
            }
            var stepInstruction = (IntegerArithmeticInst) exitLoopValue;
            // 标准化步进语句，动态变量应当放置在左侧
            var op1 = stepInstruction.getOperand1();
            var op2 = stepInstruction.getOperand2();
            boolean isOp1Dynamic = op1 instanceof Instruction && inLoopInstructions.contains((Instruction) op1);
            boolean isOp2Dynamic = op2 instanceof Instruction && inLoopInstructions.contains((Instruction) op2);
            // 两侧都是常量或都是动态量，不属于简单循环
            if (isOp1Dynamic == isOp2Dynamic) {
                throw new BuildFailedException();
            }
            // 标准化的比较语句要求左侧是动态量，右侧是常量
            // 只需要检测op1是动态的，因为op1与op2不同
            if (isOp1Dynamic) {
                return stepInstruction;
            } else {
                // 如果放反了，新建一个指令替换之
                IntegerArithmeticInst newStepInstruction;
                if (stepInstruction instanceof IntegerAddInst) {
                    newStepInstruction = new IntegerAddInst(stepInstruction.getType(), op2, op1);
                } else {
                    newStepInstruction = new IntegerSubInst(stepInstruction.getType(), op2, op1);
                }
                stepInstruction.replaceInstructionTo(newStepInstruction);
                return newStepInstruction;
            }
        }

    }

    public static SimpleLoopInfo buildFrom(Loop loop) {
        return new Builder(loop).getSimpleLoopInfo();
    }

}
