package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;

class SLInst {
    private enum TYPE {
        store, load
    }

    private final Register register;
    private final StackVar stackVar;
    private final TYPE type;

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

    public SLInst createLoad(Register reg, StackVar stk) {
        return new SLInst(reg, stk, TYPE.load);
    }

    public SLInst createStore(Register reg, StackVar stk) {
        return new SLInst(reg, stk, TYPE.store);
    }
}
