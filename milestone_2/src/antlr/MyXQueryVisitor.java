package antlr;

import java.util.*;

import org.antlr.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.w3c.dom.*;

import javax.sound.midi.SysexMessage;
import javax.xml.parsers.*;
import java.io.*;

import org.xml.sax.SAXException;

import antlr.XQueryParser.AttNameContext;
import antlr.XQueryParser.TagNameContext;


public class MyXQueryVisitor extends XQueryBaseVisitor<ArrayList<Node>> {
	ArrayList<Node> currentNodes = new ArrayList<Node>();
	Map<String, ArrayList<Node>> xqMap = new HashMap<>();
	Stack<HashMap<String, ArrayList<Node>>> xqStack = new Stack<>();

	//TODO!!! check
	Document document = null;

	@Override
	public ArrayList<Node> visitApRoot(XQueryParser.ApRootContext ctx) {
		//System.out.println("call visitApRoot");
		xlmParser(ctx.fileName().getText());
		return visitChildren(ctx);

	}

	@Override
	public ArrayList<Node> visitApCurrent(XQueryParser.ApCurrentContext ctx) {
		//System.out.println("call visitApCurrent");
		ArrayList<Node> root = xlmParser(ctx.fileName().getText());
		currentNodes = root;

		Queue<Node> queue = new LinkedList<>(this.currentNodes);
		ArrayList<Node> res = new ArrayList<>(this.currentNodes);
		getDescendent(res, queue);//TODO!
		this.currentNodes = res;
		visit(ctx.rp());
		//System.out.println("length of currentNodes: " + currentNodes.size());
		return this.currentNodes;
	}

	@Override
	public ArrayList<Node> visitRpParent(XQueryParser.RpParentContext ctx) {
		// CHECKED!
		ArrayList<Node> temp = new ArrayList<Node>();

		for(Node n: currentNodes) {
			Node temp_node = n.getParentNode();
			if(temp_node != null && !temp.contains(temp_node)) {
				//System.out.println("add a parent node");
				temp.add(temp_node);//may cause error
			}
		}

		currentNodes = temp;//change
		return temp;
	}

	@Override
	public ArrayList<Node> visitRpAttName(XQueryParser.RpAttNameContext ctx) {
		// TODO FINISHED
		ArrayList<Node> temp = new ArrayList<>();
		String target_name = ctx.attName().getText();
		for (Node n : this.currentNodes) {
			Element e = (Element) n;
			String att = e.getAttribute(target_name);
			if (att.length() > 0) {
				temp.add(n);
			}
		}
        return temp;
	}

	@Override
	public ArrayList<Node> visitRpFromCurr(XQueryParser.RpFromCurrContext ctx) {
		// TODO
        visit(ctx.rp(0));
        Queue<Node> queue = new LinkedList<>(this.currentNodes);
        ArrayList<Node> all_children = new ArrayList<>(this.currentNodes);
        getDescendent(all_children, queue);
        Set<Node> unique_curr = new HashSet<Node>(all_children);
        this.currentNodes = new ArrayList<Node>(unique_curr);
        visit(ctx.rp(1));
        Set<Node> unique_curr2 = new HashSet<Node>(this.currentNodes);
        this.currentNodes = new ArrayList<Node>(unique_curr2);
        return this.currentNodes;
	}

	@Override
	public ArrayList<Node> visitRpText(XQueryParser.RpTextContext ctx) {
		// TODO FINISHED
		ArrayList<Node> r = new ArrayList<Node>();
        for (Node node : this.currentNodes) {
        	for (Node n: getChildren(node)) {
        		if(n.getNodeType() == Node.TEXT_NODE && n.getTextContent() != null) {
        			//if null is kept, output will have a lot of \n
        			r.add(n);
        		}
        	}
        }
        this.currentNodes = r; //change
        return r;
	}

