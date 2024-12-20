package com.cosog.service.mobile;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cosog.dao.BaseDao;
import com.cosog.model.AlarmShowStyle;
import com.cosog.model.Org;
import com.cosog.model.User;
import com.cosog.model.WorkType;
import com.cosog.model.calculate.AcqInstanceOwnItem;
import com.cosog.model.calculate.AlarmInstanceOwnItem;
import com.cosog.model.calculate.DisplayInstanceOwnItem;
import com.cosog.model.calculate.PCPCalculateRequestData;
import com.cosog.model.calculate.PCPDeviceInfo;
import com.cosog.model.calculate.PCPProductionData;
import com.cosog.model.calculate.PumpingPRTFData;
import com.cosog.model.calculate.SRPCalculateRequestData;
import com.cosog.model.calculate.SRPDeviceInfo;
import com.cosog.model.calculate.SRPProductionData;
import com.cosog.model.calculate.UserInfo;
import com.cosog.model.data.DataDictionary;
import com.cosog.model.drive.ModbusProtocolConfig;
import com.cosog.service.base.BaseService;
import com.cosog.service.base.CommonDataService;
import com.cosog.service.data.DataitemsInfoService;
import com.cosog.service.right.UserManagerService;
import com.cosog.task.EquipmentDriverServerTask;
import com.cosog.task.MemoryDataManagerTask;
import com.cosog.utils.AcquisitionItemColumnsMap;
import com.cosog.utils.Config;
import com.cosog.utils.ConfigFile;
import com.cosog.utils.Page;
import com.cosog.utils.PageHandler;
import com.cosog.utils.RedisUtil;
import com.cosog.utils.SerializeObjectUnils;
import com.cosog.utils.StringManagerUtils;
import com.cosog.utils.excel.ExcelUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import oracle.sql.CLOB;
import redis.clients.jedis.Jedis;

import org.apache.commons.lang.StringUtils;
import org.hibernate.engine.jdbc.SerializableClobProxy;


@SuppressWarnings("deprecation")
@Component("mobileService")
public class MobileService<T> extends BaseService<T> {
	@SuppressWarnings("unused")
	private BaseDao dao;
	@SuppressWarnings("unused")
	@Autowired
	private CommonDataService service;
	@Autowired
	private DataitemsInfoService dataitemsInfoService;
	@Autowired
	private UserManagerService<User> userManagerService;
	
	public List<T> getOrganizationData(Class<Org> class1, String userAccount) {
		String queryString="";
		if(StringManagerUtils.isNotNull(userAccount)){
			String sql="select t.user_orgid from tbl_user t where t.user_id='"+userAccount+"'";
			List<?> list = this.findCallSql(sql);
			String orgId="";
			if(list.size()>0){
				orgId=list.get(0).toString();
			}
			if("0".equals(orgId)){
				queryString = "SELECT {Org.*} FROM tbl_org Org   "
						+ " order by Org.org_code  ";
			}else{
				queryString = "SELECT {Org.*} FROM tbl_org Org   "
						+ " start with Org.org_id=( select t2.user_orgid from tbl_user t2 where t2.user_id='"+userAccount+"' )   "
						+ " connect by Org.org_parent= prior Org.org_id "
						+ " order by Org.org_code  ";
			}
		}else{
			queryString = "SELECT {Org.*} FROM tbl_org Org   "
					+ " order by Org.org_code  ";
		}
		
		return getBaseDao().getSqlToHqlOrgObjects(queryString);
	}
	
	public String getPumpingRealtimeStatisticsDataByWellList(String data){
		StringBuffer wells= new StringBuffer();
		String json="";
		int liftingType=1;
		int type=1;
		String user = "";
		String password = "";
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					liftingType=jsonObject.getInt("LiftingType");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					type=jsonObject.getInt("StatType");
				}catch(Exception e){
					e.printStackTrace();
				}

