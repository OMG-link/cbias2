package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;

import java.util.HashMap;
import java.util.Map;

public class AsmCode {
    private Map<Function, AsmFunction> functionMap = new HashMap<>();
    private Map<GlobalVariable, AsmGlobalVariable> globalVariableMap = new HashMap<>();

    public AsmGlobalVariable getGlobalVariable(GlobalVariable key) {
        return globalVariableMap.get(key);
    }
}
