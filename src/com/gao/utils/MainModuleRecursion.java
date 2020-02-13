package com.gao.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gao.model.Module;

/**<p>描述：前台模块树形菜单递归类</p>
 * 
 * @author gao 2014-06-10
 *@version 1.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MainModuleRecursion {
	StringBuffer returnStr = new StringBuffer();

	public MainModuleRecursion() {// 构造方法里初始化模拟List
	}

	public String recursionFuncModuleFn(List list, Module module) {

		String data = "";
		if (hasChild(list, module)) {
			returnStr.append("{\"text\":\"" + module.getMdName() + "\"");
			returnStr.append(",\"expanded\":true");
			returnStr.append(",\"iconCls\":\"" + module.getMdIcon() + "\"");
			returnStr.append(",\"children\":[");
			List childList = getChildList(list, module);
			Iterator it = childList.iterator();
			while (it.hasNext()) {
				Module n = (Module) it.next();
				recursionFuncModuleFn(list, n);
			}
			returnStr.append("]},");
		} else {
			returnStr.append("{\"id\":\"");
			returnStr.append(StringManagerUtils.replaceAll(module.getMdCode()));
			returnStr.append("\",\"text\":\"");
			returnStr.append(module.getMdName());
			returnStr.append("\",\"md_icon\":\"");
			returnStr.append(module.getMdIcon());
			returnStr.append("\",\"mdCode\":\"");
			returnStr.append(StringManagerUtils.replaceAll(module.getMdCode()));
			returnStr.append("\",\"viewsrc\":\"");
			returnStr.append(StringManagerUtils.replaceAll(module.getMdUrl()));
			returnStr.append("\",\"controlsrc\":\"");
			returnStr.append(module.getMdControl());
			returnStr.append("\",\"closable\":true");
			returnStr.append(",\"iconCls\":\"" + module.getMdIcon());
			returnStr.append("\",\"leaf\":true},");
		}
		data = returnStr.toString();
		return data;
	}

	public String recursionAddModuleFn(List list, Module module) {

		String data = "";
		if (hasChild(list, module)) {
			returnStr.append("{\"text\":\"" + module.getMdName() + "\"");
			returnStr.append(",\"expanded\":true,");
			returnStr.append("\"id\":");
			returnStr.append(module.getMdId());
			returnStr.append(",\"iconCls\":\"" + module.getMdIcon() + "\"");
			returnStr.append(",\"children\":[");
			List childList = getChildList(list, module);
			Iterator it = childList.iterator();
			while (it.hasNext()) {
				Module n = (Module) it.next();
				recursionAddModuleFn(list, n);
			}
			returnStr.append("]},");
		} else {
			returnStr.append("{\"id\":");
			returnStr.append(module.getMdId());
			returnStr.append(",\"text\":\"");
			returnStr.append(module.getMdName());
			returnStr.append("\",\"md_icon\":\"");
			returnStr.append(module.getMdIcon());
			returnStr.append("\",\"mdCode\":\"");
			returnStr.append(StringManagerUtils.replaceAll(module.getMdCode()));
			returnStr.append("\",\"viewsrc\":\"");
			returnStr.append(StringManagerUtils.replaceAll(module.getMdUrl()));
			returnStr.append("\",\"controlsrc\":\"");
			returnStr.append(module.getMdControl());
			returnStr.append("\",\"closable\":true");
			returnStr.append(",\"iconCls\":\"" + module.getMdIcon());
			returnStr.append("\",\"leaf\":true},");
		}
		data = returnStr.toString();
		return data;
	}

	public static String judgeModuleType(int key) {
		// int temp = StringManagerUtils.stringTransferInteger(key);
		String result = "前台模块";
		switch (key) {
		case 0:
			result = "前台模块";
			break;
		case 1:
			result = "后台模块";
			break;

		default:
			result = "后台模块";
			break;
		}
		return result;

	}

	public String recursionModuleTreeFn(List list, Module module) {

		String data = "";
		if (hasChild(list, module)) {
			returnStr.append("{\"text\":\"" + module.getMdName() + "\"");
			returnStr.append(",\"mdShowname\":\"" + module.getMdShowname() + "\"");
			returnStr.append(",\"mdUrl\":\"" + StringManagerUtils.replaceAll(module.getMdUrl()) + "\"");
			returnStr.append(",\"mdParentid\":\"" + module.getMdParentid() + "\"");
			returnStr.append(",\"mdControl\":\"" + module.getMdControl() + "\"");
			returnStr.append(",\"mdIcon\":\"" + module.getMdIcon() + "\"");
			returnStr.append(",\"mdCode\":\"" + module.getMdCode() + "\"");
			returnStr.append(",\"mdType\":\"" + module.getMdType() + "\"");
			returnStr.append(",\"mdSeq\":\"" + module.getMdSeq() + "\"");
			returnStr.append(",\"mdId\":\"" + module.getMdId() + "\"");
			returnStr.append(",\"expanded\":false");
			returnStr.append(",\"children\":[");
			List childList = getChildList(list, module);
			Iterator it = childList.iterator();
			while (it.hasNext()) {
				Module n = (Module) it.next();
				recursionModuleTreeFn(list, n);
			}
			returnStr.append("]},");
		} else {
			returnStr.append("{\"mdId\":\"");
			returnStr.append(module.getMdId());
			returnStr.append("\",\"text\":\"");
			returnStr.append(module.getMdName());
			returnStr.append("\",\"mdShowname\":\"");
			returnStr.append(module.getMdShowname());
			returnStr.append("\",\"mdParentid\":\"");
			returnStr.append(module.getMdParentid());
			returnStr.append("\",\"mdIcon\":\"");
			returnStr.append(module.getMdIcon());
			returnStr.append("\",\"mdUrl\":\"");
			returnStr.append(module.getMdUrl());
			returnStr.append("\",\"mdControl\":\"");
			returnStr.append(module.getMdControl());
			returnStr.append("\",\"mdCode\":\"");
			returnStr.append(StringManagerUtils.replaceAll(module.getMdCode()));
			returnStr.append("\",\"mdType\":\"");
			returnStr.append(module.getMdType());
			returnStr.append("\",\"mdSeq\":\"");
			returnStr.append(module.getMdSeq());
			returnStr.append("\",\"leaf\":true},");
		}
		data = returnStr.toString();
		return data;
	}

	public boolean hasChild(List list, Module module) { // 判断是否有子节点
		return getChildList(list, module).size() > 0 ? true : false;
	}

	public List getChildList(List list, Module module) { // 得到子节点列表
		List li = new ArrayList();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Module n = (Module) it.next();
			if (n.getMdParentid().equals(module.getMdId())) {
				li.add(n);
			}
		}
		return li;
	}

	public boolean hasParent(List list, Module node) { // 判断是否有父节点
		return getParentList(list, node).size() > 0 ? true : false;
	}

	public List getParentList(List list, Module node) { // 得到子节点列表
		List li = new ArrayList();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Module n = (Module) it.next();
			if (n.getMdId().equals(node.getMdParentid())) {
				li.add(n);
			}
		}
		return li;
	}

	public String modifyStr(String returnStr) {// 修饰一下才能满足Extjs的Json格式
		return ("[" + returnStr + "]").replaceAll(",]", "]");

	}
	
	public boolean isModParentNode(String[] strArr, int tid) { // 判断是否是当前用户的父节点
		boolean result=false;
		if(strArr.length>0){
			for(int i=0;i<strArr.length;i++){
				if(!"".equals(strArr[i])&&tid==Integer.parseInt(strArr[i])){
					result=true;
					break;
				}
			}
		}
		
		return result;
	}

	public static void main(String[] args) {
		//MainModuleRecursion r = new MainModuleRecursion();
		// r.recursionFn(r.moduleList, new module(1, 0));
		// System.out.println(r.modifyStr(r.returnStr.toString()));
	}
}