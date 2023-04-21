package com.bit.newnewcc.ir.optimize.pass;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IllegalArgumentException;
import com.bit.newnewcc.ir.exception.ValueBeingUsedException;
import com.bit.newnewcc.ir.value.*;
import com.bit.newnewcc.ir.value.instruction.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * 消除 ir 中的无用代码 <br>
 * 此Pass的功能是UnreachableCodeEliminationPass的超集 <br>
 */
/* 思路如下：
 *
 * 定义基本副作用指令为：
 * 1. 从函数入口块可达的存储地址为全局变量的store指令
 * 2. 从函数入口块可达的调用外部函数的指令
 *
 * 定义传递副作用指令为：
 * 1. 从函数入口块可达的call指令，且被调用的函数有副作用
 *
 * 定义函数有副作用，当且仅当：
 * 1. 其包含副作用指令
 *
 * 定义基本有效指令为：
 * 1. 从函数入口块可达的副作用指令
 * 2. 从函数入口块可达的存储地址为局部变量的store指令
 * 3. 从函数入口块可达的控制流指令
 *
 * 定义传递有效指令为：
 * 1. 指令的结果被作为有效指令的操作数
 *
 * 步骤如下：
 * 1. 求出所有副作用指令
 * 2. 求出所有有效指令
 * 3. 删除所有非有效指令
 *
 */
// 缩写：se = side effect
public class DeadCodeEliminationPass {

    private static Collection<Instruction> getSeInstructions(Module module) {
        Set<Instruction> reachableInstructions = new HashSet<>();
        Set<Instruction> seInstructions = new HashSet<>();
        Queue<Function> queue = new ArrayDeque<>();
        var addSeInstruction = new Consumer<Instruction>() {
            private final Set<Function> pushedFunctions = new HashSet<>();

            @Override
            public void accept(Instruction instruction) {
                seInstructions.add(instruction);
                var function = instruction.getBasicBlock().getFunction();
                if (!pushedFunctions.contains(function)) {
                    pushedFunctions.add(function);
                    queue.add(function);
                }
            }
        };
        var searchBasicBlock = new Consumer<BasicBlock>() {
            private final Set<BasicBlock> visitedBlocks = new HashSet<>();

            private boolean isBasicSeInstruction(Instruction instruction) {
                // 1. 从函数入口块可达的存储地址为全局变量的store指令
                if (instruction instanceof StoreInst storeInst) {
                    if (isGlobalAddress(storeInst.getAddressOperand())) {
                        return true;
                    }
                }
                // 2. 从函数入口块可达的调用外部函数的指令
                if (instruction instanceof CallInst callInst) {
                    if (callInst.getCallee() instanceof ExternalFunction) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void accept(BasicBlock basicBlock) {
                if (visitedBlocks.contains(basicBlock)) return;
                visitedBlocks.add(basicBlock);
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (isBasicSeInstruction(instruction)) {
                        addSeInstruction.accept(instruction);
                    }
                    reachableInstructions.add(instruction);
                }
                for (BasicBlock exitBlock : basicBlock.getExitBlocks()) {
                    accept(exitBlock);
                }
            }
        };
        module.getFunctions().forEach(function -> searchBasicBlock.accept(function.getEntryBasicBlock()));
        while (!queue.isEmpty()) {
            var function = queue.remove();
            for (Operand operand : function.getUsages()) {
                if (reachableInstructions.contains(operand.getInstruction())) {
                    // 为什么要检查这么多呢？谁知道会出现什么奇怪的情况，还是限制死了不留坑
                    if (operand.getInstruction() instanceof CallInst callInst && callInst.getCallee() == function) {
                        addSeInstruction.accept(callInst);
                    }
                }
            }
        }
        return seInstructions;
    }

    private static Collection<Instruction> getValidInstructions(Module module, Collection<Instruction> seInstructions) {
        Set<Instruction> reachableInstructions = new HashSet<>();
        Set<Instruction> validInstructions = new HashSet<>();
        Queue<Instruction> queue = new ArrayDeque<>();
        Consumer<Instruction> addValidInstruction = instruction -> {
            if (!validInstructions.contains(instruction)) {
                validInstructions.add(instruction);
                queue.add(instruction);
            }
        };
        var searchBasicBlock = new Consumer<BasicBlock>() {
            private final Set<BasicBlock> visitedBlocks = new HashSet<>();

            private boolean isBasicValidInstruction(Instruction instruction) {
                // 1. 从函数入口块可达的副作用指令
                if (seInstructions.contains(instruction)) {
                    return true;
                }
                // 2. 从函数入口块可达的存储地址为局部变量的store指令
                if (instruction instanceof StoreInst storeInst) {
                    var address = storeInst.getAddressOperand();
                    if (!isGlobalAddress(address)) {
                        return true;
                    }
                }
                // 3. 从函数入口块可达的控制流指令
                if (instruction instanceof TerminateInst) {
                    return true;
                }
                return false;
            }

            @Override
            public void accept(BasicBlock basicBlock) {
                if (visitedBlocks.contains(basicBlock)) return;
                visitedBlocks.add(basicBlock);
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (isBasicValidInstruction(instruction)) {
                        addValidInstruction.accept(instruction);
                    }
                    reachableInstructions.add(instruction);
                }
                for (BasicBlock exitBlock : basicBlock.getExitBlocks()) {
                    accept(exitBlock);
                }
            }
        };
        module.getFunctions().forEach(function -> searchBasicBlock.accept(function.getEntryBasicBlock()));
        while (!queue.isEmpty()) {
            var instruction = queue.remove();
            for (Operand operand : instruction.getOperandList()) {
                if (operand.getValue() instanceof Instruction instruction1) {
                    if (reachableInstructions.contains(instruction1)) {
                        addValidInstruction.accept(instruction1);
                    }
                }
            }
        }
        return validInstructions;
    }

