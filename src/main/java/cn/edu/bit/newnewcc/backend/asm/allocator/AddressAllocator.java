package cn.edu.bit.newnewcc.backend.asm.allocator;

import cn.edu.bit.newnewcc.backend.asm.operand.Address;
import cn.edu.bit.newnewcc.backend.asm.operand.Address;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class AddressAllocator {
    Map<Value, Address> addressMap;
    public AddressAllocator() {
        addressMap = new HashMap<>();
    }
    public void allocate(Instruction instruction, Address address) {
        addressMap.put(instruction, address);
    }
    public boolean contain(Instruction instruction) {
        return addressMap.containsKey(instruction);
    }
    public Address get(Instruction instruction) {
        return addressMap.get(instruction);
    }
}
