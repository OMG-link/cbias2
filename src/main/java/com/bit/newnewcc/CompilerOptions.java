package com.bit.newnewcc;

/**
 * 编译器选项。
 */
public class CompilerOptions {
    /**
     * 输出文件类型。
     */
    public enum OutputType {
        LLVM_IR, ASM
    }

    /**
     * 优化等级。
     */
    public enum OptimizationLevel {
        /** 完全按照输入翻译。 */
        O0,
        /** 基本优化。 */
        O1,
        /** 高阶优化。 */
        O2,
    }

    /**
     * 源文件名。
     */
    public String sourceFile;

    /**
     * 输出文件类型。
     */
    public OutputType outputType = OutputType.ASM;

    /**
     * 输出文件名。
     */
    public String outputFile = "a.out";

    /**
     * 优化等级。
     */
    public OptimizationLevel optimizationLevel = OptimizationLevel.O1;

    /**
     * 是否启用严格的编译过程检查。
     * <p>
     * 当该值为true时，编译的每一个步骤后都会检查当前代码的引用关系是否有效，适用于调试过程。
     * <p>
     * 请勿在正式编译时开启此选项，以免拖慢编译速度。
     */
    public boolean enableStrictCompileProcessCheck = false;

    /**
     * 根据命令行参数构造编译器选项。
     *
     * @param args 命令行参数
     * @return 命令行参数对应的编译器选项
     */
    public static CompilerOptions fromCmdArguments(String[] args) {
        var compilerOptions = new CompilerOptions();
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                switch (args[i].substring(1)) {
                    case "o" -> compilerOptions.outputFile = args[++i];
                    case "S" -> compilerOptions.outputType = OutputType.ASM;
                    case "emit-llvm" -> compilerOptions.outputType = OutputType.LLVM_IR;
                    case "check-compile-process" -> compilerOptions.enableStrictCompileProcessCheck = true;
                    case "O0" -> compilerOptions.optimizationLevel = OptimizationLevel.O0;
                    case "O1" -> compilerOptions.optimizationLevel = OptimizationLevel.O1;
                    case "O2" -> compilerOptions.optimizationLevel = OptimizationLevel.O2;
                }
            } else {
                // 如果有多个源文件，sourceFile应该改为数组
                compilerOptions.sourceFile = args[i];
            }
        }
        return compilerOptions;
    }

    @Override
    public String toString() {
        return "CompilerOptions{" +
                "sourceFile='" + sourceFile + '\'' +
                ", outputType=" + outputType +
                ", outputFile='" + outputFile + '\'' +
                ", optimizationLevel=" + optimizationLevel +
                ", enableStrictCompileProcessCheck=" + enableStrictCompileProcessCheck +
                '}';
    }
}
