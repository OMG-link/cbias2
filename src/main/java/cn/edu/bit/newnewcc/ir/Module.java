package cn.edu.bit.newnewcc.ir;

import cn.edu.bit.newnewcc.ir.value.ExternalFunction;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * 模块 <br>
 * 每个源文件构成一个模块
 */
public class Module {
    private final Collection<Function> functions = new HashSet<>();
    private final Collection<ExternalFunction> externalFunctions = new HashSet<>();
    private final Collection<GlobalVariable> globalVariables = new HashSet<>();

    public void addFunction(Function function) {
        functions.add(function);
    }

    public Collection<Function> getFunctions() {
        return Collections.unmodifiableCollection(functions);
    }

    public void addExternalFunction(ExternalFunction externalFunction) {
        externalFunctions.add(externalFunction);
    }

    public Collection<ExternalFunction> getExternalFunctions() {
        return Collections.unmodifiableCollection(externalFunctions);
    }

    public void addGlobalVariable(GlobalVariable globalVariable) {
        globalVariables.add(globalVariable);
    }

    public Collection<GlobalVariable> getGlobalVariables() {
        return Collections.unmodifiableCollection(globalVariables);
    }

}
