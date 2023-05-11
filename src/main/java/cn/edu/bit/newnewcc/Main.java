package cn.edu.bit.newnewcc;

import org.apache.commons.cli.*;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws ParseException, IOException {
        Option outputFileName = Option.builder("o")
                .hasArg()
                .argName("file")
                .desc("Write output to <file>")
                .build();
        Option optimizationLevel = Option.builder("O")
                .hasArg()
                .argName("level")
                .desc("Use optimization level <level>")
                .build();
        Option emitAssembly = new Option("S", "Only run compilation steps");
        Option emitLLVM = new Option(
                null,
                "emit-llvm",
                false,
                "Use the LLVM representation for assembler and object files"
        );

        Options options = new Options();
        options.addOption(outputFileName);
        options.addOption(optimizationLevel);
        options.addOption(emitAssembly);
        options.addOption(emitLLVM);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getArgs().length == 0)
            error("No input files");
        if (cmd.hasOption(outputFileName) && cmd.getArgs().length > 1)
            error("Cannot specify -o when generating multiple output files");
        if (cmd.hasOption(emitAssembly) && cmd.hasOption(emitLLVM))
            error("Cannot specify both -S and --emit-llvm");

        CompilerOptions compilerOptions = CompilerOptions.builder()
                .inputFileNames(cmd.getArgs())
                .outputFileName(cmd.getOptionValue(outputFileName))
                .optimizationLevel(Integer.parseInt(cmd.getOptionValue(optimizationLevel, "0")))
                .emitAssembly(cmd.hasOption(emitAssembly))
                .emitLLVM(cmd.hasOption(emitLLVM))
                .build();

        Driver driver = new Driver(compilerOptions);
        driver.launch();
    }

    private static void error(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
