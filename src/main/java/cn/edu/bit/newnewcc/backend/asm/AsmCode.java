package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;

import java.util.Map;

public class AsmCode {
    private Map<Function, AsmFunction> functionMap;
    private Map<GlobalVariable, AsmGlobalVariable> globalVariableMap;

    public AsmGlobalVariable getGlobalVariable(GlobalVariable key) {
        return globalVariableMap.get(key);
    }
}
