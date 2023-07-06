package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.backend.asm.util.Pair;
import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerAddInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerMultiplyInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerSubInst;

import java.util.*;

/**
 * 加法转乘法 <br>
 * 仅考虑整数运算情况 <br>
 * 仅考虑了同一基本块内的情况 <br>
 * 分析每个寄存器产生时的加（减）表达式，构造更佳的计算顺序 <br>
 * 例如：c=a+a+a+a+3*a-a+b+b优化为6*a+2*b <br>
 */
public class AddToMulPass {

    private record IngredientList(Map<Value, Integer> ingredientCounts) {
    }

    private AddToMulPass() {
    }

    // 分析结果在单次 runOnModule 中是恒定的
    // 缓存分析结果以加速程序
    private final Map<Value, IngredientList> valueIngredientBuffer = new HashMap<>();

    private IngredientList createIngredientList(Value targetValue) {
        // 常整数 n 写做 1*n ，以统一形式
        if (targetValue instanceof ConstInt constInt) {
            Map<Value, Integer> ingredientCounts = new HashMap<>();
            ingredientCounts.put(ConstInt.getInstance(1), constInt.getValue());
            return new IngredientList(ingredientCounts);
        }
        // 整数加法
        else if (targetValue instanceof IntegerAddInst integerAddInst) {
            return addIngredientList(
                    getIngredientList(integerAddInst.getOperand1()),
                    getIngredientList(integerAddInst.getOperand2())
            );
        }
        // 整数减法
        else if (targetValue instanceof IntegerSubInst integerSubInst) {
            return subIngredientList(
                    getIngredientList(integerSubInst.getOperand1()),
                    getIngredientList(integerSubInst.getOperand2())
            );
        }
        // 整数乘法
        else if (targetValue instanceof IntegerMultiplyInst integerMultiplyInst) {
            if (integerMultiplyInst.getOperand1() instanceof ConstInt constInt) {
                return multIngredientList(
                        getIngredientList(integerMultiplyInst.getOperand2()),
                        constInt.getValue()
                );
            } else if (integerMultiplyInst.getOperand2() instanceof ConstInt constInt) {
                return multIngredientList(
                        getIngredientList(integerMultiplyInst.getOperand1()),
                        constInt.getValue()
                );
            }
        }
        // 默认情况
        Map<Value, Integer> ingredientCounts = new HashMap<>();
        ingredientCounts.put(targetValue, 1);
        return new IngredientList(ingredientCounts);
    }

    private IngredientList addIngredientList(IngredientList a, IngredientList b) {
        Map<Value, Integer> ingredientCounts = new HashMap<>(a.ingredientCounts);
        b.ingredientCounts.forEach((ingredient, count) -> {
            if (ingredientCounts.containsKey(ingredient)) {
                int newCount = ingredientCounts.get(ingredient) + count;
                if (newCount == 0) {
                    ingredientCounts.remove(ingredient);
                } else {
                    ingredientCounts.put(ingredient, newCount);
                }
            } else {
                ingredientCounts.put(ingredient, count);
            }
        });
        return new IngredientList(ingredientCounts);
    }

    private IngredientList subIngredientList(IngredientList a, IngredientList b) {
        Map<Value, Integer> ingredientCounts = new HashMap<>(a.ingredientCounts);
        b.ingredientCounts.forEach((ingredient, count) -> {
            if (ingredientCounts.containsKey(ingredient)) {
                int newCount = ingredientCounts.get(ingredient) - count;
                if (newCount == 0) {
                    ingredientCounts.remove(ingredient);
                } else {
                    ingredientCounts.put(ingredient, newCount);
                }
            } else {
                ingredientCounts.put(ingredient, -count);
            }
        });
        return new IngredientList(ingredientCounts);
    }

    private IngredientList multIngredientList(IngredientList a, int ratio) {
        Map<Value, Integer> ingredientCounts = new HashMap<>();
        a.ingredientCounts.forEach((ingredient, count) -> ingredientCounts.put(ingredient, count * ratio));
        return new IngredientList(ingredientCounts);
    }

    private IngredientList getIngredientList(Value value) {
        if (!valueIngredientBuffer.containsKey(value)) {
            valueIngredientBuffer.put(value, createIngredientList(value));
        }
        return valueIngredientBuffer.get(value);
    }

