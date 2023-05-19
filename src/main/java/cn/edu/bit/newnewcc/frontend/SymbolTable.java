package cn.edu.bit.newnewcc.frontend;

import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.value.BaseFunction;
import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SymbolTable {
    @lombok.Value
    public static class Entry {
        Value address;
        Constant constantValue;
    }

    private final Map<String, BaseFunction> functions = new HashMap<>();
    private final Map<String, GlobalVariable> globalVariables = new HashMap<>();
    private final Deque<Map<String, Entry>> scopeStack = new LinkedList<>();

    public void putFunction(String name, BaseFunction function) {
        functions.put(name, function);
    }

    public void putGlobalVariable(String name, GlobalVariable globalVariable) {
        globalVariables.put(name, globalVariable);
    }

    public void putLocalVariable(String name, Value address, Constant constantValue) {
        scopeStack.element().put(name, new Entry(address, constantValue));
    }

    public BaseFunction getFunction(String name) {
        return functions.get(name);
    }

    public Entry getVariable(String name) {
        for (Map<String, Entry> scope : scopeStack) {
            Entry entry = scope.get(name);
            if (entry != null) return entry;
        }

        GlobalVariable globalVariable = globalVariables.get(name);
        if (globalVariable != null)
            return new Entry(globalVariable, globalVariable.isConstant() ? globalVariable.getInitialValue() : null);

        return null;
    }

    public void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    public void popScope() {
        scopeStack.pop();
    }

    public int getScopeDepth() {
        return scopeStack.size();
    }
}