	@Override
	public ArrayList<Node> visitRpTagName(XQueryParser.RpTagNameContext ctx) {
		//System.out.println("call visitRpTagName");
        ArrayList<Node> temp = new ArrayList<Node>();
        for (Node node : this.currentNodes) {
        	ArrayList<Node> children_temp = getChildren(node);
            for (Node n : children_temp) {
                if (n.getNodeName().equals(ctx.getText()) ) {
					//&& n.getNodeType() == Node.ELEMENT_NODE
					temp.add(n);
				}
            }
        }

        this.currentNodes = temp;//change
		return temp;
	}

	@Override
	public ArrayList<Node> visitRpChildren(XQueryParser.RpChildrenContext ctx) {
		// TODO FIXed

		//all next level children of the currentNodes
		ArrayList<Node> res = new ArrayList<>();
		for(Node n: currentNodes){
			NodeList n_children = n.getChildNodes();
			for(int i = 0; i < n_children.getLength(); i++){
				Node n_child = n_children.item(i);
				if(n_child.getNodeType() == Node.ELEMENT_NODE){
					//System.out.println("child content: " + n_child.getTextContent());
					res.add(n_child);
				}
			}
		}
		currentNodes = res;
		return res;


	}

	@Override
	public ArrayList<Node> visitRpCurrent(XQueryParser.RpCurrentContext ctx) {
		// TODO FINISHED
		return this.currentNodes;
	}

	@Override
	public ArrayList<Node> visitRpConcat(XQueryParser.RpConcatContext ctx) {
		// TODO
		ArrayList<Node> storeOrigin = this.currentNodes;
		visit(ctx.rp(0));
		ArrayList<Node> rp1 = this.currentNodes;//from rp1
		currentNodes = storeOrigin;
		visit(ctx.rp(1));
		ArrayList<Node> rp2 = this.currentNodes;//from rp2
		rp1.addAll(rp2);
		//FIXME!!!
        this.currentNodes = rp1; //change
		return this.currentNodes;
	}

	@Override
	public ArrayList<Node> visitRpRoot(XQueryParser.RpRootContext ctx) {
		// TODO rp '/' rp
        visit(ctx.rp(0));
        visit(ctx.rp(1));
        Set<Node> unique_curr = new HashSet<Node>(this.currentNodes);
        ArrayList<Node> temp = new ArrayList<>(unique_curr);

		return temp;
	}

