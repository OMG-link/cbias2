package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.BaseFunction;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;

import java.util.HashMap;
import java.util.Map;

public class AsmCode {
    private final Map<BaseFunction, AsmFunction> functionMap = new HashMap<>();
    private final Map<GlobalVariable, AsmGlobalVariable> globalVariableMap = new HashMap<>();
    private final Map<Float, AsmConstantFloat> constFloatMap = new HashMap<>();
    private final Map<Long, AsmConstantLong> constLongMap = new HashMap<>();

    public AsmGlobalVariable getGlobalVariable(GlobalVariable key) {
        return globalVariableMap.get(key);
    }

    AsmConstantFloat getConstFloat(float value) {
        if (!constFloatMap.containsKey(value)) {
            constFloatMap.put(value, new AsmConstantFloat(value));
        }
        return constFloatMap.get(value);
    }
    AsmConstantLong getConstLong(long value) {
        if (!constLongMap.containsKey(value)) {
            constLongMap.put(value, new AsmConstantLong(value));
        }
        return constLongMap.get(value);
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
        for (var asmFunction : functionMap.values()) {
            asmFunction.emitCode();
        }
    }

    AsmFunction getFunction(BaseFunction baseFunction) {
        return functionMap.get(baseFunction);
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
        res.append(".section .rodata\n");
        for (var key : constFloatMap.keySet()) {
            var constFloat = constFloatMap.get(key);
            res.append(constFloat.emit()).append('\n');
        }
        for (var key : constLongMap.keySet()) {
            var constLong = constLongMap.get(key);
            res.append(constLong.emit()).append('\n');
        }
        return res.toString();
    }
}
