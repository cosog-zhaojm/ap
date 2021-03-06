package com.cosog.controller.mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cosog.controller.base.BaseController;
import com.cosog.model.Org;
import com.cosog.model.User;
import com.cosog.service.base.CommonDataService;
import com.cosog.service.mobile.MobileService;
import com.cosog.service.right.UserManagerService;
import com.cosog.utils.Config;
import com.cosog.utils.DataModelMap;
import com.cosog.utils.Page;
import com.cosog.utils.ParamUtils;
import com.cosog.utils.Recursion;
import com.cosog.utils.StringManagerUtils;
import com.cosog.utils.UnixPwdCrypt;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;;

/**
 * <P> 描述：移动端接口</p>
 * 
 * @author zhao 2018-12-28
 * 
 */
@Controller
@RequestMapping("/mobileController")
@Scope("prototype")
public class MobileController extends BaseController{

	private static final long serialVersionUID = 1L;
	@Autowired
	private CommonDataService service;
	@Autowired
	private MobileService<?> mobileService;
	@Autowired
	private UserManagerService<User> userManagerService;
	
	@RequestMapping("/userLogin")
	public String userLogin() throws Exception {
		String account = URLDecoder.decode(ParamUtils.getParameter(request, "account"), "UTF-8");
		String password = URLDecoder.decode(ParamUtils.getParameter(request, "password"), "UTF-8");
		HttpSession session=request.getSession();
		String result="";
		if (!StringManagerUtils.isNotNull(account)) {
			result="{\"success\":false,\"msg\":\"用户名不能为空\"}";
		} else if (!StringManagerUtils.isNotNull(password)) {
			result="{\"success\":false,\"msg\":\"用户密码不能为空\"}";
		} else {
			User user = this.userManagerService.doLogin(account, UnixPwdCrypt.crypt("dogVSgod", password));
			if (user != null) {
				result="{\"success\":true,\"msg\":\"登录成功\"}";
			} else {
				result="{\"success\":false,\"msg\":\"账号或密码错误\"}";
			}
		}
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw;
		try {
			pw = response.getWriter();
			pw.print(result);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping("/getOrganizationData")
	public String getOrganizationData() throws Exception {
		StringBuffer orgIdString = new StringBuffer();
		String userAccount = URLDecoder.decode(ParamUtils.getParameter(request, "account"), "UTF-8");
		List<Org> list = (List<Org>) mobileService.getOrganizationData(Org.class, userAccount);
		
		String json = "";
		StringBuffer strBuf = new StringBuffer();
		Recursion r = new Recursion();// 递归类，将org集合构建成一棵树形菜单的json
		for (Org org : list) {
			if (!r.hasParent(list, org)) {
				json = r.recursionMObileOrgTree(list, org);
			}
		}
		json = json.replaceAll(",]", "]");
		if(json.lastIndexOf(",")==json.length()-1){
			json=json.substring(0, json.length()-1);
		}
		strBuf.append(json);
		json = strBuf.toString();
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw = response.getWriter();
		pw.print(json);
		pw.flush();
		pw.close();

		return null;
	}
	
	/******
	 * 统计饼图及柱状图需要的data信息
	 * ***/
	@RequestMapping("/oilWell/realtime/statisticsData")
	public String getPumpingRealtimeStatisticsData() throws Exception {
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\":1,\"StatType\":1,\"WellList\":[\"长庆现场智能油田测试井\",\"北京室内智能油田测试井\"]}";
		String json = mobileService.getPumpingRealtimeStatisticsDataByWellList(data);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw = response.getWriter();
		pw.print(json);
		pw.flush();
		pw.close();
		return null;
	}
	
	/******
	 * 查询处于某种统计值下的实时井列表及数据
	 * ***/
	@RequestMapping("/oilWell/realtime/wellListData")
	public String getOilWellRealtimeWellListData() throws Exception {
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\":1,\"StatType\":1,\"StatValue\":\"正常\",\"WellList\":[\"长庆现场智能油田测试井\",\"北京室内智能油田测试井\"]}";
		this.pager = new Page("pagerForm", request);
		String json = mobileService.getOilWellRealtimeWellListData(data,pager);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw;
		try {
			pw = response.getWriter();
			pw.print(json);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/******
	 * 查询处于某种统计值下的井历史数据
	 * ***/
	@RequestMapping("/oilWell/realtime/wellHistoryData")
	public String getOilWellRealtimeWellHistoryData() throws Exception {
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\":1,\"StatType\":1,\"StatValue\":\"正常\",\"StartDate\":\"2021-03-09\",\"EndDate\":\"2021-03-09\",\"WellName\":\"长庆现场智能油田测试井\"}";
		this.pager = new Page("pagerForm", request);
		String json = mobileService.getOilWellRealtimeWellHistoryData(data,pager);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw;
		try {
			pw = response.getWriter();
			pw.print(json);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@RequestMapping("/oilWell/realtime/wellAnalysisData")
	public String getOilWellRealtimeWellAnalysisData()throws Exception{
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\":1,\"WellName\":\"长庆现场智能油田测试井\",\"AcqTime\":\"2021-03-09 14:08:45\"}";
		String json = this.mobileService.getOilWellRealtimeWellAnalysisData(data);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw = response.getWriter();
		pw.print(json);
		pw.flush();
		pw.close();
		return null;
	}
	
	/******
	 * 全天统计饼图及柱状图需要的data信息
	 * ***/
	@RequestMapping("/oilWell/total/statisticsData")
	public String getOilWellTotalStatisticsData() throws Exception {
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\":1,\"Date\":\"2021-03-09\",\"StatType\":1,\"WellList\":[\"长庆现场智能油田测试井\",\"北京室内智能油田测试井\"]}";
		String json = mobileService.getOilWellTotalStatisticsData(data);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw = response.getWriter();
		pw.print(json);
		pw.flush();
		pw.close();
		return null;
	}
	
	/******
	 * 查询某天处于某种统计值下的全天井列表及数据
	 * ***/
	@RequestMapping("/oilWell/total/wellListData")
	public String getOilWellTotalWellListData() throws Exception {
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\":1,\"Date\":\"2021-03-09\",\"StatType\":1,\"StatValue\":\"正常\",\"WellList\":[\"长庆现场智能油田测试井\",\"北京室内智能油田测试井\"]}";
		this.pager = new Page("pagerForm", request);
		String json = mobileService.getOilWellTotalWellListData(data,pager);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw;
		try {
			pw = response.getWriter();
			pw.print(json);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/******
	 * 查询处于某种统计值下的井全天历史数据
	 * ***/
	@RequestMapping("/oilWell/total/wellHistoryData")
	public String getOilWellTotalHistoryData() throws Exception {
		ServletInputStream ss = request.getInputStream();
		String data=StringManagerUtils.convertStreamToString(ss,"utf-8");
//		data="{}";
//		data="{\"LiftingType\": 1,\"WellName\":\"长庆现场智能油田测试井\",\"StartDate\": \"2021-03-01\",\"EndDate\": \"2021-03-09\",\"StatType\": 1,\"StatValue\": \"正常\",\"WellList\": [\"长庆现场智能油田测试井\",\"北京室内智能油田测试井\"]}";
		data="{\"LiftingType\": 1,\"StartDate\": \"2021-01-27\",\"EndDate\": \"2021-04-27\",\"StatType\": 1}";
		this.pager = new Page("pagerForm", request);
		String json = mobileService.getOilWellTotalHistoryData(data,pager);
		response.setContentType("application/json;charset=utf-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw;
		try {
			pw = response.getWriter();
			pw.print(json);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
