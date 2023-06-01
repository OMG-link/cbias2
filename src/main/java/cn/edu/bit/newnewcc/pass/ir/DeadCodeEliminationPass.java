package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;
import cn.edu.bit.newnewcc.ir.exception.ValueBeingUsedException;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.util.*;

/**
 * 消除 ir 中的无用代码
 * <p>
 * 此Pass的功能是UnreachableCodeEliminationPass的超集
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

    /**
     * 用于高效分析地址是否为全局变量 <br>
     */
    // 思路如下：
    // 每当遇到一个待定的值，先向上搜索其所有可能的来源，并将所有值标记为待定（pending）
    // 若搜索到了全局源，则从全局源开始向下传播全局地址标记
    // 最后，没有被标记的待定值都是局部地址，因为其找不到任何全局源
    private static class AddressTypeCache {

        private final Map<Value, Boolean> addressTypeCache = new HashMap<>();
        private final Queue<Value> spreadingGlobalAddresses = new ArrayDeque<>();
        private final Set<Value> pendingAddresses = new HashSet<>();

        // 确定了address为全局地址
        private void addGlobalAddress(Value address) {
            if (addressTypeCache.containsKey(address)) {
                assert addressTypeCache.get(address);
                return;
            }
            addressTypeCache.put(address, true);
            pendingAddresses.remove(address);
            spreadingGlobalAddresses.add(address);
        }

        /**
         * 向上搜索地址源头的过程
         *
         * @param address 待搜索的地址
         */
        // 将所有搜索到的值定义为pending状态
        //
        // 若当前搜索到的是：
        // 确定的全局变量：调用addGlobalAddress
        // 确定的局部变量：什么也不做，全部搜索完毕后会将pending状态的所有值标记为局部变量
        // 由其他地址确定的语句：对其他地址调用judgeAddress
        private void judgeAddress(Value address) {
            if (addressTypeCache.containsKey(address)) return;
            if (pendingAddresses.contains(address)) return;
            pendingAddresses.add(address);
            if (address instanceof GlobalVariable || address instanceof Function.FormalParameter || address instanceof LoadInst) {
                addGlobalAddress(address);
            } else if (address instanceof AllocateInst) {
                // 是局部变量，什么也不做
            } else if (address instanceof GetElementPtrInst getElementPtrInst) {
                judgeAddress(getElementPtrInst.getRootOperand());
            } else if (address instanceof PhiInst phiInst) {
                phiInst.forEach((basicBlock, value) -> judgeAddress(value));
            } else {
                throw new IllegalArgumentException(String.format("Cannot analysis variable of type %s.", address.getClass()));
            }
        }

        public boolean isGlobalAddress(Value address) {
            if (!addressTypeCache.containsKey(address)) {
                judgeAddress(address);
                while (!spreadingGlobalAddresses.isEmpty()) {
                    var globalAddress = spreadingGlobalAddresses.remove();
                    // 向下传播全局地址标记
                    for (Operand usage : globalAddress.getUsages()) {
                        var userInstruction = usage.getInstruction();
                        // 传播到所有 返回值是地址 且 受全局地址影响 的语句上
                        if (userInstruction instanceof GetElementPtrInst || userInstruction instanceof PhiInst) {
                            addGlobalAddress(userInstruction);
                        }
                    }
                }
                for (Value pendingAddress : pendingAddresses) {
                    addressTypeCache.put(pendingAddress, false);
                }
                pendingAddresses.clear();
            }
            assert addressTypeCache.containsKey(address);
            assert spreadingGlobalAddresses.isEmpty();
            assert pendingAddresses.isEmpty();
            return addressTypeCache.get(address);
        }

    }

    AddressTypeCache addressTypeCache = new AddressTypeCache();

    /// 可达指令 & 副作用指令搜索

    private final Set<Instruction> reachableInstructions = new HashSet<>();
    private final Set<BasicBlock> reachableBlocks = new HashSet<>();
    private final Set<Instruction> seInstructions = new HashSet<>();
    private final Set<Function> seFunctions = new HashSet<>();
    Queue<Function> seFunctionQueue = new ArrayDeque<>();

    private boolean isBasicSeInstruction(Instruction instruction) {
        // 1. 从函数入口块可达的存储地址为全局变量的store指令
        if (instruction instanceof StoreInst storeInst) {
            if (addressTypeCache.isGlobalAddress(storeInst.getAddressOperand())) {
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

    private void addSeInstruction(Instruction instruction) {
        seInstructions.add(instruction);
        var function = instruction.getBasicBlock().getFunction();
        if (!seFunctions.contains(function)) {
            seFunctions.add(function);
            seFunctionQueue.add(function);
        }
    }

    private void dfsForReachableInstructions(BasicBlock basicBlock) {
        if (reachableBlocks.contains(basicBlock)) return;
        reachableBlocks.add(basicBlock);
        for (Instruction instruction : basicBlock.getInstructions()) {
            if (isBasicSeInstruction(instruction)) {
                addSeInstruction(instruction);
            }
            reachableInstructions.add(instruction);
        }
        for (BasicBlock exitBlock : basicBlock.getExitBlocks()) {
            dfsForReachableInstructions(exitBlock);
        }
    }

    private void findSeInstructions(Module module) {
        for (Function function : module.getFunctions()) {
            dfsForReachableInstructions(function.getEntryBasicBlock());
        }
        while (!seFunctionQueue.isEmpty()) {
            var function = seFunctionQueue.remove();
            for (Operand operand : function.getUsages()) {
                if (reachableInstructions.contains(operand.getInstruction())) {
                    // 为什么要检查这么多呢？谁知道会出现什么奇怪的情况，还是限制死了不留坑
                    if (operand.getInstruction() instanceof CallInst callInst && callInst.getCallee() == function) {
                        addSeInstruction(callInst);
                    }
                }
            }
        }
    }

    /// 有效指令搜索

    private final Set<Instruction> validInstructions = new HashSet<>();
    private final Queue<Instruction> validInstructionQueue = new ArrayDeque<>();

    private boolean isBasicValidInstruction(Instruction instruction) {
        // 1. 从函数入口块可达的副作用指令
        if (seInstructions.contains(instruction)) {
            return true;
        }
        // 2. 从函数入口块可达的存储地址为局部变量的store指令
        if (instruction instanceof StoreInst storeInst) {
            var address = storeInst.getAddressOperand();
            if (!addressTypeCache.isGlobalAddress(address)) {
                return true;
            }
        }
        // 3. 从函数入口块可达的控制流指令
        if (instruction instanceof TerminateInst) {
            return true;
        }
        return false;
    }

    private void addValidInstruction(Instruction instruction) {
        if (!validInstructions.contains(instruction)) {
            validInstructions.add(instruction);
            validInstructionQueue.add(instruction);
        }
    }

    private void findValidInstructions() {
        for (BasicBlock reachableBlock : reachableBlocks) {
            for (Instruction instruction : reachableBlock.getInstructions()) {
                if (isBasicValidInstruction(instruction)) {
                    addValidInstruction(instruction);
                }
            }
        }
        while (!validInstructionQueue.isEmpty()) {
            var validInstruction = validInstructionQueue.remove();
            for (Operand operand : validInstruction.getOperandList()) {
                // sValidInstruction = spread valid instruction
                if (operand.getValue() instanceof Instruction sValidInstruction && reachableInstructions.contains(sValidInstruction)) {
                    addValidInstruction(sValidInstruction);
                }
            }
        }
    }

    private boolean removeInvalidInstruction(Module module) {
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
                exitBlock.removeEntryFromPhi(invalidBasicBlock);
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
        return invalidInstructions.size() > 0 || invalidBasicBlocks.size() > 0;
    }

    private boolean runOnModule_(Module module) {
        findSeInstructions(module);
        findValidInstructions();
        return removeInvalidInstruction(module);
    }

    public static boolean runOnModule(Module module) {
        return new DeadCodeEliminationPass().runOnModule_(module);
    }

}
