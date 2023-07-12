package cn.edu.bit.newnewcc.pass.ir.util;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.instruction.CallInst;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CallMap {
    public static Map<BaseFunction, Set<Function>> from(Module module) {
        Map<BaseFunction, Set<Function>> callMap = new HashMap<>();
        for (Function function : module.getFunctions()) {
            callMap.put(function, new HashSet<>());
        }
        for (ExternalFunction externalFunction : module.getExternalFunctions()) {
            callMap.put(externalFunction, new HashSet<>());
        }
        for (Function caller : module.getFunctions()) {
            for (BasicBlock basicBlock : caller.getBasicBlocks()) {
                for (Instruction instruction : basicBlock.getInstructions()) {
                    if (instruction instanceof CallInst callInst) {
                        var callee = callInst.getCallee();
                        callMap.get(callee).add(caller);
                    }
                }
            }
        }
        return callMap;
    }

}
