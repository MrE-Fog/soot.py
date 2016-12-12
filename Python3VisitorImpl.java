package pythonToJimple;

import java.util.ArrayList;
import java.util.List;

import soot.Value;
import soot.jimple.NopStmt;
import soot.jimple.StringConstant;

public class Python3VisitorImpl extends Python3BaseVisitor<Object> {
	
	private JimpleBodyBuilder jb;
	private List<Python3Parser.FuncdefContext> functionList = new ArrayList<Python3Parser.FuncdefContext>();
	
	public void setJimpleBodyBuilder(JimpleBodyBuilder jb) {
		this.jb = jb;
	}
	
	public Object visitStmt(Python3Parser.StmtContext ctx) {
		return visitChildren(ctx);
	}
	
	
	public Object visitExpr_stmt(Python3Parser.Expr_stmtContext ctx) {
		System.out.println("HERE: " + ctx.testlist_star_expr(0).getText());
		Object left = this.visit(ctx.getChild(0));
		
		if(ctx.testlist_star_expr(1) != null){
			Object right = this.visit(ctx.testlist_star_expr(1));
			if(right instanceof String) {
				right = jb.getLocal((String)right);
			}
			jb.generateSimpleAssignLocal((String)left, (Value)right);
		}
		return null;
	}
	
	public Object visitTestlist_star_expr(Python3Parser.Testlist_star_exprContext ctx) {
		return visitChildren(ctx);
	}
	
	public Object visitArith_expr(Python3Parser.Arith_exprContext ctx) {
		int childCount = ctx.getChildCount();
		if(childCount == 1) {
			Object left = this.visit(ctx.term(0));
			return left;
		}
		else {
			List<String> operators = new ArrayList<String>();
			for(int i = 0; i < childCount; i++) {
				if(ctx.getChild(i).getText().equals("+") || 
				   ctx.getChild(i).getText().equals("-")) {
					operators.add(ctx.getChild(i).getText());
				}
			}
			
			Object left = visit(ctx.getChild(0));
			Object right = visit(ctx.getChild(2));
			
			if(left instanceof String) {
				left = jb.getLocal((String)left);
			}
			if(right instanceof String) {
				right = jb.getLocal((String)right);
			}
			
			Object result = jb.generateAddition((Value)left, (Value)right, operators.get(0));
			int termCount = ctx.term().size();
			for(int i = 2, j = 1; i < termCount; i++, j++) {
				Object temp = this.visit(ctx.term(i));
				result = jb.generateAddition((Value)result, (Value)temp, operators.get(j));
			}
			return result;
		}
	}
	
	public Object visitTerm(Python3Parser.TermContext ctx) {
		int childCount = ctx.getChildCount();
		if(childCount == 1) {
			return this.visit(ctx.factor(0));
		}
		else {
			List<String> operators = new ArrayList<String>();
			for(int i = 0; i < childCount; i++) {
				if(ctx.getChild(i).getText().equals("*") || 
				   ctx.getChild(i).getText().equals("/") ||
				   ctx.getChild(i).getText().equals("//")){
					operators.add(ctx.getChild(i).getText());
				}
			}
			Object left = visit(ctx.getChild(0));
			Object right = visit(ctx.getChild(2));
			int factorCount = ctx.factor().size();
			if(left instanceof String) {
				left = jb.getLocal((String)left);
			}
			if(right instanceof String) {
				right = jb.getLocal((String)right);
			}
			Object result = jb.generateMultiplication((Value)left, (Value)right, operators.get(0));
			for(int i = 3, j = 1; i < factorCount; i++, j++) {
				Object temp = this.visit(ctx.getChild(i+1));
				result = jb.generateMultiplication((Value)result, (Value)temp, operators.get(j));
			}
			return result;
		}
	}
	public Object visitFactor(Python3Parser.FactorContext ctx) {
		return visitChildren(ctx);
	}
	
