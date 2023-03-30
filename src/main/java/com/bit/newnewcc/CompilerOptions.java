package com.bit.newnewcc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompilerOptions {
    String[] inputFileNames;
    String outputFileName;
    int optimizationLevel;
    boolean emitAssembly;
    boolean emitLLVM;
}
