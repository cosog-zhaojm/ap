package com.cosog.task;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cosog.model.DataMapping;
import com.cosog.model.calculate.AdOnlineProbeResponseData;
import com.cosog.model.calculate.AppRunStatusProbeResonanceData;
import com.cosog.model.calculate.DeviceInfo;
import com.cosog.model.drive.InitId;
import com.cosog.model.drive.InitInstance;
import com.cosog.model.drive.InitProtocol;
import com.cosog.model.drive.ModbusProtocolConfig;
import com.cosog.thread.calculate.InitIdAndIPPortThread;
import com.cosog.thread.calculate.ThreadPool;
import com.cosog.utils.AdInitMap;
import com.cosog.utils.Config;
import com.cosog.utils.CounterUtils;
import com.cosog.utils.DataModelMap;
import com.cosog.utils.JDBCUtil;
import com.cosog.utils.OracleJdbcUtis;
import com.cosog.utils.StringManagerUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


@Component("EquipmentDriverServerTask")  
public class EquipmentDriverServerTask {
	private static EquipmentDriverServerTask instance=new EquipmentDriverServerTask();
	
	private static boolean initSwitch=false;
	private static boolean initEnable=initSwitch && Config.getInstance().configFile.getAp().getOthers().getIot();
	
	public static boolean initFinished=false;
	public static EquipmentDriverServerTask getInstance(){
		return instance;
	}
	
