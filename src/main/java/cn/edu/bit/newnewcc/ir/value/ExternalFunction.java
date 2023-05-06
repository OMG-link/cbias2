package cn.edu.bit.newnewcc.ir.value;

import cn.edu.bit.newnewcc.ir.type.FunctionType;
import lombok.NonNull;

public class ExternalFunction extends AbstractFunction {
    public ExternalFunction(FunctionType type, @NonNull String functionName) {
        super(type);
        this.functionName = functionName;
    }

    private String functionName;

    @Override
    public String getValueName() {
        return functionName;
    }

    @Override
    public String getValueNameIR() {
        return '@'+functionName;
    }

    @Override
    public void setValueName(@NonNull String valueName) {
        functionName = valueName;
    }
}
