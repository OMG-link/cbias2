package com.bit.newnewcc.frontend;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.*;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.ir.value.Instruction;
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
        if (value.getType() == IntegerType.getI32() && targetType == FloatType.getFloat()) {
            SignedIntegerToFloatInst sitofp = new SignedIntegerToFloatInst(value, FloatType.getFloat());
            currentBasicBlock.addInstruction(sitofp);
            result = sitofp;
            return;
        }
        if (value.getType() == FloatType.getFloat() && targetType == IntegerType.getI32()) {
            FloatToSignedIntegerInst fptosi = new FloatToSignedIntegerInst(value, IntegerType.getI32());
            currentBasicBlock.addInstruction(fptosi);
            result = fptosi;
            return;
        }
        if (value.getType() == IntegerType.getI32() && targetType == IntegerType.getI1()) {
            IntegerCompareInst icmp = new IntegerCompareInst(
                    IntegerType.getI32(), IntegerCompareInst.Condition.NE,
                    value, ConstInt.getInstance(0)
            );
            currentBasicBlock.addInstruction(icmp);
            result = icmp;
            return;
        }
        if (value.getType() == FloatType.getFloat() && targetType == IntegerType.getI1()) {
            FloatCompareInst fcmp = new FloatCompareInst(
                    FloatType.getFloat(), FloatCompareInst.Condition.ONE,
                    value, ConstFloat.getInstance(0f)
            );
            currentBasicBlock.addInstruction(fcmp);
            result = fcmp;
            return;
        }
        if (value.getType() == IntegerType.getI1() && targetType == IntegerType.getI32()) {
            ZeroExtensionInst zext = new ZeroExtensionInst(value, IntegerType.getI32());
            currentBasicBlock.addInstruction(zext);
            result = zext;
            return;
        }
        throw new IllegalArgumentException();
    }

    private void applyUnaryOperator(Value operand, Operator operator) {
        switch (operator) {
            case POS -> result = operand;
            case NEG -> {
                if (operand.getType() == IntegerType.getI32()) {
                    IntegerSubInst sub = new IntegerSubInst(IntegerType.getI32(), ConstInt.getInstance(0), operand);
                    currentBasicBlock.addInstruction(sub);
                    result = sub;
                    return;
                }
                if (operand.getType() == FloatType.getFloat()) {
                    FloatNegateInst fneg = new FloatNegateInst(FloatType.getFloat(), operand);
                    currentBasicBlock.addInstruction(fneg);
                    result = fneg;
                    return;
                }
                throw new IllegalArgumentException();
            }
            case LNOT -> {
                if (operand.getType() == IntegerType.getI32()) {
                    IntegerCompareInst icmp = new IntegerCompareInst(
                            IntegerType.getI32(), IntegerCompareInst.Condition.EQ,
                            operand, ConstInt.getInstance(0)
                    );
                    currentBasicBlock.addInstruction(icmp);
                    result = icmp;
                    return;
                }
                if (operand.getType() == FloatType.getFloat()) {
                    FloatCompareInst fcmp = new FloatCompareInst(
                            FloatType.getFloat(), FloatCompareInst.Condition.OEQ,
                            operand, ConstFloat.getInstance(0f)
                    );
                    currentBasicBlock.addInstruction(fcmp);
                    result = fcmp;
                    applyTypeConversion(result, IntegerType.getI32());
                    return;
                }
                throw new IllegalArgumentException();
            }
            default -> throw new IllegalArgumentException();
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

        Instruction instruction = switch (operator) {
            case ADD -> {
                if (operandType == IntegerType.getI32())
                    yield new IntegerAddInst(IntegerType.getI32(), leftOperand, rightOperand);
                else if (operandType == FloatType.getFloat())
                    yield new FloatAddInst(FloatType.getFloat(), leftOperand, rightOperand);
                else
                    throw new IllegalArgumentException();
            }
            case SUB -> {
                if (operandType == IntegerType.getI32())
                    yield new IntegerSubInst(IntegerType.getI32(), leftOperand, rightOperand);
                else if (operandType == FloatType.getFloat())
                    yield new FloatSubInst(FloatType.getFloat(), leftOperand, rightOperand);
                else
                    throw new IllegalArgumentException();
            }
            case MUL -> {
                if (operandType == IntegerType.getI32())
                    yield new IntegerMultiplyInst(IntegerType.getI32(), leftOperand, rightOperand);
                else if (operandType == FloatType.getFloat())
                    yield new FloatMultiplyInst(FloatType.getFloat(), leftOperand, rightOperand);
                else
                    throw new IllegalArgumentException();
            }
            case DIV -> {
                if (operandType == IntegerType.getI32())
                    yield new IntegerSignedDivideInst(IntegerType.getI32(), leftOperand, rightOperand);
                else if (operandType == FloatType.getFloat())
                    yield new FloatDivideInst(FloatType.getFloat(), leftOperand, rightOperand);
                else
                    throw new IllegalArgumentException();
            }
            default -> throw new IllegalArgumentException();
        };

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

            AllocateInst alloca = new AllocateInst(parameter.getType());
            StoreInst store = new StoreInst(alloca, currentFunction.getFormalParameters().get(i));

            currentBasicBlock.addInstruction(alloca);
            currentBasicBlock.addInstruction(store);

            symbolTable.putLocalVariable(parameter.getName(), alloca);
        }

        visit(ctx.compoundStatement());

        symbolTable.popScope();
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
        visit(ctx.multiplicativeExpression());
        Value leftOperand = result;

        visit(ctx.additiveExpression());
        Value rightOperand = result;

        applyBinaryOperator(leftOperand, rightOperand, makeBinaryOperator(ctx.op));
        return null;
    }

    @Override
    public Void visitBinaryMultiplicativeExpression(SysYParser.BinaryMultiplicativeExpressionContext ctx) {
        visit(ctx.unaryExpression());
        Value leftOperand = result;

        visit(ctx.multiplicativeExpression());
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

        LoadInst load = new LoadInst(resultAddress);
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