	@Override
	public ArrayList<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) {
		//System.out.println("call visitRpFilter");
		visit(ctx.rp());
		ArrayList<Node> temp = new ArrayList<>(currentNodes);
		ArrayList<Node> res = new ArrayList<>();
		for (Node n : temp) {
			currentNodes = new ArrayList<>();
			currentNodes.add(n);
			ArrayList<Node> filter_res = visit(ctx.filter());
			//System.out.println("number of nodes after the filter: " + filter_res.size());
			if (filter_res.size() != 0) {
				res.add(n);
			}
		}
		this.currentNodes = res;//change
		return res;
	}

	@Override
	public ArrayList<Node> visitRpPathNodes(XQueryParser.RpPathNodesContext ctx) {
		//FINISHED
		ArrayList<Node> temp = visit(ctx.rp());
		return temp;
	}

	@Override
	public ArrayList<Node> visitFilterEqConst(XQueryParser.FilterEqConstContext ctx) {
		// TODO
        ArrayList<Node> temp =  new ArrayList<Node>();
        ArrayList<Node> left = (ArrayList<Node>) visit(ctx.rp());
        String temp_str =  ctx.StringConstant().getText();
        temp_str = temp_str.replace("\"","");
        //System.out.println("target string:" + temp_str);
        for(Node n: left){
            if (n.getTextContent().equals(temp_str)){
            	this.currentNodes = left;
            	return left;
            }
        }
        return temp;
	}

	@Override
	public ArrayList<Node> visitFilterNot(XQueryParser.FilterNotContext ctx) {
		//fixed!
		//original issue: we forget to set currentNodes back
		//System.out.println("FilterNOT: call visitFilterNot");
		ArrayList<Node> original = new ArrayList<Node>(currentNodes);
		ArrayList<Node> res_filted = visit(ctx.filter());
		currentNodes = original;

		if (res_filted.size() == 0){
			//System.out.println("FilterNOT: all current nodes satisfy 'not filter'");
			return currentNodes;
		}
		return new ArrayList<Node>();
	}

	@Override
	public ArrayList<Node> visitFilterOr(XQueryParser.FilterOrContext ctx) {
		// TODO FIXME!!!
		ArrayList<Node> original = new ArrayList<Node>(this.currentNodes);
        ArrayList<Node> rp1 = visit(ctx.filter(0));
        this.currentNodes = original;
        ArrayList<Node> rp2 = visit(ctx.filter(1));
        rp1.addAll(rp2);
        Set<Node> union = new HashSet<Node>(rp1);
        ArrayList<Node> temp = new ArrayList<Node>(union);
        //this.currentNodes = original; //check
		return temp;
	}

	@Override
	public ArrayList<Node> visitFilterAnd(XQueryParser.FilterAndContext ctx) {
		// TODO check
		ArrayList<Node> original = this.currentNodes;
        ArrayList<Node> rp1 = visit(ctx.filter(0));
        this.currentNodes = original;
        ArrayList<Node> rp2 = visit(ctx.filter(1));
        // intersection FIXME!!!
        rp1.retainAll((rp2));
        Set<Node> intersection = new HashSet<Node>(rp1);
        ArrayList<Node> temp = new ArrayList<Node>(intersection);
        //this.currentNodes = temp;//check FIXME!!!
        return temp;
	}

	@Override
	public ArrayList<Node> visitFilterRp(XQueryParser.FilterRpContext ctx) {
		// TODO check!!!
        //ArrayList<Node> temp = new ArrayList<>(this.currentNodes);
        //ArrayList<Node> rt = visit(ctx.rp());
        //this.currentNodes = temp;
        visit(ctx.rp());
        return this.currentNodes;
	}

	@Override
	public ArrayList<Node> visitFilterEq(XQueryParser.FilterEqContext ctx) {
		// TODO Auto-generated method stub
		//System.out.println("call FilterEq");
		ArrayList<Node> store = new ArrayList<>(this.currentNodes);//store original nodes

		ArrayList<Node> res0 = visit(ctx.rp(0));
		//System.out.println("first filter gives " + res0.size() + " nodes");
		this.currentNodes = store;//set currentNodes back

		ArrayList<Node> res1 = visit(ctx.rp(1));
		//System.out.println("second filter gives " + res0.size() + " nodes");
		this.currentNodes = store;

		for(Node i : res0){
			for(Node j : res1){
				//isEqualNode(): build in
				if(i.isEqualNode(j)){
					//true
					return store;
				}
			}
		}

		return new ArrayList<>();
	}

	@Override
	public ArrayList<Node> visitFilterIs(XQueryParser.FilterIsContext ctx) {
		// TODO Auto-generated method stub
		ArrayList<Node> store = new ArrayList<>(this.currentNodes);

		ArrayList<Node> res0 = visit(ctx.rp(0));
		this.currentNodes = store;//set currentNodes back

		ArrayList<Node> res1 = visit(ctx.rp(1));
		this.currentNodes = store;

		for(Node i : res0){
			for(Node j : res1){
				//isSameNode(): build in
				if(i.isSameNode(j)){
					//true
					return store;
				}
			}
		}

		return new ArrayList<>();
	}

	@Override
	public ArrayList<Node> visitFilterCurrent(XQueryParser.FilterCurrentContext ctx) {
		return visit(ctx.filter());
	}
	
	
	
	@Override
	public ArrayList<Node> visitTagName(XQueryParser.TagNameContext ctx) {
		// TODO FINISHED check FIXME!!!
		return visitChildren(ctx);
	}

	@Override
	public ArrayList<Node> visitAttName(XQueryParser.AttNameContext ctx) {
		// TODO FINISHED check FIXME!!!
		return visitChildren(ctx);
	}


	private ArrayList<Node> xlmParser(String input_f) {
		ArrayList<Node> temp = new ArrayList<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		//Build Document
		document = null;
		try {
			document = builder.parse(new File(input_f));
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		return document.getDocumentElement();
		document.getDocumentElement().normalize();
		temp.add(document);
		currentNodes = temp;
		//System.out.println("1: length of currentNodes: " + currentNodes.size());
		return temp;

	}



	private ArrayList<Node> getChildren(Node curr){
		//only retrieve children of next level
		ArrayList<Node> children = new ArrayList<Node>();
        NodeList temp = curr.getChildNodes();
        for (int i = 0; i < temp.getLength(); i++) {
        	children.add(temp.item(i));
        }
        return children;
	}
	

    private void getDescendent(ArrayList<Node> res, Queue<Node> queue) {
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (!n.hasChildNodes()) {
                continue;
            }
            NodeList children = n.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                res.add(children.item(i));
                queue.offer(children.item(i));
            }
        }
    }






    //--------------------------------------------------------------------
    //XQ START

	@Override
	public ArrayList<Node> visitXqVar(XQueryParser.XqVarContext ctx) {
		//System.out.println("call visitXqVar");
		return xqMap.get(ctx.getText());
	}

	//TODO!!! CHECK
	@Override
	public ArrayList<Node> visitXqStrConst(XQueryParser.XqStrConstContext ctx) {
		String temp_str  = ctx.StringConstant().getText(); //need to check the input string
		temp_str = temp_str.replace("\"","");
		Node nodeT = makeText(temp_str); //calling helper function
		ArrayList<Node> temp = new ArrayList<Node>();
		temp.add(nodeT);
		return temp;
	}

	@Override
	public ArrayList<Node> visitXqAp(XQueryParser.XqApContext ctx) {
		return visit(ctx.ap());
	}

	@Override public ArrayList<Node> visitXqParenthesis(XQueryParser.XqParenthesisContext ctx) {
		return visit(ctx.xq());
	}

	@Override public ArrayList<Node> visitXqConcat(XQueryParser.XqConcatContext ctx) {
		HashMap<String, ArrayList<Node>> originCopy = new HashMap<>(xqMap);
		ArrayList<Node> temp = new ArrayList<Node>();
		ArrayList<Node> ans = new ArrayList<Node>();
		ArrayList<Node> left = (ArrayList<Node>) visit(ctx.xq(0));
		temp.addAll(left);
		xqMap = originCopy;
		ArrayList<Node> right = (ArrayList<Node>) visit(ctx.xq(1));
		temp.addAll(right);
		xqMap = originCopy;

		for(Node n: temp){
			if(!ans.contains(n)){
				ans.add(n);
			}
		}
		return ans;
	}

	@Override
	public ArrayList<Node> visitXqDescend(XQueryParser.XqDescendContext ctx) {
		currentNodes = visit(ctx.xq());
		return visit(ctx.rp());
	}

	@Override
	public ArrayList<Node> visitXqFromCurr(XQueryParser.XqFromCurrContext ctx) {
		currentNodes = visit(ctx.xq());
		ArrayList<Node> res = new ArrayList<>(currentNodes);
		Queue<Node> queue = new LinkedList<>(currentNodes);
		getDescendent(res, queue);
		currentNodes = res;
		visit(ctx.rp());
		return currentNodes;
	}
	
	
	//HELPER FUNCTIONS
