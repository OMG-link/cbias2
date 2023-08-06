package cn.edu.bit.newnewcc.backend.asm.operand;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Registers {
    private Registers() {
    }

    private static final Set<Register> USABLE_REGISTERS;

    static {
        Set<Register> usableRegisters = new HashSet<>();
        for (int i = 0; i <= 31; i++) {
            if ((5 <= i && i <= 7) || (18 <= i)) {
                usableRegisters.add(IntRegister.getPhysical(i));
            }
            if (i <= 9 || 18 <= i) {
                usableRegisters.add(FloatRegister.getPhysical(i));
            }
        }
        USABLE_REGISTERS = Collections.unmodifiableSet(usableRegisters);
    }

    private static final Set<Register> CALLER_SAVED_REGISTERS;

    static {
        Set<Register> callerSavedRegisters = new HashSet<>();
        callerSavedRegisters.add(IntRegister.getPhysical(1));
        for (int i = 5; i <= 7; ++i) {
            callerSavedRegisters.add(IntRegister.getPhysical(i));
        }
        for (int i = 10; i <= 17; ++i) {
            callerSavedRegisters.add(IntRegister.getPhysical(i));
        }
        for (int i = 28; i <= 31; ++i) {
            callerSavedRegisters.add(IntRegister.getPhysical(i));
        }
        for (int i = 0; i <= 7; ++i) {
            callerSavedRegisters.add(FloatRegister.getPhysical(i));
        }
        for (int i = 10; i <= 17; ++i) {
            callerSavedRegisters.add(FloatRegister.getPhysical(i));
        }
        for (int i = 28; i <= 31; ++i) {
            callerSavedRegisters.add(FloatRegister.getPhysical(i));
        }
        CALLER_SAVED_REGISTERS = Collections.unmodifiableSet(callerSavedRegisters);
    }

    public static Set<Register> getUsableRegisters() {
        return USABLE_REGISTERS;
    }

    public static Set<Register> getCallerSavedRegisters() {
        return CALLER_SAVED_REGISTERS;
    }

    public static boolean isPreservedAcrossCalls(Register register) {
        return !CALLER_SAVED_REGISTERS.contains(register);
    }
}
