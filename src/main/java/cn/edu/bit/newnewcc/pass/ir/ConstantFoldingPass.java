package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;
import cn.edu.bit.newnewcc.ir.exception.IllegalStateException;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstBool;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 常量折叠 <br>
 * 若指令的运算结果可以在编译期推导得出，则折叠该语句 <br>
 * <br>
 * 注：对于PHI语句，只折叠其结果为Constant的情况 <br>
 */
public class ConstantFoldingPass {

    /**
     * 折叠语句 <br>
     * 若该语句的结果是定值，则返回该值；否则返回 null <br>
     * 对于 Phi 语句，仅当其结果为 Constant 时才折叠 <br>
     * 对于 Call 语句，折叠在函数内联中进行 <br>
     *
     * @param instruction 待折叠的语句
     * @return 若该语句的结果是定值，则返回该值；否则返回null
     */
    private static Value foldInstruction(Instruction instruction) {
        if (instruction instanceof ArithmeticInst arithmeticInst) {
            var op1 = arithmeticInst.getOperand1();
            var op2 = arithmeticInst.getOperand2();
            if (arithmeticInst instanceof IntegerArithmeticInst) {
                if (arithmeticInst instanceof IntegerAddInst) {
                    // 1+2 = 3
                    if (op1 instanceof ConstInt constInt1 && op2 instanceof ConstInt constInt2) {
                        return ConstInt.getInstance(constInt1.getValue() + constInt2.getValue());
                    }
                    // 0+x = x
                    if (op1 instanceof ConstInt constInt1 && constInt1.getValue() == 0) {
                        return op2;
                    }
                    // x+0 = x
                    if (op2 instanceof ConstInt constInt2 && constInt2.getValue() == 0) {
                        return op1;
                    }
                } else if (arithmeticInst instanceof IntegerSubInst) {
                    // 3-2 = 1
                    if (op1 instanceof ConstInt constInt1 && op2 instanceof ConstInt constInt2) {
                        return ConstInt.getInstance(constInt1.getValue() - constInt2.getValue());
                    }
                    // x-0 = x
                    if (op2 instanceof ConstInt constInt2 && constInt2.getValue() == 0) {
                        return op1;
                    }
                } else if (arithmeticInst instanceof IntegerMultiplyInst) {
                    // 2*3 = 6
                    if (op1 instanceof ConstInt constInt1 && op2 instanceof ConstInt constInt2) {
                        return ConstInt.getInstance(constInt1.getValue() * constInt2.getValue());
                    }
                    // 1*x = x
                    if (op1 instanceof ConstInt constInt1 && constInt1.getValue() == 1) {
                        return op2;
                    }
                    // x*1 = x
                    if (op2 instanceof ConstInt constInt2 && constInt2.getValue() == 1) {
                        return op1;
                    }
                } else if (arithmeticInst instanceof IntegerSignedDivideInst) {
                    // 6/2 = 3
                    if (op1 instanceof ConstInt constInt1 && op2 instanceof ConstInt constInt2) {
                        return ConstInt.getInstance(constInt1.getValue() / constInt2.getValue());
                    }
                    // x/1 = x
                    if (op2 instanceof ConstInt constInt2 && constInt2.getValue() == 1) {
                        return op1;
                    }
                }
            } else if (arithmeticInst instanceof FloatArithmeticInst) {
                if (arithmeticInst instanceof FloatAddInst) {
                    // 1+2 = 3
                    if (op1 instanceof ConstFloat constFloat1 && op2 instanceof ConstFloat constFloat2) {
                        return ConstFloat.getInstance(constFloat1.getValue() + constFloat2.getValue());
                    }
                    // 0+x = x
                    if (op1 instanceof ConstFloat constFloat1 && constFloat1.getValue() == 0) {
                        return op2;
                    }
                    // x+0 = x
                    if (op2 instanceof ConstFloat constFloat2 && constFloat2.getValue() == 0) {
                        return op1;
                    }
                } else if (arithmeticInst instanceof FloatSubInst) {
                    // 3-2 = 1
                    if (op1 instanceof ConstFloat constFloat1 && op2 instanceof ConstFloat constFloat2) {
                        return ConstFloat.getInstance(constFloat1.getValue() - constFloat2.getValue());
                    }
                    // x-0 = x
                    if (op2 instanceof ConstFloat constFloat2 && constFloat2.getValue() == 0) {
                        return op1;
                    }
                } else if (arithmeticInst instanceof FloatMultiplyInst) {
                    // 2*3 = 6
                    if (op1 instanceof ConstFloat constFloat1 && op2 instanceof ConstFloat constFloat2) {
                        return ConstFloat.getInstance(constFloat1.getValue() * constFloat2.getValue());
                    }
                    // 1*x = x
                    if (op1 instanceof ConstFloat constFloat1 && constFloat1.getValue() == 1) {
                        return op2;
                    }
                    // x*1 = x
                    if (op2 instanceof ConstFloat constFloat2 && constFloat2.getValue() == 1) {
                        return op1;
                    }
                } else if (arithmeticInst instanceof FloatDivideInst) {
                    // 6/2 = 3
                    if (op1 instanceof ConstFloat constFloat1 && op2 instanceof ConstFloat constFloat2) {
                        return ConstFloat.getInstance(constFloat1.getValue() / constFloat2.getValue());
                    }
                    // x/1 = x
                    if (op2 instanceof ConstFloat constFloat2 && constFloat2.getValue() == 1) {
                        return op1;
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown type of arithmetic instruction.");
            }
        } else if (instruction instanceof CompareInst compareInst) {
            var op1 = compareInst.getOperand1();
            var op2 = compareInst.getOperand2();
            if (compareInst instanceof IntegerCompareInst integerCompareInst) {
                if (op1 instanceof ConstInt constInt1 && op2 instanceof ConstInt constInt2) {
                    return ConstBool.getInstance(switch (integerCompareInst.getCondition()) {
                        case EQ -> constInt1.getValue() == constInt2.getValue();
                        case NE -> constInt1.getValue() != constInt2.getValue();
                        case SLT -> constInt1.getValue() < constInt2.getValue();
                        case SLE -> constInt1.getValue() <= constInt2.getValue();
                        case SGT -> constInt1.getValue() > constInt2.getValue();
                        case SGE -> constInt1.getValue() >= constInt2.getValue();
                    });
                }
                if (op1 == op2) {
                    return ConstBool.getInstance(switch (integerCompareInst.getCondition()) {
                        case EQ, SGE, SLE -> true;
                        case NE, SGT, SLT -> false;
                    });
                }
                if (integerCompareInst.getCondition() == IntegerCompareInst.Condition.SLE && (
                        (op1 instanceof ConstInt constInt1 && constInt1.getValue() == Integer.MIN_VALUE) ||
                                (op2 instanceof ConstInt constInt2 && constInt2.getValue() == Integer.MAX_VALUE)
                )) {
                    return ConstBool.getInstance(true);
                }
                if (integerCompareInst.getCondition() == IntegerCompareInst.Condition.SGE && (
                        (op1 instanceof ConstInt constInt1 && constInt1.getValue() == Integer.MAX_VALUE) ||
                                (op2 instanceof ConstInt constInt2 && constInt2.getValue() == Integer.MIN_VALUE)
                )) {
                    return ConstBool.getInstance(true);
                }
            } else if (compareInst instanceof FloatCompareInst floatCompareInst) {
                if (op1 instanceof ConstFloat constFloat1 && op2 instanceof ConstFloat constFloat2) {
                    return ConstBool.getInstance(switch (floatCompareInst.getCondition()) {
                        case OEQ -> constFloat1.getValue() == constFloat2.getValue();
                        case ONE -> constFloat1.getValue() != constFloat2.getValue();
                        case OLT -> constFloat1.getValue() < constFloat2.getValue();
                        case OLE -> constFloat1.getValue() <= constFloat2.getValue();
                        case OGT -> constFloat1.getValue() > constFloat2.getValue();
                        case OGE -> constFloat1.getValue() >= constFloat2.getValue();
                    });
                }
                if (op1 == op2) {
                    return ConstBool.getInstance(switch (floatCompareInst.getCondition()) {
                        case OEQ, OGE, OLE -> true;
                        case ONE, OGT, OLT -> false;
                    });
                }
                if (floatCompareInst.getCondition() == FloatCompareInst.Condition.OLE && (
                        (op1 instanceof ConstFloat constFloat1 && constFloat1.getValue() == Float.MIN_VALUE) ||
                                (op2 instanceof ConstFloat constFloat2 && constFloat2.getValue() == Float.MAX_VALUE)
                )) {
                    return ConstBool.getInstance(true);
                }
                if (floatCompareInst.getCondition() == FloatCompareInst.Condition.OGE && (
                        (op1 instanceof ConstFloat constFloat1 && constFloat1.getValue() == Float.MAX_VALUE) ||
                                (op2 instanceof ConstFloat constFloat2 && constFloat2.getValue() == Float.MIN_VALUE)
                )) {
                    return ConstBool.getInstance(true);
                }
            } else {
                throw new IllegalArgumentException("Unknown type of compare instruction.");
            }
        } else if (instruction instanceof FloatNegateInst floatNegateInst) {
            var op = floatNegateInst.getOperand1();
            if (op instanceof ConstFloat constFloat) {
                return ConstFloat.getInstance(-constFloat.getValue());
            }
        } else if (instruction instanceof FloatToSignedIntegerInst floatToSignedIntegerInst) {
            var op = floatToSignedIntegerInst.getSourceOperand();
            if (op instanceof ConstFloat constFloat) {
                return ConstInt.getInstance((int) constFloat.getValue());
            }
        } else if (instruction instanceof PhiInst phiInst) {
            // 此处用到了 ConstInt 和 ConstFloat 都是单例的性质
            Set<Value> valueSet = new HashSet<>();
            for (BasicBlock entry : phiInst.getEntrySet()) {
                valueSet.add(phiInst.getValue(entry));
            }
            if (valueSet.size() == 1) {
                var value = valueSet.iterator().next();
                if (value instanceof Constant) {
                    return value;
                }
            }
        } else if (instruction instanceof SignedIntegerToFloatInst signedIntegerToFloatInst) {
            var op = signedIntegerToFloatInst.getSourceOperand();
            if (op instanceof ConstInt constInt) {
                return ConstFloat.getInstance((float) constInt.getValue());
            }
        } else if (instruction instanceof ZeroExtensionInst zeroExtensionInst) {
            var op = zeroExtensionInst.getSourceOperand();
            if (op instanceof ConstInt constInt && zeroExtensionInst.getTargetType() instanceof IntegerType targetType) {
                if (targetType.getBitWidth() == 1) {
                    return ConstBool.getInstance(constInt.getValue() != 0);
                } else if (targetType.getBitWidth() == 32) {
                    return ConstInt.getInstance(constInt.getValue());
                } else {
                    throw new IllegalStateException("Unknown target type " + targetType);
                }
            }
        }
        return null;
    }

    private static boolean runOnBasicBlock(BasicBlock basicBlock) {
        boolean changed = false;
        for (Instruction instruction : basicBlock.getInstructions()) {
            var foldedValue = foldInstruction(instruction);
            if (foldedValue != null) {
                instruction.replaceAllUsageTo(foldedValue);
                instruction.waste();
                changed = true;
            }
        }
        return changed;
    }

    private static boolean runOnFunction(Function function) {
        boolean changed = false;
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            if (runOnBasicBlock(basicBlock)) {
                changed = true;
            }
        }
        return changed;
    }

    public static boolean runOnModule(Module module) {
        boolean mChanged = false;
        while (true) {
            boolean changed = false;
            for (Function function : module.getFunctions()) {
                if (runOnFunction(function)) {
                    changed = true;
                    mChanged = true;
                }
            }
            if (!changed) break;
        }
        return mChanged;
    }
}