//	int timesMakeNode = 0;
	public Node makeNode(String tagName, List<Node> list){
//		System.out.println("call makeNode");
//		timesMakeNode++;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = builder.newDocument();

		Node newElem = doc.createElement(tagName);
		//System.out.println("list size: " + list.size());
		for (Node node : list) {
			Node newNode = doc.importNode(node, true);
			newElem.appendChild(newNode);
		}
		return newElem;
	}
	
	//makeText
	//TODO!!! check
	public Node makeText(String stringConst){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		//Build Document
		Document doc = builder.newDocument();;
		Node textNode = doc.createTextNode(stringConst);
		return textNode;
	}
	

	@Override
	public ArrayList<Node> visitXqNew(XQueryParser.XqNewContext ctx) {
		//create a new node such as <result></result>

		//System.out.println("call XqNew");
		ArrayList<Node> res = new ArrayList<>();
		ArrayList<Node> xqRes = visit(ctx.xq());
		//System.out.println("ctx.tagName(0).getText(): " + ctx.tagName(0).getText());
		//System.out.println("xqRes length: " + xqRes.size());
		if(xqRes.size() == 0){
			xqRes = new ArrayList<Node>();
		}
		res.add(makeNode(ctx.tagName(0).getText(), xqRes));

		return res;
	}

