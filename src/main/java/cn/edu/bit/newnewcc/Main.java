package cn.edu.bit.newnewcc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String outputFilename = "a.out";
        int optimizeLevel = 0;
        boolean emitAssembly = false;
        boolean emitLLVM = false;
        List<String> inputFilenames = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o" -> outputFilename = args[++i];
                case "-O0" -> optimizeLevel = 0;
                case "-O1" -> optimizeLevel = 1;
                case "-S" -> emitAssembly = true;
                case "--emit-llvm" -> emitLLVM = true;
                default -> inputFilenames.add(args[i]);
            }
        }

        Driver driver = new Driver(
                new CompilerOptions(
                        inputFilenames.toArray(new String[0]),
                        outputFilename, optimizeLevel,
                        emitAssembly,
                        emitLLVM
                )
        );
        driver.launch();
    }

    private static void error(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
