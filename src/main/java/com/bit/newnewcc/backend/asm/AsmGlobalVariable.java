package com.bit.newnewcc.backend.asm;

public class AsmGlobalVariable {
    private final String globalVariableName;
    boolean isConstant;
    private int size, align;

    public AsmGlobalVariable(String globalVariableName, int size, boolean isConstant) {
        this.globalVariableName = globalVariableName;
        this.size = size;
        this.isConstant = isConstant;
        int tag = 0;
    }

    public boolean isConstant() {
        return isConstant;
    }
}