//	boolean printOnce = false;

	private void helperFLWR(XQueryParser.XqFLWRContext ctx, int counter, ArrayList<Node> res){
		//handle for loop via recursion

//		System.out.println("call helperFLWR");
//		System.out.println("counter: " + counter);
//		System.out.println("ctx.forClause().var().size(): " + ctx.forClause().var().size());


		if (ctx.forClause()!= null && counter != ctx.forClause().var().size()){

			String var = ctx.forClause().var(counter).getText();
//			System.out.println("var:" + var);
			ArrayList<Node> nodes = visit(ctx.forClause().xq(counter));
//			System.out.println("nodes number: " + nodes.size());
			for (Node n : nodes){
				ArrayList<Node> node_list = new ArrayList<>();
				node_list.add(n);
				xqMap.remove(var);
				xqMap.put(var, node_list);
//				System.out.println("recurrsion and call helperFLWR again");
				helperFLWR(ctx, counter + 1, res);

			}
		}
		else {
			//case: it's the last for loop
			//System.out.println("in helperFLWR, else");
			HashMap<String, ArrayList<Node>> original = new HashMap<>(xqMap);
			if (ctx.letClause() != null) {
				//System.out.println("there is a letClause");
				visit(ctx.letClause());
			}

			if (ctx.whereClause() != null && visit(ctx.whereClause()).size() == 0) {
				//if no node satisfy where cond, return
				//no need to undergo return clause
				return;
			}

			ArrayList<Node> ret = visit(ctx.returnClause());
			if (ret != null) {
				res.addAll(ret);
			}


			//reset
			xqMap = original;

		}

	}
	
	boolean needRewrite = true;

	@Override
	public ArrayList<Node> visitXqFLWR(XQueryParser.XqFLWRContext ctx) {

		System.out.println("call visitXqFLWR");
		XQueryParser.XqFLWRContext original_ctx = ctx;
		ArrayList<Node> res = new ArrayList<>();

		if(needRewrite) {
			System.out.println("need to be rewritten, proceed to rewrite process");
			HashMap<String, ArrayList<Node>> original = new HashMap<>(xqMap);
			xqStack.push(original);
			Optimized q_rewrite = new Optimized();
			String re = q_rewrite.inputRewrite(ctx);
			FileWriter writer;
			try {
				writer = new FileWriter("rewrited.txt");
				writer.write(re);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (re == "") {
				needRewrite = false;

				System.out.println("RE = EMPTY! no need to rewrite! / has been rewritten!");
				ctx = original_ctx;
				System.out.println("forClause size: " + ctx.forClause().var().size());
				if(ctx.forClause().var().size() == 2) {

					System.out.println("var0: \n"+ ctx.forClause().var(0).getText() + "\n" + "xq0:\n" + ctx.forClause().xq(0).getText());
					System.out.println("var1: \n"+ ctx.forClause().var(1).getText() + "\n" + "xq1:\n" + ctx.forClause().xq(1).getText());
				}
				helperFLWR(ctx,0,res);
			} else {
				//System.out.println(1);
				//ArrayList<Node> res = new ArrayList<>();

				//System.out.println("REWRITE:\n" + re);
				res = XQueryOpt.evalRewrite(re);
			}
		}
		else {
			System.out.println("no need to rewrite! / has been rewritten!");
			System.out.println("forClause size: " + ctx.forClause().var().size());
			if(ctx.forClause().var().size() == 2) {

				System.out.println("var0: \n"+ ctx.forClause().var(0).getText() + "\n" + "xq0:\n" + ctx.forClause().xq(0).getText());
				System.out.println("var1: \n"+ ctx.forClause().var(1).getText() + "\n" + "xq1:\n" + ctx.forClause().xq(1).getText());
			}
			helperFLWR(ctx,0,res);
		}

		return res;

	}
	
	
	@Override
	public ArrayList<Node> visitXqLet(XQueryParser.XqLetContext ctx) {
		//System.out.println("call visitXqLet");
		HashMap<String, ArrayList<Node>> temp = new HashMap<>(xqMap);
		visit(ctx.letClause());
		ArrayList<Node> res = visit(ctx.xq());
		xqMap = temp;
		return res;
	}

	@Override
	public ArrayList<Node> visitForClause(XQueryParser.ForClauseContext ctx) {
		//get all var:node pair in this for loop level
		//a for loop can contain multiple var in xq, so we need a counter
		//System.out.println("call visitForClause");
		ArrayList<Node> res = new ArrayList<>();
		return res;

	}

	@Override
	public ArrayList<Node> visitLetClause(XQueryParser.LetClauseContext ctx) {
		for (int i = 0; i < ctx.var().size(); i++) {
			String var = ctx.var(i).getText();
			ArrayList<Node> xq_res = visit(ctx.xq(i));
			xqMap.put(var, xq_res);
		}
		return currentNodes;
	}

	@Override
	public ArrayList<Node> visitWhereClause(XQueryParser.WhereClauseContext ctx) {
		//System.out.println("visitWhereClause");
		return visit(ctx.cond());
	}

	@Override
	public ArrayList<Node> visitReturnClause(XQueryParser.ReturnClauseContext ctx) {
		return visit(ctx.xq());
	}



	//XQ Cond START

	@Override
	public ArrayList<Node> visitXqCondEq(XQueryParser.XqCondEqContext ctx) {
		ArrayList<Node> originNodesCopy = new ArrayList<>(currentNodes);
		Map<String, ArrayList<Node>> originMapCopy = new HashMap<>(xqMap);
		ArrayList<Node> res0 = visit(ctx.xq(0));
		currentNodes = originNodesCopy;
		xqMap = originMapCopy;
		ArrayList<Node> res1 = visit(ctx.xq(1));
		currentNodes = originNodesCopy;
		xqMap = originMapCopy;

		for(Node i : res0){
			for(Node j : res1){
				//isEqualNode(): build in
				if(i.isEqualNode(j)){
					//true
					return originNodesCopy;
				}
			}
		}

		return new ArrayList<>();
	}

	@Override
	public ArrayList<Node> visitXqCondIs(XQueryParser.XqCondIsContext ctx) {
		//System.out.println("call XqCondIs");
		ArrayList<Node> originNodesCopy = new ArrayList<>(currentNodes);
		Map<String, ArrayList<Node>> originMapCopy = new HashMap<>(xqMap);
		ArrayList<Node>res = new ArrayList<>();

		ArrayList<Node> res0 = visit(ctx.xq(0));
		currentNodes = originNodesCopy;
		xqMap = originMapCopy;

		ArrayList<Node> res1 = visit(ctx.xq(1));
		currentNodes = originNodesCopy;
		xqMap = originMapCopy;

		for(Node i : res0){
			for(Node j : res1){
				//isSameNode(): build in
				if(i == j){
					//System.out.println("is same node");
					res.add(i);
					return res;
				}
			}
		}

		return new ArrayList<>();

	}

	@Override
	public ArrayList<Node> visitXqCondEmpty(XQueryParser.XqCondEmptyContext ctx) {
		ArrayList<Node> originNodesCopy = new ArrayList<>(currentNodes);
		ArrayList<Node> res = visit(ctx.xq());
		currentNodes = originNodesCopy;

		if(res.size() == 0){
			return currentNodes;
		}
		return new ArrayList<>();
	}

	@Override
	public ArrayList<Node> visitXqCondSatisfy(XQueryParser.XqCondSatisfyContext ctx) {
		for (int i = 0; i < ctx.var().size(); i++) {
			String var = ctx.var(i).getText();
			ArrayList<Node> nodes = visit(ctx.xq(i));
			xqMap.put(var, nodes);
		}
		return visit(ctx.cond());
	}

	@Override
	public ArrayList<Node> visitXqCondXqParenthesis(XQueryParser.XqCondXqParenthesisContext ctx) {
		return visit(ctx.cond());
	}

	@Override
	public ArrayList<Node> visitXqCondAnd(XQueryParser.XqCondAndContext ctx) {
		ArrayList<Node> originNodesCopy = currentNodes;
		ArrayList<Node> res0 = visit(ctx.cond(0));
		currentNodes = originNodesCopy;
		ArrayList<Node> res1 = visit(ctx.cond(1));
        if (!res0.isEmpty() && !res1.isEmpty()) {
            return currentNodes;
        }
        return new ArrayList<Node>();

	}

	//
	@Override
	public ArrayList<Node> visitXqCondOr(XQueryParser.XqCondOrContext ctx) {
		ArrayList<Node> originNodesCopy = new ArrayList<Node>(this.currentNodes);
		ArrayList<Node> cond1 = visit(ctx.cond(0));
		currentNodes = originNodesCopy;
		ArrayList<Node> cond2 = visit(ctx.cond(1));
		cond1.addAll(cond2);
		Set<Node> union = new HashSet<Node>(cond1);
		ArrayList<Node> res = new ArrayList<Node>(union);
		return res;
	}

	//'not' cond
	@Override
	public ArrayList<Node> visitXqCondNot(XQueryParser.XqCondNotContext ctx) {
		ArrayList<Node> original = new ArrayList<Node>(currentNodes);
		ArrayList<Node> res = visit(ctx.cond());
		if(res.size() == 0){
			currentNodes = original;
			return currentNodes;
		}
		return new ArrayList<>();
	}



	/*----------------joinClause-----------------*/
	@Override
	public ArrayList<Node> visitXqJoin(XQueryParser.XqJoinContext ctx) { 
		return visitChildren(ctx); 
	}

	@Override
	public ArrayList<Node> visitJoinClause(XQueryParser.JoinClauseContext ctx) {
		System.out.println("call visitJoinClause");
		System.out.println("xq0:\n" + ctx.xq(0).getText());
		System.out.println("xq1:\n" + ctx.xq(1).getText());

		ArrayList<Node> res0 = visit(ctx.xq(0));
		ArrayList<Node> res1 = visit(ctx.xq(1));
		System.out.println("visit first xq results in # nodes: " + res0.size());
		System.out.println("visit second xq results in # nodes: " + res1.size());

		int size = ctx.attNames(0).tagName().size();
		//results after visiting xq0 might be several attributes as in exxample 3
		//get the content of first [] and second []. The third and fourth variable in joinclause input statement
		String [] res0_arr = new String [size];
		String [] res1_arr = new String [size];
		for (int i = 0; i < size; i++){
			res0_arr[i] = ctx.attNames(0).tagName(i).getText();
			res1_arr[i] = ctx.attNames(1).tagName(i).getText();
		}
		Map<String, ArrayList<Node>> hashJoinMap0 = formMap(res0_arr, res0);
		ArrayList<Node> result = hashJoin(hashJoinMap0, res0_arr, res1_arr, res1);
		currentNodes = result;
		System.out.println("# of nodes resulting from joinClause: " + currentNodes.size());
		return result;
	}

	private Map<String, ArrayList<Node>> formMap(String [] tags, ArrayList<Node> res){
		System.out.println("call formMap()");
		HashMap<String, ArrayList<Node>> hashJoinMap = new HashMap<>();

		// res is a node call tuple and it's children are the actual nodes we need to map
		System.out.println("formMap input res.size():" + res.size());
//		System.out.println("res has " + children.size() + " kids");
		for (Node n : res){
			String key = "";
			ArrayList<Node> children = getChildren(n);
			for (Node c : children) {
				key = c.getNodeName();
//				System.out.println("tryting to store node: " + key);
				if (!hashJoinMap.containsKey(key)) {
					System.out.println("map doesn't contain that key yet: " + key);
					System.out.println("store node with key: " + key);
					ArrayList<Node> value = new ArrayList<Node>();
					value.add(c);
					hashJoinMap.put(key, value);
				}
				else{
					ArrayList<Node> value = hashJoinMap.get(key);
					value.add(c);
					hashJoinMap.replace(key,value);
				}
			}
		}
		return hashJoinMap;
	}


	private ArrayList<Node> hashJoin(Map<String, ArrayList<Node>> hashJoinMap0,  String[] res0_tags, String[] res1_tags, ArrayList<Node> res1) {
		ArrayList<Node> result = new ArrayList<Node>();
		System.out.println("call hashJoin");

		HashMap<String, ArrayList<Node>> hashJoinMap1 = new HashMap<>();
		for (Node n : res1){
			String key = "";
			ArrayList<Node> children = getChildren(n);
			for (Node c : children) {
				key = c.getNodeName();
//				System.out.println("tryting to store node: " + key);
				if (!hashJoinMap1.containsKey(key)) {
					System.out.println("map doesn't contain that key yet!");
					System.out.println("store node with key: " + key);
					ArrayList<Node> value = new ArrayList<Node>();
					value.add(c);
					hashJoinMap1.put(key, value);
				}
				else{
					ArrayList<Node> value = hashJoinMap1.get(key);
					value.add(c);
					hashJoinMap1.replace(key,value);
				}
			}
		}

		for(int i = 0; i < res0_tags.length; i++){
			String tag0 = res0_tags[i];
			System.out.println("tag0:" + tag0);
			String tag1 = res1_tags[i];
			System.out.println("tag1:" + tag1);
			ArrayList<Node> tag0nodes = hashJoinMap0.get(tag0);
			System.out.println("# of nodes under tag0: " + tag0nodes.size());
			ArrayList<Node> tag1nodes = hashJoinMap1.get(tag1);
			System.out.println("# of nodes under tag1: " + tag1nodes.size());
			for(int j = 0; j < tag0nodes.size(); j++){
				boolean isEqual = tag0nodes.get(j).isEqualNode(tag1nodes.get(j));
				if(!isEqual){
					System.out.println("return empty as nodes in filters are not equal");
					Node working0 = tag0nodes.get(j);
//					System.out.println("node0 name:"+ working0.getNodeName());
					Node working1 = tag1nodes.get(j);
//					System.out.println("node1 name:"+ working1.getNodeName());
				}
			}


//			if(! tag0node.get(0).getText(tag1node.get(0))) {
//				return result;
//			}

//			if(tag0nodes.size() != tag1nodes.size()){
//				return result;
//			}
//
//			for(int j = 0; j < tag0nodes.size(); j++){
//				String content0 = tag0nodes.get(j).getTextContent();
//				String content1 = tag1nodes.get(j).getTextContent();
//
//				if(content0 != content1){
//					System.out.println("content0: " + content0);
//					System.out.println("content1: " + content1);
//					return result;
//				}
//			}
		}

		//filter has been checked, two tuples satisfy the conditions in []
		System.out.println("filter has been checked, now put them all to result arraylist");
		for(String key : hashJoinMap0.keySet()){
			System.out.println("map0 key: " + key);
			result.addAll(hashJoinMap0.get(key));
		}
		for(String key : hashJoinMap1.keySet()){
			System.out.println("map1 key: " + key);
			result.addAll(hashJoinMap1.get(key));
		}

//		for (Node n : res1){
//			String key = "";
//			for (String tag : res1_tags) {
//				ArrayList<Node> children = getChildren(n);
//				for (Node c : children) {
//					if (tag.equals(c.getNodeName())) {
//						key += c.getTextContent();
//					}
//				}
//			}
//
//			//find a row match, join col
//			if (hashJoinMap0.containsKey(key)) {
//				ArrayList<Node> curr_values = hashJoinMap0.get(key);
//				for (Node n2 : curr_values) {
//					ArrayList<Node> temp = getChildren(n2);
//					temp.addAll(getChildren(n));
//					result.add(makeNode("tuple", temp)); // $b = $a
//				}
////				result.addAll(product(hashJoinMap0.get(key),n));
//			}
//		}
		return result;
	}


//	private Node makeElem(String tag, ArrayList<Node> list){
//		Node result = doc.createElement(tag);
//		for (Node node : list) {
//			if (node != null) {
//				Node newNode = doc_Out.importNode(node, true);
//				result.appendChild(newNode);
//			}
//		}
//		return result;
//	}











}