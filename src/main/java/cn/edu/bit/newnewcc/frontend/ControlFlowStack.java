package cn.edu.bit.newnewcc.frontend;

import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import lombok.Value;

import java.util.Deque;
import java.util.LinkedList;

public class ControlFlowStack {
    public interface ControlFlowContext {
    }

    @Value
    public static class WhileContext implements ControlFlowContext {
        BasicBlock testBlock;
        BasicBlock doneBlock;
    }

    private final Deque<ControlFlowContext> stack = new LinkedList<>();

    public void pushContext(ControlFlowContext context) {
        stack.push(context);
    }

    public void popContext() {
        stack.pop();
    }

    public ControlFlowContext getTopContext() {
        return stack.getFirst();
    }
}