	public Object visitPower(Python3Parser.PowerContext ctx) {
		Object o = visit(ctx.atom());
		if(ctx.trailer().size() > 0) {
			int argumentCount = ctx.trailer(0).arglist().argument().size();
			List<Object> arguments = new ArrayList<Object>();
			for(int i = 0; i < argumentCount; i++) {
				Object temp = visit(ctx.trailer(0).arglist().argument(i));
				arguments.add(temp);
			}
			jb.registerArguments(o, arguments);
			visitFunction((String)o);
			jb.addMethodCall((String)o);
		}
		return o;
	}
	
	public Object visitAtom(Python3Parser.AtomContext ctx) {
		
		if(ctx.NAME() != null) {
			return ctx.NAME().getText();
		}
		
		if(ctx.testlist_comp() != null) {
			Object temp = this.visit(ctx.testlist_comp());
			return temp;
		}
		Object temp = this.visitChildren(ctx);
		return temp;
	}
	
	public Object visitString(Python3Parser.StringContext ctx) {
		if(ctx.STRING_LITERAL() != null) {
			return soot.jimple.StringConstant.v(ctx.getText());
		}
		return visitChildren(ctx);
	}
	
	public Object visitNumber(Python3Parser.NumberContext ctx) {
		if(ctx.FLOAT_NUMBER() != null) {
			return soot.jimple.DoubleConstant.v(Double.parseDouble(ctx.getText()));
		}
		return visitChildren(ctx);
	}
	
	public Object visitInteger(Python3Parser.IntegerContext ctx) {
		if(ctx.DECIMAL_INTEGER() != null) {
			return soot.jimple.IntConstant.v(Integer.parseInt(ctx.getText()));
		}
		return visitChildren(ctx);
	}
	
	public Object visitIf_stmt(Python3Parser.If_stmtContext ctx) {
		Object cond = this.visit(ctx.test(0));
		if(ctx.test(0).or_test(0).and_test(0).AND().size() == 0 &&
		   ctx.test(0).or_test(0).OR().size() == 0){
			soot.jimple.NopStmt lastTgt = soot.jimple.Jimple.v().newNopStmt();
			soot.jimple.NopStmt tgt = jb.generateIfStatement((Value)cond);
			this.visit(ctx.suite(0));
			jb.addGoto(lastTgt);
			jb.addLabel(tgt);
			for(int i = 0; i < ctx.ELIF().size(); i++) {
				tgt = jb.generateElseIfStatement((Value)this.visit(ctx.test(i+1)), tgt);
				this.visit(ctx.suite(i+1));
				jb.addGoto(lastTgt);
				jb.addLabel(tgt);
			}
			if(ctx.ELSE() != null) {
				int i = ctx.suite().size();
				this.visit(ctx.suite(i-1));
				jb.addGoto(lastTgt);
			}
			jb.addLabel(lastTgt);
			
			return null;
		}
		jb.addChainLabel();
		jb.addSuccessLabel();
		this.visit(ctx.suite(0));
		jb.addFailureLabel();
		return null;
	}
	
	public Object visitWhile_stmt(Python3Parser.While_stmtContext ctx) {
		Object cond = visit(ctx.test());
		NopStmt initialTgt = soot.jimple.Jimple.v().newNopStmt();
		jb.addLabel(initialTgt);
		NopStmt tgt = jb.generateIfStatement((Value)cond);
		this.visit(ctx.suite(0));
		jb.addGoto(initialTgt);
		jb.addLabel(tgt);
		return null;
	}
	
	public Object visitFile_input(Python3Parser.File_inputContext ctx) {
		return visitChildren(ctx);
	}
	
	public Object visitShift_expr(Python3Parser.Shift_exprContext ctx) {
		Object o = visitChildren(ctx);
		//System.out.println("Got back  from visitShift_expr " + o.toString());
		return o;
	}
	
