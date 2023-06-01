package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.instruction.CallInst;
import cn.edu.bit.newnewcc.ir.value.instruction.JumpInst;
import cn.edu.bit.newnewcc.ir.value.instruction.PhiInst;
import cn.edu.bit.newnewcc.pass.ir.structure.FunctionClone;

import java.util.*;

public class FunctionInline {

    record FunctionProperty(int size, Set<Function> callees) {

    }

    private final Module module;
    private final Map<Function, FunctionProperty> propertyMap = new HashMap<>();

    private FunctionInline(Module module) {
        this.module = module;
    }

    /**
     * 收集相关信息
     */
    private void collectInformation() {
        for (Function function : module.getFunctions()) {
            Set<Function> callees = new HashSet<>();
            int size = 0;
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                for (Instruction mainInstruction : basicBlock.getMainInstructions()) {
                    if (mainInstruction instanceof CallInst callInst) {
                        if (callInst.getCallee() instanceof Function callee) {
                            callees.add(callee);
                        }
                    }
                    size++;
                }
            }
            propertyMap.put(function, new FunctionProperty(size, callees));
        }
    }

    private void inlineFunction(Function function) {
        for (Operand usage : function.getUsages()) {
            if (!(usage.getInstruction() instanceof CallInst callInst && callInst.getCallee() == function)) continue;
            // 将原有基本块按照 call 指令的位置拆分为两个
            var blockAlpha = callInst.getBasicBlock();
            var blockBeta = new BasicBlock();
            boolean startMoving = false;
            for (Instruction instruction : blockAlpha.getMainInstructions()) {
                if (startMoving) {
                    instruction.removeFromBasicBlock();
                    blockBeta.addInstruction(instruction);
                }
                if (instruction == callInst) startMoving = true;
            }
            var betaTerminate = blockAlpha.getTerminateInstruction();
            betaTerminate.removeFromBasicBlock();
            blockBeta.setTerminateInstruction(betaTerminate);
            for (Operand blockAlphaUsage : blockAlpha.getUsages()) {
                if (blockAlphaUsage.getInstruction() instanceof PhiInst) {
                    blockAlphaUsage.setValue(blockBeta);
                }
            }
            // 插入内联后的函数
            var clonedFunction = new FunctionClone(function, callInst.getArgumentList(), blockBeta);
            blockAlpha.setTerminateInstruction(new JumpInst(clonedFunction.getEntryBlock()));
            for (BasicBlock clonedBlock : clonedFunction.getBasicBlocks()) {
                function.addBasicBlock(clonedBlock);
            }
            callInst.replaceAllUsageTo(clonedFunction.getReturnValue());
        }
    }

    private boolean runOnModule() {
        boolean changed = false;
        collectInformation();
        List<Function> functions = new ArrayList<>(module.getFunctions());
        functions.sort((function1, function2) -> Integer.compare(propertyMap.get(function1).size, propertyMap.get(function2).size));
        for (Function inlinedFunction : functions) {
            if (propertyMap.get(inlinedFunction).callees.contains(inlinedFunction) || inlinedFunction.getValueName().equals("main"))
                continue;
            inlineFunction(inlinedFunction);
            module.removeFunction(inlinedFunction);
            changed = true;
        }
        return changed;
    }

    public static boolean runOnModule(Module module) {
        return new FunctionInline(module).runOnModule();
    }

}
