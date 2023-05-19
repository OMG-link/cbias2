package cn.edu.bit.newnewcc.frontend;

import cn.edu.bit.newnewcc.frontend.antlr.SysYBaseVisitor;
import cn.edu.bit.newnewcc.frontend.antlr.SysYParser;
import cn.edu.bit.newnewcc.frontend.util.Constants;
import cn.edu.bit.newnewcc.frontend.util.Types;
import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.*;
import cn.edu.bit.newnewcc.ir.value.*;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.constant.VoidValue;
import cn.edu.bit.newnewcc.ir.value.instruction.*;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class Translator extends SysYBaseVisitor<Void> {
    @lombok.Value
    private static class Parameter {
        Type type;
        String name;
    }

    private final SymbolTable symbolTable = new SymbolTable();
    private final ControlFlowStack controlFlowStack = new ControlFlowStack();
    private final Module module = new Module();

    {
        List.of(
                new ExternalFunction(
                        FunctionType.getInstance(IntegerType.getI32(), List.of()),
                        "getint"
                ),
                new ExternalFunction(
                        FunctionType.getInstance(VoidType.getInstance(), List.of(IntegerType.getI32())),
                        "putint"
                ),
                new ExternalFunction(
                        FunctionType.getInstance(FloatType.getFloat(), List.of()),
                        "getfloat"
                ),
                new ExternalFunction(
                        FunctionType.getInstance(VoidType.getInstance(), List.of(FloatType.getFloat())),
                        "putfloat"
                ),
                new ExternalFunction(
                        FunctionType.getInstance(IntegerType.getI32(), List.of()),
                        "getch"
                ),
                new ExternalFunction(
                        FunctionType.getInstance(VoidType.getInstance(), List.of(IntegerType.getI32())),
                        "putch"
                )
        ).forEach(externalFunction -> {
            module.addExternalFunction(externalFunction);
            symbolTable.putFunction(externalFunction.getValueName(), externalFunction);
        });
    }

    private Function currentFunction;
    private BasicBlock currentBasicBlock;
    private Value result;
    private Value resultAddress;

    public Module getModule() {
        return module;
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

    private List<Parameter> makeParameterList(SysYParser.ParameterListContext ctx) {
        List<Parameter> parameters = new ArrayList<>();

        for (var parameterDeclaration : ctx.parameterDeclaration()) {
            Type type = makeType(parameterDeclaration.typeSpecifier().type);

            if (!parameterDeclaration.LBRACKET().isEmpty()) {
                var listIterator = parameterDeclaration.expression()
                        .listIterator(parameterDeclaration.expression().size());
                while (listIterator.hasPrevious()) {
                    visit(listIterator.previous());

                    type = ArrayType.getInstance(((ConstInt) result).getValue(), type);
                }

                type = PointerType.getInstance(type);
            }

            String name = parameterDeclaration.Identifier().getText();

            parameters.add(new Parameter(type, name));
        }

        return parameters;
    }

    private Constant makeConstant(SysYParser.InitializerContext initializer, Type type) {
        if (initializer.expression() != null) {
            visit(initializer.expression());
            Constant constant = (Constant) result;

            if (constant.getType() != type)
                constant = Constants.convertType(constant, type);

            return constant;
        }

        return makeConstant(initializer.initializer(), (ArrayType) type);
    }

    private Constant makeConstant(List<SysYParser.InitializerContext> childInitializers, ArrayType type) {
        List<Constant> elements = new ArrayList<>();
        var listIterator = childInitializers.listIterator();

        while (listIterator.hasNext()) {
            var elementInitializer = listIterator.next();

            if (elementInitializer.expression() != null && type.getBaseType() instanceof ArrayType) {
                int count = Types.countElements(type.getBaseType());
                List<SysYParser.InitializerContext> terminalInitializers = new ArrayList<>();
                terminalInitializers.add(elementInitializer);

                for (int i = 0; i < count - 1 && listIterator.hasNext(); ++i) {
                    var nextElementInitializer = listIterator.next();

                    if (nextElementInitializer.expression() != null)
                        terminalInitializers.add(nextElementInitializer);
                    else {
                        listIterator.previous();
                        break;
                    }
                }

                elements.add(makeConstant(terminalInitializers, (ArrayType) type.getBaseType()));
            } else
                elements.add(makeConstant(elementInitializer, type.getBaseType()));
        }

        return new ConstArray(type.getBaseType(), type.getLength(), elements);
    }

    private void convertType(Value value, Type targetType) {
        if (value.getType() == IntegerType.getI32() && targetType == FloatType.getFloat())
            result = new SignedIntegerToFloatInst(value, FloatType.getFloat());
        else if (value.getType() == FloatType.getFloat() && targetType == IntegerType.getI32())
            result = new FloatToSignedIntegerInst(value, IntegerType.getI32());
        else if (value.getType() == IntegerType.getI32() && targetType == IntegerType.getI1())
            result = new IntegerCompareInst(
                    IntegerType.getI32(), IntegerCompareInst.Condition.NE,
                    value, ConstInt.getInstance(0)
            );
        else if (value.getType() == FloatType.getFloat() && targetType == IntegerType.getI1())
            result = new FloatCompareInst(
                    FloatType.getFloat(), FloatCompareInst.Condition.ONE,
                    value, ConstFloat.getInstance(0)
            );
        else if (value.getType() == IntegerType.getI1() && targetType == IntegerType.getI32())
            result = new ZeroExtensionInst(value, IntegerType.getI32());
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction((Instruction) result);
    }

    private void applyUnaryOperator(Value operand, Operator operator) {
        if (operator == Operator.POS) {
            result = operand;
            return;
        }

        if (operand.getType() == IntegerType.getI32())
            result = switch (operator) {
                case NEG -> new IntegerSubInst(IntegerType.getI32(), ConstInt.getInstance(0), operand);
                case LNOT -> new IntegerCompareInst(
                        IntegerType.getI32(), IntegerCompareInst.Condition.EQ,
                        operand, ConstInt.getInstance(0)
                );
                default -> throw new IllegalArgumentException();
            };
        else if (operand.getType() == FloatType.getFloat())
            result = switch (operator) {
                case NEG -> new FloatNegateInst(FloatType.getFloat(), operand);
                case LNOT -> new FloatCompareInst(
                        FloatType.getFloat(), FloatCompareInst.Condition.OEQ,
                        operand, ConstFloat.getInstance(0)
                );
                default -> throw new IllegalArgumentException();
            };
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction((Instruction) result);

        if (operator == Operator.LNOT)
            convertType(result, IntegerType.getI32());
    }

    private void applyBinaryArithmeticOperator(Value leftOperand, Value rightOperand, Operator operator) {
        Type operandType = Types.getCommonType(leftOperand.getType(), rightOperand.getType());

        if (!leftOperand.getType().equals(operandType)) {
            convertType(leftOperand, operandType);
            leftOperand = result;
        }
        if (!rightOperand.getType().equals(operandType)) {
            convertType(rightOperand, operandType);
            rightOperand = result;
        }

        if (operandType == IntegerType.getI32())
            result = switch (operator) {
                case ADD -> new IntegerAddInst(IntegerType.getI32(), leftOperand, rightOperand);
                case SUB -> new IntegerSubInst(IntegerType.getI32(), leftOperand, rightOperand);
                case MUL -> new IntegerMultiplyInst(IntegerType.getI32(), leftOperand, rightOperand);
                case DIV -> new IntegerSignedDivideInst(IntegerType.getI32(), leftOperand, rightOperand);
                case MOD -> new IntegerSignedRemainderInst(IntegerType.getI32(), leftOperand, rightOperand);
                default -> throw new IllegalArgumentException();
            };
        else if (operandType == FloatType.getFloat())
            result = switch (operator) {
                case ADD -> new FloatAddInst(FloatType.getFloat(), leftOperand, rightOperand);
                case SUB -> new FloatSubInst(FloatType.getFloat(), leftOperand, rightOperand);
                case MUL -> new FloatMultiplyInst(FloatType.getFloat(), leftOperand, rightOperand);
                case DIV -> new FloatDivideInst(FloatType.getFloat(), leftOperand, rightOperand);
                default -> throw new IllegalArgumentException();
            };
        else
            throw new IllegalArgumentException();

        currentBasicBlock.addInstruction((Instruction) result);
    }

    private void applyBinaryRelationalOperator(Value leftOperand, Value rightOperand, Operator operator) {
        Type operandType = Types.getCommonType(leftOperand.getType(), rightOperand.getType());

        if (!leftOperand.getType().equals(operandType)) {
            convertType(leftOperand, operandType);
            leftOperand = result;
        }
        if (!rightOperand.getType().equals(operandType)) {
            convertType(rightOperand, operandType);
            rightOperand = result;
        }

        if (operandType == IntegerType.getI32())
            result = switch (operator) {
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
            result = switch (operator) {
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

        currentBasicBlock.addInstruction((Instruction) result);
        convertType(result, IntegerType.getI32());
    }

    private void initializeVariable(SysYParser.InitializerContext initializer, Value address) {
        if (initializer.expression() != null) {
            visit(initializer.expression());

            if (result.getType() != ((PointerType) address.getType()).getBaseType())
                convertType(result, ((PointerType) address.getType()).getBaseType());

            currentBasicBlock.addInstruction(new StoreInst(address, result));
        } else
            initializeVariable(initializer.initializer(), address);
    }

    private void initializeVariable(List<SysYParser.InitializerContext> childInitializers, Value address) {
        var listIterator = childInitializers.listIterator();
        int index = 0;

        while (listIterator.hasNext()) {
            var elementInitializer = listIterator.next();
            var elementAddress = new GetElementPtrInst(
                    address, List.of(ConstInt.getInstance(0), ConstInt.getInstance(index++)));
            currentBasicBlock.addInstruction(elementAddress);

            if (elementInitializer.expression() != null && ((PointerType) elementAddress.getType()).getBaseType() instanceof ArrayType) {
                int count = Types.countElements(((PointerType) elementAddress.getType()).getBaseType());
                List<SysYParser.InitializerContext> terminalInitializers = new ArrayList<>();
                terminalInitializers.add(elementInitializer);

                for (int i = 0; i < count - 1 && listIterator.hasNext(); ++i) {
                    var nextElementInitializer = listIterator.next();

                    if (nextElementInitializer.expression() != null)
                        terminalInitializers.add(nextElementInitializer);
                    else {
                        listIterator.previous();
                        break;
                    }
                }
                initializeVariable(terminalInitializers, elementAddress);
            } else
                initializeVariable(elementInitializer, elementAddress);
        }
    }

    @Override
    public Void visitConstantDeclaration(SysYParser.ConstantDeclarationContext ctx) {
        Type type = makeType(ctx.typeSpecifier().type);

        for (var constantDefinition : ctx.constantDefinition()) {
            if (symbolTable.getScopeDepth() > 0) {
                var address = new AllocateInst(type);
                currentFunction.getEntryBasicBlock().addInstruction(address);
                String name = constantDefinition.Identifier().getText();
                symbolTable.putLocalVariable(name, address);

                visit(constantDefinition.constantInitializer());
                Value initialValue = result;
                currentBasicBlock.addInstruction(new StoreInst(address, initialValue));
            } else {
                String name = constantDefinition.Identifier().getText();

                Constant initialValue;
                if (constantDefinition.constantInitializer() == null)
                    initialValue = type.getDefaultInitialization();
                else {
                    visit(constantDefinition.constantInitializer());
                    initialValue = (Constant) result;
                }

                GlobalVariable globalVariable = new GlobalVariable(true, initialValue);
                globalVariable.setValueName(name);
                module.addGlobalVariable(globalVariable);
                symbolTable.putGlobalVariable(name, globalVariable);
            }
        }

        return null;
    }

    @Override
    public Void visitVariableDeclaration(SysYParser.VariableDeclarationContext ctx) {
        Type baseType = makeType(ctx.typeSpecifier().type);

        for (var variableDefinition : ctx.variableDefinition()) {
            Type type = baseType;

            var listIterator = variableDefinition.expression()
                    .listIterator(variableDefinition.expression().size());
            while (listIterator.hasPrevious()) {
                visit(listIterator.previous());

                type = ArrayType.getInstance(((ConstInt) result).getValue(), type);
            }

            if (symbolTable.getScopeDepth() > 0) {
                var address = new AllocateInst(type);
                currentFunction.getEntryBasicBlock().addInstruction(address);
                String name = variableDefinition.Identifier().getText();
                symbolTable.putLocalVariable(name, address);

                if (variableDefinition.initializer() != null) {
                    currentBasicBlock.addInstruction(new StoreInst(address, type.getDefaultInitialization()));
                    initializeVariable(variableDefinition.initializer(), address);
                }
            } else {
                Constant initialValue;
                if (variableDefinition.initializer() == null)
                    initialValue = type.getDefaultInitialization();
                else
                    initialValue = makeConstant(variableDefinition.initializer(), type);

                String name = variableDefinition.Identifier().getText();

                GlobalVariable globalVariable = new GlobalVariable(false, initialValue);
                globalVariable.setValueName(name);
                module.addGlobalVariable(globalVariable);
                symbolTable.putGlobalVariable(name, globalVariable);
            }
        }

        return null;
    }

    @Override
    public Void visitFunctionDefinition(SysYParser.FunctionDefinitionContext ctx) {
        String name = ctx.Identifier().getText();
        Type returnType = makeType(ctx.typeSpecifier().type);
        List<Parameter> parameters = List.of();
        if (ctx.parameterList() != null)
            parameters = makeParameterList(ctx.parameterList());
        List<Type> parameterTypes = parameters.stream().map(Parameter::getType).toList();

        FunctionType type = FunctionType.getInstance(returnType, parameterTypes);

        currentFunction = new Function(type);
        currentFunction.setValueName(name);

        module.addFunction(currentFunction);
        symbolTable.putFunction(name, currentFunction);

        currentBasicBlock = currentFunction.getEntryBasicBlock();

        visit(ctx.compoundStatement());

        if (returnType == VoidType.getInstance())
            currentBasicBlock.addInstruction(new ReturnInst(VoidValue.getInstance()));
        if (returnType == IntegerType.getI32())
            currentBasicBlock.addInstruction(new ReturnInst(ConstInt.getInstance(0)));
        if (returnType == FloatType.getFloat())
            currentBasicBlock.addInstruction(new ReturnInst(ConstFloat.getInstance(0f)));

        return null;
    }

    @Override
    public Void visitCompoundStatement(SysYParser.CompoundStatementContext ctx) {
        symbolTable.pushScope();

        if (ctx.getParent() instanceof SysYParser.FunctionDefinitionContext functionDefinition) {
            if (functionDefinition.parameterList() != null) {
                List<Parameter> parameters = makeParameterList(functionDefinition.parameterList());

                for (int i = 0; i < parameters.size(); ++i) {
                    Parameter parameter = parameters.get(i);

                    var address = new AllocateInst(parameter.getType());
                    currentFunction.getEntryBasicBlock().addInstruction(address);
                    currentBasicBlock.addInstruction(new StoreInst(address, currentFunction.getFormalParameters().get(i)));

                    symbolTable.putLocalVariable(parameter.getName(), address);
                }
            }
        }

        if (ctx.blockItem() != null)
            for (var blockItem : ctx.blockItem())
                visit(blockItem);

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
            convertType(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new BranchInst(result, thenBlock, elseBlock));

        currentBasicBlock = thenBlock;
        visit(ctx.statement(0));
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = elseBlock;
        if (ctx.statement().size() == 2)
            visit(ctx.statement(1));
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

        controlFlowStack.pushContext(new ControlFlowStack.WhileContext(testBlock, doneBlock));

        currentBasicBlock.addInstruction(new JumpInst(testBlock));

        currentBasicBlock = testBlock;

        visit(ctx.expression());
        if (result.getType() != IntegerType.getI1())
            convertType(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new BranchInst(result, bodyBlock, doneBlock));

        currentBasicBlock = bodyBlock;

        visit(ctx.statement());

        currentBasicBlock.addInstruction(new JumpInst(testBlock));

        currentBasicBlock = doneBlock;

        controlFlowStack.popContext();
        return null;
    }

    @Override
    public Void visitBreakStatement(SysYParser.BreakStatementContext ctx) {
        BasicBlock doneBlock = ((ControlFlowStack.WhileContext) controlFlowStack.getTopContext()).getDoneBlock();
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = new BasicBlock();
        currentFunction.addBasicBlock(currentBasicBlock);

        return null;
    }

    @Override
    public Void visitContinueStatement(SysYParser.ContinueStatementContext ctx) {
        BasicBlock testBlock = ((ControlFlowStack.WhileContext) controlFlowStack.getTopContext()).getTestBlock();
        currentBasicBlock.addInstruction(new JumpInst(testBlock));

        currentBasicBlock = new BasicBlock();
        currentFunction.addBasicBlock(currentBasicBlock);

        return null;
    }

    @Override
    public Void visitReturnStatement(SysYParser.ReturnStatementContext ctx) {
        if (ctx.expression() != null) {
            visit(ctx.expression());
            if (result.getType() != currentFunction.getReturnType())
                convertType(result, currentFunction.getReturnType());

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
        currentFunction.getEntryBasicBlock().addInstruction(address);

        visit(ctx.logicalOrExpression());
        if (result.getType() != IntegerType.getI1())
            convertType(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new BranchInst(result, doneBlock, falseBlock));

        currentBasicBlock = falseBlock;

        visit(ctx.logicalAndExpression());
        if (result.getType() != IntegerType.getI1())
            convertType(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = doneBlock;

        result = new LoadInst(address);
        currentBasicBlock.addInstruction((Instruction) result);

        return null;
    }

    @Override
    public Void visitBinaryLogicalAndExpression(SysYParser.BinaryLogicalAndExpressionContext ctx) {
        BasicBlock trueBlock = new BasicBlock();
        BasicBlock doneBlock = new BasicBlock();

        currentFunction.addBasicBlock(trueBlock);
        currentFunction.addBasicBlock(doneBlock);

        var address = new AllocateInst(IntegerType.getI1());
        currentFunction.getEntryBasicBlock().addInstruction(address);

        visit(ctx.logicalAndExpression());
        if (result.getType() != IntegerType.getI1())
            convertType(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new BranchInst(result, trueBlock, doneBlock));

        currentBasicBlock = trueBlock;

        visit(ctx.equalityExpression());
        if (result.getType() != IntegerType.getI1())
            convertType(result, IntegerType.getI1());

        currentBasicBlock.addInstruction(new StoreInst(address, result));
        currentBasicBlock.addInstruction(new JumpInst(doneBlock));

        currentBasicBlock = doneBlock;

        result = new LoadInst(address);
        currentBasicBlock.addInstruction((Instruction) result);

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

        Operator operator = makeBinaryOperator(ctx.op);

        if (leftOperand instanceof Constant && rightOperand instanceof Constant)
            result = Constants.applyBinaryOperator((Constant) leftOperand, (Constant) rightOperand, operator);
        else
            applyBinaryArithmeticOperator(leftOperand, rightOperand, operator);

        return null;
    }

    @Override
    public Void visitBinaryMultiplicativeExpression(SysYParser.BinaryMultiplicativeExpressionContext ctx) {
        visit(ctx.multiplicativeExpression());
        Value leftOperand = result;

        visit(ctx.unaryExpression());
        Value rightOperand = result;

        Operator operator = makeBinaryOperator(ctx.op);

        if (leftOperand instanceof Constant && rightOperand instanceof Constant)
            result = Constants.applyBinaryOperator((Constant) leftOperand, (Constant) rightOperand, operator);
        else
            applyBinaryArithmeticOperator(leftOperand, rightOperand, operator);

        return null;
    }

    @Override
    public Void visitFunctionCallExpression(SysYParser.FunctionCallExpressionContext ctx) {
        String name = ctx.Identifier().getText();
        BaseFunction function = symbolTable.getFunction(name);

        List<Value> arguments = new ArrayList<>();
        if (ctx.argumentExpressionList() != null) {
            for (var expression : ctx.argumentExpressionList().expression()) {
                visit(expression);
                arguments.add(result);
            }
        }

        result = new CallInst(function, arguments);
        currentBasicBlock.addInstruction((Instruction) result);
        return null;
    }

    @Override
    public Void visitUnaryOperatorExpression(SysYParser.UnaryOperatorExpressionContext ctx) {
        visit(ctx.unaryExpression());
        Value operand = result;
        Operator operator = makeUnaryOperator(ctx.unaryOperator().op);

        if (operand instanceof Constant)
            result = Constants.applyUnaryOperator((Constant) operand, operator);
        else
            applyUnaryOperator(operand, operator);

        return null;
    }

    @Override
    public Void visitLValue(SysYParser.LValueContext ctx) {
        String name = ctx.Identifier().getText();

        Value address = symbolTable.getVariable(name);
        for (var expression : ctx.expression()) {
            visit(expression);
            address = new GetElementPtrInst(address, List.of(ConstInt.getInstance(0), result));
            currentBasicBlock.addInstruction((Instruction) address);
        }
        resultAddress = address;

        result = new LoadInst(resultAddress);
        currentBasicBlock.addInstruction((Instruction) result);

        return null;
    }

    @Override
    public Void visitIntegerConstant(SysYParser.IntegerConstantContext ctx) {
        String text = ctx.IntegerConstant().getText();

        int value;
        if ("0".equals(text))
            value = 0;
        else if (text.startsWith("0x"))
            value = Integer.parseInt(text.substring(2), 16);
        else if (text.startsWith("0"))
            value = Integer.parseInt(text.substring(1), 8);
        else
            value = Integer.parseInt(text);

        result = ConstInt.getInstance(value);
        return null;
    }

    @Override
    public Void visitFloatingConstant(SysYParser.FloatingConstantContext ctx) {
        result = ConstFloat.getInstance(Float.parseFloat(ctx.FloatingConstant().getText()));
        return null;
    }
}