	public Object visitOr_test(Python3Parser.Or_testContext ctx) {
		int n = ctx.OR().size();
		if(n == 0) {
			return visitChildren(ctx);
		}
		int k = ctx.and_test().size();
		Object object = null;
		jb.addFailureLabel();
		for(int i = 0; i < k; i++) {
			object = visit(ctx.and_test(i));
			if(object != null)
			  jb.generateOr((Value)object);
		}
		jb.createFailureBranch();
		return object;
	}
	
	public Object visitAnd_test(Python3Parser.And_testContext ctx) {
		int n = ctx.AND().size();
		if(n == 0) {
			return visitChildren(ctx);
		}
		int k = ctx.not_test().size();
		Object object = null;
		for(int i = 0; i < k; i++) {
			object = visit(ctx.not_test(i));
			jb.generateAnd((Value)object);
			if(i+1 != k) {
				jb.addChainLabel();
			}
		}
		return null;
	}

	public Object visitNot_test(Python3Parser.Not_testContext ctx) {
		Object o = visitChildren(ctx);
		//System.out.println("Got back from visitNot_test " + o.toString());
		return o;
	}

	public Object visitComparison(Python3Parser.ComparisonContext ctx) {
		List<String> operators = new ArrayList<String>();
		int operatorCount = ctx.comp_op().size();
		int starExprCount = ctx.star_expr().size();
		
		if(starExprCount == 1)
			return this.visit(ctx.star_expr(0));
		
		Object left = this.visit(ctx.star_expr(0));
		Object right = this.visit(ctx.star_expr(1));

		if(left instanceof String) {
			left = jb.getLocal((String)left);
		}
		if(right instanceof String) {
			right = jb.getLocal((String)right);
		}
		return jb.generateComparison((Value)left, (Value)right, ctx.comp_op(0).getText());
	}
	
	public Object visitComp_op(Python3Parser.Comp_opContext ctx) {
		return ctx.getText();
	}
	
	public Object visitStar_expr(Python3Parser.Star_exprContext ctx) {
		Object o = visitChildren(ctx);
		//System.out.println("Got back  from star_expr " + o.toString());
		return o;
	}

	public Object visitExpr(Python3Parser.ExprContext ctx) { return visitChildren(ctx); }

	public Object visitXor_expr(Python3Parser.Xor_exprContext ctx) { return visitChildren(ctx); }
	
	public Object visitAnd_expr(Python3Parser.And_exprContext ctx) { return visitChildren(ctx); }
	
	public Object visitTest(Python3Parser.TestContext ctx) { return visitChildren(ctx); }
	
	public Object visitTestlist_comp(Python3Parser.Testlist_compContext ctx) {
		Object o = visitChildren(ctx);
		//System.out.println("Got back  from test_list_comp " + o.toString());
		return o;
	}
	
	public Object visitFuncdef(Python3Parser.FuncdefContext ctx) {
		//int n = ctx.parameters().typedargslist().tfpdef().size();
		//ctx.suite();
		if(!jb.isFunctionRegistered(ctx.NAME().getText())) {
			functionList.add(ctx);
			return null;
		}
		List<String> parameterNames = new ArrayList<String>();
		int n = ctx.parameters().typedargslist().tfpdef().size();
		for(int i = 0; i < n; i++) {
			parameterNames.add(ctx.parameters().typedargslist().tfpdef(i).NAME().getText());
		}
		jb.createMethod(ctx.NAME().getText(), parameterNames);
		visit(ctx.suite());
		jb.wrapUpMethod();
		return null;
	}
	
	public Object visitReturn_stmt(Python3Parser.Return_stmtContext ctx) {
		Object o = visit(ctx.testlist());
		jb.addReturnStatement(o);
		return o;
	}
	
	public void visitFunctions() {
		for(int i = 0; i < functionList.size(); i++) {
			System.out.println("Visiting function " + functionList.get(i).NAME());
			this.visitFuncdef(functionList.get(i));
		}
	}
	
	public void visitFunction(String name) {
		for(int i = 0; i < functionList.size(); i++) {
			if(functionList.get(i).NAME().getText().equals(name))
				visitFuncdef(functionList.get(i));
		}
	}
}