    private static void removeInvalidInstruction(Module module, Collection<Instruction> validInstructions) {
        var invalidInstructions = new ArrayList<Instruction>();
        var invalidBasicBlocks = new ArrayList<BasicBlock>();
        // 获取以上两个数组
        for (Function function : module.getFunctions()) {
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (!validInstructions.contains(instruction)) {
                        invalidInstructions.add(instruction);
                        // 若控制流指令无效，说明这个基本块不可达
                        if (instruction instanceof TerminateInst) {
                            invalidBasicBlocks.add(instruction.getBasicBlock());
                        }
                    }
                }
            }
        }
        // 清理基本块的出口信息
        for (BasicBlock invalidBasicBlock : invalidBasicBlocks) {
            for (BasicBlock exitBlock : invalidBasicBlock.getExitBlocks()) {
                for (Instruction leadingInstruction : exitBlock.getLeadingInstructions()) {
                    if (leadingInstruction instanceof PhiInst phiInst) {
                        phiInst.removeEntry(invalidBasicBlock);
                    }
                }
            }
            invalidBasicBlock.setTerminateInstruction(new UnreachableInst());
        }
        // 清理操作数
        for (Instruction invalidInstruction : invalidInstructions) {
            invalidInstruction.clearOperands();
        }
        // 检查块和值是否依然在被引用
        for (BasicBlock invalidBasicBlock : invalidBasicBlocks) {
            if (invalidBasicBlock.getUsages().size() > 0) {
                throw new ValueBeingUsedException();
            }
        }
        for (Instruction invalidInstruction : invalidInstructions) {
            if (invalidInstruction.getUsages().size() > 0) {
                throw new ValueBeingUsedException();
            }
        }
        // 移除无效值和无效块
        for (BasicBlock invalidBasicBlock : invalidBasicBlocks) {
            invalidBasicBlock.removeFromFunction();
        }
        for (Instruction invalidInstruction : invalidInstructions) {
            invalidInstruction.waste();
        }
    }

    private static boolean isGlobalAddress(Value address) {
        boolean answer;
        if (address instanceof GlobalVariable || address instanceof Function.FormalParameter) {
            answer = true;
        } else if (address instanceof AllocateInst) {
            answer = false;
        } else if (address instanceof GetElementPtrInst getElementPtrInst) {
            answer = isGlobalAddress(getElementPtrInst.getRootOperand());
        } else {
            throw new IllegalArgumentException(String.format("Cannot analysis variable of type %s.", address.getClass()));
        }
        return answer;
    }

    public static void optimize(Module module) {
        var seInstructions = getSeInstructions(module);
        var validInstructions = getValidInstructions(module, seInstructions);
        //todo: 删除所有非有效指令
    }

}