    /**
     * 根据指令结果的成分表，生成对应的产生序列
     *
     * @param ingredientList 指令结果的成分表
     * @return (成分表的产生序列, 结果值)
     */
    private Pair<List<Instruction>, Value> getSpawnInstructionList(IngredientList ingredientList) {
        List<Instruction> spawnList = new ArrayList<>();
        Queue<Value> addQueue = new LinkedList<>();
        Value resultValue;
        ingredientList.ingredientCounts.forEach((ingredient, count) -> {
            if (count == 0) return;
            if (count == 1) {
                addQueue.add(ingredient);
                return;
            }
            var multiplyInst = new IntegerMultiplyInst((IntegerType) ingredient.getType(), ingredient, ConstInt.getInstance(count));
            addQueue.add(multiplyInst);
            spawnList.add(multiplyInst);
        });
        // 若恰好全部消除，则结果为0
        if (addQueue.isEmpty()) {
            addQueue.add(ConstInt.getInstance(0));
        }
        // 使用队列维护待求和的数字集合，提升指令并行的可能
        while (true) {
            Value v1 = addQueue.remove();
            if (addQueue.isEmpty()) {
                resultValue = v1;
                break;
            }
            Value v2 = addQueue.remove();
            var addInst = new IntegerAddInst((IntegerType) v1.getType(), v1, v2);
            addQueue.add(addInst);
            spawnList.add(addInst);
        }
        return new Pair<>(spawnList, resultValue);
    }

    /**
     * 判断一个指令是否不可拆解 <br>
     * 此函数假定该指令已经被运行了getIngredientList。若未运行，则总是返回true <br>
     *
     * @param instruction 待判断的指令
     * @return 可拆解返回false；不可拆解返回true
     */
    private boolean isIndestructible(Instruction instruction) {
        if (!valueIngredientBuffer.containsKey(instruction)) {
            return true;
        }
        // 若指令结果的拆解方案包含自身，则认为其不可拆解
        return valueIngredientBuffer.get(instruction).ingredientCounts.containsKey(instruction);
    }

    private void runOnFunction(Function function) {
        // 先对所有主体指令尝试拆解
        // 前导指令和返回指令总是不可拆解的，出于性能考虑，不运行getIngredientList
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction mainInstruction : basicBlock.getMainInstructions()) {
                getIngredientList(mainInstruction);
            }
        }
        // 锚指令：指令的结果会被不可拆解的指令所使用
        // 例如：
        // b=a+1, c=b+1, d=c/2
        // 则b不是锚指令，c是锚指令(被d使用)，d是不可拆解指令
        var anchorInstructions = new HashSet<Instruction>();
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction instruction : basicBlock.getInstructions()) {
                if (isIndestructible(instruction)) {
                    for (Operand operand : instruction.getOperandList()) {
                        if (operand.getValue() instanceof Instruction instruction1) {
                            anchorInstructions.add(instruction1);
                        }
                    }
                }
            }
        }
        // 所有锚指令需要被重构，以改进其计算效率
        // 这种改进并不总是正面的，常常需要配合ConstantFolding,InstructionCombine,GCM等以达到最好效果
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction mainInstruction : basicBlock.getMainInstructions()) {
                // 必须检查 !isIndestructible(mainInstruction)
                // 如果不检查，则mainInstruction被删除，同时会将所有usage替换为mainInstruction，导致错误
                if (anchorInstructions.contains(mainInstruction) && !isIndestructible(mainInstruction)) {
                    var spawnResult = getSpawnInstructionList(getIngredientList(mainInstruction));
                    var spawnList = spawnResult.a;
                    var resultValue = spawnResult.b;
                    for (Instruction spawnInstruction : spawnList) {
                        spawnInstruction.insertBefore(mainInstruction);
                    }
                    mainInstruction.replaceAllUsageTo(resultValue);
                    mainInstruction.waste();
                }
            }
        }
    }

    public static void runOnModule(Module module) {
        for (Function function : module.getFunctions()) {
            // 所有基本块共用一个实例，以便缓存信息的共享
            var passInstance = new AddToMulPass();
            passInstance.runOnFunction(function);
        }
    }
}
