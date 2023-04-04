package com.bit.newnewcc.ir.util;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.value.*;

import java.io.FileWriter;
import java.io.IOException;

public class IREmitter {
    private final StringBuilder builder = new StringBuilder();

    private void emitInstruction(Instruction instruction) {
        builder.append("    ").append(instruction.toString()).append("\n");
    }

    private void emitBasicBlock(BasicBlock basicBlock) {
        builder.append(basicBlock.getValueName()).append(":\n");
        for (Instruction instruction : basicBlock.getInstructionList()) {
            emitInstruction(instruction);
        }
    }

    private void emitFunction(Function function) {
        builder.append(String.format(
                "define dso_local %s %s",
                function.getReturnType().getTypeName(),
                function.getValueNameIR()
        ));
        builder.append('(');
        boolean isFirstParameter = true;
        for (Value formalParameter : function.getFormalParameters()) {
            if(!isFirstParameter){
                builder.append(", ");
            }
            builder.append(formalParameter.getTypeName())
                    .append(' ')
                    .append(formalParameter.getValueName());
            isFirstParameter = false;
        }
        builder.append(") {\n");
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            emitBasicBlock(basicBlock);
        }
        builder.append("}\n\n");
    }

    private void emitExternalFunction(ExternalFunction externalFunction) {
        builder.append("declare ")
                .append(externalFunction.getReturnType().getTypeName())
                .append(externalFunction.getValueNameIR())
                .append('(');
        boolean isFirstParameter = true;
        for (Type parameterType : externalFunction.getParameterTypes()) {
            if(!isFirstParameter){
                builder.append(", ");
            }
            builder.append(parameterType.getTypeName());
            isFirstParameter = false;
        }
        builder.append(")\n");
    }


    private void emitGlobalVariable(GlobalVariable globalVariable) {
        // e.g.
        // @c = dso_local constant i32 3
        // @d = dso_local global i32 4
        builder.append(String.format(
                "%s = dso_local %s %s %s\n",
                globalVariable.getValueNameIR(),
                globalVariable.isConstant()?"constant":"global",
                globalVariable.getTypeName(),
                globalVariable.getInitialValue().getValueNameIR()
        ));
    }

    private void emitModule(Module module) {
        for (ExternalFunction externalFunction : module.getExternalFunctions()) {
            emitExternalFunction(externalFunction);
        }
        builder.append('\n');
        for (GlobalVariable globalVariable : module.getGlobalVariables()) {
            emitGlobalVariable(globalVariable);
        }
        builder.append('\n');
        for (Function function : module.getFunctions()) {
            emitFunction(function);
        }
        builder.append('\n');
    }

    public static void emitModule(String outputFilePath, Module module) throws IOException {
        var emitter = new IREmitter();
        emitter.emitModule(module);
        var fileWriter = new FileWriter(outputFilePath);
        fileWriter.write(emitter.builder.toString());
        fileWriter.close();
    }

}
