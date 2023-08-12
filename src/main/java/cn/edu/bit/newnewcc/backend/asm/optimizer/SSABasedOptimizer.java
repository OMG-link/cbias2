package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmBlockEnd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SSABasedOptimizer implements Optimizer {

    /**
     * 正在优化的函数
     */
    protected AsmFunction functionContext;

    /**
     * 获取一条指令优化后的结果(instList, finalRegisterMap) <br>
     * instList：计算该结果所需要的新指令 <br>
     * finalRegisterMap(regA, regB) 表示优化后，需要将regA的使用替换为regB，Map的两侧必须都是虚拟寄存器  <br>
     *
     * @param instruction 待优化的指令
     * @return 若成功优化，返回二元组(instList, finalRegisterMap)；若无法优化，返回null
     */
    protected abstract Pair<List<AsmInstruction>, Map<Register, Register>> getReplacement(AsmInstruction instruction);

    private final Map<Register, Register> registerReplacementMap = new HashMap<>();
    private final Map<Register, AsmInstruction> valueSourceMap = new HashMap<>();

    private Register getReplacedRegister(Register register) {
        var nextRegister = registerReplacementMap.get(register);
        if (nextRegister == null) {
            return register;
        } else {
            nextRegister = getReplacedRegister(nextRegister);
            registerReplacementMap.put(register, nextRegister);
            return nextRegister;
        }
    }

    /**
     * 获取寄存器中值的来源 <br>
     * 若值有多个来源，或是值的来源无法确定，返回null <br>
     *
     * @param register 寄存器
     * @return 如果值有唯一确定的来源指令，返回来源指令；否则返回null。
     */
    protected AsmInstruction getValueSource(Register register) {
        return valueSourceMap.get(register);
    }

    private AsmInstruction breakThroughMove(AsmMove asmMove) {
        var father = (Register) asmMove.getOperand(2);
        if (valueSourceMap.get(father) instanceof AsmMove asmMove1) {
            var breakThroughResult = breakThroughMove(asmMove1);
            if (breakThroughResult != null) {
                return breakThroughResult;
            } else {
                return asmMove;
            }
        } else {
            return valueSourceMap.get(father);
        }
    }

    private void preprocessValueSourceMap() {
        for (AsmInstruction instruction : functionContext.getInstrList()) {
            if (instruction instanceof AsmLabel || instruction instanceof AsmBlockEnd) continue;
            for (Register definedRegister : instruction.getDef()) {
                if (!definedRegister.isVirtual()) continue;
                if (valueSourceMap.containsKey(definedRegister)) {
                    // 此处必须 put null ，不能 remove
                    // 若是 remove ，则下一次 define 时会进入 else 分支，导致多定义的变量被错误的视作SSA
                    valueSourceMap.put(definedRegister, null);
                } else {
                    valueSourceMap.put(definedRegister, instruction);
                }
            }
        }
        // 试图穿过 Move 指令获取更早的定义指令，以获得更佳的效果
        for (Register register : valueSourceMap.keySet()) {
            var valueSource = valueSourceMap.get(register);
            if (valueSource instanceof AsmMove asmMove) {
                var breakThroughResult = breakThroughMove(asmMove);
                if (breakThroughResult != null) {
                    valueSourceMap.put(register, breakThroughResult);
                }
            }
        }
    }

    private void checkRegisterReplacementMap() {
        for (Register register : registerReplacementMap.keySet()) {
            if (!register.isVirtual()) {
                throw new RuntimeException("Replacing usage of physical register is dangerous, it is NOT ALLOWED.");
            }
            if (!registerReplacementMap.get(register).isVirtual()) {
                throw new RuntimeException("Replacing usage with physical register is dangerous, it is NOT ALLOWED.");
            }
        }
    }

    @Override
    public final boolean runOn(AsmFunction function) {
        functionContext = function;

        preprocessValueSourceMap();

        List<AsmInstruction> instrList = function.getInstrList();
        List<AsmInstruction> newInstrList = new ArrayList<>();
        int count = 0;

        for (AsmInstruction instruction : instrList) {
            if (!(instruction instanceof AsmLabel || instruction instanceof AsmBlockEnd)) {
                var pair = getReplacement(instruction);
                if (pair != null) {
                    newInstrList.addAll(pair.a);
                    registerReplacementMap.putAll(pair.b);
                    count++;
                }
            }
            newInstrList.add(instruction);
        }

        checkRegisterReplacementMap();

        for (AsmInstruction instruction : newInstrList) {
            if (instruction instanceof AsmLabel || instruction instanceof AsmBlockEnd) continue;
            for (int id : AsmInstructions.getReadRegId(instruction)) {
                if (instruction.getOperand(id) instanceof RegisterReplaceable registerReplaceable) {
                    var sourceReg = registerReplaceable.getRegister();
                    if (registerReplacementMap.containsKey(sourceReg)) {
                        var targetReg = registerReplacementMap.get(sourceReg);
                        instruction.setOperand(id, registerReplaceable.withRegister(targetReg));
                    }
                }
            }
        }

        instrList.clear();
        instrList.addAll(newInstrList);

        return count > 0;
    }


    /**
     * 判断一个指令是否使用了物理寄存器
     *
     * @param instruction 指令
     * @return 若使用了物理寄存器，返回true；否则返回false。
     */
    protected static boolean usesPhysicalRegister(AsmInstruction instruction) {
        for (int id = 1; id <= 3; id++) {
            if (instruction.getOperand(id) instanceof Register register && !register.isVirtual()) {
                return true;
            }
        }
        return false;
    }

}