				try{
					JSONArray jsonArray = jsonObject.getJSONArray("WellList");
					for(int i=0;jsonArray!=null&&i<jsonArray.size();i++){
						wells.append(""+jsonArray.getString(i)+",");
					}
					if(wells.toString().endsWith(",")){
						wells.deleteCharAt(wells.length() - 1);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		try{
			if(liftingType==1 && type==1){
				json=this.getRealTimeMonitoringFESDiagramResultStatData(user,password,wells.toString());
			}else if(type==2){
				json=this.getRealTimeMonitoringCommStatusStatData(user,password,wells.toString(),liftingType);
			}else if(type==3){
				json=this.getRealTimeMonitoringRunStatusStatData(user,password,wells.toString(),liftingType);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return json;
	}
	
	public String getRealTimeMonitoringFESDiagramResultStatData(String user,String password,String wells) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",\"DataList\":[");
		List<?> list=null;
		if(userCheckSign==1){
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			String tableName="tbl_srpacqdata_latest";
			String deviceTableName="viw_srpdevice";
			String sql="select decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultname) as resultname,t2.resultcode,count(1) "
					+ " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on  t2.deviceId=t.id"
					+ " left outer join tbl_srp_worktype t3 on  t2.resultcode=t3.resultcode"
					+ " where t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent) ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+=" group by t3.resultname,t2.resultcode "
					+ " order by t2.resultcode";
			list = this.findCallSql(sql);
		}
		for(int i=0;list!=null&&i<list.size();i++){
			Object[] obj=(Object[]) list.get(i);
			result_json.append("{\"Item\":\""+obj[0]+"\",");
			result_json.append("\"Count\":"+obj[2]+"},");
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getRealTimeMonitoringCommStatusStatData(String user,String password,String wells,int deviceType) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int online=0,goOnline=0,offline=0;
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign);
		if(userCheckSign==1){
			UserInfo userInfo=MemoryDataManagerTask.getUserInfoByAccount(user);
			Map<String,String> languageResourceMap=MemoryDataManagerTask.getLanguageResource(userInfo!=null?userInfo.getLanguageName():"");
			
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			String tableName="tbl_srpacqdata_latest";
			String deviceTableName="viw_srpdevice";
			if(deviceType==2){
				tableName="tbl_pcpacqdata_latest";
				deviceTableName="viw_pcpdevice";
			}
			String sql="select t2.commstatus,count(1) from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on  t2.deviceId=t.id "
					+ " where t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent) ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+=" group by t2.commstatus";
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				if(StringManagerUtils.stringToInteger(obj[0]+"")==1){
					online=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==2){
					goOnline=StringManagerUtils.stringToInteger(obj[1]+"");
				}else{
					offline=StringManagerUtils.stringToInteger(obj[1]+"");
				}
			}
			result_json.append(",\"DataList\":[");
			result_json.append("{\"Item\":'"+languageResourceMap.get("online")+"',");
			result_json.append("\"Count\":"+online+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("goOnline")+"',");
			result_json.append("\"Count\":"+goOnline+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("offline")+"',");
			result_json.append("\"Count\":"+offline+"}]");
		}
		
		result_json.append("}");
	
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getRealTimeMonitoringRunStatusStatData(String user,String password,String wells,int deviceType) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int run=0,stop=0,noData=0,offline=0,goOnline=0;
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign);
		if(userCheckSign==1){
			UserInfo userInfo=MemoryDataManagerTask.getUserInfoByAccount(user);
			Map<String,String> languageResourceMap=MemoryDataManagerTask.getLanguageResource(userInfo!=null?userInfo.getLanguageName():"");
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			String tableName="tbl_srpacqdata_latest";
			String deviceTableName="viw_srpdevice";
			if(deviceType==2){
				tableName="tbl_pcpacqdata_latest";
				deviceTableName="viw_pcpdevice";
			}
			String sql="select decode(t2.commstatus,0,-1,2,-2,decode(t2.runstatus,null,2,t2.runstatus)) as runstatus,count(1) from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on  t2.deviceId=t.id "
					+ " where 1=1 ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+=" group by t2.commstatus,t2.runstatus";
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				if(StringManagerUtils.stringToInteger(obj[0]+"")==1){
					run=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==0){
					stop=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==-1){
					offline=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==-2){
					goOnline=StringManagerUtils.stringToInteger(obj[1]+"");
				}else{
					noData=StringManagerUtils.stringToInteger(obj[1]+"");
				}
			}
			result_json.append(",\"DataList\":[");
			result_json.append("{\"Item\":'"+languageResourceMap.get("run")+"',\"Count\":"+run+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("stop")+"',\"Count\":"+stop+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("emptyMsg")+"',\"Count\":"+noData+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("goOnline")+"',\"Count\":"+goOnline+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("offline")+"',\"Count\":"+offline+"}]");
		}
		
		result_json.append("}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getOilWellRealtimeWellListData(String data,Page pager)throws Exception {
		String json = "";
		StringBuffer wells= new StringBuffer();
		int liftingType=1;
		int statType=1;
		String statValue="";
		String user="";
		String password="";
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					liftingType=jsonObject.getInt("LiftingType");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					statType=jsonObject.getInt("StatType");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					statValue=jsonObject.getString("StatValue");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					JSONArray jsonArray = jsonObject.getJSONArray("WellList");
					for(int i=0;jsonArray!=null&&i<jsonArray.size();i++){
						wells.append(""+jsonArray.getString(i)+",");
					}
					if(wells.toString().endsWith(",")){
						wells.deleteCharAt(wells.length() - 1);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(liftingType==1){
			json=this.getDeviceRealTimeOverview(user,password,wells.toString(), statType, statValue);
		}else{
			json=this.getPCPDeviceRealTimeOverview(user,password,wells.toString(), statType, statValue);
		}
		return json;
	}
	
	public String getDeviceRealTimeOverview(String user,String password,String wells,int statType,String statValue) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("DeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				if(!jedis.exists("AlarmShowStyle".getBytes())){
					MemoryDataManagerTask.initAlarmStyle();
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String tableName="tbl_acqdata_latest";
			String deviceTableName="tbl_device";
			
			
			String sql="select t.id,t.deviceName,"
					+ "c1.name as devicetypename,"
					+ "to_char(t2.fesdiagramAcqTime,'yyyy-mm-dd hh24:mi:ss') as acqtime,"
					+ "t2.commstatus,decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "decode(t2.runstatus,null,2,t2.runstatus),decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "t2.resultcode,decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName) as resultName,t3.optimizationSuggestion as optimizationSuggestion,"
					+ "liquidWeightProduction,oilWeightProduction,waterWeightProduction,liquidWeightProduction_L,"
					+ "liquidVolumetricProduction,oilVolumetricProduction,waterVolumetricProduction,liquidVolumetricProduction_L,"
					+ "t2.surfaceSystemEfficiency*100 as surfaceSystemEfficiency,"
					+ "t2.welldownSystemEfficiency*100 as welldownSystemEfficiency,"
					+ "t2.systemEfficiency*100 as systemEfficiency,t2.energyper100mlift,"
					+ "t2.pumpEff*100 as pumpEff,"
					+ "t2.UpStrokeIMax,t2.DownStrokeIMax,t2.iDegreeBalance,"
					+ "t2.UpStrokeWattMax,t2.DownStrokeWattMax,t2.wattDegreeBalance,"
					+ "t2.deltaradius*100 as deltaradius,"
					+ "t2.todayKWattH,"
					+ "t2.productiondata";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " left outer join tbl_srp_worktype t3 on t2.resultcode=t3.resultcode "
					+ " left outer join tbl_devicetypeinfo c1 on t.devicetype=c1.id "
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			if(statType==1 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName)='"+statValue+"'";
			}else if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+=" order by t.sortnum,t.deviceName";
			
			
			int totals=this.getTotalCountRows(sql);
			List<?> list = this.findCallSql(sql);
			
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[0]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[10]+"";
				String resultcode=obj[14]+"";
				String weightWaterCut="";
				String volumeWaterCut="";
				String productionData=obj[obj.length-1].toString();
				type = new TypeToken<SRPCalculateRequestData>() {}.getType();
				SRPCalculateRequestData srpProductionData=gson.fromJson(productionData, type);
				if(srpProductionData!=null&&srpProductionData.getProduction()!=null){
					weightWaterCut=srpProductionData.getProduction().getWeightWaterCut()+"";
					volumeWaterCut=srpProductionData.getProduction().getWaterCut()+"";
				}
				
				SRPDeviceInfo srpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("DeviceInfo".getBytes(), deviceId.getBytes())){
					srpDeviceInfo=(SRPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("DeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&srpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				int commAlarmLevel=0,resultAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==4 && alarmInstanceOwnItem.getItemList().get(j).getItemCode().equalsIgnoreCase(resultcode)){
							resultAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+deviceId+",");
				result_json.append("\"deviceName\":\""+obj[1]+"\",");
				result_json.append("\"DeviceTypeName\":\""+obj[2]+"\",");
				result_json.append("\"AcqTime\":\""+obj[3]+"\",");
				result_json.append("\"CommStatus\":"+obj[4]+",");
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				
				result_json.append("\"RunStatus\":"+obj[9]+",");
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[11]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[12]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[13])+"\",");
				result_json.append("\"ResultCode\":\""+obj[14]+"\",");
				result_json.append("\"ResultName\":\""+obj[15]+"\",");
				result_json.append("\"OptimizationSuggestion\":\""+obj[16]+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[17]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[18]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[19]+"\",");
				result_json.append("\"WeightWaterCut\":\""+weightWaterCut+"\",");
				result_json.append("\"LiquidWeightProduction_L\":\""+obj[20]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[21]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[22]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[23]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+volumeWaterCut+"\",");
				result_json.append("\"LiquidVolumetricProduction_L\":\""+obj[24]+"\",");
				
				result_json.append("\"SurfaceSystemEfficiency\":\""+obj[25]+"\",");
				result_json.append("\"WelldownSystemEfficiency\":\""+obj[26]+"\",");
				result_json.append("\"SystemEfficiency\":\""+obj[27]+"\",");
				result_json.append("\"Energyper100mlift\":\""+obj[28]+"\",");
				result_json.append("\"PumpEff\":\""+obj[29]+"\",");
				
				result_json.append("\"UpStrokeIMax\":\""+obj[30]+"\",");
				result_json.append("\"DownStrokeIMax\":\""+obj[31]+"\",");
				result_json.append("\"IDegreeBalance\":\""+obj[32]+"\",");
				
				result_json.append("\"UpStrokeWattMax\":\""+obj[33]+"\",");
				result_json.append("\"DownStrokeWattMax\":\""+obj[34]+"\",");
				result_json.append("\"WattDegreeBalance\":\""+obj[35]+"\",");
				
				result_json.append("\"Deltaradius\":\""+obj[36]+"\",");
				
				result_json.append("\"TodayKWattH\":\""+obj[37]+"\",");
				

				result_json.append("\"ResultAlarmLevel\":"+resultAlarmLevel+",");
				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(result_json.toString().endsWith(",")){
				result_json.deleteCharAt(result_json.length() - 1);
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getPCPDeviceRealTimeOverview(String user,String password,String wells,int statType,String statValue) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("PCPDeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String tableName="tbl_pcpacqdata_latest";
			String deviceTableName="tbl_device";
			
			String sql="select t.id,t.deviceName,"
					+ "c1.name as devicetypename,"
					+ "to_char(t2.acqtime,'yyyy-mm-dd hh24:mi:ss') as acqtime,"
					+ "t2.commstatus,decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "t2.runstatus,decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "t2.liquidWeightProduction,t2.oilWeightProduction,t2.waterWeightProduction,t2.liquidWeightProduction_L,"
					+ "t2.liquidVolumetricProduction,t2.oilVolumetricProduction,t2.waterVolumetricProduction,t2.liquidVolumetricProduction_L,"
					+ "t2.systemEfficiency*100 as systemEfficiency,t2.energyper100mlift,t2.pumpEff*100 as pumpEff,"
					+ "t2.todayKWattH,"
					+ "t2.productiondata";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " left outer join tbl_devicetypeinfo c1 on t.devicetype=c1.id "
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+=" order by t.sortnum,t.deviceName";
			
			int totals=this.getTotalCountRows(sql);
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[0]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[10]+"";
				
				String weightWaterCut="";
				String volumeWaterCut="";
				String productionData=obj[obj.length-1].toString();
				type = new TypeToken<PCPCalculateRequestData>() {}.getType();
				PCPCalculateRequestData pcpProductionData=gson.fromJson(productionData, type);
				if(pcpProductionData!=null&&pcpProductionData.getProduction()!=null){
					weightWaterCut=pcpProductionData.getProduction().getWeightWaterCut()+"";
					volumeWaterCut=pcpProductionData.getProduction().getWaterCut()+"";
				}
				
				PCPDeviceInfo pcpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("PCPDeviceInfo".getBytes(), deviceId.getBytes())){
					pcpDeviceInfo=(PCPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("PCPDeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&pcpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				
				int commAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+deviceId+",");
				result_json.append("\"deviceName\":\""+obj[1]+"\",");
				
				result_json.append("\"DeviceTypeName\":\""+obj[2]+"\",");
				result_json.append("\"AcqTime\":\""+obj[3]+"\",");
				result_json.append("\"CommStatus\":"+obj[4]+",");
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[5]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				
				result_json.append("\"RunStatus\":"+obj[9]+",");
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[11]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[12]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[13])+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[14]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[15]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[16]+"\",");
				result_json.append("\"WeightWaterCut\":\""+weightWaterCut+"\",");
				result_json.append("\"LiquidWeightProduction_L\":\""+obj[17]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[18]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[19]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[20]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+volumeWaterCut+"\",");
				result_json.append("\"LiquidVolumetricProduction_L\":\""+obj[21]+"\",");
				
				result_json.append("\"SystemEfficiency\":\""+obj[22]+"\",");
				result_json.append("\"Energyper100mlift\":\""+obj[23]+"\",");
				result_json.append("\"PumpEff\":\""+obj[24]+"\",");
				
				result_json.append("\"TodayKWattH\":\""+obj[25]+"\",");
				
				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(result_json.toString().endsWith(",")){
				result_json.deleteCharAt(result_json.length() - 1);
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getOilWellHistoryData(String data,Page pager)throws Exception {
		String json = "";
		int liftingType=1;
		int statType=1;
		String statValue="";
		String deviceName="";
		String startDate=StringManagerUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
		String endDate=startDate;
		String user="";
		String password="";
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					liftingType=jsonObject.getInt("LiftingType");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					statType=jsonObject.getInt("StatType");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					statValue=jsonObject.getString("StatValue");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					deviceName=jsonObject.getString("deviceName");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					startDate=jsonObject.getString("StartDate");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					endDate=jsonObject.getString("EndDate");
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(liftingType==1){
			json=this.getDeviceHistoryData(user,password,deviceName, statType, statValue, startDate, endDate);
		}else{
			json=this.getPCPDeviceHistoryData(user,password,deviceName, statType, statValue, startDate, endDate);
		}
		return json;
	}
	
	public String getDeviceHistoryData(String user,String password,String deviceName,int statType,String statValue,String startDate,String endDate) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			Gson gson = new Gson();
			java.lang.reflect.Type type=null;
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("DeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				if(!jedis.exists("AlarmShowStyle".getBytes())){
					MemoryDataManagerTask.initAlarmStyle();
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String hisTableName="tbl_srpacqdata_hist";
			String deviceTableName="tbl_device";
			String sql="select t2.id,t.id as deviceId,t.deviceName,"
					+ "to_char(t2.fesdiagramAcqTime,'yyyy-mm-dd hh24:mi:ss') as acqtime,"
					+ "t2.commstatus,decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "t2.runstatus,decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "t2.resultcode,decode(t2.commstatus,1,decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName),'' ) as resultName,t3.optimizationSuggestion as optimizationSuggestion,"
					+ "liquidWeightProduction,oilWeightProduction,waterWeightProduction,liquidWeightProduction_L,"
					+ "liquidVolumetricProduction,oilVolumetricProduction,waterVolumetricProduction,liquidVolumetricProduction_L,"
					+ "t2.surfaceSystemEfficiency*100 as surfaceSystemEfficiency,"
					+ "t2.welldownSystemEfficiency*100 as welldownSystemEfficiency,"
					+ "t2.systemEfficiency*100 as systemEfficiency,"
					+ "t2.energyper100mlift,t2.pumpEff*100 as pumpEff,"
					+ "t2.UpStrokeIMax,t2.DownStrokeIMax,t2.iDegreeBalance,"
					+ "t2.UpStrokeWattMax,t2.DownStrokeWattMax,t2.wattDegreeBalance,"
					+ "t2.deltaradius*100 as deltaradius,"
					+ "t2.todayKWattH,"
					+ "t2.productiondata";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+hisTableName+" t2 on t2.deviceId=t.id"
					+ " left outer join tbl_srp_worktype t3 on t2.resultcode=t3.resultcode "
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.fesdiagramAcqTime between to_date('"+startDate+"','yyyy-mm-dd hh24:mi:ss') and to_date('"+endDate+"','yyyy-mm-dd hh24:mi:ss') ";
			if(statType==1 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName)='"+statValue+"'";
			}else if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			
			
			if(StringManagerUtils.isNotNull(deviceName)){
				sql+= "and t.deviceName='"+deviceName+"'";
			}	
			sql+= "  order by t2.fesdiagramAcqTime desc";
			
			int totals=this.getTotalCountRows(sql);
			List<?> list = this.findCallSql(sql);
			
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[1]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[10]+"";
				String resultcode=obj[14]+"";
				
				String weightWaterCut="";
				String volumeWaterCut="";
				String productionData=obj[obj.length-1].toString();
				type = new TypeToken<SRPCalculateRequestData>() {}.getType();
				SRPCalculateRequestData srpProductionData=gson.fromJson(productionData, type);
				if(srpProductionData!=null&&srpProductionData.getProduction()!=null){
					weightWaterCut=srpProductionData.getProduction().getWeightWaterCut()+"";
					volumeWaterCut=srpProductionData.getProduction().getWaterCut()+"";
				}
				
				SRPDeviceInfo srpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("DeviceInfo".getBytes(), deviceId.getBytes())){
					srpDeviceInfo=(SRPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("DeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&srpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				int commAlarmLevel=0,resultAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==4 && alarmInstanceOwnItem.getItemList().get(j).getItemCode().equalsIgnoreCase(resultcode)){
							resultAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+obj[0]+",");
				result_json.append("\"DeviceId\":\""+deviceId+"\",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"AcqTime\":\""+obj[3]+"\",");
				result_json.append("\"CommStatus\":"+obj[4]+",");
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				result_json.append("\"RunStatus\":"+obj[9]+",");
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[11]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[12]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[13])+"\",");
				result_json.append("\"ResultCode\":\""+resultcode+"\",");
				result_json.append("\"ResultName\":\""+obj[15]+"\",");
				result_json.append("\"OptimizationSuggestion\":\""+obj[16]+"\",");
				result_json.append("\"LiquidWeightProduction\":\""+obj[17]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[18]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[19]+"\",");
				result_json.append("\"WeightWaterCut\":\""+weightWaterCut+"\",");
				result_json.append("\"LiquidWeightProduction_L\":\""+obj[20]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[21]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[22]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[23]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+volumeWaterCut+"\",");
				result_json.append("\"LiquidVolumetricProduction_L\":\""+obj[24]+"\",");
				
				result_json.append("\"SurfaceSystemEfficiency\":\""+obj[25]+"\",");
				result_json.append("\"WelldownSystemEfficiency\":\""+obj[26]+"\",");
				result_json.append("\"SystemEfficiency\":\""+obj[27]+"\",");
				result_json.append("\"Energyper100mlift\":\""+obj[28]+"\",");
				result_json.append("\"PumpEff\":\""+obj[29]+"\",");
				
				result_json.append("\"UpStrokeIMax\":\""+obj[30]+"\",");
				result_json.append("\"DownStrokeIMax\":\""+obj[31]+"\",");
				result_json.append("\"IDegreeBalance\":\""+obj[32]+"\",");
				
				result_json.append("\"UpStrokeWattMax\":\""+obj[33]+"\",");
				result_json.append("\"DownStrokeWattMax\":\""+obj[34]+"\",");
				result_json.append("\"WattDegreeBalance\":\""+obj[35]+"\",");
				result_json.append("\"Deltaradius\":\""+obj[36]+"\",");
				result_json.append("\"TodayKWattH\":\""+obj[37]+"\",");
				
				result_json.append("\"ResultAlarmLevel\":"+resultAlarmLevel+",");
				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(result_json.toString().endsWith(",")){
				result_json.deleteCharAt(result_json.length() - 1);
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getPCPDeviceHistoryData(String user,String password,String deviceName,int statType,String statValue,String startDate,String endDate) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			Gson gson = new Gson();
			java.lang.reflect.Type type=null;
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("PCPDeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String hisTableName="tbl_pcpacqdata_hist";
			String deviceTableName="tbl_device";
			
			String sql="select t2.id,t.id as deviceId,t.deviceName,"
					+ "to_char(t2.acqtime,'yyyy-mm-dd hh24:mi:ss') as acqtime,"
					+ "t2.commstatus,decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "t2.runstatus,decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "liquidWeightProduction,oilWeightProduction,waterWeightProduction,liquidWeightProduction_L,"
					+ "liquidVolumetricProduction,oilVolumetricProduction,waterVolumetricProduction,liquidVolumetricProduction_L,"
					+ "systemEfficiency*100 as systemEfficiency,energyper100mlift,pumpEff*100 as pumpEff,"
					+ "todayKWattH,"
					+ "t2.productiondata";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+hisTableName+" t2 on t2.deviceId=t.id"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.acqTime between to_date('"+startDate+"','yyyy-mm-dd hh24:mi:ss') and to_date('"+endDate+"','yyyy-mm-dd hh24:mi:ss') ";
			if(StringManagerUtils.isNotNull(deviceName)){
				sql+= "and t.deviceName='"+deviceName+"'";
			}	
			if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+= "  order by t2.acqtime desc";
			
			
			int totals=this.getTotalCountRows(sql);
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[1]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[10]+"";
				
				String weightWaterCut="";
				String volumeWaterCut="";
				String productionData=obj[obj.length-1].toString();
				type = new TypeToken<PCPCalculateRequestData>() {}.getType();
				PCPCalculateRequestData pcpProductionData=gson.fromJson(productionData, type);
				if(pcpProductionData!=null&&pcpProductionData.getProduction()!=null){
					weightWaterCut=pcpProductionData.getProduction().getWeightWaterCut()+"";
					volumeWaterCut=pcpProductionData.getProduction().getWaterCut()+"";
				}
				
				PCPDeviceInfo pcpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("PCPDeviceInfo".getBytes(), deviceId.getBytes())){
					pcpDeviceInfo=(PCPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("PCPDeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&pcpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				int commAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+obj[0]+",");
				result_json.append("\"DeviceId\":\""+deviceId+"\",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"AcqTime\":\""+obj[3]+"\",");
				result_json.append("\"CommStatus\":"+obj[4]+",");
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				result_json.append("\"RunStatus\":"+obj[9]+",");
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[11]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[12]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[13])+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[14]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[15]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[16]+"\",");
				result_json.append("\"WeightWaterCut\":\""+weightWaterCut+"\",");
				result_json.append("\"LiquidWeightProduction_L\":\""+obj[17]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[18]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[19]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[20]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+volumeWaterCut+"\",");
				result_json.append("\"LiquidVolumetricProduction_L\":\""+obj[21]+"\",");
				
				result_json.append("\"SystemEfficiency\":\""+obj[22]+"\",");
				result_json.append("\"Energyper100mlift\":\""+obj[23]+"\",");
				result_json.append("\"PumpEff\":\""+obj[24]+"\",");
				result_json.append("\"TodayKWattH\":\""+obj[25]+"\",");
				
				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(result_json.toString().endsWith(",")){
				result_json.deleteCharAt(result_json.length() - 1);
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getOilWellAnalysisData(String data) throws SQLException, IOException{
		String json="";
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				if(jsonObject!=null){
					int liftingType=1;
					try{
						liftingType=jsonObject.getInt("LiftingType");
					}catch(Exception e){
						e.printStackTrace();
					}
					if(liftingType==1){
						json=getPumpunitRealtimeWellAnalysisData(data);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return json;
	}
	
	public String singleFESDiagramData(String data) throws SQLException, IOException {
		StringBuffer result_json = new StringBuffer();
		ConfigFile configFile=Config.getInstance().configFile;
		String prodCol=" t.liquidWeightProduction";
		if(configFile.getAp().getOthers().getProductionUnit().equalsIgnoreCase("stere")){
			prodCol=" t.liquidVolumetricProduction";
		}
		String user="";
		String password="";
		String deviceName="";
		String acqTime="";
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					deviceName=jsonObject.getString("deviceName");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					acqTime=jsonObject.getString("AcqTime");
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			String sql="";
			String hisTableName="tbl_srpacqdata_hist";
			String deviceTableName="tbl_device";
			
			sql="select t.id, t2.deviceName, to_char(t.fesdiagramAcqTime,'yyyy-mm-dd hh24:mi:ss') as acqTime, "
					+ " t.position_curve,t.load_curve,t.power_curve,t.current_curve,"
					+ " t.upperLoadline, t.lowerloadline, t.fmax, t.fmin, t.stroke, t.SPM, "+prodCol+", "
					+ " t3.resultName,t3.optimizationSuggestion "
					+ " from "+hisTableName+" t, "+deviceTableName+" t2,tbl_srp_worktype t3"
					+ " where t.deviceId=t2.id and t.resultcode=t3.resultcode"
					+ " and t2.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.deviceName='" + deviceName + "' "
					+ " and t.fesdiagramAcqTime = to_date('"+ acqTime +"','yyyy-MM-dd hh24:mi:ss')";
			List<?> list=this.findCallSql(sql);
			
			
			for (int i = 0; i < list.size(); i++) {
				Object[] obj = (Object[]) list.get(i);
				CLOB realClob=null;
				SerializableClobProxy   proxy=null;
				String sStr="";
		        String fStr="";
		        String wattStr="";
		        String aStr="";
		        String pointCount="";
		        if(obj[3]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[3]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					sStr=StringManagerUtils.CLOBtoString(realClob);
					if(StringManagerUtils.isNotNull(sStr)){
						pointCount=sStr.split(",").length+"";
					}
				}
		        if(obj[4]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[4]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					fStr=StringManagerUtils.CLOBtoString(realClob);
				}
		        if(obj[5]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[5]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					wattStr=StringManagerUtils.CLOBtoString(realClob);
				}
		        if(obj[6]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[6]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					aStr=StringManagerUtils.CLOBtoString(realClob);
				}
		        
				result_json.append("{ \"Id\":\"" + obj[0] + "\",");
				result_json.append("\"deviceName\":\"" + obj[1] + "\",");
				result_json.append("\"AcqTime\":\"" + obj[2] + "\",");
				result_json.append("\"PointCount\":\""+pointCount+"\","); 
				result_json.append("\"UpperLoadLine\":\"" + obj[7] + "\",");
				result_json.append("\"LowerLoadLine\":\"" + obj[8] + "\",");
				result_json.append("\"Fmax\":\""+obj[9]+"\",");
				result_json.append("\"Fmin\":\""+obj[10]+"\",");
				result_json.append("\"Stroke\":\""+obj[11]+"\",");
				result_json.append("\"SPM\":\""+obj[12]+"\",");
				result_json.append("\"LiquidProduction\":\""+obj[13]+"\",");
				result_json.append("\"ResultName\":\""+obj[14]+"\",");
				result_json.append("\"OptimizationSuggestion\":\""+obj[15]+"\",");
				result_json.append("\"S\":["+sStr+"],"); 
				result_json.append("\"F\":["+fStr+"],"); 
				result_json.append("\"Watt\":["+wattStr+"],"); 
				result_json.append("\"A\":["+aStr+"]},");
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("null", "");
	}
	
	public String historyFESDiagramData(String data) throws SQLException, IOException {
		StringBuffer result_json = new StringBuffer();
		ConfigFile configFile=Config.getInstance().configFile;
		String prodCol=" t.liquidWeightProduction";
		if(configFile.getAp().getOthers().getProductionUnit().equalsIgnoreCase("stere")){
			prodCol=" t.liquidVolumetricProduction";
		}
		String user="";
		String password="";
		String deviceName="";
		String startDate=StringManagerUtils.getCurrentTime("yyyy-MM-dd HH:mm:ss");
		String endDate=startDate;
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					deviceName=jsonObject.getString("deviceName");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					startDate=jsonObject.getString("StartDate");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					endDate=jsonObject.getString("EndDate");
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			String sql="";
			String hisTableName="tbl_srpacqdata_hist";
			String deviceTableName="tbl_device";
			
			sql="select t.id, t2.deviceName, to_char(t.fesdiagramAcqTime,'yyyy-mm-dd hh24:mi:ss') as acqTime, "
					+ " t.position_curve,t.load_curve,t.power_curve,t.current_curve,"
					+ " t.upperLoadline, t.lowerloadline, t.fmax, t.fmin, t.stroke, t.SPM, "+prodCol+", "
					+ " t3.resultName,t3.optimizationSuggestion "
					+ " from "+hisTableName+" t, "+deviceTableName+" t2,tbl_srp_worktype t3"
					+ " where t.deviceId=t2.id and t.resultcode=t3.resultcode"
					+ " and t2.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  ";
			if(StringManagerUtils.isNotNull(deviceName)){
				sql+= " and t2.deviceName='" + deviceName + "' ";
			}
			sql+= " and t.fesdiagramAcqTime between to_date('"+ startDate +"','yyyy-MM-dd hh24:mi:ss') and to_date('"+ endDate +"','yyyy-MM-dd hh24:mi:ss')";
			sql+=" order by t.fesdiagramAcqTime desc";
			List<?> list=this.findCallSql(sql);
			
			for (int i = 0; i < list.size(); i++) {
				Object[] obj = (Object[]) list.get(i);
				CLOB realClob=null;
				SerializableClobProxy   proxy=null;
				String sStr="";
		        String fStr="";
		        String wattStr="";
		        String aStr="";
		        String pointCount="";
		        if(obj[3]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[3]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					sStr=StringManagerUtils.CLOBtoString(realClob);
					if(StringManagerUtils.isNotNull(sStr)){
						pointCount=sStr.split(",").length+"";
					}
				}
		        if(obj[4]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[4]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					fStr=StringManagerUtils.CLOBtoString(realClob);
				}
		        if(obj[5]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[5]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					wattStr=StringManagerUtils.CLOBtoString(realClob);
				}
		        if(obj[6]!=null){
					proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[6]);
					realClob = (CLOB) proxy.getWrappedClob(); 
					aStr=StringManagerUtils.CLOBtoString(realClob);
				}
		        
				result_json.append("{ \"Id\":\"" + obj[0] + "\",");
				result_json.append("\"deviceName\":\"" + obj[1] + "\",");
				result_json.append("\"AcqTime\":\"" + obj[2] + "\",");
				result_json.append("\"PointCount\":\""+pointCount+"\","); 
				result_json.append("\"UpperLoadLine\":\"" + obj[7] + "\",");
				result_json.append("\"LowerLoadLine\":\"" + obj[8] + "\",");
				result_json.append("\"Fmax\":\""+obj[9]+"\",");
				result_json.append("\"Fmin\":\""+obj[10]+"\",");
				result_json.append("\"Stroke\":\""+obj[11]+"\",");
				result_json.append("\"SPM\":\""+obj[12]+"\",");
				result_json.append("\"LiquidProduction\":\""+obj[13]+"\",");
				result_json.append("\"ResultName\":\""+obj[14]+"\",");
				result_json.append("\"OptimizationSuggestion\":\""+obj[15]+"\",");
				result_json.append("\"S\":["+sStr+"],"); 
				result_json.append("\"F\":["+fStr+"],"); 
				result_json.append("\"Watt\":["+wattStr+"],"); 
				result_json.append("\"A\":["+aStr+"]},");
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("null", "");
	}
	
	public String getPumpunitRealtimeWellAnalysisData(String data) throws SQLException, IOException{
		StringBuffer result_json = new StringBuffer();
		ConfigFile configFile=Config.getInstance().configFile;
		String deviceName="";
		String acqTime="";
		String user="";
		String password="";
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		try{
			JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
			String json="";
			if(jsonObject!=null){
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					deviceName=jsonObject.getString("deviceName");
				}catch(Exception e){
					e.printStackTrace();
				}
				try{
					acqTime=jsonObject.getString("AcqTime");
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		
		if(userCheckSign==1){
			if(StringManagerUtils.isNotNull(deviceName) && StringManagerUtils.isNotNull(acqTime)){
				String prodCol=" t.liquidWeightProduction,t.oilWeightProduction,t.waterWeightProduction,"
						+ " t.availablePlungerstrokeProd_W,t.pumpClearanceLeakProd_W,t.tvleakWeightProduction,t.svleakWeightProduction,t.gasInfluenceProd_W,";
				if(configFile.getAp().getOthers().getProductionUnit().equalsIgnoreCase("stere")){
					prodCol=" t.liquidVolumetricProduction,t.oilVolumetricProduction,t.waterVolumetricProduction,"
							+ " t.availablePlungerstrokeProd_V,t.pumpClearanceLeakProd_V,t.tvleakVolumetricProduction,t.svleakVolumetricProduction,t.gasInfluenceProd_V,";;
				}
				String hisTableName="tbl_srpacqdata_hist";
				String deviceTableName="tbl_device";
				String sql="select t3.resultName,t3.optimizationSuggestion,"//0~1
						+ " t.UpStrokeWattMax,t.DownStrokeWattMax,t.wattDegreeBalance,"//2~4
						+ " t.UpStrokeIMax,t.DownStrokeIMax,t.iDegreeBalance,"//5~7
						+ " t.deltaRadius*100,"//8
						+ prodCol//9~16
						+ " t.theoreticalProduction,"//17
						+ " t.fullnessCoefficient,t.plungerstroke,t.availableplungerstroke,"//18~20
						+ " t.levelDifferenceValue,t.calcProducingfluidLevel,"//21~22
						+ " t.submergence,"//23
						+ " t.stroke,t.spm,"//24~25
						+ " t.fmax,t.fmin,t.fmax-t.fmin as deltaF,"//26~28
						+ " t.upperloadline,t.lowerloadline,t.upperloadline-t.lowerloadline as deltaLoadLine,area,"//29~32
						+ " t.averageWatt,t.polishrodPower,t.waterPower,"//33~35
						+ " t.surfaceSystemEfficiency*100,t.welldownSystemEfficiency*100,t.systemEfficiency*100,t.energyPer100mLift,"//36~39
						+ " t.pumpEff1*100,t.pumpEff2*100,t.pumpEff3*100,t.pumpEff4*100,t.pumpEff*100,"//40~44
						+ " t.rodFlexLength,t.tubingFlexLength,t.inertiaLength,"//45~47
						+ " t.pumpintakep,t.pumpintaket,t.pumpintakegol,t.pumpintakevisl,t.pumpintakebo,"//48~52
						+ " t.pumpoutletp,t.pumpoutlett,t.pumpOutletGol,t.pumpoutletvisl,t.pumpoutletbo,"//53~57
						+ " t.productiondata"//58
						+ " from "+hisTableName+" t, "+deviceTableName+" t2,tbl_srp_worktype t3"
						+ " where t.deviceId=t2.id and t.resultcode=t3.resultcode"
						+ " and  t2.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
						+ " and  t2.deviceName='"+deviceName+"' and t.fesdiagramAcqTime=to_date('"+acqTime+"','yyyy-mm-dd hh24:mi:ss')"; 
				List<?> list = this.findCallSql(sql);
				if(list.size()>0){
					Object[] obj=(Object[]) list.get(0);
					
					String waterCut="";
					String pumpBoreDiameter="";
					String pumpSettingDepth="";
					String producingFluidLevel="";
					String productionData=obj[obj.length-1].toString();
					type = new TypeToken<SRPCalculateRequestData>() {}.getType();
					SRPCalculateRequestData srpProductionData=gson.fromJson(productionData, type);
					if(srpProductionData!=null && srpProductionData.getProduction()!=null){
						if(configFile.getAp().getOthers().getProductionUnit().equalsIgnoreCase("stere")){
							waterCut=srpProductionData.getProduction().getWaterCut()+"";
						}else{
							waterCut=srpProductionData.getProduction().getWeightWaterCut()+"";
						}
						pumpSettingDepth=srpProductionData.getProduction().getPumpSettingDepth()+"";
						producingFluidLevel=srpProductionData.getProduction().getProducingfluidLevel()+"";
					}
					if(srpProductionData!=null && srpProductionData.getPump()!=null){
						pumpBoreDiameter=srpProductionData.getPump().getPumpBoreDiameter()*1000+"";
					}
					
					result_json.append("\"ResultName\":\""+obj[0]+"\",");
					result_json.append("\"OptimizationSuggestion\":\""+obj[1]+"\",");
					
					result_json.append("\"UpStrokeWattMax\":\""+obj[2]+"\",");
					result_json.append("\"DownStrokeWattMax\":\""+obj[3]+"\",");
					result_json.append("\"WattDegreeBalance\":\""+obj[4]+"\",");
					
					result_json.append("\"UpStrokeIMax\":\""+obj[5]+"\",");
					result_json.append("\"DownStrokeIMax\":\""+obj[6]+"\",");
					result_json.append("\"IDegreeBalance\":\""+obj[7]+"\",");
					
					result_json.append("\"DeltaRadius\":\""+obj[8]+"\",");
					
					result_json.append("\"LiquidProduction\":\""+obj[9]+"\",");
					result_json.append("\"OilProduction\":\""+obj[10]+"\",");
					result_json.append("\"WaterProduction\":\""+obj[11]+"\",");
					result_json.append("\"waterCut\":\""+waterCut+"\",");
					
					result_json.append("\"AvailablePlungerstrokeProd\":\""+obj[12]+"\",");
					result_json.append("\"PumpClearanceLeakProd\":\""+obj[13]+"\",");
					result_json.append("\"TvleakProduction\":\""+obj[14]+"\",");
					result_json.append("\"SvleakProduction\":\""+obj[15]+"\",");
					result_json.append("\"GasInfluenceProd\":\""+obj[16]+"\",");
					
					result_json.append("\"TheoreticalProduction\":\""+obj[17]+"\",");
					result_json.append("\"FullnessCoefficient\":\""+obj[18]+"\",");
					result_json.append("\"PlungerStroke\":\""+obj[19]+"\",");
					result_json.append("\"AvailablePlungerStroke\":\""+obj[20]+"\",");
					
					result_json.append("\"PumpBoreDiameter\":\""+pumpBoreDiameter+"\",");
					result_json.append("\"PumpSettingDepth\":\""+pumpSettingDepth+"\",");
					result_json.append("\"ProducingFluidLevel\":\""+producingFluidLevel+"\",");
					result_json.append("\"LevelDifferenceValue\":\""+obj[21]+"\",");
					result_json.append("\"CalcProducingfluidLevel\":\""+obj[22]+"\",");
					result_json.append("\"Submergence\":\""+obj[23]+"\",");
					
					result_json.append("\"Stroke\":\""+obj[24]+"\",");
					result_json.append("\"SPM\":\""+obj[25]+"\",");
					result_json.append("\"Fmax\":\""+obj[26]+"\",");
					result_json.append("\"Fmin\":\""+obj[27]+"\",");
					result_json.append("\"DeltaF\":\""+obj[28]+"\",");
					
					
					result_json.append("\"UpperLoadLine\":\""+obj[29]+"\",");
					result_json.append("\"LowerLoadLine\":\""+obj[30]+"\",");
					result_json.append("\"DeltaLoadLine\":\""+obj[31]+"\",");
					result_json.append("\"Area\":\""+obj[32]+"\",");
					
					result_json.append("\"AverageWatt\":\""+obj[33]+"\",");
					result_json.append("\"PolishrodPower\":\""+obj[34]+"\",");
					result_json.append("\"WaterPower\":\""+obj[35]+"\",");
					result_json.append("\"SurfaceSystemEfficiency\":\""+obj[36]+"\",");
					result_json.append("\"WelldownSystemEfficiency\":\""+obj[37]+"\",");
					result_json.append("\"SystemEfficiency\":\""+obj[38]+"\",");
					result_json.append("\"EnergyPer100mLift\":\""+obj[39]+"\",");
					
					result_json.append("\"PumpEff1\":\""+obj[40]+"\",");
					result_json.append("\"PumpEff2\":\""+obj[41]+"\",");
					result_json.append("\"PumpEff3\":\""+obj[42]+"\",");
					result_json.append("\"PumpEff4\":\""+obj[43]+"\",");
					result_json.append("\"PumpEff\":\""+obj[44]+"\",");
					result_json.append("\"RodFlexLength\":\""+obj[45]+"\",");
					result_json.append("\"TubingFlexLength\":\""+obj[46]+"\",");
					result_json.append("\"InertiaLength\":\""+obj[47]+"\",");
					
					result_json.append("\"PumpIntakeP\":\""+obj[48]+"\",");
					result_json.append("\"PumpIntakeT\":\""+obj[49]+"\",");
					result_json.append("\"PumpIntakeGOL\":\""+obj[50]+"\",");
					result_json.append("\"PumpIntakeVisl\":\""+obj[51]+"\",");
					result_json.append("\"PumpIntakeBo\":\""+obj[52]+"\",");
					
					result_json.append("\"PumpOutletP\":\""+obj[53]+"\",");
					result_json.append("\"PumpOutletT\":\""+obj[54]+"\",");
					result_json.append("\"PumpOutletGOL\":\""+obj[55]+"\",");
					result_json.append("\"PumpOutletVisl\":\""+obj[56]+"\",");
					result_json.append("\"PumpOutletBo\":\""+obj[57]+"\"");
				}
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("}");
		return result_json.toString().replaceAll("null", "");
	}
	
	public String getOilWellTotalStatisticsData(String data){
		StringBuffer wells= new StringBuffer();
		String json="";
		int liftingType=1;
		int type=1;
		String user="";
		String password="";
		String date=StringManagerUtils.getCurrentTime();
		try{
			JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
			try{
				user=jsonObject.getString("User");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				password=jsonObject.getString("Password");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				liftingType=jsonObject.getInt("LiftingType");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				type=jsonObject.getInt("StatType");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				date=jsonObject.getString("Date");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				JSONArray jsonArray = jsonObject.getJSONArray("WellList");
				for(int i=0;jsonArray!=null&&i<jsonArray.size();i++){
					wells.append(""+jsonArray.getString(i)+",");
				}
				if(wells.toString().endsWith(",")){
					wells.deleteCharAt(wells.length() - 1);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		try{
			if(liftingType==1 && type==1){
				json=this.getTotalFESDiagramResultStatData(user,password,wells.toString(),date);
			}else if(type==2){
				json=this.getTotalCommStatusStatData(user,password,wells.toString(),liftingType,date);
			}else if(type==3){
				json=this.getTotalRunStatusStatData(user,password,wells.toString(),liftingType,date);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return json;
	}
	
	public String getTotalFESDiagramResultStatData(String user,String password,String wells,String date) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",\"Date\":\""+date+"\",\"DataList\":[");
		if(userCheckSign==1){
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			String sql="select decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultname) as resultname,t2.resultcode,count(1) "
					+ " from tbl_device t "
					+ " left outer join tbl_srpdailycalculationdata t2 on t2.deviceId=t.id "
					+ " left outer join tbl_srp_worktype t3 on t2.resultcode=t3.resultcode "
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ "and t2.caldate=to_date('"+date+"','yyyy-mm-dd') ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+= "group by t3.resultname,t2.resultcode order by t2.resultcode";
			
			List<?> list = this.findCallSql(sql);
			
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				result_json.append("{\"Item\":\""+obj[0]+"\",");
				result_json.append("\"Count\":"+obj[2]+"},");
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getTotalCommStatusStatData(String user,String password,String wells,int deviceType,String date) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int online=0,goOnline=0,offline=0;
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",\"Date\":\""+date+"\",\"DataList\":[");
		
		if(userCheckSign==1){
			UserInfo userInfo=MemoryDataManagerTask.getUserInfoByAccount(user);
			Map<String,String> languageResourceMap=MemoryDataManagerTask.getLanguageResource(userInfo!=null?userInfo.getLanguageName():"");
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			String tableName="tbl_srpdailycalculationdata";
			String deviceTableName="tbl_device";
			if(deviceType!=1){
				tableName="tbl_pcpdailycalculationdata";
				deviceTableName="tbl_device";
			}
			
			String sql="select t2.commstatus,count(1) "
					+ " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.caldate=to_date('"+date+"','yyyy-mm-dd') ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+=" group by t2.commstatus";
			
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				if(StringManagerUtils.stringToInteger(obj[0]+"")==1){
					online=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==2){
					goOnline=StringManagerUtils.stringToInteger(obj[1]+"");
				}else{
					offline=StringManagerUtils.stringToInteger(obj[1]+"");
				}
			}
			
			result_json.append("{\"Item\":'"+languageResourceMap.get("online")+"',");
			result_json.append("\"Count\":"+online+"},");
			
			result_json.append("{\"Item\":'"+languageResourceMap.get("goOnline")+"',");
			result_json.append("\"Count\":"+goOnline+"},");
			
			result_json.append("{\"Item\":'"+languageResourceMap.get("offline")+"',");
			result_json.append("\"Count\":"+offline+"}");
		}
		
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getTotalRunStatusStatData(String user,String password,String wells,int deviceType,String date) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int run=0,stop=0,noData=0,offline=0,goOnline=0;
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",\"Date\":\""+date+"\",\"DataList\":[");
		if(userCheckSign==1){
			UserInfo userInfo=MemoryDataManagerTask.getUserInfoByAccount(user);
			Map<String,String> languageResourceMap=MemoryDataManagerTask.getLanguageResource(userInfo!=null?userInfo.getLanguageName():"");
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			String tableName="tbl_srpdailycalculationdata";
			String deviceTableName="tbl_device";
			if(deviceType!=1){
				tableName="tbl_pcpdailycalculationdata";
				deviceTableName="tbl_device";
			}
			
			String sql="select decode(t2.commstatus,0,-1,2,-2,decode(t2.runstatus,null,2,t2.runstatus)) as runstatus,count(1) "
					+ " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.caldate=to_date('"+date+"','yyyy-mm-dd') ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+=" group by t2.commstatus,t2.runstatus";
			
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				if(StringManagerUtils.stringToInteger(obj[0]+"")==1){
					run=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==0){
					stop=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==-1){
					offline=StringManagerUtils.stringToInteger(obj[1]+"");
				}else if(StringManagerUtils.stringToInteger(obj[0]+"")==-2){
					goOnline=StringManagerUtils.stringToInteger(obj[1]+"");
				}else{
					noData=StringManagerUtils.stringToInteger(obj[1]+"");
				}
			}
			result_json.append("{\"Item\":'"+languageResourceMap.get("run")+"',\"Count\":"+run+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("stop")+"',\"Count\":"+stop+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("emptyMsg")+"',\"Count\":"+noData+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("goOnline")+"',\"Count\":"+goOnline+"},");
			result_json.append("{\"Item\":'"+languageResourceMap.get("offline")+"',\"Count\":"+offline+"}");
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getOilWellTotalWellListData(String data,Page pager)throws Exception {
		StringBuffer wells= new StringBuffer();
		String result = "";
		int liftingType=1;
		String date=StringManagerUtils.getCurrentTime();
		int statType=1;
		String statValue="";
		String user = "";
		String password = "";
		try{
			JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
			try{
				user=jsonObject.getString("User");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				password=jsonObject.getString("Password");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				liftingType=jsonObject.getInt("LiftingType");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				date=jsonObject.getString("Date");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				statType=jsonObject.getInt("StatType");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				statValue=jsonObject.getString("StatValue");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				JSONArray jsonArray = jsonObject.getJSONArray("WellList");
				for(int i=0;jsonArray!=null&&i<jsonArray.size();i++){
					wells.append(""+jsonArray.getString(i)+",");
				}
				if(wells.toString().endsWith(",")){
					wells.deleteCharAt(wells.length() - 1);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		if(liftingType==1){
			result=this.getDeviceTotalOverview(user,password,wells.toString(), statType, statValue, date);
		}else{
			result=this.getPCPDeviceTotalOverview(user,password,wells.toString(), statType, statValue, date);
		}
		return result;
	}
	
	public String getDeviceTotalOverview(String user,String password,String wells,int statType,String statValue,String date) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("DeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String tableName="tbl_srpdailycalculationdata";
			String deviceTableName="tbl_device";
			
			String sql="select t2.id,t.id as deviceId,t.deviceName,to_char(t2.caldate,'yyyy-mm-dd') as caldate,t2.ExtendedDays,"
					+ "decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "t2.resultcode,decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName) as resultName,t3.optimizationSuggestion as optimizationSuggestion,"
					+ "t2.liquidWeightProduction,t2.oilWeightProduction,t2.waterWeightProduction,t2.weightWaterCut,"
					+ "t2.liquidVolumetricProduction,t2.oilVolumetricProduction,t2.waterVolumetricProduction,t2.volumeWaterCut,"
					+ "t2.fullnesscoefficient,t2.pumpsettingdepth,t2.producingfluidlevel,t2.submergence,"
					+ "t2.wattDegreeBalance,t2.iDegreeBalance,t2.deltaRadius*100,"
					+ "t2.surfaceSystemEfficiency*100 as surfaceSystemEfficiency,"
					+ "t2.welldownSystemEfficiency*100 as welldownSystemEfficiency,"
					+ "t2.systemEfficiency*100 as systemEfficiency,"
					+ "t2.energyper100mlift,t2.todayKWattH,"
					+ "t2.pumpEff*100 as pumpEff";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " left outer join tbl_srp_worktype t3 on t2.resultcode=t3.resultcode "
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.caldate= to_date('"+date+"','yyyy-mm-dd') ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			if(statType==1 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName)='"+statValue+"'";
			}else if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+=" order by t.sortnum,t.deviceName";
			
			List<?> list = this.findCallSql(sql);
			
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[1]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[9]+"";
				String resultcode=obj[13]+"";
				
				
				SRPDeviceInfo srpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("DeviceInfo".getBytes(), deviceId.getBytes())){
					srpDeviceInfo=(SRPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("DeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&srpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				int commAlarmLevel=0,resultAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==4 && alarmInstanceOwnItem.getItemList().get(j).getItemCode().equalsIgnoreCase(resultcode)){
							resultAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+obj[0]+",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"Caldate\":\""+obj[3]+"\",");
				result_json.append("\"ExtendedDays\":\""+obj[4]+"\",");
				
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[10]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[11]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[12])+"\",");
				result_json.append("\"ResultName\":\""+obj[14]+"\",");
				result_json.append("\"OptimizationSuggestion\":\""+obj[15]+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[16]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[17]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[18]+"\",");
				result_json.append("\"WeightWaterCut\":\""+obj[19]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[20]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[21]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[22]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+obj[23]+"\",");
				
				result_json.append("\"FullnessCoefficient\":\""+obj[24]+"\",");
				result_json.append("\"PumpSettingDepth\":\""+obj[25]+"\",");
				result_json.append("\"ProducingFluidlevel\":\""+obj[26]+"\",");
				result_json.append("\"Submergence\":\""+obj[27]+"\",");
				
				result_json.append("\"IDegreeBalance\":\""+obj[28]+"\",");
				result_json.append("\"WattDegreeBalance\":\""+obj[29]+"\",");
				result_json.append("\"Deltaradius\":\""+obj[30]+"\",");
				
				result_json.append("\"SurfaceSystemEfficiency\":\""+obj[31]+"\",");
				result_json.append("\"WelldownSystemEfficiency\":\""+obj[32]+"\",");
				result_json.append("\"SystemEfficiency\":\""+obj[33]+"\",");
				result_json.append("\"Energyper100mlift\":\""+obj[34]+"\",");
				result_json.append("\"TodayKWattH\":\""+obj[35]+"\",");
				result_json.append("\"PumpEff\":\""+obj[36]+"\",");

				result_json.append("\"ResultAlarmLevel\":"+resultAlarmLevel+",");
				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getPCPDeviceTotalOverview(String user,String password,String wells,int statType,String statValue,String date) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("PCPDeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String tableName="tbl_pcpdailycalculationdata";
			String deviceTableName="tbl_device";
			
			String sql="select t2.id,t.id as deviceId,t.deviceName,to_char(caldate,'yyyy-mm-dd') as caldate,extendedDays,"
					+ "decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "liquidWeightProduction,oilWeightProduction,waterWeightProduction,weightWaterCut,"
					+ "liquidVolumetricProduction,oilVolumetricProduction,waterVolumetricProduction,volumeWaterCut,"
					+ "t2.pumpsettingdepth,t2.producingfluidlevel,t2.submergence,"
					+ "t2.systemEfficiency*100 as systemEfficiency,"
					+ "t2.todayKWattH,"
					+ "t2.pumpEff*100 as pumpEff";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.caldate= to_date('"+date+"','yyyy-mm-dd') ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+=" order by t.sortnum,t.deviceName";
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[1]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[9]+"";
				
				PCPDeviceInfo pcpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("PCPDeviceInfo".getBytes(), deviceId.getBytes())){
					pcpDeviceInfo=(PCPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("PCPDeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&pcpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				int commAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+obj[0]+",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"Caldate\":\""+obj[3]+"\",");
				result_json.append("\"ExtendedDays\":\""+obj[4]+"\",");
				
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[10]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[11]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[12])+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[13]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[14]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[15]+"\",");
				result_json.append("\"WeightWaterCut\":\""+obj[16]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[17]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[18]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[19]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+obj[20]+"\",");
				
				result_json.append("\"PumpSettingDepth\":\""+obj[21]+"\",");
				result_json.append("\"ProducingFluidlevel\":\""+obj[22]+"\",");
				result_json.append("\"Submergence\":\""+obj[23]+"\",");
				
				result_json.append("\"SystemEfficiency\":\""+obj[24]+"\",");
				result_json.append("\"TodayKWattH\":\""+obj[25]+"\",");
				result_json.append("\"PumpEff\":\""+obj[26]+"\",");

				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			result_json.append("]}");
			if(jedis!=null){
				jedis.close();
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getOilWellTotalHistoryData(String data,Page pager)throws Exception {
		String result = "";
		int liftingType=1;
		String startDate=StringManagerUtils.getCurrentTime();
		String endDate=StringManagerUtils.getCurrentTime();
		String deviceName="";
		int statType=1;
		String statValue="";
		String user="";
		String password="";
		try{
			JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
			try{
				user=jsonObject.getString("User");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				password=jsonObject.getString("Password");
			}catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				liftingType=jsonObject.getInt("LiftingType");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				startDate=jsonObject.getString("StartDate");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				endDate=jsonObject.getString("EndDate");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				deviceName=jsonObject.getString("deviceName");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				statType=jsonObject.getInt("StatType");
			}catch(Exception e){
				e.printStackTrace();
			}
			try{
				statValue=jsonObject.getString("StatValue");
			}catch(Exception e){
				e.printStackTrace();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		if(liftingType==1){
			result=this.getDeviceTotalHistory(user,password,deviceName.toString(), statType, statValue, startDate,endDate);
		}else{
			result=this.getPCPDeviceTotalHistory(user,password,deviceName.toString(), statType, statValue, startDate,endDate);
		}
		return result;
	}
	
	public String getDeviceTotalHistory(String user,String password,String deviceName,int statType,String statValue,String startDate,String endDate) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("DeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String tableName="tbl_srpdailycalculationdata";
			String deviceTableName="tbl_device";
			
			String sql="select t2.id,t.id as deviceId,t.deviceName,to_char(t2.caldate,'yyyy-mm-dd') as caldate,t2.ExtendedDays,"
					+ "decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "t2.resultcode,decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName) as resultName,t3.optimizationSuggestion as optimizationSuggestion,"
					+ "t2.liquidWeightProduction,t2.oilWeightProduction,t2.waterWeightProduction,t2.weightWaterCut,"
					+ "t2.liquidVolumetricProduction,t2.oilVolumetricProduction,t2.waterVolumetricProduction,t2.volumeWaterCut,"
					+ "t2.fullnesscoefficient,t2.pumpsettingdepth,t2.producingfluidlevel,t2.submergence,"
					+ "t2.wattDegreeBalance,t2.iDegreeBalance,t2.deltaRadius*100,"
					+ "t2.surfaceSystemEfficiency*100 as surfaceSystemEfficiency,"
					+ "t2.welldownSystemEfficiency*100 as welldownSystemEfficiency,"
					+ "t2.systemEfficiency*100 as systemEfficiency,"
					+ "t2.energyper100mlift,t2.todayKWattH,"
					+ "t2.pumpEff*100 as pumpEff";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " left outer join tbl_srp_worktype t3 on t2.resultcode=t3.resultcode "
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.caldate between to_date('"+startDate+"','yyyy-mm-dd') and to_date('"+endDate+"','yyyy-mm-dd')+1";
			if(StringManagerUtils.isNotNull(deviceName)){
				sql+= "and t.deviceName='"+deviceName+"'";
			}
			if(statType==1 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.resultcode,0,'无数据',null,'无数据',t3.resultName)='"+statValue+"'";
			}else if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+=" order by t2.caldate,t.sortnum,t.deviceName";
			
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[1]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[9]+"";
				String resultcode=obj[13]+"";
				
				
				SRPDeviceInfo srpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("DeviceInfo".getBytes(), deviceId.getBytes())){
					srpDeviceInfo=(SRPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("DeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&srpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), srpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				int commAlarmLevel=0,resultAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==4 && alarmInstanceOwnItem.getItemList().get(j).getItemCode().equalsIgnoreCase(resultcode)){
							resultAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+obj[0]+",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"Caldate\":\""+obj[3]+"\",");
				result_json.append("\"ExtendedDays\":\""+obj[4]+"\",");
				
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[10]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[11]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[12])+"\",");
				result_json.append("\"ResultName\":\""+obj[14]+"\",");
				result_json.append("\"OptimizationSuggestion\":\""+obj[15]+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[16]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[17]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[18]+"\",");
				result_json.append("\"WeightWaterCut\":\""+obj[19]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[20]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[21]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[22]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+obj[23]+"\",");
				
				result_json.append("\"FullnessCoefficient\":\""+obj[24]+"\",");
				result_json.append("\"PumpSettingDepth\":\""+obj[25]+"\",");
				result_json.append("\"ProducingFluidlevel\":\""+obj[26]+"\",");
				result_json.append("\"Submergence\":\""+obj[27]+"\",");
				
				result_json.append("\"IDegreeBalance\":\""+obj[28]+"\",");
				result_json.append("\"WattDegreeBalance\":\""+obj[29]+"\",");
				result_json.append("\"Deltaradius\":\""+obj[30]+"\",");
				
				result_json.append("\"SurfaceSystemEfficiency\":\""+obj[31]+"\",");
				result_json.append("\"WelldownSystemEfficiency\":\""+obj[32]+"\",");
				result_json.append("\"SystemEfficiency\":\""+obj[33]+"\",");
				result_json.append("\"Energyper100mlift\":\""+obj[34]+"\",");
				result_json.append("\"TodayKWattH\":\""+obj[35]+"\",");
				result_json.append("\"PumpEff\":\""+obj[36]+"\",");

				result_json.append("\"ResultAlarmLevel\":"+resultAlarmLevel+",");
				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getPCPDeviceTotalHistory(String user,String password,String deviceName,int statType,String statValue,String startDate,String endDate) throws IOException, SQLException{
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			Jedis jedis=null;
			try{
				jedis = RedisUtil.jedisPool.getResource();
				if(!jedis.exists("PCPDeviceInfo".getBytes())){
					MemoryDataManagerTask.loadDeviceInfo(null,0,"update");
				}
				
				if(!jedis.exists("AlarmInstanceOwnItem".getBytes())){
					MemoryDataManagerTask.loadAlarmInstanceOwnItemById("","update");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String tableName="tbl_pcpdailycalculationdata";
			String deviceTableName="tbl_device";
			
			String sql="select t2.id,t.id as deviceId,t.deviceName,to_char(caldate,'yyyy-mm-dd') as caldate,extendedDays,"
					+ "decode(t2.commstatus,1,'在线',2,'上线','离线') as commStatusName,"
					+ "t2.commtime,t2.commtimeefficiency,t2.commrange,"
					+ "decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据')) as runStatusName,"
					+ "t2.runtime,t2.runtimeefficiency,t2.runrange,"
					+ "liquidWeightProduction,oilWeightProduction,waterWeightProduction,weightWaterCut,"
					+ "liquidVolumetricProduction,oilVolumetricProduction,waterVolumetricProduction,volumeWaterCut,"
					+ "t2.pumpsettingdepth,t2.producingfluidlevel,t2.submergence,"
					+ "t2.systemEfficiency*100 as systemEfficiency,"
					+ "t2.todayKWattH,"
					+ "t2.pumpEff*100 as pumpEff";
			sql+= " from "+deviceTableName+" t "
					+ " left outer join "+tableName+" t2 on t2.deviceId=t.id"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  "
					+ " and t2.caldate between to_date('"+startDate+"','yyyy-mm-dd') and to_date('"+endDate+"','yyyy-mm-dd')+1";
			if(StringManagerUtils.isNotNull(deviceName)){
				sql+= "and t.deviceName='"+deviceName+"'";
			}
			if(statType==2 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,1,'在线',2,'上线','离线')='"+statValue+"'";
			}else if(statType==3 && StringManagerUtils.isNotNull(statValue)){
				sql+=" and decode(t2.commstatus,0,'离线',2,'上线',decode(t2.runstatus,1,'运行',0,'停止','无数据'))='"+statValue+"'";
			}
			sql+=" order by t2.caldate,t.sortnum,t.deviceName";
			
			int totals=this.getTotalCountRows(sql);
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj=(Object[]) list.get(i);
				String deviceId=obj[1]+"";
				String commStatusName=obj[5]+"";
				String runStatusName=obj[9]+"";
				
				PCPDeviceInfo pcpDeviceInfo=null;
				if(jedis!=null&&jedis.hexists("PCPDeviceInfo".getBytes(), deviceId.getBytes())){
					pcpDeviceInfo=(PCPDeviceInfo)SerializeObjectUnils.unserizlize(jedis.hget("PCPDeviceInfo".getBytes(), deviceId.getBytes()));
				}
				
				AlarmInstanceOwnItem alarmInstanceOwnItem=null;
				if(jedis!=null&&pcpDeviceInfo!=null&&jedis.hexists("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes())){
					alarmInstanceOwnItem=(AlarmInstanceOwnItem) SerializeObjectUnils.unserizlize(jedis.hget("AlarmInstanceOwnItem".getBytes(), pcpDeviceInfo.getAlarmInstanceCode().getBytes()));
				}
				
				
				int commAlarmLevel=0,runAlarmLevel=0;
				if(alarmInstanceOwnItem!=null){
					for(int j=0;j<alarmInstanceOwnItem.itemList.size();j++){
						if(alarmInstanceOwnItem.getItemList().get(j).getType()==3 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(commStatusName)){
							commAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}else if(alarmInstanceOwnItem.getItemList().get(j).getType()==6 && alarmInstanceOwnItem.getItemList().get(j).getItemName().equalsIgnoreCase(runStatusName)){
							runAlarmLevel=alarmInstanceOwnItem.getItemList().get(j).getAlarmLevel();
						}
					}
				}
				
				result_json.append("{\"Id\":"+obj[0]+",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"Caldate\":\""+obj[3]+"\",");
				result_json.append("\"ExtendedDays\":\""+obj[4]+"\",");
				
				result_json.append("\"CommStatusName\":\""+commStatusName+"\",");
				result_json.append("\"CommTime\":\""+obj[6]+"\",");
				result_json.append("\"CommTimeEfficiency\":\""+obj[7]+"\",");
				result_json.append("\"CommRange\":\""+StringManagerUtils.CLOBObjectToString(obj[8])+"\",");
				
				result_json.append("\"RunStatusName\":\""+runStatusName+"\",");
				result_json.append("\"RunTime\":\""+obj[10]+"\",");
				result_json.append("\"RunTimeEfficiency\":\""+obj[11]+"\",");
				result_json.append("\"RunRange\":\""+StringManagerUtils.CLOBObjectToString(obj[12])+"\",");
				
				result_json.append("\"LiquidWeightProduction\":\""+obj[13]+"\",");
				result_json.append("\"OilWeightProduction\":\""+obj[14]+"\",");
				result_json.append("\"WaterWeightProduction\":\""+obj[15]+"\",");
				result_json.append("\"WeightWaterCut\":\""+obj[16]+"\",");
				
				result_json.append("\"LiquidVolumetricProduction\":\""+obj[17]+"\",");
				result_json.append("\"OilVolumetricProduction\":\""+obj[18]+"\",");
				result_json.append("\"WaterVolumetricProduction\":\""+obj[19]+"\",");
				result_json.append("\"VolumeWaterCut\":\""+obj[20]+"\",");
				
				result_json.append("\"PumpSettingDepth\":\""+obj[21]+"\",");
				result_json.append("\"ProducingFluidlevel\":\""+obj[22]+"\",");
				result_json.append("\"Submergence\":\""+obj[23]+"\",");
				
				result_json.append("\"SystemEfficiency\":\""+obj[24]+"\",");
				result_json.append("\"TodayKWattH\":\""+obj[25]+"\",");
				result_json.append("\"PumpEff\":\""+obj[26]+"\",");

				result_json.append("\"CommAlarmLevel\":"+commAlarmLevel+",");
				result_json.append("\"RunAlarmLevel\":"+runAlarmLevel+"},");
			}
			if(jedis!=null){
				jedis.close();
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getOilWellInformation(String data,Page pager)throws Exception {
		String json = "";
		StringBuffer wells= new StringBuffer();
		int liftingType=1;
		String user="";
		String password="";
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					liftingType=jsonObject.getInt("LiftingType");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					JSONArray jsonArray = jsonObject.getJSONArray("WellList");
					for(int i=0;jsonArray!=null&&i<jsonArray.size();i++){
						wells.append(""+jsonArray.getString(i)+",");
					}
					if(wells.toString().endsWith(",")){
						wells.deleteCharAt(wells.length() - 1);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(liftingType==1){
			json=this.getSRPInformation(user,password,wells.toString());
		}else{
			json=this.getPCPInformation(user,password,wells.toString());
		}
		return json;
	}
	
	public String getSRPInformation(String user,String password,String wells) {
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		Map<String,String> languageResourceMap=MemoryDataManagerTask.getLanguageResource(Config.getInstance().configFile.getAp().getOthers().getLoginLanguage());
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			Gson gson = new Gson();
			java.lang.reflect.Type type=null;
			String tableName="viw_srpdevice";
			String sql = "select t.id,t.orgName,t.deviceName,t.applicationScenariosName,"//3
					+ " t.instanceName,t.displayInstanceName,t.alarmInstanceName,t.reportinstancename,"//4~7
					+ " t.tcptype,t.signInId,t.slave,t.peakdelay,"//8~11
					+ " t.videoUrl1,decode(t4.role_videokeyedit,1,t2.appkey,'') as appkey1,decode(t4.role_videokeyedit,1,t2.secret,'') as secret1,"//12~14
					+ " t.videoUrl2,decode(t4.role_videokeyedit,1,t3.appkey,'') as appkey2,decode(t4.role_videokeyedit,1,t3.secret,'') as secret2,"//15~17
					+ " t.sortNum,t.statusName,"//18~19
					+ " t.productiondata,"//20
					+ " t.manufacturer,t.model,t.stroke,"//21~23
					+ " decode( lower(t.crankrotationdirection),'clockwise','"+languageResourceMap.get("clockwise")+"','anticlockwise','"+languageResourceMap.get("anticlockwise")+"','' ) as crankrotationdirection,"//24
					+ " t.offsetangleofcrank,t.crankgravityradius,t.singlecrankweight,t.singlecrankpinweight,t.structuralunbalance,"//25~29
					+ " t.balanceinfo"//30
					+ " from "+tableName+" t "
					+ " left outer join tbl_videokey t2 on t.videokeyid1=t2.id"
					+ " left outer join tbl_videokey t3 on t.videokeyid2=t3.id"
					+ " left outer join tbl_role t4 on t4.role_id=(select u.user_type from tbl_user u where u.user_id='"+user+"')"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+= " order by t.sortnum,t.deviceName ";
			
			List<?> list=this.findCallSql(sql);
			Object[] obj=null;
			for(int i=0;i<list.size();i++){
				obj=(Object[]) list.get(i);
				String productionDataStr=obj[20]+"";
				String balanceInfo=obj[30]+"";
				String crudeOilDensity="",waterDensity="",naturalGasRelativeDensity="",saturationPressure="",
						reservoirDepth="",reservoirTemperature="",
						tubingPressure="",casingPressure="",wellHeadTemperature="",waterCut="",productionGasOilRatio="",producingfluidLevel="",pumpSettingDepth="",
						barrelType="",pumpGrade="",pumpBoreDiameter="",plungerLength="",
						tubingStringInsideDiameter="",casingStringInsideDiameter="",
						rodGrade1="",rodOutsideDiameter1="",rodInsideDiameter1="",rodLength1="",
						rodGrade2="",rodOutsideDiameter2="",rodInsideDiameter2="",rodLength2="",
						rodGrade3="",rodOutsideDiameter3="",rodInsideDiameter3="",rodLength3="",
						rodGrade4="",rodOutsideDiameter4="",rodInsideDiameter4="",rodLength4="",
						netGrossRatio="",netGrossValue="";
				String balanceWeight="",balancePosition="";
				if(StringManagerUtils.isNotNull(productionDataStr)){
					type = new TypeToken<SRPProductionData>() {}.getType();
					SRPProductionData productionData=gson.fromJson(productionDataStr, type);
					if(productionData!=null){
						if(productionData.getFluidPVT()!=null){
							crudeOilDensity=productionData.getFluidPVT().getCrudeOilDensity()+"";
							waterDensity=productionData.getFluidPVT().getWaterDensity()+"";
							naturalGasRelativeDensity=productionData.getFluidPVT().getNaturalGasRelativeDensity()+"";
							saturationPressure=productionData.getFluidPVT().getSaturationPressure()+"";
						}
						if(productionData.getReservoir()!=null){
							reservoirDepth=productionData.getReservoir().getDepth()+"";
							reservoirTemperature=productionData.getReservoir().getTemperature()+"";
						}
						if(productionData.getProduction()!=null){
							tubingPressure=productionData.getProduction().getTubingPressure()+"";
							casingPressure=productionData.getProduction().getCasingPressure()+"";
							wellHeadTemperature=productionData.getProduction().getWellHeadTemperature()+"";
							waterCut=productionData.getProduction().getWaterCut()+"";
							productionGasOilRatio=productionData.getProduction().getProductionGasOilRatio()+"";
							producingfluidLevel=productionData.getProduction().getProducingfluidLevel()+"";
							pumpSettingDepth=productionData.getProduction().getPumpSettingDepth()+"";
						}
						if(productionData.getPump()!=null){
							if("L".equalsIgnoreCase(productionData.getPump().getBarrelType())){
								barrelType=languageResourceMap.get("barrelType_L");
							}else{
								barrelType=languageResourceMap.get("barrelType_H");
							}
							pumpGrade=productionData.getPump().getPumpGrade()+"";
							pumpBoreDiameter=productionData.getPump().getPumpBoreDiameter()*1000+"";
							plungerLength=productionData.getPump().getPlungerLength()+"";
						}
						if(productionData.getTubingString()!=null && productionData.getTubingString().getEveryTubing()!=null && productionData.getTubingString().getEveryTubing().size()>0){
							tubingStringInsideDiameter=productionData.getTubingString().getEveryTubing().get(0).getInsideDiameter()*1000+"";
						}
						if(productionData.getCasingString()!=null && productionData.getCasingString().getEveryCasing()!=null && productionData.getCasingString().getEveryCasing().size()>0){
							casingStringInsideDiameter=productionData.getCasingString().getEveryCasing().get(0).getInsideDiameter()*1000+"";
						}
						if(productionData.getRodString()!=null && productionData.getRodString().getEveryRod()!=null && productionData.getRodString().getEveryRod().size()>0){
							rodGrade1=productionData.getRodString().getEveryRod().get(0).getGrade()+"";
							rodOutsideDiameter1=productionData.getRodString().getEveryRod().get(0).getOutsideDiameter()*1000+"";
							rodInsideDiameter1=productionData.getRodString().getEveryRod().get(0).getInsideDiameter()*1000+"";
							rodLength1=productionData.getRodString().getEveryRod().get(0).getLength()+"";
							if(productionData.getRodString().getEveryRod().size()>1){
								rodGrade2=productionData.getRodString().getEveryRod().get(1).getGrade()+"";
								rodOutsideDiameter2=productionData.getRodString().getEveryRod().get(1).getOutsideDiameter()*1000+"";
								rodInsideDiameter2=productionData.getRodString().getEveryRod().get(1).getInsideDiameter()*1000+"";
								rodLength2=productionData.getRodString().getEveryRod().get(1).getLength()+"";
								if(productionData.getRodString().getEveryRod().size()>2){
									rodGrade3=productionData.getRodString().getEveryRod().get(2).getGrade()+"";
									rodOutsideDiameter3=productionData.getRodString().getEveryRod().get(2).getOutsideDiameter()*1000+"";
									rodInsideDiameter3=productionData.getRodString().getEveryRod().get(2).getInsideDiameter()*1000+"";
									rodLength3=productionData.getRodString().getEveryRod().get(2).getLength()+"";
									if(productionData.getRodString().getEveryRod().size()>3){
										rodGrade4=productionData.getRodString().getEveryRod().get(3).getGrade()+"";
										rodOutsideDiameter4=productionData.getRodString().getEveryRod().get(3).getOutsideDiameter()*1000+"";
										rodInsideDiameter4=productionData.getRodString().getEveryRod().get(3).getInsideDiameter()*1000+"";
										rodLength4=productionData.getRodString().getEveryRod().get(3).getLength()+"";
									}
								}
							}
						}
						if(productionData.getManualIntervention()!=null){
							netGrossRatio=productionData.getManualIntervention().getNetGrossRatio()+"";
							netGrossValue=productionData.getManualIntervention().getNetGrossValue()+"";
						}
					}
				}
				
				if(StringManagerUtils.isNotNull(balanceInfo)){
					type = new TypeToken<SRPCalculateRequestData.Balance>() {}.getType();
					SRPCalculateRequestData.Balance balance=gson.fromJson(balanceInfo, type);
					if(balance!=null && balance.getEveryBalance().size()>0){
						for(int j=0;j<balance.getEveryBalance().size();j++){
							balanceWeight+=balance.getEveryBalance().get(j).getWeight()+"";
							balancePosition+=balance.getEveryBalance().get(j).getPosition()+"";
							if(j<balance.getEveryBalance().size()-1){
								balanceWeight+=",";
								balancePosition+=",";
							}
						}
					}
				}
				
				result_json.append("{\"Id\":\""+(i+1)+"\",");
				result_json.append("\"OrgName\":\""+obj[1]+"\",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"ApplicationScenariosName\":\""+obj[3]+"\",");
				result_json.append("\"InstanceName\":\""+obj[4]+"\",");
				result_json.append("\"DisplayInstanceName\":\""+obj[5]+"\",");
				result_json.append("\"AlarmInstanceName\":\""+obj[6]+"\",");
				result_json.append("\"ReportInstanceName\":\""+obj[7]+"\",");
				
				result_json.append("\"TcpType\":\""+(obj[8]+"").replaceAll(" ", "").toLowerCase().replaceAll("tcpserver", "TCP Server").replaceAll("tcpclient", "TCP Client")+"\",");
				result_json.append("\"SignInId\":\""+obj[9]+"\",");
				result_json.append("\"Slave\":\""+obj[10]+"\",");
				result_json.append("\"PeakDelay\":\""+obj[11]+"\",");
				
				result_json.append("\"VideoUrl1\":\""+obj[12]+"\",");
				result_json.append("\"Appkey1\":\""+obj[13]+"\",");
				result_json.append("\"Secret1\":\""+obj[14]+"\",");
				
				result_json.append("\"VideoUrl2\":\""+obj[15]+"\",");
				result_json.append("\"appkey2\":\""+obj[16]+"\",");
				result_json.append("\"secret2\":\""+obj[17]+"\",");
				
				result_json.append("\"SortNum\":\""+obj[18]+"\",");
				result_json.append("\"StatusName\":\""+obj[19]+"\",");
				
				result_json.append("\"CrudeOilDensity\":\""+crudeOilDensity+"\",");
				result_json.append("\"WaterDensity\":\""+waterDensity+"\",");
				result_json.append("\"NaturalGasRelativeDensity\":\""+naturalGasRelativeDensity+"\",");
				result_json.append("\"SaturationPressure\":\""+saturationPressure+"\",");
				result_json.append("\"ReservoirDepth\":\""+reservoirDepth+"\",");
				result_json.append("\"ReservoirTemperature\":\""+reservoirTemperature+"\",");
				result_json.append("\"TubingPressure\":\""+tubingPressure+"\",");
				result_json.append("\"CasingPressure\":\""+casingPressure+"\",");
				result_json.append("\"WellHeadTemperature\":\""+wellHeadTemperature+"\",");
				result_json.append("\"WaterCut\":\""+waterCut+"\",");
				result_json.append("\"ProductionGasOilRatio\":\""+productionGasOilRatio+"\",");
				result_json.append("\"ProducingfluidLevel\":\""+producingfluidLevel+"\",");
				result_json.append("\"PumpSettingDepth\":\""+pumpSettingDepth+"\",");
				result_json.append("\"BarrelType\":\""+barrelType+"\",");
				result_json.append("\"PumpGrade\":\""+pumpGrade+"\",");
				result_json.append("\"PumpBoreDiameter\":\""+pumpBoreDiameter+"\",");
				result_json.append("\"PlungerLength\":\""+plungerLength+"\",");
				result_json.append("\"TubingStringInsideDiameter\":\""+tubingStringInsideDiameter+"\",");
				result_json.append("\"CasingStringInsideDiameter\":\""+casingStringInsideDiameter+"\",");
				result_json.append("\"RodGrade1\":\""+rodGrade1+"\",");
				result_json.append("\"RodOutsideDiameter1\":\""+rodOutsideDiameter1+"\",");
				result_json.append("\"RodInsideDiameter1\":\""+rodInsideDiameter1+"\",");
				result_json.append("\"RodLength1\":\""+rodLength1+"\",");
				result_json.append("\"RodGrade2\":\""+rodGrade2+"\",");
				result_json.append("\"RodOutsideDiameter2\":\""+rodOutsideDiameter2+"\",");
				result_json.append("\"RodInsideDiameter2\":\""+rodInsideDiameter2+"\",");
				result_json.append("\"RodLength2\":\""+rodLength2+"\",");
				result_json.append("\"RodGrade3\":\""+rodGrade3+"\",");
				result_json.append("\"RodOutsideDiameter3\":\""+rodOutsideDiameter3+"\",");
				result_json.append("\"RodInsideDiameter3\":\""+rodInsideDiameter3+"\",");
				result_json.append("\"RodLength3\":\""+rodLength3+"\",");
				result_json.append("\"RodGrade4\":\""+rodGrade4+"\",");
				result_json.append("\"RodOutsideDiameter4\":\""+rodOutsideDiameter4+"\",");
				result_json.append("\"RodInsideDiameter4\":\""+rodInsideDiameter4+"\",");
				result_json.append("\"RodLength4\":\""+rodLength4+"\",");
				
				result_json.append("\"NetGrossRatio\":\""+netGrossRatio+"\",");
				result_json.append("\"NetGrossValue\":\""+netGrossValue+"\",");
				
				result_json.append("\"Manufacturer\":\""+obj[21]+"\",");
				result_json.append("\"Model\":\""+obj[22]+"\",");
				result_json.append("\"Stroke\":\""+obj[23]+"\",");
				result_json.append("\"CrankRotationDirection\":\""+obj[24]+"\",");
				result_json.append("\"OffsetAngleOfCrank\":\""+obj[25]+"\",");
				result_json.append("\"CrankGravityRadius\":\""+obj[26]+"\",");
				result_json.append("\"SingleCrankWeight\":\""+obj[27]+"\",");
				result_json.append("\"SingleCrankPinWeight\":\""+obj[28]+"\",");
				result_json.append("\"StructuralUnbalance\":\""+obj[29]+"\",");
				
				
				result_json.append("\"BalanceWeight\":\""+balanceWeight+"\",");
				result_json.append("\"BalancePosition\":\""+balancePosition+"\"},");
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	
	public String getPCPInformation(String user,String password,String wells) {
		StringBuffer result_json = new StringBuffer();
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			String [] wellList=null;
			if(StringManagerUtils.isNotNull(wells)){
				wellList=wells.split(",");
			}
			Gson gson = new Gson();
			java.lang.reflect.Type type=null;
			String tableName="viw_srpdevice";
			String sql = "select t.id,t.orgName,t.deviceName,t.applicationScenariosName,"//0~3
					+ " t.instanceName,t.displayInstanceName,t.alarmInstanceName,t.reportinstancename,"//4~7
					+ " t.tcptype,t.signInId,t.slave,t.peakdelay,"//8~11
					+ " t.videoUrl1,decode(t4.role_videokeyedit,1,t2.appkey,'') as appkey1,decode(t4.role_videokeyedit,1,t2.secret,'') as secret1,"//12~14
					+ " t.videoUrl2,decode(t4.role_videokeyedit,1,t3.appkey,'') as appkey2,decode(t4.role_videokeyedit,1,t3.secret,'') as secret2,"//15~17
					+ " t.sortNum,t.statusName,"//18~19
					+ " t.productiondata"//20
					+ " from "+tableName+" t "
					+ " left outer join tbl_videokey t2 on t.videokeyid1=t2.id"
					+ " left outer join tbl_videokey t3 on t.videokeyid2=t3.id"
					+ " left outer join tbl_role t4 on t4.role_id=(select u.user_type from tbl_user u where u.user_id='"+user+"')"
					+ " where  t.orgid in( select org.org_id from tbl_org org start with org.org_id=(select u.user_orgid from tbl_user u where u.user_id='"+user+"' ) connect by prior  org_id=org_parent)  ";
			if(wellList!=null){
				sql+=" and t.deviceName in ( "+StringManagerUtils.joinStringArr2(wellList, ",")+" )";
			}
			sql+= " order by t.sortnum,t.deviceName ";
			
			List<?> list=this.findCallSql(sql);
			Object[] obj=null;
			for(int i=0;i<list.size();i++){
				obj=(Object[]) list.get(i);
				String productionDataStr=obj[20]+"";
				String crudeOilDensity="",waterDensity="",naturalGasRelativeDensity="",saturationPressure="",
						reservoirDepth="",reservoirTemperature="",
						tubingPressure="",casingPressure="",wellHeadTemperature="",waterCut="",productionGasOilRatio="",producingfluidLevel="",pumpSettingDepth="",
						barrelLength="",barrelSeries="",rotorDiameter="",QPR="",
						tubingStringInsideDiameter="",casingStringInsideDiameter="",
						rodGrade1="",rodOutsideDiameter1="",rodInsideDiameter1="",rodLength1="",
						rodGrade2="",rodOutsideDiameter2="",rodInsideDiameter2="",rodLength2="",
						rodGrade3="",rodOutsideDiameter3="",rodInsideDiameter3="",rodLength3="",
						rodGrade4="",rodOutsideDiameter4="",rodInsideDiameter4="",rodLength4="",
						netGrossRatio="",netGrossValue="";
				
				if(StringManagerUtils.isNotNull(productionDataStr)){
					type = new TypeToken<PCPProductionData>() {}.getType();
					PCPProductionData productionData=gson.fromJson(productionDataStr, type);
					if(productionData!=null){
						if(productionData.getFluidPVT()!=null){
							crudeOilDensity=productionData.getFluidPVT().getCrudeOilDensity()+"";
							waterDensity=productionData.getFluidPVT().getWaterDensity()+"";
							naturalGasRelativeDensity=productionData.getFluidPVT().getNaturalGasRelativeDensity()+"";
							saturationPressure=productionData.getFluidPVT().getSaturationPressure()+"";
						}
						if(productionData.getReservoir()!=null){
							reservoirDepth=productionData.getReservoir().getDepth()+"";
							reservoirTemperature=productionData.getReservoir().getTemperature()+"";
						}
						if(productionData.getProduction()!=null){
							tubingPressure=productionData.getProduction().getTubingPressure()+"";
							casingPressure=productionData.getProduction().getCasingPressure()+"";
							wellHeadTemperature=productionData.getProduction().getWellHeadTemperature()+"";
							waterCut=productionData.getProduction().getWaterCut()+"";
							productionGasOilRatio=productionData.getProduction().getProductionGasOilRatio()+"";
							producingfluidLevel=productionData.getProduction().getProducingfluidLevel()+"";
							pumpSettingDepth=productionData.getProduction().getPumpSettingDepth()+"";
						}
						if(productionData.getPump()!=null){
							barrelLength=productionData.getPump().getBarrelLength()+"";
							barrelSeries=productionData.getPump().getBarrelSeries()+"";
							rotorDiameter=productionData.getPump().getRotorDiameter()*1000+"";
							QPR=productionData.getPump().getQPR()*1000*1000+"";
						}
						if(productionData.getTubingString()!=null && productionData.getTubingString().getEveryTubing()!=null && productionData.getTubingString().getEveryTubing().size()>0){
							tubingStringInsideDiameter=productionData.getTubingString().getEveryTubing().get(0).getInsideDiameter()*1000+"";
						}
						if(productionData.getCasingString()!=null && productionData.getCasingString().getEveryCasing()!=null && productionData.getCasingString().getEveryCasing().size()>0){
							casingStringInsideDiameter=productionData.getCasingString().getEveryCasing().get(0).getInsideDiameter()*1000+"";
						}
						if(productionData.getRodString()!=null && productionData.getRodString().getEveryRod()!=null && productionData.getRodString().getEveryRod().size()>0){
							rodGrade1=productionData.getRodString().getEveryRod().get(0).getGrade()+"";
							rodOutsideDiameter1=productionData.getRodString().getEveryRod().get(0).getOutsideDiameter()*1000+"";
							rodInsideDiameter1=productionData.getRodString().getEveryRod().get(0).getInsideDiameter()*1000+"";
							rodLength1=productionData.getRodString().getEveryRod().get(0).getLength()+"";
							if(productionData.getRodString().getEveryRod().size()>1){
								rodGrade2=productionData.getRodString().getEveryRod().get(1).getGrade()+"";
								rodOutsideDiameter2=productionData.getRodString().getEveryRod().get(1).getOutsideDiameter()*1000+"";
								rodInsideDiameter2=productionData.getRodString().getEveryRod().get(1).getInsideDiameter()*1000+"";
								rodLength2=productionData.getRodString().getEveryRod().get(1).getLength()+"";
								if(productionData.getRodString().getEveryRod().size()>2){
									rodGrade3=productionData.getRodString().getEveryRod().get(2).getGrade()+"";
									rodOutsideDiameter3=productionData.getRodString().getEveryRod().get(2).getOutsideDiameter()*1000+"";
									rodInsideDiameter3=productionData.getRodString().getEveryRod().get(2).getInsideDiameter()*1000+"";
									rodLength3=productionData.getRodString().getEveryRod().get(2).getLength()+"";
									if(productionData.getRodString().getEveryRod().size()>3){
										rodGrade4=productionData.getRodString().getEveryRod().get(3).getGrade()+"";
										rodOutsideDiameter4=productionData.getRodString().getEveryRod().get(3).getOutsideDiameter()*1000+"";
										rodInsideDiameter4=productionData.getRodString().getEveryRod().get(3).getInsideDiameter()*1000+"";
										rodLength4=productionData.getRodString().getEveryRod().get(3).getLength()+"";
									}
								}
							}
						}
						if(productionData.getManualIntervention()!=null){
							netGrossRatio=productionData.getManualIntervention().getNetGrossRatio()+"";
							netGrossValue=productionData.getManualIntervention().getNetGrossValue()+"";
						}
					}
				}
				
				result_json.append("{\"Id\":\""+(i+1)+"\",");
				result_json.append("\"OrgName\":\""+obj[1]+"\",");
				result_json.append("\"deviceName\":\""+obj[2]+"\",");
				result_json.append("\"ApplicationScenariosName\":\""+obj[3]+"\",");
				result_json.append("\"InstanceName\":\""+obj[4]+"\",");
				result_json.append("\"DisplayInstanceName\":\""+obj[5]+"\",");
				result_json.append("\"AlarmInstanceName\":\""+obj[6]+"\",");
				result_json.append("\"ReportInstanceName\":\""+obj[7]+"\",");
				
				result_json.append("\"TcpType\":\""+(obj[8]+"").replaceAll(" ", "").toLowerCase().replaceAll("tcpserver", "TCP Server").replaceAll("tcpclient", "TCP Client")+"\",");
				result_json.append("\"SignInId\":\""+obj[9]+"\",");
				result_json.append("\"Slave\":\""+obj[10]+"\",");
				result_json.append("\"PeakDelay\":\""+obj[11]+"\",");
				
				result_json.append("\"VideoUrl1\":\""+obj[12]+"\",");
				result_json.append("\"Appkey1\":\""+obj[13]+"\",");
				result_json.append("\"Secret1\":\""+obj[14]+"\",");
				
				result_json.append("\"VideoUrl2\":\""+obj[15]+"\",");
				result_json.append("\"appkey2\":\""+obj[16]+"\",");
				result_json.append("\"secret2\":\""+obj[17]+"\",");
				
				result_json.append("\"SortNum\":\""+obj[18]+"\",");
				result_json.append("\"StatusName\":\""+obj[19]+"\",");
				
				result_json.append("\"CrudeOilDensity\":\""+crudeOilDensity+"\",");
				result_json.append("\"WaterDensity\":\""+waterDensity+"\",");
				result_json.append("\"NaturalGasRelativeDensity\":\""+naturalGasRelativeDensity+"\",");
				result_json.append("\"SaturationPressure\":\""+saturationPressure+"\",");
				result_json.append("\"ReservoirDepth\":\""+reservoirDepth+"\",");
				result_json.append("\"ReservoirTemperature\":\""+reservoirTemperature+"\",");
				result_json.append("\"TubingPressure\":\""+tubingPressure+"\",");
				result_json.append("\"CasingPressure\":\""+casingPressure+"\",");
				result_json.append("\"WellHeadTemperature\":\""+wellHeadTemperature+"\",");
				result_json.append("\"WaterCut\":\""+waterCut+"\",");
				result_json.append("\"ProductionGasOilRatio\":\""+productionGasOilRatio+"\",");
				result_json.append("\"ProducingfluidLevel\":\""+producingfluidLevel+"\",");
				result_json.append("\"PumpSettingDepth\":\""+pumpSettingDepth+"\",");

				result_json.append("\"BarrelLength\":\""+barrelLength+"\",");
				result_json.append("\"BarrelSeries\":\""+barrelSeries+"\",");
				result_json.append("\"RotorDiameter\":\""+rotorDiameter+"\",");
				result_json.append("\"QPR\":\""+QPR+"\",");

				result_json.append("\"TubingStringInsideDiameter\":\""+tubingStringInsideDiameter+"\",");
				result_json.append("\"CasingStringInsideDiameter\":\""+casingStringInsideDiameter+"\",");
				result_json.append("\"RodGrade1\":\""+rodGrade1+"\",");
				result_json.append("\"RodOutsideDiameter1\":\""+rodOutsideDiameter1+"\",");
				result_json.append("\"RodInsideDiameter1\":\""+rodInsideDiameter1+"\",");
				result_json.append("\"RodLength1\":\""+rodLength1+"\",");
				result_json.append("\"RodGrade2\":\""+rodGrade2+"\",");
				result_json.append("\"RodOutsideDiameter2\":\""+rodOutsideDiameter2+"\",");
				result_json.append("\"RodInsideDiameter2\":\""+rodInsideDiameter2+"\",");
				result_json.append("\"RodLength2\":\""+rodLength2+"\",");
				result_json.append("\"RodGrade3\":\""+rodGrade3+"\",");
				result_json.append("\"RodOutsideDiameter3\":\""+rodOutsideDiameter3+"\",");
				result_json.append("\"RodInsideDiameter3\":\""+rodInsideDiameter3+"\",");
				result_json.append("\"RodLength3\":\""+rodLength3+"\",");
				result_json.append("\"RodGrade4\":\""+rodGrade4+"\",");
				result_json.append("\"RodOutsideDiameter4\":\""+rodOutsideDiameter4+"\",");
				result_json.append("\"RodInsideDiameter4\":\""+rodInsideDiameter4+"\",");
				result_json.append("\"RodLength4\":\""+rodLength4+"\",");
				
				result_json.append("\"NetGrossRatio\":\""+netGrossRatio+"\",");
				result_json.append("\"NetGrossValue\":\""+netGrossValue+"\"},");
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
	
	public String getPumpingModelInformation(String data,Page pager,String language)throws Exception {
		StringBuffer result_json = new StringBuffer();
		String user="";
		String password="";
		String manufacturer="";
		String model="";
		Map<String,String> languageResourceMap=MemoryDataManagerTask.getLanguageResource(language);
		if(StringManagerUtils.isNotNull(data)){
			try{
				JSONObject jsonObject = JSONObject.fromObject(data);//解析数据
				try{
					user=jsonObject.getString("User");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					password=jsonObject.getString("Password");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					manufacturer=jsonObject.getString("Manufacturer");
				}catch(Exception e){
					e.printStackTrace();
				}
				
				try{
					model=jsonObject.getString("Model");
				}catch(Exception e){
					e.printStackTrace();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		int userCheckSign=this.userManagerService.userCheck(user, password);
		result_json.append("{ \"ResultStatus\":"+userCheckSign+",");
		result_json.append("\"DataList\":[");
		if(userCheckSign==1){
			String sql = "select t.id,t.manufacturer,t.model,t.stroke,decode(t.crankrotationdirection,'Anticlockwise','"+languageResourceMap.get("anticlockwise")+"','Clockwise','"+languageResourceMap.get("clockwise")+"','') as crankrotationdirection,"
					+ " t.offsetangleofcrank,t.crankgravityradius,t.singlecrankweight,t.singlecrankpinweight,"
					+ " t.structuralunbalance,t.balanceweight,"
					+ " t.prtf"
					+ " from tbl_pumpingmodel t where 1=1";
			if (StringManagerUtils.isNotNull(manufacturer)) {
				sql += " and t.manufacturer = '"+manufacturer+"'";
			}
			if (StringManagerUtils.isNotNull(model)) {
				sql += " and t.model = '"+model+"'";
			}
			sql+= " order by t.id,t.manufacturer,t.model";
			List<?> list = this.findCallSql(sql);
			for(int i=0;i<list.size();i++){
				Object[] obj = (Object[]) list.get(i);
				
				String prtfStr="";
				if(obj[11]!=null){
					try {
						SerializableClobProxy   proxy = (SerializableClobProxy)Proxy.getInvocationHandler(obj[11]);
						CLOB realClob = (CLOB) proxy.getWrappedClob(); 
						prtfStr=StringManagerUtils.CLOBtoString(realClob);
					} catch (SQLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(!StringManagerUtils.isNotNull(prtfStr)){
					prtfStr="{}";
				}
				
				result_json.append("{\"Id\":\""+obj[0]+"\",");
				result_json.append("\"Manufacturer\":\""+obj[1]+"\",");
				result_json.append("\"Model\":\""+obj[2]+"\",");
				result_json.append("\"Stroke\":\""+obj[3]+"\",");
				result_json.append("\"CrankRotationDirection\":\""+obj[4]+"\",");
				result_json.append("\"OffsetAngleOfCrank\":\""+obj[5]+"\",");
				result_json.append("\"CrankGravityRadius\":\""+obj[6]+"\",");
				result_json.append("\"SingleCrankWeight\":\""+obj[7]+"\",");
				result_json.append("\"SingleCrankPinWeight\":\""+obj[8]+"\",");
				result_json.append("\"StructuralUnbalance\":\""+obj[9]+"\",");
				result_json.append("\"BalanceWeight\":\""+obj[10]+"\",");
				result_json.append("\"PumpingPRTF\":"+prtfStr+"},");
			}
		}
		if(result_json.toString().endsWith(",")){
			result_json.deleteCharAt(result_json.length() - 1);
		}
		result_json.append("]}");
		return result_json.toString().replaceAll("\"null\"", "\"\"");
	}
}
