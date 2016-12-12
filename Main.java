package pythonToJimple;

import java.io.FileInputStream;
import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Main {

    public static void main(String[] args) throws IOException {
    	String fileName = "C:\\Users\\Fernando\\Desktop\\antl\\lib\\Foo.py";
    	ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(fileName));
    	Python3Lexer lexer = new Python3Lexer(input);
    	CommonTokenStream tokens = new CommonTokenStream(lexer);
    	Python3Parser parser = new Python3Parser(tokens);
    	ParseTree tree = parser.file_input();
    	System.out.println(tree.toStringTree(parser));
    	Python3VisitorImpl visitor = new Python3VisitorImpl();
    	JimpleBodyBuilder jb = new JimpleBodyBuilder();
    	jb.setup();
    	visitor.setJimpleBodyBuilder(jb);
    	visitor.visit(tree);
    	jb.wrapUp();
    }
}