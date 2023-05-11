package cn.edu.bit.newnewcc.frontend;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.FunctionType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.constant.VoidValue;
import cn.edu.bit.newnewcc.ir.value.instruction.*;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class Translator extends SysYBaseVisitor<Void> {
    private enum Operator {
        POS, NEG,
        LNOT,
        ADD, SUB, MUL, DIV, MOD,
        LT, GT, LE, GE, EQ, NE,
        LAND, LOR
    }

    private final SymbolTable symbolTable = new SymbolTable();
    private Module currentModule;
    private Function currentFunction;
    private BasicBlock currentBasicBlock;
    private Value result;
    private Value resultAddress;

    public Module getModule() {
        return currentModule;
    }

    private Type makeType(Token token) {
        return switch (token.getType()) {
            case SysYParser.VOID -> VoidType.getInstance();
            case SysYParser.INT -> IntegerType.getI32();
            case SysYParser.FLOAT -> FloatType.getFloat();
            default -> throw new IllegalArgumentException();
        };
    }

    private Operator makeUnaryOperator(Token token) {
        return switch (token.getType()) {
            case SysYParser.ADD -> Operator.POS;
            case SysYParser.SUB -> Operator.NEG;
            case SysYParser.LNOT -> Operator.LNOT;
            default -> throw new IllegalArgumentException();
        };
    }

    private Operator makeBinaryOperator(Token token) {
        return switch (token.getType()) {
            case SysYParser.ADD -> Operator.ADD;
            case SysYParser.SUB -> Operator.SUB;
            case SysYParser.MUL -> Operator.MUL;
            case SysYParser.DIV -> Operator.DIV;
            case SysYParser.MOD -> Operator.MOD;
            case SysYParser.LT -> Operator.LT;
            case SysYParser.GT -> Operator.GT;
            case SysYParser.LE -> Operator.LE;
            case SysYParser.GE -> Operator.GE;
            case SysYParser.EQ -> Operator.EQ;
            case SysYParser.NE -> Operator.NE;
            case SysYParser.LAND -> Operator.LAND;
            case SysYParser.LOR -> Operator.LOR;
            default -> throw new IllegalArgumentException();
        };
    }

    private Type commonType(Type firstType, Type secondType) {
        if (firstType.equals(secondType)) return firstType;
        if (firstType == IntegerType.getI32() && secondType == FloatType.getFloat()) return FloatType.getFloat();
        if (firstType == FloatType.getFloat() && secondType == IntegerType.getI32()) return FloatType.getFloat();
        throw new IllegalArgumentException();
    }

    private void applyTypeConversion(Value value, Type targetType) {
        Instruction instruction;

        if (value.getType() == IntegerType.getI32() && targetType == FloatType.getFloat())
            instruction = new SignedIntegerToFloatInst(value, FloatType.getFloat());
        else if (value.getType() == FloatType.getFloat() && targetType == IntegerType.getI32())
            instruction = new FloatToSignedIntegerInst(value, IntegerType.getI32());
        else if (value.getType() == IntegerType.getI32() && targetType == IntegerType.getI1())
            instruction = new IntegerCompareInst(
                    IntegerType.getI32(), IntegerCompareInst.Condition.NE,
                    value, ConstInt.getInstance(0)
            );
        else if (value.getType() == FloatType.getFloat() && targetType == IntegerType.getI1())
            instruction = new FloatCompareInst(
                    FloatType.getFloat(), FloatCompareInst.Condition.ONE,
                    value, ConstFloat.getInstance(0f)
            );
        else if (value.getType() == IntegerType.getI1() && targetType == IntegerType.getI32())
            instruction = new ZeroExtensionInst(value, IntegerType.getI32());
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction(instruction);
        result = instruction;
    }

    private void applyUnaryOperator(Value operand, Operator operator) {
        if (operator == Operator.POS) {
            result = operand;
            return;
        }

        Instruction instruction;
        if (operand.getType() == IntegerType.getI32())
            instruction = switch (operator) {
                case NEG -> new IntegerSubInst(IntegerType.getI32(), ConstInt.getInstance(0), operand);
                case LNOT -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.EQ,
                        operand, ConstInt.getInstance(0)
                );
                default -> throw new IllegalArgumentException();
            };
        else if (operand.getType() == FloatType.getFloat())
            instruction = switch (operator) {
                case NEG -> new FloatNegateInst(FloatType.getFloat(), operand);
                case LNOT -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OEQ,
                        operand, ConstFloat.getInstance(0f)
                );
                default -> throw new IllegalArgumentException();
            };
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction(instruction);
        result = instruction;

        if (operator == Operator.LNOT)
            applyTypeConversion(result, IntegerType.getI32());
    }

    public void applyBinaryArithmeticOperator(Value leftOperand, Value rightOperand, Operator operator) {
        Type operandType = commonType(leftOperand.getType(), rightOperand.getType());

        if (leftOperand.getType() != operandType) {
            applyTypeConversion(leftOperand, operandType);
            leftOperand = result;
        }

        if (rightOperand.getType() != operandType) {
            applyTypeConversion(rightOperand, operandType);
            rightOperand = result;
        }

        Instruction instruction;
        if (operandType == IntegerType.getI32())
            instruction = switch (operator) {
                case ADD -> new IntegerAddInst(IntegerType.getI32(), leftOperand, rightOperand);
                case SUB -> new IntegerSubInst(IntegerType.getI32(), leftOperand, rightOperand);
                case MUL -> new IntegerMultiplyInst(IntegerType.getI32(), leftOperand, rightOperand);
                case DIV -> new IntegerSignedDivideInst(IntegerType.getI32(), leftOperand, rightOperand);
                case MOD -> new IntegerSignedRemainderInst(IntegerType.getI32(), leftOperand, rightOperand);
                default -> throw new IllegalArgumentException();
            };
        else if (operandType == FloatType.getFloat())
            instruction = switch (operator) {
                case ADD -> new FloatAddInst(FloatType.getFloat(), leftOperand, rightOperand);
                case SUB -> new FloatSubInst(FloatType.getFloat(), leftOperand, rightOperand);
                case MUL -> new FloatMultiplyInst(FloatType.getFloat(), leftOperand, rightOperand);
                case DIV -> new FloatDivideInst(FloatType.getFloat(), leftOperand, rightOperand);
                default -> throw new IllegalArgumentException();
            };
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction(instruction);
        result = instruction;
    }

    public void applyBinaryRelationalOperator(Value leftOperand, Value rightOperand, Operator operator) {
        Type operandType = commonType(leftOperand.getType(), rightOperand.getType());

        if (leftOperand.getType() != operandType) {
            applyTypeConversion(leftOperand, operandType);
            leftOperand = result;
        }

        if (rightOperand.getType() != operandType) {
            applyTypeConversion(rightOperand, operandType);
            rightOperand = result;
        }

        Instruction instruction;
        if (operandType == IntegerType.getI32())
            instruction = switch (operator) {
                case LT -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.SLT,
                        leftOperand, rightOperand
                );
                case GT -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.SGT,
                        leftOperand, rightOperand
                );
                case LE -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.SLE,
                        leftOperand, rightOperand
                );
                case GE -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.SGE,
                        leftOperand, rightOperand
                );
                case EQ -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.EQ,
                        leftOperand, rightOperand
                );
                case NE -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.NE,
                        leftOperand, rightOperand
                );
                default -> throw new IllegalArgumentException();
            };
        else if (operandType == FloatType.getFloat())
            instruction = switch (operator) {
                case LT -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OLT,
                        leftOperand, rightOperand
                );
                case GT -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OGT,
                        leftOperand, rightOperand
                );
                case LE -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OLE,
                        leftOperand, rightOperand
                );
                case GE -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OGE,
                        leftOperand, rightOperand
                );
                case EQ -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OEQ,
                        leftOperand, rightOperand
                );
                case NE -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.ONE,
                        leftOperand, rightOperand
                );
                default -> throw new IllegalArgumentException();
            };
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction(instruction);
        result = instruction;
        applyTypeConversion(result, IntegerType.getI32());
    }

    @Override
    public Void visitCompilationUnit(SysYParser.CompilationUnitContext ctx) {
        currentModule = new Module();

        for (var functionDefinition : ctx.functionDefinition())
            visit(functionDefinition);

        return null;
    }

    @Override
    public Void visitVariableDeclaration(SysYParser.VariableDeclarationContext ctx) {
        Type type = makeType(ctx.typeSpecifier().type);

        for (var variableDefinition : ctx.variableDefinition()) {
            String name = variableDefinition.Identifier().getText();

            var address = new AllocateInst(type);
            currentBasicBlock.addInstruction(address);
            symbolTable.putLocalVariable(name, address);
        }

        return null;
    }

    @Override
    public Void visitFunctionDefinition(SysYParser.FunctionDefinitionContext ctx) {
        String name = ctx.Identifier().getText();

        Type returnType = makeType(ctx.typeSpecifier().type);

        List<Type> parameterTypes = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for (var parameterDeclaration : ctx.parameterList().parameterDeclaration()) {
                parameterTypes.add(makeType(parameterDeclaration.typeSpecifier().type));
            }
        }

        FunctionType type = FunctionType.getInstance(returnType, parameterTypes);

        currentFunction = new Function(type);
        currentFunction.setValueName(name);
        currentModule.addFunction(currentFunction);
        symbolTable.putFunction(name, currentFunction);
        currentBasicBlock = currentFunction.getEntryBasicBlock();

        visit(ctx.compoundStatement());

        if (currentBasicBlock.getInstructions().isEmpty()) {
            if (returnType == VoidType.getInstance())
                currentBasicBlock.addInstruction(new ReturnInst(VoidValue.getInstance()));
            if (returnType == IntegerType.getI32())
                currentBasicBlock.addInstruction(new ReturnInst(ConstInt.getInstance(0)));
            if (returnType == FloatType.getFloat())
                currentBasicBlock.addInstruction(new ReturnInst(ConstFloat.getInstance(0f)));
        }

        return null;
    }

    @Override
    public Void visitCompoundStatement(SysYParser.CompoundStatementContext ctx) {
        symbolTable.pushScope();

        if (ctx.getParent() instanceof SysYParser.FunctionDefinitionContext functionDefinition) {
            @lombok.Value
            class Parameter {
                Type type;
                String name;
            }

            List<Parameter> parameters = new ArrayList<>();
            if (functionDefinition.parameterList() != null) {
                for (var parameterDeclaration : functionDefinition.parameterList().parameterDeclaration()) {
                    parameters.add(new Parameter(
                            makeType(parameterDeclaration.typeSpecifier().type),
                            parameterDeclaration.Identifier().getText()
                    ));
                }
            }

            for (int i = 0; i < parameters.size(); ++i) {
                Parameter parameter = parameters.get(i);

                var address = new AllocateInst(parameter.getType());
                currentBasicBlock.addInstruction(address);
                currentBasicBlock.addInstruction(new StoreInst(address, currentFunction.getFormalParameters().get(i)));

                symbolTable.putLocalVariable(parameter.getName(), address);
            }
        }

        if (ctx.blockItem() != null) {
            for (var blockItem : ctx.blockItem()) {
                visit(blockItem);
            }
        }

        symbolTable.popScope();
        return null;
    }

    @Override
    public Void visitAssignmentStatement(SysYParser.AssignmentStatementContext ctx) {
        visit(ctx.expression());
        Value value = result;

        visit(ctx.lValue());
        Value address = resultAddress;

        currentBasicBlock.addInstruction(new StoreInst(address, value));

        return null;
    }

    @Override
    public Void visitIfStatement(SysYParser.IfStatementContext ctx) {
        BasicBlock thenBlock = new BasicBlock();
        BasicBlock elseBlock = new BasicBlock();
        BasicBlock doneBlock = new BasicBlock();

        currentFunction.addBasicBlock(thenBlock);
        currentFunction.addBasicBlock(elseBlock);
        currentFunction.addBasicBlock(doneBlock);

        visit(ctx.expression());
        if (result.getType() != IntegerType.getI1())
            applyTypeConversion(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new BranchInst(result, thenBlock, elseBlock));

        currentBasicBlock = thenBlock;
        visit(ctx.statement(0));
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = elseBlock;
        if (ctx.statement().size() == 2) {
            visit(ctx.statement(1));
        }
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = doneBlock;
        return null;
    }

    @Override
    public Void visitWhileStatement(SysYParser.WhileStatementContext ctx) {
        BasicBlock testBlock = new BasicBlock();
        BasicBlock bodyBlock = new BasicBlock();
        BasicBlock doneBlock = new BasicBlock();

        currentFunction.addBasicBlock(testBlock);
        currentFunction.addBasicBlock(bodyBlock);
        currentFunction.addBasicBlock(doneBlock);

        currentBasicBlock.addInstruction(new JumpInst(testBlock));

        currentBasicBlock = testBlock;

        visit(ctx.expression());
        if (result.getType() != IntegerType.getI1())
            applyTypeConversion(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new BranchInst(result, bodyBlock, doneBlock));

        currentBasicBlock = bodyBlock;

        visit(ctx.statement());

        currentBasicBlock.addInstruction(new JumpInst(testBlock));

        currentBasicBlock = doneBlock;
        return null;
    }

    @Override
    public Void visitReturnStatement(SysYParser.ReturnStatementContext ctx) {
        if (ctx.expression() != null) {
            visit(ctx.expression());

            if (result.getType() != currentFunction.getReturnType()) {
                applyTypeConversion(result, currentFunction.getReturnType());
            }

            currentBasicBlock.addInstruction(new ReturnInst(result));
        } else
            currentBasicBlock.addInstruction(new ReturnInst(VoidValue.getInstance()));

        currentBasicBlock = new BasicBlock();
        currentFunction.addBasicBlock(currentBasicBlock);
        return null;
    }

    @Override
    public Void visitBinaryLogicalOrExpression(SysYParser.BinaryLogicalOrExpressionContext ctx) {
        BasicBlock falseBlock = new BasicBlock();
        BasicBlock doneBlock = new BasicBlock();

        currentFunction.addBasicBlock(falseBlock);
        currentFunction.addBasicBlock(doneBlock);

        var address = new AllocateInst(IntegerType.getI1());
        currentBasicBlock.addInstruction(address);

        visit(ctx.logicalOrExpression());
        if (result.getType() != IntegerType.getI1())
            applyTypeConversion(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new BranchInst(result, doneBlock, falseBlock));

        currentBasicBlock = falseBlock;

        visit(ctx.logicalAndExpression());
        if (result.getType() != IntegerType.getI1())
            applyTypeConversion(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = doneBlock;

        var value = new LoadInst(address);
        currentBasicBlock.addInstruction(value);
        result = value;

        return null;
    }

    @Override
    public Void visitBinaryLogicalAndExpression(SysYParser.BinaryLogicalAndExpressionContext ctx) {
        BasicBlock trueBlock = new BasicBlock();
        BasicBlock doneBlock = new BasicBlock();

        currentFunction.addBasicBlock(trueBlock);
        currentFunction.addBasicBlock(doneBlock);

        var address = new AllocateInst(IntegerType.getI1());
        currentBasicBlock.addInstruction(address);

        visit(ctx.logicalAndExpression());
        if (result.getType() != IntegerType.getI1())
            applyTypeConversion(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new BranchInst(result, trueBlock, doneBlock));

        currentBasicBlock = trueBlock;

        visit(ctx.equalityExpression());
        if (result.getType() != IntegerType.getI1())
            applyTypeConversion(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = doneBlock;

        var value = new LoadInst(address);
        currentBasicBlock.addInstruction(value);
        result = value;

        return null;
    }

    @Override
    public Void visitBinaryEqualityExpression(SysYParser.BinaryEqualityExpressionContext ctx) {
        visit(ctx.equalityExpression());
        Value leftOperand = result;

        visit(ctx.relationalExpression());
        Value rightOperand = result;

        applyBinaryRelationalOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
        return null;
    }

    @Override
    public Void visitBinaryRelationalExpression(SysYParser.BinaryRelationalExpressionContext ctx) {
        visit(ctx.relationalExpression());
        Value leftOperand = result;

        visit(ctx.additiveExpression());
        Value rightOperand = result;

        applyBinaryRelationalOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
        return null;
    }

    @Override
    public Void visitBinaryAdditiveExpression(SysYParser.BinaryAdditiveExpressionContext ctx) {
        visit(ctx.additiveExpression());
        Value leftOperand = result;

        visit(ctx.multiplicativeExpression());
        Value rightOperand = result;

        applyBinaryArithmeticOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
        return null;
    }

    @Override
    public Void visitBinaryMultiplicativeExpression(SysYParser.BinaryMultiplicativeExpressionContext ctx) {
        visit(ctx.multiplicativeExpression());
        Value leftOperand = result;

        visit(ctx.unaryExpression());
        Value rightOperand = result;

        applyBinaryArithmeticOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
        return null;
    }

    @Override
    public Void visitUnaryOperatorExpression(SysYParser.UnaryOperatorExpressionContext ctx) {
        visit(ctx.unaryExpression());
        applyUnaryOperator(result, makeUnaryOperator(ctx.unaryOperator().op));
        return null;
    }

    @Override
    public Void visitLValue(SysYParser.LValueContext ctx) {
        String name = ctx.Identifier().getText();
        resultAddress = symbolTable.getLocalVariable(name);

        var value = new LoadInst(resultAddress);
        currentBasicBlock.addInstruction(value);

        result = value;
        return null;
    }

    @Override
    public Void visitIntegerConstant(SysYParser.IntegerConstantContext ctx) {
        result = ConstInt.getInstance(Integer.parseInt(ctx.IntegerConstant().getText()));
        return null;
    }

    @Override
    public Void visitFloatingConstant(SysYParser.FloatingConstantContext ctx) {
        result = ConstFloat.getInstance(Float.parseFloat(ctx.FloatingConstant().getText()));
        return null;
    }
}
