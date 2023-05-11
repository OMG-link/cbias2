package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IntegrityVerifyFailedException;
import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.instruction.AllocateInst;
import cn.edu.bit.newnewcc.ir.value.instruction.PhiInst;

import java.util.*;

/**
 * IR 语义分析 <br>
 * 用于检查 IR 语义是否正确，可以发现部分优化过程产生的错误 <br>
 * 此 Pass 不会对 IR 进行任何修改 <br>
 */
public class IrSemanticAnalysisPass {

    private final Set<Value> globalValues = new HashSet<>();
    private Set<Value> localValues;
    private Map<BasicBlock, Set<BasicBlock>> basicBlockEntries;

    private IrSemanticAnalysisPass() {
    }

    private void verifyInstruction(Instruction instruction) {
        for (Operand operand : instruction.getOperandList()) {
            if (!operand.hasValueBound()) {
                throw new IntegrityVerifyFailedException("Operand has no value bound.");
            }
            if (!(operand.getValue() instanceof Constant || localValues.contains(operand.getValue()))) {
                System.out.println(instruction);
                throw new IntegrityVerifyFailedException("Value bound cannot be used as an operand.");
            }
        }
    }

    private void verifyBasicBlock(BasicBlock basicBlock, boolean isFunctionEntry) {
        basicBlock.getInstructions().forEach(this::verifyInstruction);
        basicBlock.getLeadingInstructions().forEach(instruction -> {
            if (instruction instanceof AllocateInst && !isFunctionEntry) {
                throw new IntegrityVerifyFailedException("Alloca instruction must be placed in entry block.");
            }
        });
        if (basicBlock.getTerminateInstruction() == null) {
            throw new IntegrityVerifyFailedException("Basic block has no terminate instruction.");
        }
    }

    private void verifyFunction(Function function) {
        // Collect all local values and basic block entries
        localValues = new HashSet<>(globalValues);
        localValues.addAll(function.getFormalParameters());
        localValues.addAll(function.getBasicBlocks());
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction instruction : basicBlock.getInstructions()) {
                if (instruction.getType() != VoidType.getInstance()) {
                    localValues.add(instruction);
                }
            }
        }
        // Check use relationship
        function.getBasicBlocks().forEach(basicBlock ->
                verifyBasicBlock(basicBlock, basicBlock == function.getEntryBasicBlock())
        );
        // Check phi instructions
        basicBlockEntries = new HashMap<>();
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            basicBlockEntries.put(basicBlock, new HashSet<>());
        }
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            basicBlock.getExitBlocks().forEach(
                    exitBlock -> basicBlockEntries.get(exitBlock).add(basicBlock)
            );
        }
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction instruction : basicBlock.getLeadingInstructions()) {
                if (instruction instanceof PhiInst phiInst) {
                    if (!Objects.equals(phiInst.getEntrySet(), basicBlockEntries.get(basicBlock))) {
                        throw new IntegrityVerifyFailedException("Phi instruction's entry map does not match basic block entries.");
                    }
                }
            }
        }
    }

    private void verifyModule(Module module) {
        globalValues.addAll(module.getExternalFunctions());
        globalValues.addAll(module.getFunctions());
        globalValues.addAll(module.getGlobalVariables());
        module.getFunctions().forEach(this::verifyFunction);
    }

    public static void verify(Module module) {
        new IrSemanticAnalysisPass().verifyModule(module);
    }

}
