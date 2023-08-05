package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;

public class AsmCall extends AsmInstruction {
    public AsmCall(String functionName) {
        super("call", new Label(functionName, true), null, null);
    }
}
