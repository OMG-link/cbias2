package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.instruction.CallInst;
import cn.edu.bit.newnewcc.ir.value.instruction.LoadInst;
import cn.edu.bit.newnewcc.ir.value.instruction.StoreInst;

import java.util.HashMap;
import java.util.Map;

/**
 * 访存优化，包括：
 * <li>
 *     <ul>全局变量临时局部化</ul>
 *     <ul>连续load语句的合并</ul>
 * </li>
 */
public class MemoryAccessOptimizePass {

    private static boolean localizeGlobalVariable(BasicBlock basicBlock) {
        // changed指代是否有优化产生，非优化的修改不影响changed
        boolean changed = false;
        Map<GlobalVariable, Value> gvBuffer = new HashMap<>();
        Map<GlobalVariable, Value> writeBackBuffer = new HashMap<>();
        for (Instruction mainInstruction : basicBlock.getMainInstructions()) {
            if (mainInstruction instanceof LoadInst loadInst && loadInst.getAddressOperand() instanceof GlobalVariable globalVariable) {
                if (gvBuffer.containsKey(globalVariable)) {
                    loadInst.replaceAllUsageTo(gvBuffer.get(globalVariable));
                    loadInst.waste();
                    changed = true;
                } else {
                    gvBuffer.put(globalVariable, loadInst);
                }
            } else if (mainInstruction instanceof StoreInst storeInst && storeInst.getAddressOperand() instanceof GlobalVariable globalVariable) {
                gvBuffer.put(globalVariable, storeInst.getValueOperand());
                writeBackBuffer.put(globalVariable, storeInst.getValueOperand());
                storeInst.waste();
            } else if (mainInstruction instanceof CallInst callInst) {
                gvBuffer.clear();
                writeBackBuffer.forEach((globalVariable, value) -> {
                    var storeInst = new StoreInst(globalVariable, value);
                    storeInst.insertBefore(callInst);
                });
                writeBackBuffer.clear();
            }
        }
        writeBackBuffer.forEach((globalVariable, value) -> {
            var storeInst = new StoreInst(globalVariable, value);
            // addInstruction的行为是加在mainInstructions的末尾
            basicBlock.addInstruction(storeInst);
        });
        return changed;
    }

    private static boolean mergeDuplicatedLoad(BasicBlock basicBlock) {
        boolean changed = false;
        Map<Value, Value> addressValueMap = new HashMap<>();
        for (Instruction mainInstruction : basicBlock.getMainInstructions()) {
            if (mainInstruction instanceof LoadInst loadInst) {
                Value address = loadInst.getAddressOperand();
                if (!addressValueMap.containsKey(address)) {
                    addressValueMap.put(address, loadInst);
                } else {
                    loadInst.replaceAllUsageTo(addressValueMap.get(address));
                    loadInst.waste();
                    changed = true;
                }
            } else if (mainInstruction instanceof StoreInst) { // 内存变动
                addressValueMap.clear();
            } else if (mainInstruction instanceof CallInst) { // 控制流转移
                addressValueMap.clear();
            }
        }
        return changed;
    }

    private static boolean runOnBasicBlock(BasicBlock basicBlock) {
        return localizeGlobalVariable(basicBlock) | mergeDuplicatedLoad(basicBlock);
    }

    public static boolean runOnModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctions()) {
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                changed |= runOnBasicBlock(basicBlock);
            }
        }
        return changed;
    }
}
