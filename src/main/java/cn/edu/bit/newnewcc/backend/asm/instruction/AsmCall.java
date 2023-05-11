package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;

public class AsmCall extends AsmInstruction {
    public AsmCall(String functionName) {
        super("call", new GlobalTag(functionName, true), null, null);
    }
}
