package com.bit.newnewcc.frontend;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.*;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.ir.value.Instruction;
import com.bit.newnewcc.ir.value.instruction.AllocateInst;
import com.bit.newnewcc.ir.value.instruction.LoadInst;
import com.bit.newnewcc.ir.value.instruction.ReturnInst;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class TranslationVisitor extends SysYBaseVisitor<Void> {
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
        String name = ctx.Identifier().getText();
        Type returnType = makeType(ctx.typeSpecifier().type);

        List<String> parameterNames = new ArrayList<>();
        List<Type> parameterTypes = new ArrayList<>();

        if (ctx.parameterList() != null) {
            for (var parameterDeclaration : ctx.parameterList().parameterDeclaration()) {
                parameterTypes.add(makeType(parameterDeclaration.typeSpecifier().type));
                parameterNames.add(parameterDeclaration.Identifier().getText());
            }
        }

        FunctionType type = FunctionType.getInstance(returnType, parameterTypes);
        currentFunction = new Function(type);
        currentFunction.setValueName(name);
        currentModule.addFunction(currentFunction);
        symbolTable.putFunction(name, currentFunction);

        currentBasicBlock = new BasicBlock();
        currentFunction.addBasicBlock(currentBasicBlock);

        symbolTable.pushScope();

        for (int i = 0; i < parameterTypes.size(); ++i) {
            String parameterName = parameterNames.get(i);
            Type parameterType = parameterTypes.get(i);
            AllocateInst address = new AllocateInst(parameterType);
            currentBasicBlock.addInstruction(address);
            symbolTable.putLocalVariable(parameterName, address);
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
    public Void visitLValue(SysYParser.LValueContext ctx) {
        String name = ctx.Identifier().getText();
        resultAddress = symbolTable.getLocalVariable(name);

        Instruction value = new LoadInst(resultAddress);
        currentBasicBlock.addInstruction(value);
        result = value;

        return null;
    }
}
