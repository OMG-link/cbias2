package cn.edu.bit.newnewcc.frontend;

import cn.edu.bit.newnewcc.ir.value.Constant;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ConstantTable {
    private final Map<String, Constant> globalConstants = new HashMap<>();
    private final Deque<Map<String, Constant>> scopeStack = new LinkedList<>();

    public void putGlobalConstant(String name, Constant constant) {
        globalConstants.put(name, constant);
    }

    public void putLocalConstant(String name, Constant constant) {
        scopeStack.element().put(name, constant);
    }

    public Constant getConstant(String name) {
        for (Map<String, Constant> scope : scopeStack) {
            Constant constant = scope.get(name);
            if (constant != null) return constant;
        }
        return globalConstants.get(name);
    }

    public void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    public void popScope() {
        scopeStack.pop();
    }
}
