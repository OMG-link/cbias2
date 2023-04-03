package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IndexOutOfBoundsException;
import com.bit.newnewcc.ir.type.PointerType;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

public class GetElementPtrInst extends Instruction {
    private final Operand rootOperand;
    private final List<Operand> indexOperands;

    public GetElementPtrInst(Value root, List<Value> indexes) {
        super(analysisDereferencedType(root.getType(), indexes.size() - 1));
        this.rootOperand = new Operand(this, root.getType(), root);
        this.indexOperands = new ArrayList<>();
        for (var index : indexes) {
            indexOperands.add(new Operand(this, index.getType(), index));
        }
    }

    public Value getRootOperand() {
        return rootOperand.getValue();
    }

    public void setRootOperand(Value value) {
        rootOperand.setValue(value);
    }

    public Value getIndexAt(int index) {
        if (index < 0 || index >= indexOperands.size()) {
            throw new IndexOutOfBoundsException(index, 0, indexOperands.size());
        }
        return indexOperands.get(index).getValue();
    }

    public void setIndexAt(int index, Value value) {
        if (index < 0 || index >= indexOperands.size()) {
            throw new IndexOutOfBoundsException(index, 0, indexOperands.size());
        }
        indexOperands.get(index).setValue(value);
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(rootOperand);
        list.addAll(indexOperands);
        return list;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(String.format(
                "%s = getelementptr %s, ptr %s",
                this.getValueNameIR(),
                this.getTypeName(),
                getRootOperand().getValueNameIR()
        ));
        for (var indexOperand : indexOperands) {
            var index = indexOperand.getValue();
            builder.append(String.format(
                    ", %s %s",
                    index.getTypeName(),
                    index.getValueNameIR()
            ));
        }
        return builder.toString();
    }

    public static Type analysisDereferencedType(Type rootType, int dereferenceCount) {
        for (var i = 0; i < dereferenceCount; i++) {
            rootType = ((PointerType) rootType).getBaseType();
        }
        return rootType;
    }
}
