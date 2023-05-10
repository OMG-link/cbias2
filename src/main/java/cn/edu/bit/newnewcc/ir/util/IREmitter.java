package cn.edu.bit.newnewcc.ir.util;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.*;

import java.io.IOException;
import java.util.StringJoiner;

public class IREmitter {
    private final StringBuilder builder = new StringBuilder();

    private void emitInstruction(Instruction instruction) {
        builder.append("    ").append(instruction.toString()).append("\n");
    }

    private void emitBasicBlock(BasicBlock basicBlock) {
        builder.append(basicBlock.getValueName()).append(":\n");
        basicBlock.getInstructions().forEach(this::emitInstruction);
    }

    /**
     * 为所有变量分配名字 <br>
     * LLVM IR要求数字名称按出现顺序递增 <br>
     *
     * @param function 待分配名字的函数
     */
    private void nameFunction(Function function) {
        for (Value formalParameter : function.getFormalParameters()) {
            formalParameter.getValueNameIR();
        }
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            basicBlock.getValueNameIR();
            for (Instruction instruction : basicBlock.getInstructions()) {
                if (instruction.getType() != VoidType.getInstance()) {
                    instruction.getValueNameIR();
                }
            }
        }
    }

    private void emitFunction(Function function) {
        nameFunction(function);
        builder.append(String.format(
                "define dso_local %s %s",
                function.getReturnType().getTypeName(),
                function.getValueNameIR()
        ));
        builder.append('(');
        StringJoiner joiner = new StringJoiner(", ");
        for (Value formalParameter : function.getFormalParameters()) {
            joiner.add(formalParameter.getTypeName() + " " + formalParameter.getValueNameIR());
        }
        builder.append(joiner);
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
            if (!isFirstParameter) {
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
                globalVariable.isConstant() ? "constant" : "global",
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

    public static String emit(Module module) throws IOException {
        var emitter = new IREmitter();
        emitter.emitModule(module);
        return emitter.builder.toString();
    }
}
