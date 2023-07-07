package cn.edu.bit.newnewcc.backend.asm.allocator;

import cn.edu.bit.newnewcc.backend.asm.operand.Address;
import cn.edu.bit.newnewcc.backend.asm.operand.AddressContent;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class AddressAllocator {
    Map<Value, AddressContent> addressMap;
    public AddressAllocator() {
        addressMap = new HashMap<>();
    }
    public void allocate(Instruction instruction, AddressContent address) {
        addressMap.put(instruction, address);
    }
    public boolean contain(Instruction instruction) {
        return addressMap.containsKey(instruction);
    }
    public AddressContent get(Instruction instruction) {
        return addressMap.get(instruction);
    }
}
