package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.PointerType;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载指令
 */
public class LoadInst extends MemoryInst{
    private final Operand addressOperand;

    /**
     * @param type 待加载的数据的类型
     */
    public LoadInst(Type type){
        super(type);
        this.addressOperand = new Operand(this, PointerType.getInstance(type), null);
    }

    public LoadInst(Value address){
        this(((PointerType) address.getType()).getBaseType());
        this.addressOperand.setValue(address);
    }

    public Value getAddressOperand() {
        return addressOperand.getValue();
    }

    public void setAddressOperand(Value value) {
        addressOperand.setValue(value);
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(addressOperand);
        return list;
    }

    @Override
    public String toString() {
        return String.format(
                "%s = load %s, ptr %s",
                this.getValueNameIR(),
                this.getTypeName(),
                getAddressOperand().getValueNameIR()
        );
    }
}
