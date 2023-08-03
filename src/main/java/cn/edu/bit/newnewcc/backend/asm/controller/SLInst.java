package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;

class SLInst {
    private enum TYPE {
        store, load
    }

    Register register;
    StackVar stackVar;
    TYPE type;

    Register getRegister() {
        return register;
    }

    StackVar getStackVar() {
        return stackVar;
    }

    boolean isLoad() {
        return type == TYPE.load;
    }

    boolean isStore() {
        return type == TYPE.store;
    }

    private SLInst(Register reg, StackVar stk, TYPE type) {
        this.register = reg;
        this.stackVar = stk;
        this.type = type;
    }

    public SLInst getLoad(Register reg, StackVar stk) {
        return new SLInst(reg, stk, TYPE.load);
    }

    public SLInst getStore(Register reg, StackVar stk) {
        return new SLInst(reg, stk, TYPE.store);
    }
}
