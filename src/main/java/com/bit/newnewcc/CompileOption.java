package com.bit.newnewcc;

/**
 * 编译器选项。
 */
public class CompileOption {
    /**
     * 输出文件类型。
     */
    public enum OutputType {
        LLVM_IR, ASM
    }

    /**
     * 优化等级。
     */
    public enum OptimizeLevel {
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
    public OptimizeLevel optimizeLevel = OptimizeLevel.O1;

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
    public static CompileOption fromCmdArguments(String[] args) {
        var compileOption = new CompileOption();
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                switch (args[i].substring(1)) {
                    case "o" -> compileOption.outputFile = args[++i];
                    case "S" -> compileOption.outputType = OutputType.ASM;
                    case "emit-llvm" -> compileOption.outputType = OutputType.LLVM_IR;
                    case "check-compile-process" -> compileOption.enableStrictCompileProcessCheck = true;
                    case "O0" -> compileOption.optimizeLevel = OptimizeLevel.O0;
                    case "O1" -> compileOption.optimizeLevel = OptimizeLevel.O1;
                    case "O2" -> compileOption.optimizeLevel = OptimizeLevel.O2;
                }
            } else {
                // 如果有多个源文件，sourceFile应该改为数组
                compileOption.sourceFile = args[i];
            }
        }
        return compileOption;
    }

    @Override
    public String toString() {
        return "CompileOption{" +
                "sourceFile='" + sourceFile + '\'' +
                ", outputType=" + outputType +
                ", outputFile='" + outputFile + '\'' +
                ", optimizeLevel=" + optimizeLevel +
                ", enableStrictCompileProcessCheck=" + enableStrictCompileProcessCheck +
                '}';
    }
}
