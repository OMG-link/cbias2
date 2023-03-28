package com.bit.newnewcc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompilerOptions {
    String[] sourceFileNames;
    String outputFileName;
    int optimizationLevel;
}
