package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.CompilationProcessCheckFailedException;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerAddInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerMultiplyInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerSubInst;
import cn.edu.bit.newnewcc.ir.value.instruction.ShiftLeftInst;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 常数乘法强度削减
 */
public class ConstIntMultiplyReducePass {

    /**
     * 构建乘法值的操作数
     */
    private static abstract class Operand {
        /**
         * 操作的等级，每进行一次算数运算加一级
         */
        public final int level;

        protected Operand(int level) {
            this.level = level;
        }

    }

    /**
     * 常数
     */
    private static class ConstantOperand extends Operand {
        public final int value;

        public ConstantOperand(int value) {
            super(0);
            this.value = value;
        }

    }

    /**
     * 乘法的原始值
     */
    private static class VariableOperand extends Operand {
        private VariableOperand() {
            super(0);
        }

        private static final VariableOperand instance = new VariableOperand();

        public static VariableOperand getInstance() {
            return instance;
        }

    }

    /**
     * 用于标记某个值不可削减
     */
    private static class NotReducible extends Operand {
        private NotReducible() {
            super(0x3fffffff); // 使用该值以避免两个值相加产生溢出
        }

        private static final NotReducible instance = new NotReducible();

        public static NotReducible getInstance() {
            return instance;
        }

    }

    /**
     * 另一个操作作为操作数
     */
    private static class Operation extends Operand {
        public enum Type {
            ADD, SUB, SHL
        }

        public final Type type;
        public final Operand operand1, operand2;

        public Operation(Type type, Operand operand1, Operand operand2) {
            super(operand1.level + operand2.level + 1);
            this.type = type;
            this.operand1 = operand1;
            this.operand2 = operand2;
        }

    }

    private static final int OPERATION_LIMIT = 5;

    private static Map<Integer, Operand> operandMap = new HashMap<>();

    private static boolean isInitialized = false;

    private static void addOperationIfPossible(ArrayList<Pair<Integer, Operand>> levelLdOperations, int idAdd, Operand odAdd) {
        if (!operandMap.containsKey(idAdd)) {
            operandMap.put(idAdd, odAdd);
            levelLdOperations.add(new Pair<>(idAdd, odAdd));
        }
    }

    private static void initialize() {
        ArrayList<ArrayList<Pair<Integer, Operand>>> operations = new ArrayList<>();
        var level0Operations = new ArrayList<Pair<Integer, Operand>>();
        addOperationIfPossible(level0Operations, 0, new ConstantOperand(0));
        addOperationIfPossible(level0Operations, 1, VariableOperand.getInstance());
        operations.add(level0Operations);
        // ld -> level dest
        // id -> integer dest
        // i1 -> integer 1
        // o1 -> operand 1
        for (int ld = 1; ld <= OPERATION_LIMIT; ld++) {
            var levelLdOperations = new ArrayList<Pair<Integer, Operand>>();
            for (int l1 = 0; l1 < ld; l1++) {
                for (int l2 = 0; l1 + l2 < ld; l2++) {
                    // ADD, SUB
                    for (var pair1 : operations.get(l1)) {
                        var i1 = pair1.a;
                        var o1 = pair1.b;
                        for (var pair2 : operations.get(l2)) {
                            var i2 = pair2.a;
                            var o2 = pair2.b;
                            var idAdd = i1 + i2;
                            var odAdd = new Operation(Operation.Type.ADD, o1, o2);
                            addOperationIfPossible(levelLdOperations, idAdd, odAdd);
                            var idSub = i1 - i2;
                            var odSub = new Operation(Operation.Type.SUB, o1, o2);
                            addOperationIfPossible(levelLdOperations, idSub, odSub);
                        }
                    }
                }
                // SHL
                for (var pair1 : operations.get(l1)) {
                    var i1 = pair1.a;
                    var o1 = pair1.b;
                    if (i1 == 0) continue;
                    for (int i = 0; ; i++) {
                        var idShl = i1 << i;
                        if (i != 0) {
                            var odShl = new Operation(Operation.Type.SHL, o1, new ConstantOperand(i));
                            addOperationIfPossible(levelLdOperations, idShl, odShl);
                        }
                        if ((idShl & (1 << 31)) != 0) break;
                    }
                }
            }
            operations.add(levelLdOperations);
        }
        for (ArrayList<Pair<Integer, Operand>> operationList : operations) {
            for (Pair<Integer, Operand> pair : operationList) {
                operandMap.put(pair.a, pair.b);
            }
        }
        isInitialized = true;
    }

    private static Operand reduceValue(int multipliedConstant) {
        return operandMap.getOrDefault(multipliedConstant, NotReducible.getInstance());
    }

    private static Pair<Value, ArrayList<Instruction>> generateConstructInstructions(Value variable, Operand plan) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        if (plan instanceof VariableOperand) {
            return new Pair<>(variable, instructions);
        } else if (plan instanceof ConstantOperand constantOperand) {
            return new Pair<>(ConstInt.getInstance(constantOperand.value), instructions);
        } else if (plan instanceof Operation operation) {
            var pair1 = generateConstructInstructions(variable, operation.operand1);
            var pair2 = generateConstructInstructions(variable, operation.operand2);
            instructions.addAll(pair1.b);
            instructions.addAll(pair2.b);
            var finalInstruction = switch (operation.type) {
                case ADD -> new IntegerAddInst(IntegerType.getI32(), pair1.a, pair2.a);
                case SUB -> new IntegerSubInst(IntegerType.getI32(), pair1.a, pair2.a);
                case SHL -> new ShiftLeftInst(IntegerType.getI32(), pair1.a, pair2.a);
            };
            instructions.add(finalInstruction);
            return new Pair<>(finalInstruction, instructions);
        } else {
            throw new CompilationProcessCheckFailedException("Unknown type of operand: " + plan.getClass());
        }
    }

    public static boolean runOnInstruction(Instruction instruction) {
        if (!(instruction instanceof IntegerMultiplyInst integerMultiplyInst
                && integerMultiplyInst.getOperand2() instanceof ConstInt multipliedConstant)) return false;
        if (!isInitialized) initialize();
        Operand plan = reduceValue(multipliedConstant.getValue());
        if (plan instanceof NotReducible) return false;
        var pair = generateConstructInstructions(integerMultiplyInst.getOperand1(), plan);
        var finalValue = pair.a;
        var instructionList = pair.b;
        for (Instruction constructingInstruction : instructionList) {
            constructingInstruction.insertBefore(integerMultiplyInst);
        }
        integerMultiplyInst.replaceAllUsageTo(finalValue);
        integerMultiplyInst.waste();
        return true;
    }

    public static boolean runOnBasicBlock(BasicBlock basicBlock) {
        boolean changed = false;
        for (Instruction instruction : basicBlock.getInstructions()) {
            changed |= runOnInstruction(instruction);
        }
        return changed;
    }

    public static boolean runOnFunction(Function function) {
        boolean changed = false;
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            changed |= runOnBasicBlock(basicBlock);
        }
        return changed;
    }

    public static boolean runOnModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctions()) {
            changed |= runOnFunction(function);
        }
        return changed;
    }

}
