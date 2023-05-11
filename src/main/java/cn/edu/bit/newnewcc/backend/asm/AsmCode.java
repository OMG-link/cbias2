package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.ir.value.AbstractFunction;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;
import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;

import java.util.HashMap;
import java.util.Map;

public class AsmCode {
    private Map<AbstractFunction, AsmFunction> functionMap = new HashMap<>();
    private Map<GlobalVariable, AsmGlobalVariable> globalVariableMap = new HashMap<>();
    //private Map<ConstFloat, GlobalTag> constFloatMap = new HashMap<>();
    //此处应维护一个浮点常量表，用于读取浮点常量

    public AsmGlobalVariable getGlobalVariable(GlobalVariable key) {
        return globalVariableMap.get(key);
    }

    public AsmCode(Module module) {
        for (var globalVariable : module.getGlobalVariables()) {
            AsmGlobalVariable variable = new AsmGlobalVariable(globalVariable);
            globalVariableMap.put(globalVariable, variable);
        }
        for (var function : module.getExternalFunctions()) {
            AsmFunction asmFunction = new AsmFunction(function, this);
            functionMap.put(function, asmFunction);
        }
        for (var function : module.getFunctions()) {
            AsmFunction asmFunction = new AsmFunction(function, this);
            functionMap.put(function, asmFunction);
        }
    }

    public String emit() {
        StringBuilder res = new StringBuilder(".option nopic\n.attribute arch, \"rv64i2p0_m2p0_a2p0_f2p0_d2p0_c2p0\"\n");
        res.append(".attribute unaligned_access, 0\n");
        res.append(".attribute stack_align, 16\n");
        for (var gvar : globalVariableMap.values()) {
            res.append(gvar.emit());
        }
        for (var fvar : functionMap.values()) {
            if (!fvar.isExternal()) {
                res.append(fvar.emit());
            }
        }
        return res.toString();
    }
}
