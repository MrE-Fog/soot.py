package pythonToJimple;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import soot.ArrayType;
import soot.DoubleType;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.javaToJimple.Util;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.util.Chain;

public class JimpleBodyBuilder {
	SootClass sClass;
    SootMethod method;
    private LocalGenerator localGenerator;
    private HashMap<String, Local> localsMap = new HashMap<String, Local>();
    private List<soot.jimple.Stmt> labelList = new ArrayList<soot.jimple.Stmt>();
    private soot.jimple.NopStmt successTgt;
    private soot.jimple.NopStmt failureTgt;
    private soot.jimple.NopStmt chainTgt;
    private soot.jimple.NopStmt elseTgt;
    private Stack<soot.jimple.NopStmt> stackTgt = new Stack<soot.jimple.NopStmt>();
    private HashMap<String, List<Value>> functionMap = new HashMap<String, List<Value>>();
    private Stack<SootMethod> methodStack = new Stack<SootMethod>();
    private Stack<LocalGenerator> localGeneratorStack = new Stack<LocalGenerator>();
    private Stack<Type> returnTypeStack = new Stack<Type>();
    private HashMap<String, Boolean> hasReturnValueTable = new HashMap<String, Boolean>();
    private Stack<HashMap<String, Local>> localsMapStack = new Stack<HashMap<String, Local>>();
    