	@SuppressWarnings({ "static-access", "unused" })
	@Scheduled(fixedRate = 1000*60*60*24*365*100)
	public void driveServerTast(){
		
		CounterUtils.reset();
		CounterUtils.timer();
		
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		StringManagerUtils stringManagerUtils=new StringManagerUtils();
		String allOfflineUrl=stringManagerUtils.getProjectUrl()+"/api/acq/allDeviceOffline";
		
		initWellCommStatus();
		if(Config.getInstance().configFile.getAp().getOthers().getIot()){
			initWellCommStatusByOnlineProbe();//检测当前已在线的设备,并更新状态
		}
		initWellDaliyData();
		MemoryDataManagerTask.loadMemoryData();
		
		ThreadPool executor=null;
		try {
			executor = adInit();
			initFinished=true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(Config.getInstance().configFile.getAp().getOthers().getIot()){
			boolean sendMsg=false;
			exampleDataManage();
			do{
				DriverProbeResponse driverProbeResponse=adInitProbe();
				String Ver="";
				if(driverProbeResponse!=null){
					sendMsg=false;
					if(!driverProbeResponse.getHttpServerInitStatus()){
						try {
							initServerConfig();
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						driverProbeResponse=adInitProbe();
						if(driverProbeResponse==null){
							continue;
						}
					}
					if(!driverProbeResponse.getProtocolInitStatus()){
						initProtocolConfig("","");
						initInstanceConfig(null,"");
						initSMSInstanceConfig(null,"");
						if(executor!=null && executor.isCompletedByTaskCount()){
							//清空内存
							AdInitMap.cleanData();
							initDriverAcquisitionInfoConfig(null,0,"");
						}
						driverProbeResponse=adInitProbe();
						if(driverProbeResponse==null){
							continue;
						}
					}
					
					if(driverProbeResponse!=null && !driverProbeResponse.getInstanceInitStatus()){
						while(driverProbeResponse!=null && ( (!driverProbeResponse.getProtocolInitStatus())||(!driverProbeResponse.getInstanceInitStatus()) )){
							if(!driverProbeResponse.getProtocolInitStatus()){
								initProtocolConfig("","");
								initInstanceConfig(null,"");
								initSMSInstanceConfig(null,"");
								if(executor!=null && executor.isCompletedByTaskCount()){
									//清空内存
									AdInitMap.cleanData();
									initDriverAcquisitionInfoConfig(null,0,"");
								}
								driverProbeResponse=adInitProbe();
							}
							if(driverProbeResponse!=null && !driverProbeResponse.getInstanceInitStatus()){
								initInstanceConfig(null,"");
								initSMSInstanceConfig(null,"");
								if(executor!=null && executor.isCompletedByTaskCount()){
									//清空内存
									AdInitMap.cleanData();
									initDriverAcquisitionInfoConfig(null,0,"");
								}
								driverProbeResponse=adInitProbe();
							}
						}
					}
					
					if(driverProbeResponse!=null && !driverProbeResponse.getSMSInitStatus()){
//						initSMSDevice(null,"");
					}
					
					if(!( driverProbeResponse.getIDInitStatus() || driverProbeResponse.getIPPortInitStatus() )){
						while(driverProbeResponse!=null 
								&& ( 
										(!driverProbeResponse.getProtocolInitStatus())
										||(!driverProbeResponse.getInstanceInitStatus()) 
										||(!( driverProbeResponse.getIDInitStatus() || driverProbeResponse.getIPPortInitStatus() ))
									)
								){
							if(!driverProbeResponse.getProtocolInitStatus()){
								initProtocolConfig("","");
								initInstanceConfig(null,"");
								initSMSInstanceConfig(null,"");
								if(executor!=null && executor.isCompletedByTaskCount()){
									//清空内存
									AdInitMap.cleanData();
									initDriverAcquisitionInfoConfig(null,0,"");
								}
								driverProbeResponse=adInitProbe();
							}
							if(driverProbeResponse!=null && !driverProbeResponse.getInstanceInitStatus()){
								initInstanceConfig(null,"");
								initSMSInstanceConfig(null,"");
								if(executor!=null && executor.isCompletedByTaskCount()){
									//清空内存
									AdInitMap.cleanData();
									initDriverAcquisitionInfoConfig(null,0,"");
								}
								driverProbeResponse=adInitProbe();
							}
							if(!( driverProbeResponse.getIDInitStatus() || driverProbeResponse.getIPPortInitStatus() )){
								if(executor!=null && executor.isCompletedByTaskCount()){
									//清空内存
									AdInitMap.cleanData();
									initDriverAcquisitionInfoConfig(null,0,"");
								}
								driverProbeResponse=adInitProbe();
							}
						}
					}
					Ver=driverProbeResponse.getVer();
				}else{
					if(!sendMsg){
						StringManagerUtils.sendPostMethod(allOfflineUrl, "","utf-8",0,0);
						sendMsg=true;
					}
				}
				try {
					Thread.sleep(1000*1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}while(true);
		}
	}
	
	
	public void adProbeAndInit(){
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		StringManagerUtils stringManagerUtils=new StringManagerUtils();
		String allOfflineUrl=stringManagerUtils.getProjectUrl()+"/api/acq/allDeviceOffline";
		
		initWellCommStatus();
		if(Config.getInstance().configFile.getAp().getOthers().getIot()){
			initWellCommStatusByOnlineProbe();//检测当前已在线的设备,并更新状态
		}
		initWellDaliyData();
		MemoryDataManagerTask.loadMemoryData();
		
		ThreadPool executor=null;
		try {
			executor = adInit();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(Config.getInstance().configFile.getAp().getOthers().getIot()){
			boolean sendMsg=false;
			do{
				DriverProbeResponse driverProbeResponse=adInitProbe();
				String Ver="";
				if(driverProbeResponse!=null){
					sendMsg=false;
					if(!driverProbeResponse.getHttpServerInitStatus()){
						try {
							initServerConfig();
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						driverProbeResponse=adInitProbe();
					}
					if(!driverProbeResponse.getProtocolInitStatus()){
						initProtocolConfig("","");
						driverProbeResponse=adInitProbe();
					}
					if(!driverProbeResponse.getInstanceInitStatus()){
						if(!driverProbeResponse.getProtocolInitStatus()){
							initProtocolConfig("","");
							driverProbeResponse=adInitProbe();
						}
						initInstanceConfig(null,"");
						initSMSInstanceConfig(null,"");
						driverProbeResponse=adInitProbe();
					}
					if(!driverProbeResponse.getSMSInitStatus()){
//						initSMSDevice(null,"");
					}
					if(!( driverProbeResponse.getIDInitStatus() || driverProbeResponse.getIPPortInitStatus() )){
						if(!driverProbeResponse.getInstanceInitStatus()){
							if(!driverProbeResponse.getProtocolInitStatus()){
								initProtocolConfig("","");
								driverProbeResponse=adInitProbe();
							}
							initInstanceConfig(null,"");
							initSMSInstanceConfig(null,"");
							driverProbeResponse=adInitProbe();
						}
						
						if(executor!=null && executor.isCompletedByTaskCount()){
							//清空内存
							AdInitMap.cleanData();
							initDriverAcquisitionInfoConfig(null,0,"");
						}
					}
					Ver=driverProbeResponse.getVer();
				}else{
					if(!sendMsg){
						StringManagerUtils.sendPostMethod(allOfflineUrl, "","utf-8",0,0);
						sendMsg=true;
					}
				}
				try {
					Thread.sleep(1000*1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}while(true);
		}
	}
	
	public static ThreadPool adInit() throws MalformedURLException, InterruptedException{
		ThreadPool executor=null;
		try{
			if(Config.getInstance().configFile.getAp().getOthers().getIot() ){
				Gson gson = new Gson();
				java.lang.reflect.Type type=null;
				String adStatusUrl=Config.getInstance().configFile.getAd().getProbe().getApp();
				String adStatusProbeResponseDataStr=StringManagerUtils.sendPostMethod(adStatusUrl, "","utf-8",0,0);
				type = new TypeToken<AppRunStatusProbeResonanceData>() {}.getType();
				AppRunStatusProbeResonanceData adStatusProbeResonanceData=gson.fromJson(adStatusProbeResponseDataStr, type);
				if(adStatusProbeResonanceData!=null){
					initServerConfig();
					initProtocolConfig("","");
					initInstanceConfig(null,"");
					initSMSInstanceConfig(null,"");
					initSMSDevice(null,"");
					initDriverAcquisitionInfoConfig(null,0,"");
					executor = new ThreadPool("adInit",
							Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getCorePoolSize(), 
							Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getMaximumPoolSize(), 
							Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getKeepAliveTime(), 
							TimeUnit.SECONDS, 
							Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getWattingCount());
					while (!executor.isCompletedByTaskCount()) {
						System.out.println(executor.getExecutor().getTaskCount()+","+executor.getExecutor().getCompletedTaskCount());
						Thread.sleep(1000*1);
				    }
					System.out.println("线程池任务执行完毕！");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return executor;
	}
	
	public static DriverProbeResponse adInitProbe(){
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		String probeUrl=Config.getInstance().configFile.getAd().getProbe().getInit();
		String responseData=StringManagerUtils.sendPostMethod(probeUrl, "","utf-8",0,0);
		type = new TypeToken<DriverProbeResponse>() {}.getType();
		DriverProbeResponse driverProbeResponse=gson.fromJson(responseData, type);
		return driverProbeResponse;
	}
	
	public static AdOnlineProbeResponseData adOnlineProbe(){
		Gson gson = new Gson();
		java.lang.reflect.Type type=null;
		String probeUrl=Config.getInstance().configFile.getAd().getProbe().getOnline();
		String responseData=StringManagerUtils.sendPostMethod(probeUrl, "","utf-8",0,0);
		type = new TypeToken<AdOnlineProbeResponseData>() {}.getType();
		AdOnlineProbeResponseData adOnlineProbeResponseData=gson.fromJson(responseData, type);
		return adOnlineProbeResponseData;
	}
	
	
	public static class ExampleDataManageThread extends Thread{
		private String deviceName;
		private int cycle;
		private int wait;
		public ExampleDataManageThread(String deviceName,int cycle,int wait) {
			super();
			this.deviceName = deviceName;
			this.cycle = cycle;
			this.wait = wait;
		}
		@SuppressWarnings("static-access")
		public void run(){
			try {
				Thread.currentThread().sleep(1000*wait);
				StringManagerUtils stringManagerUtils=new StringManagerUtils();
				String url=Config.getInstance().configFile.getAd().getInit().getServer().getContent().getIdAcqGroupDataPushURL();
				
				String onlineUrl=Config.getInstance().configFile.getAd().getInit().getServer().getContent().getIdOnlineStatusPushURL();
				
				String path="";
				path=stringManagerUtils.getFilePath(deviceName+"_01.json","example/");
				String data=stringManagerUtils.readFile(path,"utf-8");
				
				path=stringManagerUtils.getFilePath(deviceName+"_02.json","example/");
				String data2=stringManagerUtils.readFile(path,"utf-8");
				
				
				int i=0;
				while(true){
					try {
						if("srp01".equalsIgnoreCase(deviceName)){
							int index=i%30+1;
							String indexStr=index<10?("0"+index):(index+"");
							path=stringManagerUtils.getFilePath(deviceName+"_"+indexStr+".json","example/");
							data=stringManagerUtils.readFile(path,"utf-8");
							StringManagerUtils.sendPostMethod(url, data,"utf-8",0,0);
						}else{
							if(i%2==0){
								StringManagerUtils.sendPostMethod(url, data,"utf-8",0,0);
							}else{
								StringManagerUtils.sendPostMethod(url, data2,"utf-8",0,0);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					i++;
					Thread.sleep(1000*cycle);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("static-access")
	public static void exampleDataManage(){
		if(Config.getInstance().configFile.getAp().getOthers().getSimulateAcqEnable()){
			try {
				int sendCycle=Config.getInstance().configFile.getAp().getOthers().getSendCycle();
				int timeDifference=Config.getInstance().configFile.getAp().getOthers().getTimeDifference();
//				sendCycle=60;
//				timeDifference=0;
				new ExampleDataManageThread("srp01",sendCycle,timeDifference*0).start();
				new ExampleDataManageThread("srp02",sendCycle,timeDifference*1).start();
				new ExampleDataManageThread("srp03",sendCycle,timeDifference*2).start();
				new ExampleDataManageThread("srp04",sendCycle,timeDifference*0).start();
				new ExampleDataManageThread("srp05",sendCycle,timeDifference*1).start();
				new ExampleDataManageThread("srp06",sendCycle,timeDifference*2).start();
				new ExampleDataManageThread("srp07",sendCycle,timeDifference*0).start();
				new ExampleDataManageThread("srp08",sendCycle,timeDifference*1).start();
//				new ExampleDataManageThread("srp09",sendCycle,timeDifference*2).start();
//				new ExampleDataManageThread("srp10",sendCycle,timeDifference*3).start();
				
//				new ExampleDataManageThread("srp11",sendCycle,timeDifference*0).start();
//				new ExampleDataManageThread("srp12",sendCycle,timeDifference*0).start();
				
				new ExampleDataManageThread("pcp01",sendCycle,timeDifference*0).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public static class DriverProbeResponse{
		public boolean ProtocolInitStatus;
		public boolean InstanceInitStatus;
		public boolean IDInitStatus;
		public boolean IPPortInitStatus;
		public boolean HttpServerInitStatus;
		public boolean SMSInitStatus;
		public String Ver;
		
		public boolean getProtocolInitStatus() {
			return ProtocolInitStatus;
		}
		public void setProtocolInitStatus(boolean protocolInitStatus) {
			ProtocolInitStatus = protocolInitStatus;
		}
		public boolean getIDInitStatus() {
			return IDInitStatus;
		}
		public void setIDInitStatus(boolean iDInitStatus) {
			IDInitStatus = iDInitStatus;
		}
		public boolean getHttpServerInitStatus() {
			return HttpServerInitStatus;
		}
		public void setHttpServerInitStatus(boolean httpServerInitStatus) {
			HttpServerInitStatus = httpServerInitStatus;
		}
		public String getVer() {
			return Ver;
		}
		public void setVer(String ver) {
			Ver = ver;
		}
		public boolean getInstanceInitStatus() {
			return InstanceInitStatus;
		}
		public void setInstanceInitStatus(boolean instanceInitStatus) {
			InstanceInitStatus = instanceInitStatus;
		}
		public boolean getSMSInitStatus() {
			return SMSInitStatus;
		}
		public void setSMSInitStatus(boolean sMSInitStatus) {
			SMSInitStatus = sMSInitStatus;
		}
		public boolean getIPPortInitStatus() {
			return IPPortInitStatus;
		}
		public void setIPPortInitStatus(boolean iPPortInitStatus) {
			IPPortInitStatus = iPPortInitStatus;
		}
	}
	
	@SuppressWarnings({ "resource" })
	public static int syncDataMappingTable(){
		Map<String, Object> dataModelMap=DataModelMap.getMapObject();
		List<String> acquisitionItemNameList=MemoryDataManagerTask.getAcquisitionItemNameList();
		List<String> protocolExtendedFieldNameList=MemoryDataManagerTask.getProtocolExtendedFieldNameList();
		
		
		int result=0;
		try {
			//删除重复映射
			try {
				int delCount=0;
				String queryDelItemSql="select count(1) from  (select t2.mappingcolumn,count(1) as cn from TBL_DATAMAPPING t2 group by t2.mappingcolumn) v where v.cn>1";
				List<Object[]> queryDelItemList=OracleJdbcUtis.query(queryDelItemSql);
				
				if(queryDelItemList!=null && queryDelItemList.size()>0){
					delCount=StringManagerUtils.stringToInteger(queryDelItemList.get(0)[0]+"");
				}
				if(delCount>0){
					String delSql="delete from TBL_DATAMAPPING t where t.id in ( select v2.id from"
							+ " (select max(t1.id) as id,t1.mappingcolumn from TBL_DATAMAPPING t1 "
							+ " where t1.mappingcolumn in ( select v.mappingcolumn from  "
							+ " (select t2.mappingcolumn,count(1) as cn from TBL_DATAMAPPING t2 group by t2.mappingcolumn) v "
							+ " where v.cn>1 )"
							+ " group by t1.mappingcolumn) v2"
							+ " )";
					result=OracleJdbcUtis.executeSqlUpdate(delSql);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//删除不存在的协议项映射
			try {
				String delSql="";
				List<String> itemNameList=new ArrayList<>();
				String queryItemSql="select t.name from TBL_DATAMAPPING t where upper(t.mappingcolumn) like 'C_CLOUMN%'";
				
				List<Object[]> queryItemList=OracleJdbcUtis.query(queryItemSql);
				for(int i=0;i<queryItemList.size();i++){
					itemNameList.add(queryItemList.get(i)[0]+"");
				}
				
				for(String itemName:itemNameList){
					if(!StringManagerUtils.existOrNot(acquisitionItemNameList, itemName, true)){
						delSql="delete from tbl_datamapping t "
								+ " where t.name='"+itemName+"'"
								+ " and upper(t.mappingcolumn) like 'C_CLOUMN%'";
						result=OracleJdbcUtis.executeSqlUpdate(delSql);
					}
				}
				
				//拓展字段
				itemNameList=new ArrayList<>();
				queryItemSql="select t.name from TBL_DATAMAPPING t where upper(t.mappingcolumn) like 'EXTENDEDFIELD_CLOUMN%'";
				
				queryItemList=OracleJdbcUtis.query(queryItemSql);
				for(int i=0;i<queryItemList.size();i++){
					itemNameList.add(queryItemList.get(i)[0]+"");
				}
				
				for(String itemName:itemNameList){
					if(!StringManagerUtils.existOrNot(protocolExtendedFieldNameList, itemName, true)){
						delSql="delete from tbl_datamapping t "
								+ " where t.name='"+itemName+"'"
								+ " and upper(t.mappingcolumn) like 'EXTENDEDFIELD_CLOUMN%'";
						result=OracleJdbcUtis.executeSqlUpdate(delSql);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			//添加映射
			Map<String,DataMapping> loadProtocolMappingColumnByTitleMap=MemoryDataManagerTask.getProtocolMappingColumnByTitle(0);
			Map<String,DataMapping> protocolExtendedFieldColumnByTitleMap=MemoryDataManagerTask.getProtocolMappingColumnByTitle(1);
			Map<String,DataMapping> loadProtocolMappingColumnMap=MemoryDataManagerTask.getProtocolMappingColumn();
			
			for(int i=0;i<acquisitionItemNameList.size();i++){
				if(!StringManagerUtils.dataMappingKeyExistOrNot(loadProtocolMappingColumnByTitleMap, acquisitionItemNameList.get(i),false)){
					String addMappingColumn="C_CLOUMN";
					int index=1;
					while(true){
						if(!StringManagerUtils.dataMappingKeyExistOrNot(loadProtocolMappingColumnMap, addMappingColumn+index,false)){
							addMappingColumn=addMappingColumn+index;
							DataMapping dataMapping=new DataMapping();
							dataMapping.setName(acquisitionItemNameList.get(i));
							dataMapping.setMappingColumn(addMappingColumn);
							loadProtocolMappingColumnMap.put(dataMapping.getMappingColumn(), dataMapping);
							loadProtocolMappingColumnByTitleMap.put(dataMapping.getName(), dataMapping);
							break;
						}
						index++;
					}
					
					if(!"C_CLOUMN".equalsIgnoreCase(addMappingColumn)){
						String addSql="insert into tbl_datamapping(name,mappingcolumn) values('"+acquisitionItemNameList.get(i)+"','"+addMappingColumn+"')";
						result=OracleJdbcUtis.executeSqlUpdate(addSql);
					}
				}
			}
			
			//拓展字段映射
			for(int i=0;i<protocolExtendedFieldNameList.size();i++){
				if(!StringManagerUtils.dataMappingKeyExistOrNot(protocolExtendedFieldColumnByTitleMap, protocolExtendedFieldNameList.get(i),false)){
					String addMappingColumn="EXTENDEDFIELD_CLOUMN";
					int index=1;
					while(true){
						if(!StringManagerUtils.dataMappingKeyExistOrNot(loadProtocolMappingColumnMap, addMappingColumn+index,false)){
							addMappingColumn=addMappingColumn+index;
							DataMapping dataMapping=new DataMapping();
							dataMapping.setName(protocolExtendedFieldNameList.get(i));
							dataMapping.setMappingColumn(addMappingColumn);
							loadProtocolMappingColumnMap.put(dataMapping.getMappingColumn(), dataMapping);
							protocolExtendedFieldColumnByTitleMap.put(dataMapping.getName(), dataMapping);
							break;
						}
						index++;
					}
					
					if(!"EXTENDEDFIELD_CLOUMN".equalsIgnoreCase(addMappingColumn)){
						String addSql="insert into tbl_datamapping(name,mappingcolumn) values('"+protocolExtendedFieldNameList.get(i)+"','"+addMappingColumn+"')";
						result=OracleJdbcUtis.executeSqlUpdate(addSql);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		MemoryDataManagerTask.loadProtocolMappingColumnByTitle(0);
		MemoryDataManagerTask.loadProtocolMappingColumnByTitle(1);
		MemoryDataManagerTask.loadProtocolMappingColumn();
		return result;
	}
	
	public static int syncProtocolRunStatusConfig(){
		ModbusProtocolConfig modbusProtocolConfig=MemoryDataManagerTask.getModbusProtocolConfig();
		if(modbusProtocolConfig==null){
			return 0;
		}
		int result=0;
		try {
			String deleteRunStatusConfigSql="delete from tbl_runstatusconfig t where t.id not in"
					+ "( select t2.id from tbl_runstatusconfig t2,tbl_datamapping t3 "
					+ " where t2.protocoltype=t3.protocoltype and t2.itemname=t3.name "
					+ " and t2.itemmappingcolumn=t3.mappingcolumn "
//					+ "and upper(t3.calcolumn)='RUNSTATUS'"
					+ ")";
			result=OracleJdbcUtis.executeSqlUpdate(deleteRunStatusConfigSql);
			MemoryDataManagerTask.loadProtocolRunStatusConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	

	public static int loadAcquisitionItemColumns(){
		MemoryDataManagerTask.loadAcquisitionItemNameList();
		MemoryDataManagerTask.loadProtocolExtendedFieldNameList();
		syncDataMappingTable();
		return 0;
	}
	
	@SuppressWarnings("static-access")
	public static void initProtocolConfig(String protocolName,String method){
		if(!StringManagerUtils.isNotNull(method)){
			method="update";
		}
		String initUrl=Config.getInstance().configFile.getAd().getInit().getProtocol();
		Gson gson = new Gson();
		ModbusProtocolConfig modbusProtocolConfig=MemoryDataManagerTask.getModbusProtocolConfig();
		InitProtocol initProtocol=null;
		if(modbusProtocolConfig!=null){
			if("delete".equalsIgnoreCase(method)){
				initProtocol=new InitProtocol();
				initProtocol.setProtocolName(protocolName);
				initProtocol.setMethod(method);
				StringManagerUtils.printLog("删除协议："+gson.toJson(initProtocol));
				if(initEnable){
					StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initProtocol),"utf-8",0,0);
				}
			}else{
				if(StringManagerUtils.isNotNull(protocolName)){
					for(int i=0;i<modbusProtocolConfig.getProtocol().size();i++){
						if(protocolName.equalsIgnoreCase(modbusProtocolConfig.getProtocol().get(i).getName())){
							initProtocol=new InitProtocol(modbusProtocolConfig.getProtocol().get(i));
							initProtocol.setMethod(method);
							StringManagerUtils.printLog("协议初始化："+gson.toJson(initProtocol));
							if(initEnable){
								StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initProtocol),"utf-8",0,0);
							}
							break;
						}
					}
				}else{
					for(int i=0;i<modbusProtocolConfig.getProtocol().size();i++){
						initProtocol=new InitProtocol(modbusProtocolConfig.getProtocol().get(i));
						initProtocol.setMethod(method);
						StringManagerUtils.printLog("协议初始化："+gson.toJson(initProtocol));
						if(initEnable){
							StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initProtocol),"utf-8",0,0);
						}
					}
				}
			}
			
			//同步数据库字段
			loadAcquisitionItemColumns();
		}
	}
	
	public static int initInstanceConfigByProtocolName(String protocolName,String method){
		String sql="select t.name from tbl_protocolinstance t,tbl_acq_unit_conf t2 where t.unitid=t2.id and t2.protocol='"+protocolName+"'";
		List<String> instanceList=new ArrayList<String>();
		List<Object[]> list=OracleJdbcUtis.query(sql);
		try {
			
			for(Object[] obj:list){
				instanceList.add(obj[0]+"");
			}
			if(instanceList.size()>0){
				initInstanceConfig(instanceList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int initInstanceConfigByAcqUnitName(String unitName,String method){
		String sql="select t.name from tbl_protocolinstance t,tbl_acq_unit_conf t2 where t.unitid=t2.id and t2.unit_name='"+unitName+"'";
		List<String> instanceList=new ArrayList<String>();
		List<Object[]> list=OracleJdbcUtis.query(sql);
		try {
			for(Object[] obj:list){
				instanceList.add(obj[0]+"");
			}
			if(instanceList.size()>0){
				initInstanceConfig(instanceList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int initInstanceConfigByAcqUnitId(String unitId,String method){
		String sql="select t.name from tbl_protocolinstance t where t.unitid="+unitId;
		List<String> instanceList=new ArrayList<String>();
		List<Object[]> list=OracleJdbcUtis.query(sql);
		try {
			for(Object[] obj : list){
				instanceList.add(obj[0]+"");
			}
			if(instanceList.size()>0){
				initInstanceConfig(instanceList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int initInstanceConfigByAcqGroupName(String groupName,String method){
		String sql="select distinct(t.name) from tbl_protocolinstance t,tbl_acq_unit_conf t2,tbl_acq_group2unit_conf t3,tbl_acq_group_conf t4 "
				+ " where t.unitid=t2.id and t2.id=t3.unitid and t3.groupid=t4.id and t4.group_name='"+groupName+"'";
		List<String> instanceList=new ArrayList<String>();
		List<Object[]> list=OracleJdbcUtis.query(sql);
		try {
			for(Object[] obj:list){
				instanceList.add(obj[0]+"");
			}
			if(instanceList.size()>0){
				initInstanceConfig(instanceList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int initInstanceConfigByAcqGroupId(String groupId,String method){
		String sql="select distinct(t.name) from tbl_protocolinstance t,tbl_acq_unit_conf t2,tbl_acq_group2unit_conf t3,tbl_acq_group_conf t4 where t.unitid=t2.id and t2.id=t3.unitid and t3.groupid=t4.id and t4.id="+groupId;
		List<String> instanceList=new ArrayList<String>();
		List<Object[]> list=OracleJdbcUtis.query(sql);
		try {
			for(Object[] obj:list){
				instanceList.add(obj[0]+"");
			}
			if(instanceList.size()>0){
				initInstanceConfig(instanceList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	@SuppressWarnings("static-access")
	public static int initInstanceConfig(List<String> instanceList,String method){
		String initUrl=Config.getInstance().configFile.getAd().getInit().getInstance();
		Gson gson = new Gson();
		int result=0;
		String instances=StringManagerUtils.joinStringArr2(instanceList, ",");
		if(!StringManagerUtils.isNotNull(method)){
			method="update";
		}
		
		if("delete".equalsIgnoreCase(method)){
			for(int i=0;instanceList!=null&&i<instanceList.size();i++){
				InitInstance initInstance=new InitInstance();
				initInstance.setInstanceName(instanceList.get(i));
				initInstance.setMethod(method);
				StringManagerUtils.printLog("删除实例："+gson.toJson(initInstance));
				if(initEnable){
					StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initInstance),"utf-8",0,0);
				}
			}
		}else{
			String instanceSql="select t.name,t.acqprotocoltype,t.ctrlprotocoltype,"
					+ " t.SignInPrefixSuffixHex,t.signinprefix,t.signinsuffix,t.SignInIDHex,"
					+ " t.HeartbeatPrefixSuffixHex,t.heartbeatprefix,t.heartbeatsuffix,"
					+ " t.packetsendinterval,"
					+ " t3.name "
					+ " from tbl_protocolinstance t "
					+ " left outer join tbl_acq_unit_conf t2 on t.unitid=t2.id "
					+ " left outer join tbl_protocol t3 on t2.protocol=t3.name"
					+ " where 1=1";
			String sql="select t5.name as instanceName,t2.id as groupId,t2.group_name,t2.type,t2.grouptiminginterval,"
					+ " t.itemname,t.itemcode, t6.name as protocolName "
					+ " from tbl_acq_item2group_conf t,"
					+ " tbl_acq_group_conf t2,"
					+ " tbl_acq_group2unit_conf t3,"
					+ " tbl_acq_unit_conf t4,"
					+ " tbl_protocolinstance t5,"
					+ " tbl_protocol t6 "
					+ " where t.groupid=t2.id and t2.id=t3.groupid and t3.unitid=t4.id and t4.id=t5.unitid and t4.protocol=t6.name ";
			
			
			if(StringManagerUtils.isNotNull(instances)){
				sql+=" and t5.name in("+instances+")";
				instanceSql+=" and t.name in("+instances+")";
			}
			sql+= " order by t5.sort,t.groupid,t.id";
			instanceSql+=" order by t.sort";
			
			List<Object[]> instanceQueryList=OracleJdbcUtis.query(instanceSql);
			List<Object[]> itemsQueryList=OracleJdbcUtis.query(sql);
			
			
			
			if(instanceQueryList!=null && instanceQueryList.size()>0){
				Map<String,InitInstance> InstanceListMap=new LinkedHashMap<String,InitInstance>();
				
				
				for(Object[] obj:instanceQueryList){
					InitInstance initInstance=new InitInstance();
					initInstance.setMethod(method);
					initInstance.setInstanceName(obj[0]+"");
					initInstance.setProtocolName(obj[11]+"");
					initInstance.setAcqProtocolType(obj[1]+"");
					initInstance.setCtrlProtocolType(obj[2]+"");
					
					initInstance.setSignInPrefixSuffixHex(StringManagerUtils.stringToInteger(obj[3]+"")==1);
					initInstance.setSignInPrefix((obj[4]+"").replaceAll("null", ""));
					initInstance.setSignInSuffix((obj[5]+"").replaceAll("null", ""));
					initInstance.setSignInIDHex(StringManagerUtils.stringToInteger(obj[6]+"")==1);
					
					initInstance.setHeartbeatPrefixSuffixHex(StringManagerUtils.stringToInteger(obj[7]+"")==1);
					initInstance.setHeartbeatPrefix((obj[8]+"").replaceAll("null", ""));
					initInstance.setHeartbeatSuffix((obj[9]+"").replaceAll("null", ""));
					
					initInstance.setPacketSendInterval(StringManagerUtils.stringToInteger(obj[10]+""));
					
					initInstance.setAcqGroup(new ArrayList<InitInstance.Group>());
					initInstance.setCtrlGroup(new ArrayList<InitInstance.Group>());
					
					InstanceListMap.put(initInstance.getInstanceName(), initInstance);
				}
				
				for(Entry<String, InitInstance> entry:InstanceListMap.entrySet()){
					InitInstance initInstance=entry.getValue();
					String key=entry.getKey();
					ModbusProtocolConfig.Protocol protocol=MemoryDataManagerTask.getProtocolByName(initInstance.getProtocolName());
					
					for(Object[] obj:itemsQueryList){
						String instanceName=obj[0]+"";
						int groupId=StringManagerUtils.stringToInteger(obj[1]+"");
						String groupName=obj[2]+"";
						int groupType=StringManagerUtils.stringToInteger(obj[3]+"");
						int groupTimingInterval=StringManagerUtils.stringToInteger(obj[4]+"");
						
						String itemName=obj[5]+"";
						String protocolName=obj[7]+"";
						if(instanceName.equalsIgnoreCase(initInstance.getInstanceName())){
							if(!initInstance.containGroup(groupType, groupId)){
								InitInstance.Group group=new InitInstance.Group();
								group.setId(groupId);
								group.setGroupTimingInterval(groupTimingInterval);
								group.setAddr(new ArrayList<Integer>());
								if(groupType==0){
									initInstance.getAcqGroup().add(group);
								}else if(groupType==1){
									initInstance.getCtrlGroup().add(group);
								}
							}
							InitInstance.Group group=initInstance.getGroup(groupType, groupId);
							if(group!=null){
								ModbusProtocolConfig.Items item=MemoryDataManagerTask.getProtocolItem(protocol, itemName);
								if(item!=null && !StringManagerUtils.existOrNot(group.getAddr(), item.getAddr())){
									group.getAddr().add(item.getAddr());
								}
							}
						}
					}
				}
				for(Entry<String, InitInstance> entry:InstanceListMap.entrySet()){
					InitInstance initInstance=entry.getValue();
					String key=entry.getKey();
					for(InitInstance.Group group:initInstance.getAcqGroup() ){
						Collections.sort(group.getAddr());
					}
					for(InitInstance.Group group:initInstance.getCtrlGroup() ){
						Collections.sort(group.getAddr());
					}
					
					try {
						StringManagerUtils.printLog("实例初始化："+gson.toJson(entry.getValue()));
						if(initEnable){
							StringManagerUtils.sendPostMethod(initUrl, gson.toJson(entry.getValue()),"utf-8",0,0);
						}
					}catch (Exception e) {
						continue;
					}
				}
				
			}
		}
		return result;
	}
	
	@SuppressWarnings("static-access")
	public static int initInstanceConfig2(List<String> instanceList,String method){
		String initUrl=Config.getInstance().configFile.getAd().getInit().getInstance();
		Gson gson = new Gson();
		int result=0;
		String instances=StringManagerUtils.joinStringArr2(instanceList, ",");
		if(!StringManagerUtils.isNotNull(method)){
			method="update";
		}
		
		if("delete".equalsIgnoreCase(method)){
			for(int i=0;instanceList!=null&&i<instanceList.size();i++){
				InitInstance initInstance=new InitInstance();
				initInstance.setInstanceName(instanceList.get(i));
				initInstance.setMethod(method);
				StringManagerUtils.printLog("删除实例："+gson.toJson(initInstance));
				if(initEnable){
					StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initInstance),"utf-8",0,0);
				}
			}
		}else{
			String sql="select t.name,t.acqprotocoltype,t.ctrlprotocoltype,"//1~3
					+ " t.SignInPrefixSuffixHex,t.signinprefix,t.signinsuffix,t.SignInIDHex,"//4~7
					+ " t.HeartbeatPrefixSuffixHex,t.heartbeatprefix,t.heartbeatsuffix,"//8~10
					+ " t.packetsendinterval,"//11
					+ " t2.protocol,t2.unit_code,t4.group_code,t4.grouptiminginterval,t4.type,"//12~16
//					+ " listagg(t5.itemname, ',') within group(order by t5.id ) key "//17
					+ " rtrim(xmlagg(xmlparse(content t5.itemname || ',' wellformed) order by t5.id).getclobval(),',' ) key"
					+ " from tbl_protocolinstance t "
					+ " left outer join tbl_acq_unit_conf t2 on t.unitid=t2.id "
					+ " left outer join tbl_acq_group2unit_conf t3 on t2.id=t3.unitid "
					+ " left outer join tbl_acq_group_conf t4 on t3.groupid=t4.id "
					+ " left outer join tbl_acq_item2group_conf t5 on t4.id=t5.groupid  "
					+ " where 1=1 ";
			if(StringManagerUtils.isNotNull(instances)){
				sql+=" and t.name in("+instances+")";
			}
			sql+= "group by t.name,t.acqprotocoltype,t.ctrlprotocoltype,"
					+ "t.SignInPrefixSuffixHex,t.signinprefix,t.signinsuffix,t.SignInIDHex,"
					+ "t.HeartbeatPrefixSuffixHex,t.heartbeatprefix,t.heartbeatsuffix,"
					+ "t.packetsendinterval,"
					+ "t2.protocol,t2.unit_code,t4.group_code,t4.grouptiminginterval,t4.type";
			Map<String,InitInstance> InstanceListMap=new HashMap<String,InitInstance>();
			ModbusProtocolConfig modbusProtocolConfig=MemoryDataManagerTask.getModbusProtocolConfig();
			Connection conn = null;   
			PreparedStatement pstmt = null;   
			ResultSet rs = null;
			conn=OracleJdbcUtis.getConnection();
			if(conn==null || modbusProtocolConfig==null){
	        	return -1;
	        }
			try {
				pstmt = conn.prepareStatement(sql);
				rs=pstmt.executeQuery();
				while(rs.next()){
					InitInstance initInstance=InstanceListMap.get(rs.getString(1));
					if(initInstance==null){
						initInstance=new InitInstance();
						initInstance.setMethod(method);
						initInstance.setInstanceName(rs.getString(1));
						initInstance.setProtocolName(rs.getString(12));
						initInstance.setAcqProtocolType(rs.getString(2));
						initInstance.setCtrlProtocolType(rs.getString(3));
						
						initInstance.setSignInPrefixSuffixHex(rs.getInt(4)==1);
						initInstance.setSignInPrefix(rs.getString(5)==null?"":rs.getString(5));
						initInstance.setSignInSuffix(rs.getString(6)==null?"":rs.getString(6));
						initInstance.setSignInIDHex(rs.getInt(7)==1);
						
						initInstance.setHeartbeatPrefixSuffixHex(rs.getInt(8)==1);
						initInstance.setHeartbeatPrefix(rs.getString(9)==null?"":rs.getString(9));
						initInstance.setHeartbeatSuffix(rs.getString(10)==null?"":rs.getString(10));
						
						initInstance.setPacketSendInterval(rs.getInt(11));
						
						initInstance.setAcqGroup(new ArrayList<InitInstance.Group>());
						initInstance.setCtrlGroup(new ArrayList<InitInstance.Group>());
					}
					if(StringManagerUtils.isNotNull(rs.getString(14))){
						InitInstance.Group group=new InitInstance.Group();
						group.setGroupTimingInterval(rs.getInt(15));
						group.setAddr(new ArrayList<Integer>());
						int groupType=rs.getInt(16);
						String itemsStr="";
						try {
							itemsStr=StringManagerUtils.CLOBtoString2(rs.getClob(17));
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						if(StringManagerUtils.isNotNull(itemsStr)){
							String[] itemsArr=itemsStr.split(",");
							for(int i=0;i<modbusProtocolConfig.getProtocol().size();i++){
								if(modbusProtocolConfig.getProtocol().get(i).getName().equalsIgnoreCase(rs.getString(12))){
									for(int j=0;j<itemsArr.length;j++){
										for(int k=0;k<modbusProtocolConfig.getProtocol().get(i).getItems().size();k++){
											if(itemsArr[j].equalsIgnoreCase(modbusProtocolConfig.getProtocol().get(i).getItems().get(k).getTitle())){
												if(!StringManagerUtils.existOrNot(group.getAddr(), modbusProtocolConfig.getProtocol().get(i).getItems().get(k).getAddr())){
													String rwType=modbusProtocolConfig.getProtocol().get(i).getItems().get(k).getRWType();
													if((groupType==1&&(!"r".equalsIgnoreCase(rwType))) || (groupType==0&&(!"w".equalsIgnoreCase(rwType)))){
														group.getAddr().add(modbusProtocolConfig.getProtocol().get(i).getItems().get(k).getAddr());
													}
												}
												break;
											}
										}
									}
									Collections.sort(group.getAddr());
									break;
								}
							}
						}
						if(groupType==1){//控制组
							initInstance.getCtrlGroup().add(group);
						}else{
							initInstance.getAcqGroup().add(group);
						}
					}
					InstanceListMap.put(initInstance.getInstanceName(), initInstance);
				}
				result=InstanceListMap.size();
				for(Entry<String, InitInstance> entry:InstanceListMap.entrySet()){
					try {
						StringManagerUtils.printLog("实例初始化："+gson.toJson(entry.getValue()));
						if(initEnable){
							StringManagerUtils.sendPostMethod(initUrl, gson.toJson(entry.getValue()),"utf-8",0,0);
						}
					}catch (Exception e) {
						continue;
					}
				}
			} catch (SQLException e) {
				StringManagerUtils.printLog("实例初始化sql："+sql);
				e.printStackTrace();
			} finally{
				OracleJdbcUtis.closeDBConnection(conn, pstmt, rs);
			}
		}
		return result;
	}
	
	//初始化短信实例
	@SuppressWarnings("static-access")
	public static int initSMSInstanceConfig(List<String> instanceList,String method){
		ModbusProtocolConfig modbusProtocolConfig=MemoryDataManagerTask.getModbusProtocolConfig();
		
		String initUrl=Config.getInstance().configFile.getAd().getInit().getInstance();
		Gson gson = new Gson();
		int result=0;
		String instances=StringManagerUtils.joinStringArr2(instanceList, ",");
		if(!StringManagerUtils.isNotNull(method)){
			method="update";
		}
		
		if("delete".equalsIgnoreCase(method)){
			for(int i=0;instanceList!=null&&i<instanceList.size();i++){
				InitInstance initInstance=new InitInstance();
				initInstance.setInstanceName(instanceList.get(i));
				initInstance.setMethod(method);
				StringManagerUtils.printLog("删除短信实例："+gson.toJson(initInstance));
				if(initEnable){
					StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initInstance),"utf-8",0,0);
				}
			}
		}else{
			String sql="select t.id,t.name,t.code,t.acqprotocoltype,t.ctrlprotocoltype,t.sort from tbl_protocolsmsinstance t where 1=1 ";
			if(StringManagerUtils.isNotNull(instances)){
				sql+=" and t.name in("+instances+")";
			}
			sql+= " order by t.sort";
			Connection conn = null;   
			PreparedStatement pstmt = null;   
			ResultSet rs = null;
			conn=OracleJdbcUtis.getConnection();
			if(conn==null || modbusProtocolConfig==null){
	        	return -1;
	        }
			try {
				pstmt = conn.prepareStatement(sql);
				rs=pstmt.executeQuery();
				while(rs.next()){
					InitInstance initInstance=new InitInstance();
					initInstance.setMethod(method);
					initInstance.setInstanceName(rs.getString(2));
					initInstance.setAcqProtocolType(rs.getString(4));
					initInstance.setCtrlProtocolType(rs.getString(5));
					StringManagerUtils.printLog("短信实例初始化："+gson.toJson(initInstance));
					if(initEnable){
						StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initInstance),"utf-8",0,0);
					}
				}
			} catch (SQLException e) {
				StringManagerUtils.printLog("ID短信实例初始化sql："+sql);
				e.printStackTrace();
			} finally{
				OracleJdbcUtis.closeDBConnection(conn, pstmt, rs);
			}
		}
		return result;
	}
	
	@SuppressWarnings({ "static-access" })
	public static int initDriverAcquisitionInfoConfig(List<String> wellList,int condition,String method){
		try{
			ThreadPool executor = new ThreadPool("adInit",
					Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getCorePoolSize(), 
					Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getMaximumPoolSize(), 
					Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getKeepAliveTime(), 
					TimeUnit.SECONDS, 
					Config.getInstance().configFile.getAp().getThreadPool().getInitIdAndIpPort().getWattingCount());
			if(!StringManagerUtils.isNotNull(method)){
				method="update";
			}
			List<DeviceInfo> deviceList=MemoryDataManagerTask.getDeviceInfo();
			for(int i=0;i<deviceList.size();i++){
				boolean matching=false;
				DeviceInfo deviceInfo=deviceList.get(i);
				if(wellList==null){
					matching=true;
				}else{
					if(condition==0){
						if(StringManagerUtils.existOrNot(wellList, deviceInfo.getId()+"", false)){
							matching=true;
						}
					}else if(condition==1){
						if(StringManagerUtils.existOrNot(wellList, deviceInfo.getDeviceName()+"", false)){
							matching=true;
						}
					}
				}
				
				if(matching){
					executor.execute(new InitIdAndIPPortThread(deviceInfo, 0,initEnable,method));
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			
		}
		return 0;
	}
	
	public static int initDriverAcquisitionInfoConfigByProtocolName(String protocolName,String method){
		List<String> wellList=new ArrayList<String>();
		String sql="";
		try {
			sql="select t.id from tbl_device t where t.instancecode in ( select t2.code from tbl_protocolinstance t2,tbl_acq_unit_conf t3 where t2.unitid=t3.id and t3.protocol='"+protocolName+"' )";
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initDriverAcquisitionInfoConfig(wellList,0,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	@SuppressWarnings("resource")
	public static int initDriverAcquisitionInfoConfigByProtocolInstance(String instanceCode,String method){
		List<String> wellList=new ArrayList<String>();
		try {
			String sql="select t.id from tbl_device t where t.instancecode='"+instanceCode+"'";
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initDriverAcquisitionInfoConfig(wellList,0,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	@SuppressWarnings("resource")
	public static int initDriverAcquisitionInfoConfigByProtocolInstanceId(String instanceId,String method){
		List<String> wellList=new ArrayList<String>();
		try {
			String sql="select t.id from tbl_device t,tbl_protocolinstance t2 where t.instancecode=t2.code and t2.id="+instanceId;
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initDriverAcquisitionInfoConfig(wellList,0,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	@SuppressWarnings("resource")
	public static int initDriverAcquisitionInfoConfigByAcqUnitId(String unitId,String method){
		List<String> wellList=new ArrayList<String>();
		try {
			String sql="select t.id from tbl_device t,tbl_protocolinstance t2,tbl_acq_unit_conf t3 where t.instancecode=t2.code and t2.unitid=t3.id and t3.id="+unitId;
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initDriverAcquisitionInfoConfig(wellList,0,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int initDriverAcquisitionInfoConfigByAcqGroupId(String groupId,String method){
		List<String> wellList=new ArrayList<String>();
		try {
			String sql="select t.id from tbl_device t,tbl_protocolinstance t2,tbl_acq_unit_conf t3 ,tbl_acq_group2unit_conf t4,tbl_acq_group_conf t5 "
					+ " where t.instancecode=t2.code and t2.unitid=t3.id and t3.id=t4.unitid and t4.groupid=t5.id and t5.id="+groupId;
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initDriverAcquisitionInfoConfig(wellList,0,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	@SuppressWarnings("static-access")
	public static int initSMSDevice(List<String> wellList,String method){
		String initUrl=Config.getInstance().configFile.getAd().getInit().getSMS();
		Gson gson = new Gson();
		int result=0;
		String wellName=StringManagerUtils.joinStringArr2(wellList, ",");
		if(!StringManagerUtils.isNotNull(method)){
			method="update";
		}
		String sql="select t.devicename,t.signinid,t2.name "
				+ " from tbl_smsdevice t,tbl_protocolsmsinstance t2 "
				+ " where t.instancecode=t2.code ";
		if("update".equalsIgnoreCase(method)){
			sql+= " and t.signinid is not null";
		}	
		if(StringManagerUtils.isNotNull(wellName)){
			sql+=" and t.devicename in("+wellName+")";
		}
		
		try {
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				InitId initId=new InitId();
				initId.setMethod(method);
				initId.setID(obj[1]+"");
				initId.setInstanceName(obj[2]+"");
				StringManagerUtils.printLog("短信设备初始化："+gson.toJson(initId));
				if(initEnable){
					StringManagerUtils.sendPostMethod(initUrl, gson.toJson(initId),"utf-8",0,0);
				}
			}
		} catch (Exception e) {
			result=-1;
			StringManagerUtils.printLog("ID初始化sql："+sql);
			e.printStackTrace();
		}
		return result;
	}
	
	public static int initSMSDeviceByInstanceCode(String instanceCode,String method){
		String sql="select t.deviceName from tbl_smsdevice t where t.instancecode='"+instanceCode+"'";
		List<String> wellList=new ArrayList<String>();
		try {
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initSMSDevice(wellList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static int initSMSDeviceByInstanceId(String instanceId,String method){
		String sql="select t.deviceName from tbl_smsdevice t,tbl_protocolsmsinstance t2 where t.instancecode=t2.code and t2.id="+instanceId;
		List<String> wellList=new ArrayList<String>();
		try {
			List<Object[]> list=OracleJdbcUtis.query(sql);
			for(Object[] obj:list){
				wellList.add(obj[0]+"");
			}
			if(wellList.size()>0){
				initSMSDevice(wellList,method);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static void initServerConfig() throws MalformedURLException{
		String initUrl=Config.getInstance().configFile.getAd().getInit().getServer().getUrl();
		StringBuffer json_buff = new StringBuffer();
		StringManagerUtils stringManagerUtils=new StringManagerUtils();
		try {

			json_buff.append("{");
			json_buff.append("\"IDOnlineStatusPushURL\":\""+Config.getInstance().configFile.getAd().getInit().getServer().getContent().getIdOnlineStatusPushURL()+"\",");
			json_buff.append("\"IDAcqGroupDataPushURL\":\""+Config.getInstance().configFile.getAd().getInit().getServer().getContent().getIdAcqGroupDataPushURL()+"\",");
			json_buff.append("\"IPPortOnlineStatusPushURL\":\""+Config.getInstance().configFile.getAd().getInit().getServer().getContent().getIpPortOnlineStatusPushURL()+"\",");
			json_buff.append("\"IPPortAcqGroupDataPushURL\":\""+Config.getInstance().configFile.getAd().getInit().getServer().getContent().getIpPortAcqGroupDataPushURL()+"\"");
			json_buff.append("}");
			StringManagerUtils.printLog("服务初始化："+json_buff.toString());
			if(initEnable){
				StringManagerUtils.sendPostMethod(initUrl,json_buff.toString(),"utf-8",0,0);
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void initWellCommStatusByOnlineProbe(){
		AdOnlineProbeResponseData adOnlineProbeResponseData =adOnlineProbe();
		if(adOnlineProbeResponseData!=null){
			if(adOnlineProbeResponseData.getOnlineID()!=null && adOnlineProbeResponseData.getOnlineID().size()>0 ){
				String initSRPCommSql="update tbl_acqdata_latest t set t.commstatus=1 "
						+ " where t.commstatus<>1 "
						+ " and t.deviceid in ( select t2.id from tbl_device t2 where t2.tcptype='TCP Client' and t2.signinid in("+StringManagerUtils.joinStringArr2(adOnlineProbeResponseData.getOnlineID(), ",")+") )";
				try {
					JDBCUtil.updateRecord(initSRPCommSql, null);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			if(adOnlineProbeResponseData.getOnlineIPPort()!=null && adOnlineProbeResponseData.getOnlineIPPort().size()>0 ){
				String initSRPCommSql="update tbl_acqdata_latest t set t.commstatus=1 "
						+ " where t.commstatus<>1 "
						+ " and t.deviceid in ( select t2.id from tbl_device t2 where t2.tcptype='TCP Server' and t2.ipport in("+StringManagerUtils.joinStringArr2(adOnlineProbeResponseData.getOnlineIPPort(), ",")+") )";
				try {
					JDBCUtil.updateRecord(initSRPCommSql, null);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	public static int initWellCommStatus(){
		String initSRPCommSql="update tbl_acqdata_latest t set t.commstatus=0 ";
		int result=0;
		try {
			result = JDBCUtil.updateRecord(initSRPCommSql, null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static int initWellDaliyData(){
		
		int result=0;
		try {
			result = JDBCUtil.callProcedure("prd_init_device_daily", null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static int initWellSRPCommStatus(){
		String initCommSql="update tbl_srpacqdata_latest t set t.upcommstatus=0,t.downcommstatus=0";
		int result=0;
		try {
			result = JDBCUtil.updateRecord(initCommSql, null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@SuppressWarnings("static-access")
	public static void sendDeviceOfflineInfo(List<String> wellList){
		if(wellList==null || wellList.size()==0){
			return;
		}
		try{
			StringManagerUtils stringManagerUtils=new StringManagerUtils();
			List<DeviceInfo> deviceList=MemoryDataManagerTask.getDeviceInfo();
			String url=stringManagerUtils.getProjectUrl()+"/api/acq/id/online";
			String key="ID";
			if(deviceList!=null){
				for(int i=0;i<deviceList.size();i++){
					StringBuffer json_buff = new StringBuffer();
					DeviceInfo deviceInfo=deviceList.get(i);
					if(StringManagerUtils.existOrNot(wellList, deviceInfo.getId()+"", false)){
						if("TCPServer".equalsIgnoreCase(deviceInfo.getTcpType().replaceAll(" ", ""))){
							url=stringManagerUtils.getProjectUrl()+"/api/acq/ipport/online";
							key="IPPort";
						}else{
							url=stringManagerUtils.getProjectUrl()+"/api/acq/id/online";
							key="ID";
						}
						json_buff.append("{");
						json_buff.append("\""+key+"\":\""+deviceInfo.getSignInId()+"\",");
						json_buff.append("\"Slave\":\""+deviceInfo.getSlave()+"\",");
						json_buff.append("\"Status\":false");
						json_buff.append("}");
						StringManagerUtils.sendPostMethod(url, json_buff.toString(),"utf-8",0,0);
					}
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			
		}
	}
}	
