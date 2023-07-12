package cn.edu.bit.newnewcc.pass.ir.util;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.instruction.CallInst;
import cn.edu.bit.newnewcc.ir.value.instruction.StoreInst;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PureFunctionDetect {
    private static class Detector {

        private final Module module;
        private final Map<Function, Set<Function>> callers = new HashMap<>();
        private final Set<Function> externalCallers = new HashSet<>();
        private final Set<Function> nonPureFunctions = new HashSet<>();
        private final GlobalAddressDetector globalAddressDetector = new GlobalAddressDetector();

        public Detector(Module module) {
            this.module = module;
        }

        /**
         * 分析每个函数被哪些函数调用
         */
        private void analysisCallers() {
            for (Function function : module.getFunctions()) {
                callers.put(function, new HashSet<>());
            }
            for (Function caller : module.getFunctions()) {
                for (BasicBlock basicBlock : caller.getBasicBlocks()) {
                    for (Instruction instruction : basicBlock.getInstructions()) {
                        if (instruction instanceof CallInst callInst) {
                            if (callInst.getCallee() instanceof Function callee) {
                                callers.get(callee).add(caller);
                            } else if (callInst.getCallee() instanceof ExternalFunction) {
                                externalCallers.add(caller);
                            } else {
                                throw new RuntimeException("Unknown class of function " + callInst.getCallee().getClass());
                            }
                        }
                    }
                }
            }
        }

        /**
         * 标记一个函数不是纯函数
         *
         * @param function 被标记的函数
         */
        private void addNonPureFunction(Function function) {
            if (nonPureFunctions.contains(function)) return;
            nonPureFunctions.add(function);
            for (Function caller : callers.get(function)) {
                addNonPureFunction(caller);
            }
        }

        /**
         * 检查某个函数是否为纯函数 <br>
         * 此方法<b>不考虑</b>调用非纯函数导致本函数变成非纯函数的情况 <br>
         *
         * @param function 被检查的函数
         * @return 若被检查的函数是纯函数，返回true；否则返回false。
         */
        // 导致非纯函数的原因包括：
        // 1. 调用非纯函数
        // 2. 引用全局变量
        // 3. 向除局部变量以外的位置store
        private boolean isPureFunction(Function function) {
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                for (Instruction instruction : basicBlock.getInstructions()) {
                    // 2. 引用全局变量
                    for (Operand operand : instruction.getOperandList()) {
                        if (operand.getValue() instanceof GlobalVariable) {
                            return false;
                        }
                    }
                    // 3. 向除局部变量以外的位置store
                    if (instruction instanceof StoreInst storeInst) {
                        if (globalAddressDetector.isGlobalAddress(storeInst.getAddressOperand())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        public Set<Function> getPureFunctions() {
            analysisCallers();
            // 外部函数全部认为是非纯函数
            // 调用外部函数的都不是纯函数
            for (Function externalCaller : externalCallers) {
                addNonPureFunction(externalCaller);
            }
            // 检查每个函数是否为纯函数
            for (Function function : module.getFunctions()) {
                if (!isPureFunction(function)) {
                    addNonPureFunction(function);
                }
            }
            // 生成结果
            var result = new HashSet<Function>();
            for (Function function : module.getFunctions()) {
                if (!nonPureFunctions.contains(function)) {
                    result.add(function);
                }
            }
            return result;
        }

    }

    public static Set<Function> getPureFunctions(Module module) {
        return new Detector(module).getPureFunctions();
    }

}