	public void setup() {    
        // Resolve dependencies
        Scene.v().loadClassAndSupport("java.lang.Object");
        Scene.v().loadClassAndSupport("java.lang.System");
           
        // Declare 'public class HelloWorld'   
        sClass = new SootClass("Yay", Modifier.PUBLIC);
        
        // 'extends Object'
        sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(sClass);
           
        // Create the method, public static void main(String[])
        method = new SootMethod("main",
                Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        
        sClass.addMethod(method);
        JimpleBody body = Jimple.v().newBody(method);
        localGenerator = new LocalGenerator(body);
        method.setActiveBody(body);
        methodStack.push(method);
        localGeneratorStack.push(localGenerator);
        localsMapStack.push(localsMap);
        Chain units = body.getUnits();
        Local arg, tmpRef;
        
        // Add some locals, java.lang.String l0
        arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1));
        body.getLocals().add(arg);
        units.add(Jimple.v().newIdentityStmt(arg, 
                Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0)));
	}
	
	public void wrapUp() {
		Chain units = method.getActiveBody().getUnits();
		units.add(Jimple.v().newReturnVoidStmt());
		OutputStream streamOut = null;
		try {
			String filename = SourceLocator.v().getFileNameFor(sClass, Options.output_format_jimple);
			streamOut = new FileOutputStream(filename);
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
			Printer.v().printTo(sClass, writerOut);
			writerOut.flush();
			writerOut.close();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			if (streamOut != null)
				try {
					streamOut.close();
				} catch (IOException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
		}

	}
	
	public Value generateMultiplication(Value v1, Value v2, String operator) {
		System.out.println("Will generate multiplication");
		Chain units = method.getActiveBody().getUnits();
		soot.jimple.BinopExpr binop = null;
		if(operator.equals("*")) {
			binop = Jimple.v().newMulExpr(v1, v2);
		}
		else if(operator.equals("/")) {
			binop = Jimple.v().newDivExpr(v1, v2);
		}
		else if(operator.equals("//")) {
			binop = Jimple.v().newRemExpr(v1, v2);
		}
		Type type = null;
		if(v1.getType() == IntType.v() && v2.getType() == IntType.v()) {
			type = IntType.v();
		}
		else if(v1.getType() == DoubleType.v() || v2.getType() == DoubleType.v()) {
			type = DoubleType.v();
		}
		Local local = localGenerator.generateLocal(type);
	    System.out.println(local.getName() + "=" + v1.toString() + operator + v2.toString());
		soot.jimple.AssignStmt assignStmt = soot.jimple.Jimple.v().newAssignStmt(local, binop);
	    units.add(assignStmt);
	   	return local;
	}
	
	public Value generateAddition(Value v1, Value v2, String operator) {
		System.out.println("Will generate addition");
		Chain units = method.getActiveBody().getUnits();
		soot.jimple.BinopExpr binop = null;
		if(operator.equals("+")) {
			binop = Jimple.v().newAddExpr(v1, v2);
		}
		else if(operator.equals("-")) {
			binop = Jimple.v().newSubExpr(v1, v2);
		}
		Type type = null;
		if(v1.getType() == IntType.v() && v2.getType() == IntType.v()) {
			type = IntType.v();
		}
		else if(v1.getType() == DoubleType.v() || v2.getType() == DoubleType.v()) {
			type = DoubleType.v();
		}
		Local local = localGenerator.generateLocal(type);
	    System.out.println(local.getName() + "=" + v1.toString() + operator + v2.toString());
		soot.jimple.AssignStmt assignStmt = soot.jimple.Jimple.v().newAssignStmt(local, binop);
	    units.add(assignStmt);
		return local;
	}
	
	public Value generateComparison(Value v1, Value v2, String operator) {
		soot.jimple.BinopExpr binop = null;
		if(operator.equals("==")) {
			binop = Jimple.v().newEqExpr(v1, v2);
		}
		else if(operator.equals(">")) {
			binop = Jimple.v().newGtExpr(v1, v2);
		}
		else if(operator.equals("<")) {
			binop = Jimple.v().newLtExpr(v1, v2);
			Jimple.v().newLtExpr(v1, v2);
		}
		else if(operator.equals(">=")) {
			binop = Jimple.v().newGeExpr(v1, v2);
		}
		else if(operator.equals("<=")) {
			binop = Jimple.v().newLeExpr(v1, v2);
		}
		return binop;
	}
	
	public void generateAnd(Value test) {
		soot.jimple.NopStmt brchTgt;// = soot.jimple.Jimple.v().newNopStmt();
		soot.jimple.NopStmt endTgt;
		chainTgt = soot.jimple.Jimple.v().newNopStmt();
		if(failureTgt == null) {
			failureTgt = soot.jimple.Jimple.v().newNopStmt();
		}
		soot.jimple.IfStmt ifStmt = soot.jimple.Jimple.v().newIfStmt(test, chainTgt);
        method.getActiveBody().getUnits().add(ifStmt);
        soot.jimple.Stmt goto1 = soot.jimple.Jimple.v().newGotoStmt(failureTgt);
        method.getActiveBody().getUnits().add(goto1);
        if(!labelList.contains(failureTgt)) {
        	labelList.add(failureTgt);
        }
	}
	
	public void addChainLabel() {
		if(chainTgt != null)
		  method.getActiveBody().getUnits().add(chainTgt);
	}
	public void addSuccessLabel() {
		if(successTgt == null) {
			successTgt = soot.jimple.Jimple.v().newNopStmt();
		}
		method.getActiveBody().getUnits().add(successTgt);
	}
	
	public void createFailureBranch() {
		failureTgt = soot.jimple.Jimple.v().newNopStmt();
		soot.jimple.Stmt goto1 = soot.jimple.Jimple.v().newGotoStmt(failureTgt);
        method.getActiveBody().getUnits().add(goto1);
	}
	
	public void addFailureLabel() {
		if(failureTgt != null && !method.getActiveBody().getUnits().contains(failureTgt))
		  method.getActiveBody().getUnits().add(failureTgt);
		for(int i = 0; i < labelList.size(); i++) {
			if(!method.getActiveBody().getUnits().contains(labelList.get(i))) {
				method.getActiveBody().getUnits().add(labelList.get(i));
			}
		}
	}
	
	public void generateOr(Value test) {
		soot.jimple.NopStmt brchTgt;
		
		if(successTgt == null)
		  successTgt = soot.jimple.Jimple.v().newNopStmt();
		soot.jimple.IfStmt ifStmt = soot.jimple.Jimple.v().newIfStmt(test, successTgt);
        method.getActiveBody().getUnits().add(ifStmt);
	}
	
	public soot.jimple.NopStmt generateIfStatement(Value test) {
		soot.jimple.NopStmt endTgt = soot.jimple.Jimple.v().newNopStmt();
        soot.jimple.NopStmt brchTgt = soot.jimple.Jimple.v().newNopStmt();
        
        soot.jimple.IfStmt ifStmt = soot.jimple.Jimple.v().newIfStmt(test, brchTgt);
        method.getActiveBody().getUnits().add(ifStmt);
        soot.jimple.Stmt goto1 = soot.jimple.Jimple.v().newGotoStmt(endTgt);
        method.getActiveBody().getUnits().add(goto1);
        
        method.getActiveBody().getUnits().add(brchTgt);
        elseTgt = endTgt;
        stackTgt.push(endTgt);
        return elseTgt;
	}
	
	public NopStmt generateElseIfStatement(Value test, NopStmt tgt) {
		soot.jimple.NopStmt endTgt = soot.jimple.Jimple.v().newNopStmt();
        soot.jimple.NopStmt brchTgt = soot.jimple.Jimple.v().newNopStmt();
        
        soot.jimple.IfStmt ifStmt = soot.jimple.Jimple.v().newIfStmt(test, brchTgt);
        method.getActiveBody().getUnits().add(ifStmt);
        soot.jimple.Stmt goto1 = soot.jimple.Jimple.v().newGotoStmt(endTgt);
        method.getActiveBody().getUnits().add(goto1);
        
        method.getActiveBody().getUnits().add(brchTgt);
        return endTgt;
	}
	
	public void addLabel(NopStmt tgt) {
		method.getActiveBody().getUnits().add(tgt);
	}
	
	public void addGoto(NopStmt tgt) {
		Stmt goto1 = soot.jimple.Jimple.v().newGotoStmt(tgt);
		method.getActiveBody().getUnits().add(goto1);
	}
	public soot.Value generateSimpleAssignLocal(String left, Value right){
        soot.jimple.AssignStmt stmt;
        Value local = generateLocal(left, right.getType()); //temporary
        stmt = soot.jimple.Jimple.v().newAssignStmt(local, right);
        method.getActiveBody().getUnits().add(stmt);
        if (local instanceof soot.Local){
            return local;
        }
        else {
            return right;
        }
    
    }
	
	public Value generateLocal(String name) {
		if(localsMap.containsKey(name)) {
			System.out.println("contains " + name);
			return localsMap.get(name);
		}
		Local local = Jimple.v().newLocal(name, IntType.v());
		localsMap.put(name, local);
		method.getActiveBody().getLocals().add(local);
		return local;
	}
	
	public Value generateLocal(String name, Type type) {
		if(localsMap.containsKey(name)) {
			System.out.println("contains " + name);
			return localsMap.get(name);
		}
		System.out.println("LOOKING FOR LOCAL " + name);
        Local local = Jimple.v().newLocal(name, type); 
		localsMap.put(name, local);
		method.getActiveBody().getLocals().add(local);
		return local;
	}
	
	public Value getLocal(String name) {
		return localsMap.get(name);
	}
	
	public void registerArguments(Object o, List<Object> arguments) {
		List<Value> values = new ArrayList<Value>();
		for(int i = 0; i < arguments.size(); i++) {
			Object arg = arguments.get(i);
			if(arg instanceof String) {
				values.add(getLocal((String) o));
			}
			else if(arg instanceof Value) {
				values.add(((Value) arg));
			}
		}
		functionMap.put((String)o, values);
	}
	public boolean isFunctionRegistered(String name) {
		return functionMap.get(name) != null;
	}
	
	public void createMethod(String name, List<String> parameterNames) {
		List<Value> parameterValues = functionMap.get(name);
		List<Type> parameterTypes = new ArrayList<Type>();
		for(int i = 0; i < parameterValues.size(); i++) {
			parameterTypes.add(parameterValues.get(i).getType());
		}
        SootMethod aMethod = new SootMethod(name,
                parameterTypes,
                VoidType.v(), Modifier.PUBLIC);
        sClass.addMethod(aMethod);
        JimpleBody body = Jimple.v().newBody(aMethod);
        LocalGenerator aLocalGenerator = new LocalGenerator(body);
        localGeneratorStack.push(aLocalGenerator);
        
        HashMap<String, Local> localsMap = new HashMap<String, Local>();
        localsMapStack.push(localsMap);
        
        Local localObject = aLocalGenerator.generateLocal(sClass.getType());
        soot.RefType type = sClass.getType();
        soot.jimple.ThisRef thisRef = soot.jimple.Jimple.v().newThisRef(type);
        soot.jimple.Stmt thisStmt = soot.jimple.Jimple.v().newIdentityStmt(localObject, thisRef);
        body.getUnits().add(thisStmt);
        
        for(int i = 0; i < parameterTypes.size(); i++) {
        	Local local = (Local)generateLocal(parameterNames.get(i), parameterTypes.get(i));
        	localsMap.put(local.getName(), local);
        	System.out.println("LOCAL NAME WAS " + local.getName() + "!!!!!!!!!!!");
        	soot.jimple.ParameterRef paramRef = Jimple.v().newParameterRef(local.getType(), i);
        	soot.jimple.Stmt stmt = soot.jimple.Jimple.v().newIdentityStmt(local, paramRef);
        	body.getUnits().add(stmt);
        }
        aMethod.setActiveBody(body);
        methodStack.push(aMethod);
        method = methodStack.peek();
        localGenerator = localGeneratorStack.peek();
        localsMap = localsMapStack.peek();
	}
	
	public void addReturnStatement(Object o) {
		Value v = null;
		if(o instanceof String) {
			v = getLocal((String) o);
		}
		else if(o instanceof Value) {
			v = ((Value) o);
		}
		soot.jimple.ReturnStmt retStmtLocal = soot.jimple.Jimple.v().newReturnStmt(v);
		method.getActiveBody().getUnits().add(retStmtLocal);
		hasReturnValueTable.put(method.getName(), true);
		returnTypeStack.push(v.getType());
	}
	
	public void addMethodCall(String name) {
		soot.SootMethodRef invokeMeth;
		Type type = null;
		if(!hasReturnValueTable.containsKey(name)) {
			type = VoidType.v();
		}
		else {
			type = returnTypeStack.pop();
		}
		sClass.getMethodByName(name).setReturnType(type);
		List<Type> parameterTypes = new ArrayList<Type>();
		List<Value> parameterValues = functionMap.get(name);
		for(int i = 0; i < parameterValues.size(); i++) {
			parameterTypes.add(parameterValues.get(i).getType());
		}
		invokeMeth = soot.Scene.v().makeMethodRef(sClass, name, parameterTypes, type, false);
		Local  base = localGenerator.generateLocal(sClass.getType());
		soot.jimple.InvokeExpr invokeExpr = soot.jimple.Jimple.v().newVirtualInvokeExpr(base, invokeMeth, parameterValues);
		method.getActiveBody().getUnits().add(soot.jimple.Jimple.v().newInvokeStmt(invokeExpr));
	}
	
	public void wrapUpMethod() {
		methodStack.pop();
		localGeneratorStack.pop();
		localsMapStack.pop();
		method = methodStack.peek();
		localGenerator = localGeneratorStack.peek();
		localsMap = localsMapStack.peek();
	}
}
