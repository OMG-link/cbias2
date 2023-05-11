package cn.edu.bit.newnewcc;

import cn.edu.bit.newnewcc.frontend.Translator;
import cn.edu.bit.newnewcc.frontend.antlr.SysYLexer;
import cn.edu.bit.newnewcc.frontend.antlr.SysYParser;
import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.util.IREmitter;
import cn.edu.bit.newnewcc.pass.IrPassManager;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Driver {
    private final CompilerOptions compilerOptions;

    public Driver(CompilerOptions compilerOptions) {
        this.compilerOptions = compilerOptions;
    }

    public void launch() throws IOException {
        for (String inputFileName : compilerOptions.getInputFileNames()) {
            CharStream input = CharStreams.fromFileName(inputFileName);
            SysYLexer lexer = new SysYLexer(input);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            SysYParser parser = new SysYParser(tokenStream);
            ParseTree tree = parser.compilationUnit();
            Translator visitor = new Translator();
            visitor.visit(tree);
            Module module = visitor.getModule();

            // 在IR层面优化代码
            IrPassManager.optimize(module, compilerOptions.getOptimizationLevel());
            // 输出LLVM IR格式的文件
            if (compilerOptions.isEmitLLVM()) {
                try (var fileOutputStream = new FileOutputStream(compilerOptions.getOutputFileName())) {
                    fileOutputStream.write(IREmitter.emit(module).getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
}
