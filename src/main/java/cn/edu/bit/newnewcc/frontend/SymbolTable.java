package cn.edu.bit.newnewcc.frontend;

import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.value.AbstractFunction;
import cn.edu.bit.newnewcc.ir.value.Function;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SymbolTable {
    private final Map<String, AbstractFunction> functions = new HashMap<>();
    private final Deque<Map<String, Value>> scopeStack = new LinkedList<>();

    public void putFunction(String name, Function function) {
        functions.put(name, function);
    }

    public AbstractFunction getFunction(String name) {
        return functions.get(name);
    }

    public void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    public void popScope() {
        scopeStack.pop();
    }

    public void putLocalVariable(String name, Value address) {
        scopeStack.element().put(name, address);
    }

    public Value getLocalVariable(String name) {
        for (Map<String, Value> scope : scopeStack) {
            Value address = scope.get(name);
            if (address != null) return address;
        }
        return null;
    }
}
