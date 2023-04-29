package com.bit.newnewcc.frontend;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.ir.value.instruction.AllocateInst;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Function> functions = new HashMap<>();
    private final Deque<Map<String, AllocateInst>> scopeStack = new LinkedList<>();

    public void putFunction(String name, Function function) {
        functions.put(name, function);
    }

    public Function getFunction(String name) {
        return functions.get(name);
    }

    public void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    public void popScope() {
        scopeStack.pop();
    }

    public void putLocalVariable(String name, AllocateInst address) {
        scopeStack.element().put(name, address);
    }

    public AllocateInst getLocalVariable(String name) {
        for (Map<String, AllocateInst> scope : scopeStack) {
            AllocateInst address = scope.get(name);
            if (address != null) return address;
        }
        return null;
    }
}
