package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.PointerType;
import com.bit.newnewcc.ir.type.VoidType;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储操作 <br>
 * 将值存入到目标地址 <br>
 */
public class StoreInst extends MemoryInst {

    /**
     * 地址操作数：存储值的地址
     */
    private final Operand addressOperand;
    /**
     * 值操作数：待存储的值
     */
    private final Operand valueOperand;

    /**
     * @param valueType 存储值的类型
     */
    public StoreInst(Type valueType) {
        super(VoidType.getInstance());
        this.addressOperand = new Operand(this, PointerType.getInstance(valueType), null);
        this.valueOperand = new Operand(this, valueType, null);
    }

    /**
     * @param address 存储操作的目标地址
     * @param value   存储的值
     */
    public StoreInst(Value address, Value value) {
        this(value.getType());
        setAddressOperand(address);
        setValueOperand(value);
    }

    /**
     * @return 地址操作数的值
     */
    public Value getAddressOperand() {
        return addressOperand.getValue();
    }

    /**
     * @param value 地址操作数的值
     */
    public void setAddressOperand(Value value) {
        addressOperand.setValue(value);
    }

    /**
     * @return 待存储的值
     */
    public Value getValueOperand() {
        return valueOperand.getValue();
    }

    /**
     * @param value 待存储的值
     */
    public void setValueOperand(Value value) {
        valueOperand.setValue(value);
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(addressOperand);
        list.add(valueOperand);
        return list;
    }

    @Override
    public String toString() {
        return String.format(
                "store %s %s, %s %s",
                valueOperand.getValue().getTypeName(),
                valueOperand.getValue().getValueNameIR(),
                addressOperand.getValue().getTypeName(),
                addressOperand.getValue().getValueNameIR()
        );
    }
}
