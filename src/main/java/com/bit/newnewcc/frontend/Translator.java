package com.bit.newnewcc.frontend;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.*;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.ir.value.Instruction;
import com.bit.newnewcc.ir.value.VoidValue;
import com.bit.newnewcc.ir.value.constant.ConstFloat;
import com.bit.newnewcc.ir.value.constant.ConstInt;
import com.bit.newnewcc.ir.value.instruction.*;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class Translator extends SysYBaseVisitor<Void> {
    private enum Operator {
        POS, NEG,
        LNOT,
        ADD, SUB, MUL, DIV, MOD,
        LT, GT, LE, GE, EQ, NE,
        LAND, LOR;

        public boolean isArithmetic() {
            return List.of(POS, NEG, ADD, SUB, MUL, DIV, MOD).contains(this);
        }

        public boolean isRelational() {
            return List.of(LT, GT, LE, GE, EQ, NE).contains(this);
        }

        public boolean isLogical() {
            return List.of(LNOT, LAND, LOR).contains(this);
        }
    }

    private final SymbolTable symbolTable = new SymbolTable();
    private Module currentModule;
    private Function currentFunction;
    private BasicBlock currentBasicBlock;
    private Value result;
    private AllocateInst resultAddress;

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

        if (operator.isLogical()) {
            applyTypeConversion(result, IntegerType.getI32());
        }
    }

    public void applyBinaryOperator(Value leftOperand, Value rightOperand, Operator operator) {
        Type operandType;
        if (operator.isLogical()) operandType = IntegerType.getI1();
        else operandType = commonType(leftOperand.getType(), rightOperand.getType());

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
                case ADD -> new FloatAddInst(FloatType.getFloat(), leftOperand, rightOperand);
                case SUB -> new FloatSubInst(FloatType.getFloat(), leftOperand, rightOperand);
                case MUL -> new FloatMultiplyInst(FloatType.getFloat(), leftOperand, rightOperand);
                case DIV -> new FloatDivideInst(FloatType.getFloat(), leftOperand, rightOperand);
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

        if (operator.isRelational() || operator.isLogical()) {
            applyTypeConversion(result, IntegerType.getI32());
        }
    }

    @Override
    public Void visitCompilationUnit(SysYParser.CompilationUnitContext ctx) {
        currentModule = new Module();

        for (var functionDefinition : ctx.functionDefinition()) {
            visit(functionDefinition);
        }

        return null;
    }

    @Override
    public Void visitVariableDeclaration(SysYParser.VariableDeclarationContext ctx) {
        Type type = makeType(ctx.typeSpecifier().type);

        for (var variableDefinition : ctx.variableDefinition()) {
            String name = variableDefinition.Identifier().getText();

            var alloca = new AllocateInst(type);
            currentBasicBlock.addInstruction(alloca);
            symbolTable.putLocalVariable(name, alloca);
        }

        return null;
    }

    @Override
    public Void visitFunctionDefinition(SysYParser.FunctionDefinitionContext ctx) {
        @lombok.Value
        class Parameter {
            Type type;
            String name;
        }

        String name = ctx.Identifier().getText();
        Type returnType = makeType(ctx.typeSpecifier().type);

        List<Parameter> parameters = new ArrayList<>();

        if (ctx.parameterList() != null) {
            for (var parameterDeclaration : ctx.parameterList().parameterDeclaration()) {
                parameters.add(new Parameter(
                        makeType(parameterDeclaration.typeSpecifier().type),
                        parameterDeclaration.Identifier().getText()
                ));
            }
        }

        FunctionType type = FunctionType.getInstance(
                returnType,
                parameters.stream().map(parameter -> parameter.type).toList()
        );

        currentFunction = new Function(type);
        currentFunction.setValueName(name);
        currentModule.addFunction(currentFunction);
        symbolTable.putFunction(name, currentFunction);

        currentBasicBlock = currentFunction.getEntryBasicBlock();

        symbolTable.pushScope();

        for (int i = 0; i < parameters.size(); ++i) {
            Parameter parameter = parameters.get(i);

            var alloca = new AllocateInst(parameter.getType());
            var store = new StoreInst(alloca, currentFunction.getFormalParameters().get(i));

            currentBasicBlock.addInstruction(alloca);
            currentBasicBlock.addInstruction(store);

            symbolTable.putLocalVariable(parameter.getName(), alloca);
        }

        visit(ctx.compoundStatement());

        if (returnType == VoidType.getInstance()) {
            currentBasicBlock.addInstruction(new ReturnInst(VoidValue.getInstance()));
        }

        if (currentBasicBlock.getInstructions().isEmpty()) {
            if (returnType == IntegerType.getI32())
                currentBasicBlock.addInstruction(new ReturnInst(ConstInt.getInstance(0)));
            if (returnType == FloatType.getFloat())
                currentBasicBlock.addInstruction(new ReturnInst(ConstFloat.getInstance(0f)));
        }

        symbolTable.popScope();
        return null;
    }

    @Override
    public Void visitAssignmentStatement(SysYParser.AssignmentStatementContext ctx) {
        visit(ctx.expression());
        Value value = result;

        visit(ctx.lValue());
        var address = resultAddress;

        var store = new StoreInst(address, value);
        currentBasicBlock.addInstruction(store);

        return null;
    }

    @Override
    public Void visitReturnStatement(SysYParser.ReturnStatementContext ctx) {
        visit(ctx.expression());

        if (result.getType() != currentFunction.getReturnType()) {
            applyTypeConversion(result, currentFunction.getReturnType());
        }

        currentBasicBlock.addInstruction(new ReturnInst(result));
        return null;
    }

    @Override
    public Void visitBinaryAdditiveExpression(SysYParser.BinaryAdditiveExpressionContext ctx) {
        visit(ctx.additiveExpression());
        Value leftOperand = result;

        visit(ctx.multiplicativeExpression());
        Value rightOperand = result;

        applyBinaryOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
        return null;
    }

    @Override
    public Void visitBinaryMultiplicativeExpression(SysYParser.BinaryMultiplicativeExpressionContext ctx) {
        visit(ctx.multiplicativeExpression());
        Value leftOperand = result;

        visit(ctx.unaryExpression());
        Value rightOperand = result;

        applyBinaryOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
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

        var load = new LoadInst(resultAddress);
        currentBasicBlock.addInstruction(load);

        result = load;
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
