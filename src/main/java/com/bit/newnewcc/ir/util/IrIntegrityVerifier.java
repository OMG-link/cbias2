package com.bit.newnewcc.ir.util;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IntegrityVerifyFailedException;
import com.bit.newnewcc.ir.type.VoidType;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Constant;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.ir.value.Instruction;
import com.bit.newnewcc.ir.value.instruction.AllocateInst;
import com.bit.newnewcc.ir.value.instruction.PhiInst;

import java.util.*;

public class IrIntegrityVerifier {

    private final Set<Value> globalValues = new HashSet<>();
    private Set<Value> localValues;
    private Map<BasicBlock, Set<BasicBlock>> basicBlockEntries;

    private IrIntegrityVerifier() {
    }

    private void verifyInstruction(Instruction instruction) {
        for (Operand operand : instruction.getOperandList()) {
            if (!operand.hasValueBound()) {
                throw new IntegrityVerifyFailedException("Operand has no value bound.");
            }
            if (!(operand.getValue() instanceof Constant || localValues.contains(operand.getValue()))) {
                throw new IntegrityVerifyFailedException("Value bounded cannot be used as an operand.");
            }
        }
    }

    private void verifyBasicBlock(BasicBlock basicBlock, boolean isFunctionEntry) {
        basicBlock.getInstructionList().forEach(this::verifyInstruction);
        basicBlock.getLeadingInstructions().forEachRemaining(instruction -> {
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
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
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
            basicBlock.getTerminateInstruction().getExits().forEach(
                    exitBlock -> basicBlockEntries.get(exitBlock).add(basicBlock)
            );
        }
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            basicBlock.getLeadingInstructions().forEachRemaining(instruction -> {
                if (instruction instanceof PhiInst phiInst) {
                    if (!Objects.equals(phiInst.getEntrySet(), basicBlockEntries.get(basicBlock))) {
                        throw new IntegrityVerifyFailedException("Phi instruction's entry map does not match basic block entries.");
                    }
                }
            });
        }
    }

    private void verifyModule(Module module) {
        globalValues.addAll(module.getExternalFunctions());
        globalValues.addAll(module.getFunctions());
        globalValues.addAll(module.getGlobalVariables());
        module.getFunctions().forEach(this::verifyFunction);
    }

    public static void verify(Module module) {
        new IrIntegrityVerifier().verifyModule(module);
    }

}
