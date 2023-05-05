package com.bit.newnewcc.frontend;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.*;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Function;
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
        LAND, LOR
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
            SignedIntegerToFloatInst sitofp = new SignedIntegerToFloatInst(IntegerType.getI32(), FloatType.getFloat());
            currentBasicBlock.addInstruction(sitofp);
            result = sitofp;
        }
        throw new IllegalArgumentException();
    }

    private void applyUnaryOperator(Value value, Operator operator) {
        switch (operator) {
            case POS -> result = value;
            case NEG -> {
                if (value.getType() == IntegerType.getI32()) {
                    IntegerSubInst sub = new IntegerSubInst(IntegerType.getI32(), ConstInt.getInstance(0), value);
                    currentBasicBlock.addInstruction(sub);
                    result = sub;
                    return;
                }
                if (value.getType() == FloatType.getFloat()) {
                    FloatNegateInst fneg = new FloatNegateInst(FloatType.getFloat(), value);
                    currentBasicBlock.addInstruction(fneg);
                    result = fneg;
                    return;
                }
                throw new IllegalArgumentException();
            }
            case LNOT -> {
                if (value.getType() == IntegerType.getI32()) {
                    IntegerCompareInst icmp = new IntegerCompareInst(
                            IntegerType.getI32(), IntegerCompareInst.Condition.EQ,
                            value, ConstInt.getInstance(0)
                    );
                    currentBasicBlock.addInstruction(icmp);
                    result = icmp;
                    return;
                }
                if (value.getType() == FloatType.getFloat()) {
                    FloatCompareInst fcmp = new FloatCompareInst(
                            FloatType.getFloat(), FloatCompareInst.Condition.OEQ,
                            value, ConstFloat.getInstance(0f)
                    );
                    currentBasicBlock.addInstruction(fcmp);
                    result = fcmp;
                    return;
                }
                throw new IllegalArgumentException();
            }
            default -> throw new IllegalArgumentException();
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
        currentBasicBlock.addInstruction(new ReturnInst(result));
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
