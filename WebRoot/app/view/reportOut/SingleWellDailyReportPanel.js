var singleWellRangeReportHelper=null;
var singleWellDailyReportHelper=null;
Ext.define("AP.view.reportOut.SingleWellDailyReportPanel", {
    extend: 'Ext.panel.Panel',
    alias: 'widget.SingleWellDailyReportPanel',
    layout: 'fit',
    id: 'SingleWellDailyReportPanel_view',
    border: false,
    initComponent: function () {
        var me = this;
        var deviceCombStore = new Ext.data.JsonStore({
        	pageSize:defaultWellComboxSize,
            fields: [{
                name: "boxkey",
                type: "string"
            }, {
                name: "boxval",
                type: "string"
            }],
            proxy: {
            	url: context + '/wellInformationManagerController/loadWellComboxList',
                type: "ajax",
                actionMethods: {
                    read: 'POST'
                },
                reader: {
                    type: 'json',
                    rootProperty: 'list',
                    totalProperty: 'totals'
                }
            },
            autoLoad: true,
            listeners: {
                beforeload: function (store, options) {
                	var leftOrg_Id = Ext.getCmp('leftOrg_Id').getValue();
                    var deviceName = Ext.getCmp('SingleWellDailyReportPanelWellListCombo_Id').getValue();
                    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
                    var new_params = {
                        orgId: leftOrg_Id,
                        deviceType: deviceType,
                        deviceName: deviceName
                    };
                    Ext.apply(store.proxy.extraParams,new_params);
                }
            }
        });
        
        var deviceCombo = Ext.create(
                'Ext.form.field.ComboBox', {
                    fieldLabel: loginUserLanguageResource.deviceName,
                    id: "SingleWellDailyReportPanelWellListCombo_Id",
                    labelWidth: getLabelWidth(loginUserLanguageResource.deviceName,loginUserLanguage),
                    width: (getLabelWidth(loginUserLanguageResource.deviceName,loginUserLanguage)+110),
                    labelAlign: 'left',
                    queryMode: 'remote',
                    typeAhead: true,
                    store: deviceCombStore,
                    autoSelect: false,
                    editable: true,
                    triggerAction: 'all',
                    displayField: "boxval",
                    valueField: "boxkey",
                    pageSize:comboxPagingStatus,
                    minChars:0,
                    emptyText: '--'+loginUserLanguageResource.all+'--',
                    blankText: '--'+loginUserLanguageResource.all+'--',
                    listeners: {
                        expand: function (sm, selections) {
                            deviceCombo.getStore().loadPage(1); // 加载井下拉框的store
                        },
                        select: function (combo, record, index) {
                        	Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").setValue(-1);
                        	Ext.getCmp("SingleWellDailyReportGridPanel_Id").getStore().load();
                        }
                    }
                });
        
        
        
        Ext.apply(me, {
            tbar: [{
                xtype: 'button',
                text: loginUserLanguageResource.refresh,
                iconCls: 'note-refresh',
                hidden:false,
                handler: function (v, o) {
                	var gridPanel = Ext.getCmp("SingleWellDailyReportGridPanel_Id");
        			if (isNotVal(gridPanel)) {
        				gridPanel.getStore().load();
        			}else{
        				Ext.create('AP.store.reportOut.SingleWellDailyReportWellListStore');
        			}
                }
    		},'-',deviceCombo,'-', {
                xtype: 'datefield',
                anchor: '100%',
                fieldLabel: loginUserLanguageResource.date,
                labelWidth: getLabelWidth(loginUserLanguageResource.date,loginUserLanguage),
                width: (getLabelWidth(loginUserLanguageResource.date,loginUserLanguage)+100),
                format: 'Y-m-d',
                id: 'SingleWellDailyReportStartDate_Id',
                listeners: {
                	select: function (combo, record, index) {
                        try {
                        	if(Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id=='SingleWellDailyReportTabPanel_id'){
                            	Ext.getCmp("SingleWellDailyReportDate_Id").setValue("");
                            	Ext.getCmp("SingleWellDailyReportDate_Id").setRawValue("");
                        	}
                        	
                        	CreateSingleWellReportTable();
                        	CreateSingleWellReportCurve();
                        } catch (ex) {
                            Ext.Msg.alert(loginUserLanguageResource.tip, loginUserLanguageResource.dataQueryFailure);
                        }
                    }
                }
            },{
                xtype: 'datefield',
                anchor: '100%',
                hidden: false,
                fieldLabel: loginUserLanguageResource.timeTo,
                labelWidth: getLabelWidth(loginUserLanguageResource.timeTo,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.timeTo,loginUserLanguage)+95,
                format: 'Y-m-d ',
                id: 'SingleWellDailyReportEndDate_Id',
                value: new Date(),
                listeners: {
                	select: function (combo, record, index) {
                        try {
                        	if(Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id=='SingleWellDailyReportTabPanel_id'){
                            	Ext.getCmp("SingleWellDailyReportDate_Id").setValue("");
                            	Ext.getCmp("SingleWellDailyReportDate_Id").setRawValue("");
                        	}
                        	CreateSingleWellReportTable();
                        	CreateSingleWellReportCurve();
                        } catch (ex) {
                            Ext.Msg.alert(loginUserLanguageResource.tip, loginUserLanguageResource.dataQueryFailure);
                        }
                    }
                }
            },'-',{
                xtype: 'button',
                text: loginUserLanguageResource.search,
                iconCls: 'search',
                hidden:false,
                handler: function (v, o) {
                	if(Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id=='SingleWellDailyReportTabPanel_id'){
                    	Ext.getCmp("SingleWellDailyReportDate_Id").setValue("");
                    	Ext.getCmp("SingleWellDailyReportDate_Id").setRawValue("");
                	}
                	CreateSingleWellReportTable();
                	CreateSingleWellReportCurve();
                }
    		},{
            	id: 'SingleWellDailyReportDeviceListSelectRow_Id',
            	xtype: 'textfield',
                value: -1,
                hidden: true
             }],
            layout: 'border',
            border: false,
            items: [{
            	region: 'west',
            	width: '20%',
            	title: loginUserLanguageResource.deviceList,
            	id: 'SingleWellDailyReportWellListPanel_Id',
            	collapsible: true, // 是否可折叠
                collapsed:false,//是否折叠
                split: true, // 竖折叠条
            	layout: "fit"
            },{
            	region: 'center',
            	xtype: 'tabpanel',
            	id:"SingleWellReportTabPanel_Id",
                activeTab: 0,
                border: false,
                tabPosition: 'top',
                items: [{
                	id:'SingleWellDailyReportTabPanel_id',
                	title:loginUserLanguageResource.hourlyReport,
                	iconCls: 'check3',
                	layout:'border',
                	border: false,
                	items:[{
                		region:'north',
                		height:'50%',
                		title:loginUserLanguageResource.reportCurve,
                		collapsible: true, // 是否可折叠
                        collapsed:false,//是否折叠
                        split: true, // 竖折叠条
                        id:'SingleWellDailyReportCurvePanel_id',
                        html: '<div id="SingleWellDailyReportCurveDiv_Id" style="width:100%;height:100%;"></div>',
                        listeners: {
                            resize: function (abstractcomponent, adjWidth, adjHeight, options) {
                                if ($("#SingleWellDailyReportCurveDiv_Id").highcharts() != undefined) {
                                	highchartsResize("SingleWellDailyReportCurveDiv_Id");
                                }
                            }
                        }
                	},{
                		region: 'center',
                		title:loginUserLanguageResource.reportData,
                        layout: "fit",
                    	id:'SingleWellDailyReportPanel_id',
                    	tbar:[{
                            xtype: 'button',
                            text: loginUserLanguageResource.forward,
                            iconCls: 'forward',
                            id:'SingleWellDailyReportForwardBtn_Id',
                            handler: function (v, o) {
                            	var str = Ext.getCmp("SingleWellDailyReportDate_Id").rawValue;
                            	var startDate = new Date(Date.parse(str .replace(/-/g, '/')));
                            	var day=-1;
                            	var value = startDate.getTime();
                            	value += day * (24 * 3600 * 1000);
                            	var endDate = new Date(value);
                            	Ext.getCmp("SingleWellDailyReportDate_Id").setValue(endDate);
                            	CreateSingleWellDailyReportTable();
                            	CreateSingleWellDailyReportCurve();
                            }
                        },'-',{
                            xtype: 'datefield',
                            anchor: '100%',
                            hidden: false,
                            editable:false,
                            readOnly:true,
                            width: 90,
                            format: 'Y-m-d ',
                            id: 'SingleWellDailyReportDate_Id',
                            listeners: {
                            	change ( thisField, newValue, oldValue, eOpts )  {
                            		var startDateStr=Ext.getCmp("SingleWellDailyReportStartDate_Id").rawValue;
                            		var endDateStr=Ext.getCmp("SingleWellDailyReportEndDate_Id").rawValue;
                            		var reportDateStr=Ext.getCmp("SingleWellDailyReportDate_Id").rawValue;
                            		
                            		var startDate = new Date(Date.parse(startDateStr .replace(/-/g, '/'))).getTime();
                            		var endDate = new Date(Date.parse(endDateStr .replace(/-/g, '/'))).getTime();
                            		var reportDate = new Date(Date.parse(reportDateStr .replace(/-/g, '/'))).getTime();
                            		
                            		
                            		if(reportDate>startDate){
                            			Ext.getCmp("SingleWellDailyReportForwardBtn_Id").enable();
                            		}else{
                            			Ext.getCmp("SingleWellDailyReportForwardBtn_Id").disable();
                            		}
                            		
                            		if(reportDate<endDate){
                            			Ext.getCmp("SingleWellDailyReportBackwardsBtn_Id").enable();
                            		}else{
                            			Ext.getCmp("SingleWellDailyReportBackwardsBtn_Id").disable();
                            		}
                            	}
                            }
                        },'-',{
                            xtype: 'button',
                            text: loginUserLanguageResource.backward,
                            id:'SingleWellDailyReportBackwardsBtn_Id',
                            iconCls: 'backwards',
                            handler: function (v, o) {
                            	var str = Ext.getCmp("SingleWellDailyReportDate_Id").rawValue;
                            	var startDate = new Date(Date.parse(str .replace(/-/g, '/')));
                            	var day=1;
                            	var value = startDate .getTime();
                            	value += day * (24 * 3600 * 1000);
                            	var endDate = new Date(value);
                            	Ext.getCmp("SingleWellDailyReportDate_Id").setValue(endDate);
                            	CreateSingleWellDailyReportTable();
                            	CreateSingleWellDailyReportCurve();
                            }
                        },'-',{
                        	xtype : "combobox",
            				fieldLabel : loginUserLanguageResource.interval,
                            labelWidth: getLabelWidth(loginUserLanguageResource.interval,loginUserLanguage),
                            width: (getLabelWidth(loginUserLanguageResource.interval,loginUserLanguage)+100),
            				id : 'SingleWellDailyReportIntervalComb_Id',
            				triggerAction : 'all',
            				selectOnFocus : true,
            			    forceSelection : true,
            			    value:2,
            			    allowBlank: false,
            				editable : false,
            				store : new Ext.data.SimpleStore({
            							fields : ['value', 'text'],
            							data : [[2, loginUserLanguageResource.twoHours],[1, loginUserLanguageResource.oneHour]]
            						}),
            				displayField : 'text',
            				valueField : 'value',
            				queryMode : 'local',
            				emptyText : loginUserLanguageResource.selectInterval,
            				blankText : loginUserLanguageResource.selectInterval,
            				listeners : {
            					select:function(v,o){
            						CreateSingleWellDailyReportTable();
                                	CreateSingleWellDailyReportCurve();
            					}
            				}
                        }, '->',{
                            xtype: 'button',
                            text: loginUserLanguageResource.exportData,
                            iconCls: 'export',
                            handler: function (v, o) {
                            	ExportSingleWellDailyReportData();
                            }
                        },'-',{
                            xtype: 'button',
                            text: loginUserLanguageResource.bulkExportData,
                            iconCls: 'export',
                            handler: function (v, o) {
                            	batchExportSingleWellDailyReportData();
                            }
                        },'-',{
                            xtype: 'button',
                            text: loginUserLanguageResource.save,
                            iconCls: 'save',
                            disabled: loginUserRoleReportEdit!=1,
                            handler: function (v, o) {
                            	singleWellDailyReportHelper.saveData();
                            }
                        },'-', {
                            id: 'SingleWellDailyReportTotalCount_Id',
                            xtype: 'component',
                            tpl: loginUserLanguageResource.totalCount + ': {count}',
                            style: 'margin-right:15px'
                        }],
                        html:'<div class="SingleWellDailyReportContainer" style="width:100%;height:100%;"><div class="con" id="SingleWellDailyReportDiv_id"></div></div>',
                        listeners: {
                        	resize: function (thisPanel, width, height, oldWidth, oldHeight, eOpts) {
                        		if(singleWellDailyReportHelper!=null && singleWellDailyReportHelper.hot!=undefined){
                        			var newWidth=width;
                            		var newHeight=height-22-1;//减去工具条高度
                            		var header=thisPanel.getHeader();
                            		if(header){
                            			newHeight=newHeight-header.lastBox.height-2;
                            		}
                            		singleWellDailyReportHelper.hot.updateSettings({
                            			width:newWidth,
                            			height:newHeight
                            		});
                            	}
                        	}
                        }
                	}]
                },{
                	id:'SingleWellRangeReportTabPanel_id',
                	title:loginUserLanguageResource.dailyReport,
                	layout:'border',
                	border: false,
                	items:[{
                		region:'north',
                		height:'50%',
                		title:loginUserLanguageResource.reportCurve,
                		collapsible: true, // 是否可折叠
                        collapsed:false,//是否折叠
                        split: true, // 竖折叠条
                        id:'SingleWellRangeReportCurvePanel_id',
                        html: '<div id="SingleWellRangeReportCurveDiv_Id" style="width:100%;height:100%;"></div>',
                        listeners: {
                            resize: function (abstractcomponent, adjWidth, adjHeight, options) {
                                if ($("#SingleWellRangeReportCurveDiv_Id").highcharts() != undefined) {
                                	highchartsResize("SingleWellRangeReportCurveDiv_Id");
                                }
                            }
                        }
                	},{
                		region: 'center',
                		title:loginUserLanguageResource.reportData,
                        layout: "fit",
                    	id:'SingleWellRangeReportPanel_id',
                    	tbar:['->',{
                            xtype: 'button',
                            text: loginUserLanguageResource.exportData,
                            iconCls: 'export',
                            handler: function (v, o) {
                            	ExportSingleWellRangeReportData();
                            }
                        },'-',{
                            xtype: 'button',
                            text: loginUserLanguageResource.bulkExportData,
                            iconCls: 'export',
                            handler: function (v, o) {
                            	batchExportSingleWellRangeReportData();
                            }
                        },'-',{
                            xtype: 'button',
                            text: loginUserLanguageResource.save,
                            iconCls: 'save',
                            disabled: loginUserRoleReportEdit!=1,
                            handler: function (v, o) {
                            	singleWellRangeReportHelper.saveData();
                            }
                        },'-', {
                            id: 'SingleWellRangeReportTotalCount_Id',
                            xtype: 'component',
                            tpl: loginUserLanguageResource.totalCount + ': {count}',
                            style: 'margin-right:15px'
                        }],
                        html:'<div class="SingleWellRangeReportContainer" style="width:100%;height:100%;"><div class="con" id="SingleWellRangeReportDiv_id"></div></div>',
                        listeners: {
                        	resize: function (thisPanel, width, height, oldWidth, oldHeight, eOpts) {
                        		if(singleWellRangeReportHelper!=null && singleWellRangeReportHelper.hot!=undefined){
                        			var newWidth=width;
                        			var newHeight=height-22-1;//减去工具条高度
                            		var header=thisPanel.getHeader();
                            		if(header){
                            			newHeight=newHeight-header.lastBox.height-2;
                            		}
                            		singleWellRangeReportHelper.hot.updateSettings({
                            			width:newWidth,
                            			height:newHeight
                            		});
                            	}
                        	}
                        }
                	}]
                }],
                listeners: {
                	beforetabchange ( tabPanel, newCard, oldCard, eOpts ) {
        				oldCard.setIconCls(null);
        				newCard.setIconCls('check3');
        			},
        			tabchange: function (tabPanel, newCard, oldCard, obj) {
                    	if(Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id=='SingleWellDailyReportTabPanel_id'){
                        	Ext.getCmp("SingleWellDailyReportDate_Id").setValue("");
                        	Ext.getCmp("SingleWellDailyReportDate_Id").setRawValue("");
                    	}
                    	CreateSingleWellReportTable();
                		CreateSingleWellReportCurve();
                    }
                }
            }]

        });
        me.callParent(arguments);
    }
});

function CreateSingleWellReportTable(){
	var SingleWellReportTabPanelActiveId=Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id;
	if(SingleWellReportTabPanelActiveId=='SingleWellDailyReportTabPanel_id'){
		CreateSingleWellDailyReportTable();
	}else if(SingleWellReportTabPanelActiveId=='SingleWellRangeReportTabPanel_id'){
		CreateSingleWellRangeReportTable();
	}
}


function CreateSingleWellRangeReportTable(){
	var orgId = Ext.getCmp('leftOrg_Id').getValue();
    var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
    var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
    
    var deviceName='';
    var deviceId=0;
    var calculateType=0;
    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
    var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
    if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
    	deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
    	deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
    	calculateType=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.calculateType;
    }
    
    Ext.getCmp("SingleWellRangeReportPanel_id").el.mask(loginUserLanguageResource.loading).show();
	Ext.Ajax.request({
		method:'POST',
		url:context + '/reportDataMamagerController/getSingleWellRangeReportData',
		success:function(response) {
			Ext.getCmp("SingleWellRangeReportPanel_id").getEl().unmask();
			var result =  Ext.JSON.decode(response.responseText);
			
			var startDate=Ext.getCmp('SingleWellDailyReportStartDate_Id');
            if(startDate.rawValue==''||null==startDate.rawValue){
            	startDate.setValue(result.startDate);
            }
            var endDate=Ext.getCmp('SingleWellDailyReportEndDate_Id');
            if(endDate.rawValue==''||null==endDate.rawValue){
            	endDate.setValue(result.endDate);
            }
            
			if(singleWellRangeReportHelper!=null){
				if(singleWellRangeReportHelper.hot!=undefined){
					singleWellRangeReportHelper.hot.destroy();
				}
				singleWellRangeReportHelper=null;
			}
			if(result.success){
				if(singleWellRangeReportHelper==null || singleWellRangeReportHelper.hot==undefined){
					singleWellRangeReportHelper = SingleWellRangeReportHelper.createNew("SingleWellRangeReportDiv_id","SingleWellRangeReportContainer",result.template,result.data,result.columns);
					singleWellRangeReportHelper.createTable();
				}
			}else{
				$("#SingleWellRangeReportDiv_id").html('');
			}
			
			Ext.getCmp("SingleWellRangeReportTotalCount_Id").update({count: result.data.length});
		},
		failure:function(){
			Ext.MessageBox.alert(loginUserLanguageResource.error,loginUserLanguageResource.errorInfo);
		},
		params: {
			orgId: orgId,
			deviceId:deviceId,
			deviceName: deviceName,
			calculateType: calculateType,
			startDate: startDate,
			endDate: endDate,
			reportType: 0,
            deviceType:deviceType
        }
	});
};


var SingleWellRangeReportHelper = {
	    createNew: function (divId, containerid,templateData,contentData,columns) {
	        var singleWellRangeReportHelper = {};
	        singleWellRangeReportHelper.templateData=templateData;
	        singleWellRangeReportHelper.contentData=contentData;
	        singleWellRangeReportHelper.columns=columns;
	        singleWellRangeReportHelper.get_data = {};
	        singleWellRangeReportHelper.data=[];
	        singleWellRangeReportHelper.sourceData=[];
	        singleWellRangeReportHelper.hot = '';
	        singleWellRangeReportHelper.container = document.getElementById(divId);
	        singleWellRangeReportHelper.columnCount=0;
	        singleWellRangeReportHelper.editData={};
	        singleWellRangeReportHelper.contentUpdateList = [];
	        
	        singleWellRangeReportHelper.colWidths=[];
	        if(loginUserLanguage=='zh_CN'){
	        	singleWellRangeReportHelper.colWidths=singleWellRangeReportHelper.templateData.columnWidths_zh_CN
	        }else if(loginUserLanguage=='en'){
	        	singleWellRangeReportHelper.colWidths=singleWellRangeReportHelper.templateData.columnWidths_en
		    }else if(loginUserLanguage=='ru'){
	        	singleWellRangeReportHelper.colWidths=singleWellRangeReportHelper.templateData.columnWidths_ru
		    }
	        
	        singleWellRangeReportHelper.initData=function(){
	        	singleWellRangeReportHelper.data=[];
	        	for(var i=0;i<singleWellRangeReportHelper.templateData.header.length;i++){
	        		if(loginUserLanguage=='zh_CN'){
	        			singleWellRangeReportHelper.templateData.header[i].title=singleWellRangeReportHelper.templateData.header[i].title_zh_CN;
	        		}else if(loginUserLanguage=='en'){
	        			singleWellRangeReportHelper.templateData.header[i].title=singleWellRangeReportHelper.templateData.header[i].title_en;
	        		}else if(loginUserLanguage=='ru'){
	        			singleWellRangeReportHelper.templateData.header[i].title=singleWellRangeReportHelper.templateData.header[i].title_ru;
	        		}
	        		
	        		singleWellRangeReportHelper.templateData.header[i].title.push('');
	        		singleWellRangeReportHelper.columnCount=singleWellRangeReportHelper.templateData.header[i].title.length;
	        		
	        		var valueArr=[];
	        		var sourceValueArr=[];
	        		for(var j=0;j<singleWellRangeReportHelper.templateData.header[i].title.length;j++){
	        			valueArr.push(singleWellRangeReportHelper.templateData.header[i].title[j]);
	        			sourceValueArr.push(singleWellRangeReportHelper.templateData.header[i].title[j]);
	        		}
	        		
	        		singleWellRangeReportHelper.data.push(valueArr);
	        		singleWellRangeReportHelper.sourceData.push(sourceValueArr);
		        }
	        	for(var i=0;i<singleWellRangeReportHelper.contentData.length;i++){
	        		var valueArr=[];
	        		var sourceValueArr=[];
	        		for(var j=0;j<singleWellRangeReportHelper.contentData[i].length;j++){
	        			valueArr.push(singleWellRangeReportHelper.contentData[i][j]);
	        			sourceValueArr.push(singleWellRangeReportHelper.contentData[i][j]);
	        		}
	        		
	        		singleWellRangeReportHelper.data.push(valueArr);
		        	singleWellRangeReportHelper.sourceData.push(sourceValueArr);
		        }
	        	for(var i=singleWellRangeReportHelper.templateData.header.length;i<singleWellRangeReportHelper.data.length;i++){
	        		for(var j=0;j<singleWellRangeReportHelper.data[i].length;j++){
	        			var editable=false
	        			for(var k=0;k<singleWellRangeReportHelper.templateData.editable.length;k++){
	        				if( i>=singleWellRangeReportHelper.templateData.editable[k].startRow 
	                				&& i<=singleWellRangeReportHelper.templateData.editable[k].endRow
	                				&& j>=singleWellRangeReportHelper.templateData.editable[k].startColumn 
	                				&& j<=singleWellRangeReportHelper.templateData.editable[k].endColumn
	                		){
	        					editable=true;
	        					break;
	                		}
	        			}
	        			
	        			var value=singleWellRangeReportHelper.data[i][j];
		                if((!editable)&&value.length>12){
		                	value=value.substring(0, 11)+"...";
		                	singleWellRangeReportHelper.data[i][j]=value;
		                }
	        		}
	        	}
	        }
	        
	        singleWellRangeReportHelper.addStyle = function (instance, td, row, col, prop, value, cellProperties) {
	        	Handsontable.renderers.TextRenderer.apply(this, arguments);
	        	if(singleWellRangeReportHelper!=null && singleWellRangeReportHelper.hot!=null){
	        		for(var i=0;i<singleWellRangeReportHelper.templateData.header.length;i++){
		        		if(row==i){
		        			if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle)){
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.fontWeight)){
		        					td.style.fontWeight = singleWellRangeReportHelper.templateData.header[i].tdStyle.fontWeight;
		        				}
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.fontSize)){
		        					td.style.fontSize = singleWellRangeReportHelper.templateData.header[i].tdStyle.fontSize;
		        				}
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.fontFamily)){
//		        					td.style.fontFamily = singleWellRangeReportHelper.templateData.header[i].tdStyle.fontFamily;
		        				}
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.height)){
		        					td.style.height = singleWellRangeReportHelper.templateData.header[i].tdStyle.height;
		        				}
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.color)){
		        					td.style.color = singleWellRangeReportHelper.templateData.header[i].tdStyle.color;
		        				}
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.backgroundColor)){
		        					td.style.backgroundColor = singleWellRangeReportHelper.templateData.header[i].tdStyle.backgroundColor;
		        				}
		        				if(isNotVal(singleWellRangeReportHelper.templateData.header[i].tdStyle.textAlign)){
		        					td.style.textAlign = singleWellRangeReportHelper.templateData.header[i].tdStyle.textAlign;
		        				}
		        			}
		        			break;
		        		}
		        	}
	        		if(row>=singleWellRangeReportHelper.templateData.header.length){
	        			td.style.whiteSpace='nowrap'; //文本不换行
		            	td.style.overflow='hidden';//超出部分隐藏
		            	td.style.textOverflow='ellipsis';//使用省略号表示溢出的文本
	        		}
	        		
	        	}
	        }
	        
	        singleWellRangeReportHelper.addEditableColor = function (instance, td, row, col, prop, value, cellProperties) {
	             Handsontable.renderers.TextRenderer.apply(this, arguments);
	             td.style.color='#ff0000'; 
	             td.style.whiteSpace='nowrap'; //文本不换行
	             td.style.overflow='hidden';//超出部分隐藏
	             td.style.textOverflow='ellipsis';//使用省略号表示溢出的文本
	        }
	        
	        singleWellRangeReportHelper.hiddenColumn = function (instance, td, row, col, prop, value, cellProperties) {
	            Handsontable.renderers.TextRenderer.apply(this, arguments);
	            td.style.display = 'none';
	        }

	        // 实现标题居中
	        singleWellRangeReportHelper.titleCenter = function () {
	            $(containerid).width($($('.wtHider')[0]).width());
	        }

	        singleWellRangeReportHelper.createTable = function () {
	            singleWellRangeReportHelper.container.innerHTML = "";
	            singleWellRangeReportHelper.hot = new Handsontable(singleWellRangeReportHelper.container, {
	            	licenseKey: '96860-f3be6-b4941-2bd32-fd62b',
	            	data: singleWellRangeReportHelper.data,
	            	hiddenColumns: {
	                    columns: [singleWellRangeReportHelper.columnCount-1],
	                    indicators: false,
	                    copyPasteEnabled: false
	                },
//	            	columns:singleWellRangeReportHelper.columns,
	            	fixedRowsTop:singleWellRangeReportHelper.templateData.fixedRowsTop, 
	                fixedRowsBottom: singleWellRangeReportHelper.templateData.fixedRowsBottom,
//	                fixedColumnsLeft:1, //固定左侧多少列不能水平滚动
	                rowHeaders: false,
	                colHeaders: false,
					rowHeights: singleWellRangeReportHelper.templateData.rowHeights,
					colWidths: singleWellRangeReportHelper.colWidths,
//					colWidths: [50, 120, 105, 100, 130, 105, 100, 130, 140, 120, 100, 100, 100, 80, 140, 120, 150, 120, 140, 140, 100, 130, 130, 130, 150, 120, 75],
					rowHeaders: false, //显示行头
//					rowHeaders(index) {
//					    return 'Row ' + (index + 1);
//					},
					colHeaders: false, //显示列头
//					colHeaders(index) {
//					    return 'Col ' + (index + 1);
//					},
					stretchH: 'all',
					columnSorting: true, //允许排序
	                allowInsertRow:false,
	                sortIndicator: true,
	                manualColumnResize: true, //当值为true时，允许拖动，当为false时禁止拖动
	                manualRowResize: true, //当值为true时，允许拖动，当为false时禁止拖动
	                filters: true,
	                renderAllRows: true,
	                search: true,
	                mergeCells: singleWellRangeReportHelper.templateData.mergeCells,
	                contextMenu: {
	                    items: {
	                        "copy": {
	                            name: loginUserLanguageResource.contextMenu_copy
	                        },
	                        "cut": {
	                            name: loginUserLanguageResource.contextMenu_cut
	                        }
	                    }
	                }, 
	                cells: function (row, col, prop) {
	                	var cellProperties = {};
	                    var visualRowIndex = this.instance.toVisualRow(row);
	                    var visualColIndex = this.instance.toVisualColumn(col);
	                    cellProperties.renderer = singleWellRangeReportHelper.addStyle;
	                    cellProperties.readOnly = true;
	                    if(singleWellRangeReportHelper.templateData.editable!=null && singleWellRangeReportHelper.templateData.editable.length>0){
	                    	for(var i=0;i<singleWellRangeReportHelper.templateData.editable.length;i++){
	                    		if( row>=singleWellRangeReportHelper.templateData.editable[i].startRow 
	                    				&& row<=singleWellRangeReportHelper.templateData.editable[i].endRow
	                    				&& col>=singleWellRangeReportHelper.templateData.editable[i].startColumn 
	                    				&& col<=singleWellRangeReportHelper.templateData.editable[i].endColumn
	                    		){
	                    			cellProperties.readOnly = false;
	                    			cellProperties.renderer = singleWellRangeReportHelper.addEditableColor;
	                    		}
	                    	}
	                    }
	                    
	                    return cellProperties;
	                },
	                afterOnCellMouseOver: function(event, coords, TD){
	                	if(coords.col>=0 && coords.row>=0 && singleWellRangeReportHelper!=null&&singleWellRangeReportHelper.hot!=''&&singleWellRangeReportHelper.hot!=undefined && singleWellRangeReportHelper.hot.getDataAtCell!=undefined){
	                		var rawValue=singleWellRangeReportHelper.sourceData[coords.row][coords.col];
	                		if(coords.row>=singleWellRangeReportHelper.templateData.header.length){
	                			if(isNotVal(rawValue)){
	                				var showValue=rawValue;
	            					var rowChar=90;
	            					var maxWidth=rowChar*10;
	            					if(rawValue.length>rowChar){
	            						showValue='';
	            						let arr = [];
	            						let index = 0;
	            						while(index<rawValue.length){
	            							arr.push(rawValue.slice(index,index +=rowChar));
	            						}
	            						for(var i=0;i<arr.length;i++){
	            							showValue+=arr[i];
	            							if(i<arr.length-1){
	            								showValue+='<br>';
	            							}
	            						}
	            					}
	                				if(!isNotVal(TD.tip)){
	                					TD.tip = Ext.create('Ext.tip.ToolTip', {
			                			    target: event.target,
			                			    maxWidth:maxWidth,
			                			    html: showValue,
			                			    listeners: {
			                			    	hide: function (thisTip, eOpts) {
			                                	},
			                                	close: function (thisTip, eOpts) {
			                                	}
			                                }
			                			});
	                				}else{
	                					TD.tip.setHtml(showValue);
	                				}
	                			}
	                		}
	                	}
	                },
	                afterOnCellMouseOut: function(event, coords, TD){
	                	if(singleWellRangeReportHelper!=null&&singleWellRangeReportHelper.hot!=''&&singleWellRangeReportHelper.hot!=undefined && singleWellRangeReportHelper.hot.getDataAtCell!=undefined){
	                		var value=singleWellRangeReportHelper.sourceData[coords.row][coords.col];
	                		if(coords.row>=singleWellRangeReportHelper.templateData.header.length){
//	                			TD.outerHTML='<td class="htDimmed">'+TD.innerText+'</td>';
	                		}
	                	}
	                },
	                afterChange: function (changes, source) {
	                    //params 参数 1.column num , 2,id, 3,oldvalue , 4.newvalue
	                    if (singleWellRangeReportHelper!=null && singleWellRangeReportHelper.hot!=undefined && singleWellRangeReportHelper.hot!='' && changes != null) {
	                        for (var i = 0; i < changes.length; i++) {
	                            var params = [];
	                            var index = changes[i][0]; //行号码
	                            var rowdata = singleWellRangeReportHelper.hot.getDataAtRow(index);
	                            
	                            var editCellInfo={};
	                            var editRow=changes[i][0];
	                            var editCol=changes[i][1];
	                            var column=singleWellRangeReportHelper.columns[editCol];
	                            
	                            editCellInfo.editRow=editRow;
	                            editCellInfo.editCol=editCol;
	                            editCellInfo.column=column;
	                            editCellInfo.recordId=rowdata[rowdata.length-1]
	                            editCellInfo.oldValue=changes[i][2];
	                            editCellInfo.newValue=changes[i][3];
	                            editCellInfo.header=false;
	                            if(editCellInfo.editRow<singleWellRangeReportHelper.templateData.header.length){
	                            	editCellInfo.header=true;
	                            }
	                            
	                            var isExit=false;
	                            for(var j=0;j<singleWellRangeReportHelper.contentUpdateList.length;j++){
	                            	if(editCellInfo.editRow==singleWellRangeReportHelper.contentUpdateList[j].editRow && editCellInfo.editCol==singleWellRangeReportHelper.contentUpdateList[j].editCol){
	                            		singleWellRangeReportHelper.contentUpdateList[j].newValue=editCellInfo.newValue;
	                            		isExit=true;
	                            		break;
	                            	}
	                            }
	                            if(!isExit){
	                            	singleWellRangeReportHelper.contentUpdateList.push(editCellInfo);
	                            }
	                        }
	                    }
	                }
	            });
	        }
	        singleWellRangeReportHelper.getData = function (data) {
	        	
	        }
	        
	        singleWellRangeReportHelper.saveData = function () {
	        	if(singleWellRangeReportHelper.contentUpdateList.length>0){
	        		singleWellRangeReportHelper.editData.contentUpdateList=singleWellRangeReportHelper.contentUpdateList;
	        		var deviceName='';
	        	    var deviceId=0;
	        	    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
	        	    var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
	        	    if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
	        	    	deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
	        	    	deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
	        	    }
//	        		alert(JSON.stringify(singleWellRangeReportHelper.editData));
	        		Ext.Ajax.request({
	                    method: 'POST',
	                    url: context + '/reportDataMamagerController/saveSingleWellRangeDailyReportData',
	                    success: function (response) {
	                        rdata = Ext.JSON.decode(response.responseText);
	                        if (rdata.success) {
	                        	Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.saveSuccessfully);
	                        	singleWellRangeReportHelper.clearContainer();
	                        	CreateSingleWellReportTable();
	                        	CreateSingleWellReportCurve();
	                        } else {
	                        	singleWellRangeReportHelper.clearContainer();
	                        	Ext.MessageBox.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.saveFailure+"</font>");
	                        }
	                    },
	                    failure: function () {
	                        Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.requestFailure);
	                    },
	                    params: {
	                    	deviceId:deviceId,
	                    	deviceName:deviceName,
	                    	data: JSON.stringify(singleWellRangeReportHelper.editData),
	                        deviceType: deviceType
	                    }
	                });
	        	}else{
	        		Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.noDataChange);
	        	}
	        }
	        singleWellRangeReportHelper.clearContainer = function () {
	        	singleWellRangeReportHelper.editData={};
            	singleWellRangeReportHelper.contentUpdateList=[];
	        }

	        var init = function () {
	        	singleWellRangeReportHelper.initData();
	        }

	        init();
	        return singleWellRangeReportHelper;
	    }
	};

function CreateSingleWellDailyReportTable(){
	var orgId = Ext.getCmp('leftOrg_Id').getValue();
    var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
    var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
    var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id').rawValue;
    var interval = Ext.getCmp('SingleWellDailyReportIntervalComb_Id').getValue();
    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
    
    var deviceName='';
    var deviceId=0;
    var calculateType=0;
    var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
    if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
    	deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
    	deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
    	calculateType=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.calculateType;
    }
    
    Ext.getCmp("SingleWellDailyReportPanel_id").el.mask(loginUserLanguageResource.loading).show();
	Ext.Ajax.request({
		method:'POST',
		url:context + '/reportDataMamagerController/getSingleWellDailyReportData',
		success:function(response) {
			Ext.getCmp("SingleWellDailyReportPanel_id").getEl().unmask();
			var result =  Ext.JSON.decode(response.responseText);
			
			var startDate=Ext.getCmp('SingleWellDailyReportStartDate_Id');
            if(startDate.rawValue==''||null==startDate.rawValue){
            	startDate.setValue(result.startDate);
            }
            var endDate=Ext.getCmp('SingleWellDailyReportEndDate_Id');
            if(endDate.rawValue==''||null==endDate.rawValue){
            	endDate.setValue(result.endDate);
            }
            var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id');
            if(reportDate.rawValue==''||null==reportDate.rawValue){
            	reportDate.setValue(result.reportDate);
            }
            
			if(singleWellDailyReportHelper!=null){
				if(singleWellDailyReportHelper.hot!=undefined){
					singleWellDailyReportHelper.hot.destroy();
				}
				singleWellDailyReportHelper=null;
			}
			if(result.success){
				if(singleWellDailyReportHelper==null || singleWellDailyReportHelper.hot==undefined){
					singleWellDailyReportHelper = SingleWellDailyReportHelper.createNew("SingleWellDailyReportDiv_id","SingleWellDailyReportContainer",result.template,result.data,result.columns,result.totalCount);
					singleWellDailyReportHelper.createTable();
				}
			}else{
				$("#SingleWellDailyReportDiv_id").html('');
			}
			
			Ext.getCmp("SingleWellDailyReportTotalCount_Id").update({count: result.data.length});
		},
		failure:function(){
			Ext.MessageBox.alert(loginUserLanguageResource.error,loginUserLanguageResource.errorInfo);
		},
		params: {
			orgId: orgId,
			deviceId:deviceId,
			deviceName: deviceName,
			startDate: startDate,
			endDate: endDate,
			reportDate: reportDate,
			reportType: 2,
			interval: interval,
            deviceType:deviceType,
            calculateType:calculateType
        }
	});
};


var SingleWellDailyReportHelper = {
	    createNew: function (divId, containerid,templateData,contentData,columns,totalCount) {
	        var singleWellDailyReportHelper = {};
	        singleWellDailyReportHelper.templateData=templateData;
	        singleWellDailyReportHelper.contentData=contentData;
	        singleWellDailyReportHelper.columns=columns;
	        singleWellDailyReportHelper.get_data = {};
	        singleWellDailyReportHelper.data=[];
	        singleWellDailyReportHelper.sourceData=[];
	        singleWellDailyReportHelper.hot = '';
	        singleWellDailyReportHelper.container = document.getElementById(divId);
	        singleWellDailyReportHelper.columnCount=0;
	        singleWellDailyReportHelper.editData={};
	        singleWellDailyReportHelper.contentUpdateList = [];
	        singleWellDailyReportHelper.totalCount=totalCount;
	        
	        singleWellDailyReportHelper.colWidths=[];
	        if(loginUserLanguage=='zh_CN'){
	        	singleWellDailyReportHelper.colWidths=singleWellDailyReportHelper.templateData.columnWidths_zh_CN
	        }else if(loginUserLanguage=='en'){
	        	singleWellDailyReportHelper.colWidths=singleWellDailyReportHelper.templateData.columnWidths_en
		    }else if(loginUserLanguage=='ru'){
	        	singleWellDailyReportHelper.colWidths=singleWellDailyReportHelper.templateData.columnWidths_ru
		    }
	        
	        singleWellDailyReportHelper.initData=function(){
	        	singleWellDailyReportHelper.data=[];
	        	for(var i=0;i<singleWellDailyReportHelper.templateData.header.length;i++){
	        		if(loginUserLanguage=='zh_CN'){
	        			singleWellDailyReportHelper.templateData.header[i].title=singleWellDailyReportHelper.templateData.header[i].title_zh_CN;
	        		}else if(loginUserLanguage=='en'){
	        			singleWellDailyReportHelper.templateData.header[i].title=singleWellDailyReportHelper.templateData.header[i].title_en;
	        		}else if(loginUserLanguage=='ru'){
	        			singleWellDailyReportHelper.templateData.header[i].title=singleWellDailyReportHelper.templateData.header[i].title_ru;
	        		}
	        		
	        		
	        		singleWellDailyReportHelper.templateData.header[i].title.push('');
	        		singleWellDailyReportHelper.columnCount=singleWellDailyReportHelper.templateData.header[i].title.length;
	        		
	        		var valueArr=[];
	        		var sourceValueArr=[];
	        		for(var j=0;j<singleWellDailyReportHelper.templateData.header[i].title.length;j++){
	        			valueArr.push(singleWellDailyReportHelper.templateData.header[i].title[j]);
	        			sourceValueArr.push(singleWellDailyReportHelper.templateData.header[i].title[j]);
	        		}
	        		
	        		singleWellDailyReportHelper.data.push(valueArr);
	        		singleWellDailyReportHelper.sourceData.push(sourceValueArr);
		        }
	        	for(var i=0;i<singleWellDailyReportHelper.contentData.length;i++){
	        		var valueArr=[];
	        		var sourceValueArr=[];
	        		for(var j=0;j<singleWellDailyReportHelper.contentData[i].length;j++){
	        			valueArr.push(singleWellDailyReportHelper.contentData[i][j]);
	        			sourceValueArr.push(singleWellDailyReportHelper.contentData[i][j]);
	        		}
	        		
	        		singleWellDailyReportHelper.data.push(valueArr);
		        	singleWellDailyReportHelper.sourceData.push(sourceValueArr);
		        }
	        	for(var i=singleWellDailyReportHelper.templateData.header.length;i<singleWellDailyReportHelper.data.length;i++){
	        		for(var j=0;j<singleWellDailyReportHelper.data[i].length;j++){
	        			var editable=false
	        			for(var k=0;k<singleWellDailyReportHelper.templateData.editable.length;k++){
	        				if( i>=singleWellDailyReportHelper.templateData.editable[k].startRow 
	                				&& i<=singleWellDailyReportHelper.templateData.editable[k].endRow
	                				&& j>=singleWellDailyReportHelper.templateData.editable[k].startColumn 
	                				&& j<=singleWellDailyReportHelper.templateData.editable[k].endColumn
	                		){
	        					editable=true;
	        					break;
	                		}
	        			}
	        			
	        			var value=singleWellDailyReportHelper.data[i][j];
	        			var valueLength=12;
		                if((!editable)&&value.length>valueLength){
		                	value=value.substring(0, valueLength-1)+"...";
		                	singleWellDailyReportHelper.data[i][j]=value;
		                }
	        		}
	        	}
	        }
	        
	        singleWellDailyReportHelper.addStyle = function (instance, td, row, col, prop, value, cellProperties) {
	        	Handsontable.renderers.TextRenderer.apply(this, arguments);
	        	if(singleWellDailyReportHelper!=null && singleWellDailyReportHelper.hot!=null){
	        		for(var i=0;i<singleWellDailyReportHelper.templateData.header.length;i++){
		        		if(row==i){
		        			if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle)){
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.fontWeight)){
		        					td.style.fontWeight = singleWellDailyReportHelper.templateData.header[i].tdStyle.fontWeight;
		        				}
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.fontSize)){
		        					td.style.fontSize = singleWellDailyReportHelper.templateData.header[i].tdStyle.fontSize;
		        				}
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.fontFamily)){
//		        					td.style.fontFamily = singleWellDailyReportHelper.templateData.header[i].tdStyle.fontFamily;
		        				}
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.height)){
		        					td.style.height = singleWellDailyReportHelper.templateData.header[i].tdStyle.height;
		        				}
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.color)){
		        					td.style.color = singleWellDailyReportHelper.templateData.header[i].tdStyle.color;
		        				}
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.backgroundColor)){
		        					td.style.backgroundColor = singleWellDailyReportHelper.templateData.header[i].tdStyle.backgroundColor;
		        				}
		        				if(isNotVal(singleWellDailyReportHelper.templateData.header[i].tdStyle.textAlign)){
		        					td.style.textAlign = singleWellDailyReportHelper.templateData.header[i].tdStyle.textAlign;
		        				}
		        			}
		        			break;
		        		}
		        	}
	        		if(row>=singleWellDailyReportHelper.templateData.header.length){
		        		td.style.whiteSpace='nowrap'; //文本不换行
		            	td.style.overflow='hidden';//超出部分隐藏
		            	td.style.textOverflow='ellipsis';//使用省略号表示溢出的文本
		        	}
	        	}
	        }
	        
	        singleWellDailyReportHelper.addEditableColor = function (instance, td, row, col, prop, value, cellProperties) {
	             Handsontable.renderers.TextRenderer.apply(this, arguments);
	             td.style.color='#ff0000';  
	             td.style.whiteSpace='nowrap'; //文本不换行
	             td.style.overflow='hidden';//超出部分隐藏
	             td.style.textOverflow='ellipsis';//使用省略号表示溢出的文本
	        }
	        
	        singleWellDailyReportHelper.hiddenColumn = function (instance, td, row, col, prop, value, cellProperties) {
	            Handsontable.renderers.TextRenderer.apply(this, arguments);
	            td.style.display = 'none';
	        }

	        // 实现标题居中
	        singleWellDailyReportHelper.titleCenter = function () {
	            $(containerid).width($($('.wtHider')[0]).width());
	        }

	        singleWellDailyReportHelper.createTable = function () {
	            singleWellDailyReportHelper.container.innerHTML = "";
	            singleWellDailyReportHelper.hot = new Handsontable(singleWellDailyReportHelper.container, {
	            	licenseKey: '96860-f3be6-b4941-2bd32-fd62b',
	            	data: singleWellDailyReportHelper.data,
	            	hiddenColumns: {
	                    columns: [singleWellDailyReportHelper.columnCount-1],
	                    indicators: false,
	                    copyPasteEnabled: false
	                },
//	            	columns:singleWellDailyReportHelper.columns,
	            	fixedRowsTop:singleWellDailyReportHelper.templateData.fixedRowsTop, 
	                fixedRowsBottom: singleWellDailyReportHelper.templateData.fixedRowsBottom,
//	                fixedColumnsLeft:1, //固定左侧多少列不能水平滚动
	                rowHeaders: false,
	                colHeaders: false,
					rowHeights: singleWellDailyReportHelper.templateData.rowHeights,
					colWidths: singleWellDailyReportHelper.colWidths,
//					colWidths: [50, 120, 105, 100, 130, 105, 100, 130, 140, 120, 100, 100, 100, 100, 100, 100, 80, 140, 120, 150, 120, 140, 140, 100, 130, 130, 130, 150, 120, 75],
					rowHeaders: false, //显示行头
//					rowHeaders(index) {
//					    return 'Row ' + (index + 1);
//					},
					colHeaders: false, //显示列头
//					colHeaders(index) {
//					    return 'Col ' + (index + 1);
//					},
					stretchH: 'all',
					columnSorting: true, //允许排序
	                allowInsertRow:false,
	                sortIndicator: true,
	                manualColumnResize: true, //当值为true时，允许拖动，当为false时禁止拖动
	                manualRowResize: true, //当值为true时，允许拖动，当为false时禁止拖动
	                filters: true,
	                renderAllRows: true,
	                search: true,
	                mergeCells: singleWellDailyReportHelper.templateData.mergeCells,
	                cells: function (row, col, prop) {
	                	var cellProperties = {};
	                    var visualRowIndex = this.instance.toVisualRow(row);
	                    var visualColIndex = this.instance.toVisualColumn(col);
	                    cellProperties.renderer = singleWellDailyReportHelper.addStyle;
	                    cellProperties.readOnly = true;
	                    if(singleWellDailyReportHelper.templateData.editable!=null && singleWellDailyReportHelper.templateData.editable.length>0){
	                    	for(var i=0;i<singleWellDailyReportHelper.templateData.editable.length;i++){
	                    		if( row>=singleWellDailyReportHelper.templateData.editable[i].startRow 
	                    				&& row<=singleWellDailyReportHelper.templateData.editable[i].endRow
	                    				&& col>=singleWellDailyReportHelper.templateData.editable[i].startColumn 
	                    				&& col<=singleWellDailyReportHelper.templateData.editable[i].endColumn
	                    		){
	                    			cellProperties.readOnly = false;
	                    			cellProperties.renderer = singleWellDailyReportHelper.addEditableColor;
	                    		}
	                    	}
	                    }
	                    if(row>=singleWellDailyReportHelper.templateData.header.length+singleWellDailyReportHelper.totalCount){
	                    	cellProperties.readOnly = true;
	                    }
	                    return cellProperties;
	                },
	                afterOnCellMouseOver: function(event, coords, TD){
	                	if(coords.col>=0 && coords.row>=0 && singleWellDailyReportHelper!=null&&singleWellDailyReportHelper.hot!=''&&singleWellDailyReportHelper.hot!=undefined && singleWellDailyReportHelper.hot.getDataAtCell!=undefined){
	                		var rawValue=singleWellDailyReportHelper.sourceData[coords.row][coords.col];
	                		if(coords.row>=singleWellDailyReportHelper.templateData.header.length){
	                			if(isNotVal(rawValue)){
	                				var showValue=rawValue;
	            					var rowChar=90;
	            					var maxWidth=rowChar*10;
	            					if(rawValue.length>rowChar){
	            						showValue='';
	            						let arr = [];
	            						let index = 0;
	            						while(index<rawValue.length){
	            							arr.push(rawValue.slice(index,index +=rowChar));
	            						}
	            						for(var i=0;i<arr.length;i++){
	            							showValue+=arr[i];
	            							if(i<arr.length-1){
	            								showValue+='<br>';
	            							}
	            						}
	            					}
	                				if(!isNotVal(TD.tip)){
	                					TD.tip = Ext.create('Ext.tip.ToolTip', {
			                			    target: event.target,
			                			    maxWidth:maxWidth,
			                			    html: showValue,
			                			    listeners: {
			                			    	hide: function (thisTip, eOpts) {
			                                	},
			                                	close: function (thisTip, eOpts) {
			                                	}
			                                }
			                			});
	                				}else{
	                					TD.tip.setHtml(showValue);
	                				}
	                			}
	                		}
	                	}
	                },
	                afterOnCellMouseOut: function(event, coords, TD){
	                	if(singleWellDailyReportHelper!=null&&singleWellDailyReportHelper.hot!=''&&singleWellDailyReportHelper.hot!=undefined && singleWellDailyReportHelper.hot.getDataAtCell!=undefined){
	                		var value=singleWellDailyReportHelper.sourceData[coords.row][coords.col];
	                		if(coords.row>=singleWellDailyReportHelper.templateData.header.length){
//	                			TD.outerHTML='<td class="htDimmed">'+TD.innerText+'</td>';
	                		}
	                	}
	                },
	                afterChange: function (changes, source) {
	                    //params 参数 1.column num , 2,id, 3,oldvalue , 4.newvalue
	                    if (singleWellDailyReportHelper!=null && singleWellDailyReportHelper.hot!=undefined && singleWellDailyReportHelper.hot!='' && changes != null) {
	                        for (var i = 0; i < changes.length; i++) {
	                            var params = [];
	                            var index = changes[i][0]; //行号码
	                            var rowdata = singleWellDailyReportHelper.hot.getDataAtRow(index);
	                            
	                            var editCellInfo={};
	                            var editRow=changes[i][0];
	                            var editCol=changes[i][1];
	                            var column=singleWellDailyReportHelper.columns[editCol];
	                            
	                            editCellInfo.editRow=editRow;
	                            editCellInfo.editCol=editCol;
	                            editCellInfo.column=column;
	                            editCellInfo.recordId=rowdata[rowdata.length-1]
	                            editCellInfo.oldValue=changes[i][2];
	                            editCellInfo.newValue=changes[i][3];
	                            editCellInfo.header=false;
	                            if(editCellInfo.editRow<singleWellDailyReportHelper.templateData.header.length){
	                            	editCellInfo.header=true;
	                            }
	                            
	                            var isExit=false;
	                            for(var j=0;j<singleWellDailyReportHelper.contentUpdateList.length;j++){
	                            	if(editCellInfo.editRow==singleWellDailyReportHelper.contentUpdateList[j].editRow && editCellInfo.editCol==singleWellDailyReportHelper.contentUpdateList[j].editCol){
	                            		singleWellDailyReportHelper.contentUpdateList[j].newValue=editCellInfo.newValue;
	                            		isExit=true;
	                            		break;
	                            	}
	                            }
	                            if(!isExit){
	                            	singleWellDailyReportHelper.contentUpdateList.push(editCellInfo);
	                            }
	                        }
	                    }
	                }
	            });
	        }
	        singleWellDailyReportHelper.getData = function (data) {
	        	
	        }
	        
	        singleWellDailyReportHelper.saveData = function () {
	        	if(singleWellDailyReportHelper.contentUpdateList.length>0){
	        		singleWellDailyReportHelper.editData.contentUpdateList=singleWellDailyReportHelper.contentUpdateList;
	        		var deviceName='';
	        	    var deviceId=0;
	        	    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
	        	    var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
	        	    if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
	        	    	deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
	        	    	deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
	        	    }
//	        		alert(JSON.stringify(singleWellDailyReportHelper.editData));
	        		Ext.Ajax.request({
	                    method: 'POST',
	                    url: context + '/reportDataMamagerController/saveSingleWellDailyDailyReportData',
	                    success: function (response) {
	                        rdata = Ext.JSON.decode(response.responseText);
	                        if (rdata.success) {
	                        	Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.saveSuccessfully);
	                        	singleWellDailyReportHelper.clearContainer();
	                        	CreateSingleWellReportTable();
	                        	CreateSingleWellReportCurve();
	                        } else {
	                        	singleWellDailyReportHelper.clearContainer();
	                        	Ext.MessageBox.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.saveFailure+"</font>");
	                        }
	                    },
	                    failure: function () {
	                        Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.requestFailure);
	                    },
	                    params: {
	                    	deviceId:deviceId,
	                    	deviceName:deviceName,
	                    	data: JSON.stringify(singleWellDailyReportHelper.editData),
	                        deviceType: deviceType
	                    }
	                });
	        	}else{
	        		Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.noDataChange);
	        	}
	        }
	        singleWellDailyReportHelper.clearContainer = function () {
	        	singleWellDailyReportHelper.editData={};
            	singleWellDailyReportHelper.contentUpdateList=[];
	        }

	        var init = function () {
	        	singleWellDailyReportHelper.initData();
	        }

	        init();
	        return singleWellDailyReportHelper;
	    }
	};

function createSingleWellDailyReportWellListDataColumn(columnInfo) {
    var myArr = columnInfo;

    var myColumns = "[";
    for (var i = 0; i < myArr.length; i++) {
        var attr = myArr[i];
        var width_ = "";
        var lock_ = "";
        var hidden_ = "";
        if (attr.hidden == true) {
            hidden_ = ",hidden:true";
        }
        if (isNotVal(attr.lock)) {
            //lock_ = ",locked:" + attr.lock;
        }
        if (isNotVal(attr.width)) {
            width_ = ",width:" + attr.width;
        }
        myColumns += "{text:'" + attr.header + "',lockable:true,align:'center' "+width_;
        if (attr.dataIndex == 'id') {
            myColumns += ",xtype: 'rownumberer',sortable : false,locked:false";
        }else if (attr.dataIndex.toUpperCase()=='commStatusName'.toUpperCase()) {
            myColumns += ",sortable : false,dataIndex:'" + attr.dataIndex + "',renderer:function(value,o,p,e){return adviceCommStatusColor(value,o,p,e);}";
        }else if (attr.dataIndex.toUpperCase()=='slave'.toUpperCase()) {
            myColumns += ",sortable : false,locked:true,dataIndex:'" + attr.dataIndex + "',renderer:function(value){if(isNotVal(value)){return \"<span data-qtip=\"+(value==undefined?\"\":value)+\">\"+(value==undefined?\"\":value)+\"</span>\";}}";
        } else if (attr.dataIndex.toUpperCase() == 'acqTime'.toUpperCase()) {
            myColumns += ",sortable : false,locked:false,dataIndex:'" + attr.dataIndex + "',renderer:function(value,o,p,e){return adviceTimeFormat(value,o,p,e);}";
        } else {
            myColumns += hidden_ + lock_ + ",sortable : false,dataIndex:'" + attr.dataIndex + "',renderer:function(value){if(isNotVal(value)){return \"<span data-qtip=\"+(value==undefined?\"\":value)+\">\"+(value==undefined?\"\":value)+\"</span>\";}}";
        }
        myColumns += "}";
        if (i < myArr.length - 1) {
            myColumns += ",";
        }
    }
    myColumns += "]";
    return myColumns;
};

function CreateSingleWellReportCurve(){
	var SingleWellReportTabPanelActiveId=Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id;
	if(SingleWellReportTabPanelActiveId=='SingleWellDailyReportTabPanel_id'){
		CreateSingleWellDailyReportCurve();
	}else if(SingleWellReportTabPanelActiveId=='SingleWellRangeReportTabPanel_id'){
		CreateSingleWellRangeReportCurve();
	}
}

function CreateSingleWellRangeReportCurve(){
	var orgId = Ext.getCmp('leftOrg_Id').getValue();
    var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
    var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
    
    var deviceName='';
    var deviceId=0;
    var calculateType=0;
    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
    var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
    if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
    	deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
    	deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
    	calculateType=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.calculateType;
    }
    
    Ext.getCmp("SingleWellRangeReportCurvePanel_id").el.mask(loginUserLanguageResource.loading).show();
	Ext.Ajax.request({
		method:'POST',
		url:context + '/reportDataMamagerController/getSingleWellRangeReportCurveData',
		success:function(response) {
			Ext.getCmp("SingleWellRangeReportCurvePanel_id").getEl().unmask();
			var result =  Ext.JSON.decode(response.responseText);
			
			var startDate=Ext.getCmp('SingleWellDailyReportStartDate_Id');
            if(startDate.rawValue==''||null==startDate.rawValue){
            	startDate.setValue(result.startDate);
            }
            var endDate=Ext.getCmp('SingleWellDailyReportEndDate_Id');
            if(endDate.rawValue==''||null==endDate.rawValue){
            	endDate.setValue(result.endDate);
            }
            
		    var data = result.list;
		    var graphicSet=result.graphicSet;
		    var timeFormat='%m-%d';
		    var defaultColors=["#7cb5ec", "#434348", "#90ed7d", "#f7a35c", "#8085e9", "#f15c80", "#e4d354", "#2b908f", "#f45b5b", "#91e8e1"];
		    var tickInterval = 1;
		    tickInterval = Math.floor(data.length / 10) + 1;
		    if(tickInterval<100){
		    	tickInterval=100;
		    }
		    var title = result.deviceName+ '-' + loginUserLanguageResource.daliyReportCurve;
		    var legendName =result.curveItems;
		    var curveConf=result.curveConf;
		    var color=[];
		    var color_l=[];
		    var color_r=[];
		    var color_all=[];
		    for(var i=0;i<curveConf.length;i++){
		    	var singleColor=defaultColors[i%defaultColors.length];
		    	if(curveConf[i].color!=''){
		    		singleColor='#'+curveConf[i].color;
		    	}
		    	color.push(singleColor);
		    	
		    	if(curveConf[i].yAxisOpposite){
		    		color_r.push(singleColor);
		    	}else{
		    		color_l.push(singleColor);
		    	}
		    }
		    
		    var series = [];
		    var series_l=[];
		    var series_r=[];
		    var yAxis= [];
		    var yAxis_l= [];
		    var yAxis_r= [];
		   		    
		    for (var i = 0; i < legendName.length; i++) {
		        var maxValue=null;
		        var minValue=null;
		        var allPositive=true;//全部是非负数
		        var allNegative=true;//全部是负值
		        
		        var singleSeries={};
		        singleSeries.name=legendName[i];
		        singleSeries.type='spline';
		        singleSeries.lineWidth=curveConf[i].lineWidth;
		        singleSeries.dashStyle=curveConf[i].dashStyle;
		        singleSeries.marker={enabled: false};
		        singleSeries.yAxis=i;
		        singleSeries.data=[];
		        for (var j = 0; j < data.length; j++) {
		        	var pointData=[];
		        	pointData.push(Date.parse(data[j].calDate.replace(/-/g, '/')));
		        	pointData.push(data[j].data[i]);
		        	singleSeries.data.push(pointData);
		        	if(parseFloat(data[j].data[i])<0){
		            	allPositive=false;
		            }else if(parseFloat(data[j].data[i])>=0){
		            	allNegative=false;
		            }
		        }
		        if(curveConf[i].yAxisOpposite){
		        	series_r.push(singleSeries);
		        }else{
		        	series_l.push(singleSeries);
		        }
		        
		        var opposite=curveConf[i].yAxisOpposite;
		        if(allNegative){
		        	maxValue=0;
		        }else if(allPositive){
		        	minValue=0;
		        }
		        if(JSON.stringify(graphicSet) != "{}" && isNotVal(graphicSet.Report) ){
			    	for(var j=0;j<graphicSet.Report.length;j++){
			    		if(graphicSet.Report[j].itemCode!=undefined && graphicSet.Report[j].itemCode.toUpperCase()==result.curveItemCodes[i].toUpperCase()){
			    			if(isNotVal(graphicSet.Report[j].yAxisMaxValue)){
					    		maxValue=parseFloat(graphicSet.Report[j].yAxisMaxValue);
					    	}
					    	if(isNotVal(graphicSet.Report[j].yAxisMinValue)){
					    		minValue=parseFloat(graphicSet.Report[j].yAxisMinValue);
					    	}
					    	break;
			    		}
			    	}
			    }
		        
		        var singleAxis={
		        		max:maxValue,
		        		min:minValue,
		        		title: {
		                    text: legendName[i],
		                    style: {
		                        color: color[i],
		                    }
		                },
		                labels: {
		                	style: {
		                        color: color[i],
		                    }
		                },
		                opposite:opposite
		          };
		        if(curveConf[i].yAxisOpposite){
		        	yAxis_r.push(singleAxis);
		        }else{
		        	yAxis_l.push(singleAxis);
		        }
		        
		    }
		    for(var i=yAxis_l.length-1;i>=0;i--){
		    	yAxis.push(yAxis_l[i]);
		    }
		    for(var i=0;i<yAxis_r.length;i++){
		    	yAxis.push(yAxis_r[i]);
		    }
		    
		    for(var i=0;i<series_l.length;i++){
		    	series_l[i].yAxis=series_l.length-1-i;
		    	series.push(series_l[i]);
		    }
		    for(var i=0;i<series_r.length;i++){
		    	series_r[i].yAxis=series_l.length+i;
		    	series.push(series_r[i]);
		    }
		    
		    for(var i=0;i<color_l.length;i++){
		    	color_all.push(color_l[i]);
		    }
		    for(var i=0;i<color_r.length;i++){
		    	color_all.push(color_r[i]);
		    }
		    initSingleWellDailyReportCurveChartFn(series, tickInterval, 'SingleWellRangeReportCurveDiv_Id', title, '', '', yAxis, color_all,true,timeFormat);
		},
		failure:function(){
			Ext.MessageBox.alert(loginUserLanguageResource.error,loginUserLanguageResource.errorInfo);
		},
		params: {
			orgId: orgId,
			deviceId:deviceId,
			deviceName: deviceName,
			calculateType: calculateType,
			startDate: startDate,
			endDate: endDate,
			reportType: 0,
            deviceType:deviceType
        }
	});
};

function CreateSingleWellDailyReportCurve(){
	var orgId = Ext.getCmp('leftOrg_Id').getValue();
    var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
    var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
    var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id').rawValue;
    var interval = Ext.getCmp('SingleWellDailyReportIntervalComb_Id').getValue();
    
    var deviceName='';
    var deviceId=0;
    var calculateType=0;
    var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
    var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
    if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
    	deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
    	deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
    	calculateType=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.calculateType;
    }
    
    Ext.getCmp("SingleWellDailyReportCurvePanel_id").el.mask(loginUserLanguageResource.loading).show();
	Ext.Ajax.request({
		method:'POST',
		url:context + '/reportDataMamagerController/getSingleWellDailyReportCurveData',
		success:function(response) {
			Ext.getCmp("SingleWellDailyReportCurvePanel_id").getEl().unmask();
			var result =  Ext.JSON.decode(response.responseText);
			
			var startDate=Ext.getCmp('SingleWellDailyReportStartDate_Id');
            if(startDate.rawValue==''||null==startDate.rawValue){
            	startDate.setValue(result.startDate);
            }
            var endDate=Ext.getCmp('SingleWellDailyReportEndDate_Id');
            if(endDate.rawValue==''||null==endDate.rawValue){
            	endDate.setValue(result.endDate);
            }
            
            var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id');
            if(reportDate.rawValue==''||null==reportDate.rawValue){
            	reportDate.setValue(result.reportDate);
            }
            
		    var data = result.list;
		    var graphicSet=result.graphicSet;
		    var timeFormat='%H:%M';
		    
		    var defaultColors=["#7cb5ec", "#434348", "#90ed7d", "#f7a35c", "#8085e9", "#f15c80", "#e4d354", "#2b908f", "#f45b5b", "#91e8e1"];
		    var tickInterval = 1;
		    tickInterval = Math.floor(data.length / 10) + 1;
		    if(tickInterval<100){
		    	tickInterval=100;
		    }
		    var title = result.deviceName+ "-" + loginUserLanguageResource.hourlyReportCurve+ "-"+result.reportDate;
		    var legendName =result.curveItems;
		    var curveConf=result.curveConf;
		    var color=[];
		    var color_l=[];
		    var color_r=[];
		    var color_all=[];
		    for(var i=0;i<curveConf.length;i++){
		    	var singleColor=defaultColors[i%defaultColors.length];
		    	if(curveConf[i].color!=''){
		    		singleColor='#'+curveConf[i].color;
		    	}
		    	color.push(singleColor);
		    	
		    	if(curveConf[i].yAxisOpposite){
		    		color_r.push(singleColor);
		    	}else{
		    		color_l.push(singleColor);
		    	}
		    }
		    
		    var series = [];
		    var series_l=[];
		    var series_r=[];
		    var yAxis= [];
		    var yAxis_l= [];
		    var yAxis_r= [];
		   		    
		    for (var i = 0; i < legendName.length; i++) {
		        var maxValue=null;
		        var minValue=null;
		        var allPositive=true;//全部是非负数
		        var allNegative=true;//全部是负值
		        
		        var singleSeries={};
		        singleSeries.name=legendName[i];
		        singleSeries.type='spline';
		        singleSeries.lineWidth=curveConf[i].lineWidth;
		        singleSeries.dashStyle=curveConf[i].dashStyle;
		        singleSeries.marker={enabled: false};
		        singleSeries.yAxis=i;
		        singleSeries.data=[];
		        for (var j = 0; j < data.length; j++) {
		        	var pointData=[];
		        	pointData.push(Date.parse(data[j].calDate.replace(/-/g, '/')));
		        	pointData.push(data[j].data[i]);
		        	singleSeries.data.push(pointData);
		        	if(parseFloat(data[j].data[i])<0){
		            	allPositive=false;
		            }else if(parseFloat(data[j].data[i])>=0){
		            	allNegative=false;
		            }
		        }
		        if(curveConf[i].yAxisOpposite){
		        	series_r.push(singleSeries);
		        }else{
		        	series_l.push(singleSeries);
		        }
		        
		        var opposite=curveConf[i].yAxisOpposite;
		        if(allNegative){
		        	maxValue=0;
		        }else if(allPositive){
		        	minValue=0;
		        }
		        if(JSON.stringify(graphicSet) != "{}" && isNotVal(graphicSet.DailyReport) ){
			    	for(var j=0;j<graphicSet.DailyReport.length;j++){
			    		if(graphicSet.DailyReport[j].itemCode!=undefined && graphicSet.DailyReport[j].itemCode.toUpperCase()==result.curveItemCodes[i].toUpperCase()){
			    			if(isNotVal(graphicSet.DailyReport[j].yAxisMaxValue)){
					    		maxValue=parseFloat(graphicSet.DailyReport[j].yAxisMaxValue);
					    	}
					    	if(isNotVal(graphicSet.DailyReport[j].yAxisMinValue)){
					    		minValue=parseFloat(graphicSet.DailyReport[j].yAxisMinValue);
					    	}
					    	break;
			    		}
			    	}
			    }
		        
		        var singleAxis={
		        		max:maxValue,
		        		min:minValue,
		        		title: {
		                    text: legendName[i],
		                    style: {
		                        color: color[i],
		                    }
		                },
		                labels: {
		                	style: {
		                        color: color[i],
		                    }
		                },
		                opposite:opposite
		          };
		        if(curveConf[i].yAxisOpposite){
		        	yAxis_r.push(singleAxis);
		        }else{
		        	yAxis_l.push(singleAxis);
		        }
		        
		    }
		    for(var i=yAxis_l.length-1;i>=0;i--){
		    	yAxis.push(yAxis_l[i]);
		    }
		    for(var i=0;i<yAxis_r.length;i++){
		    	yAxis.push(yAxis_r[i]);
		    }
		    
		    for(var i=0;i<series_l.length;i++){
		    	series_l[i].yAxis=series_l.length-1-i;
		    	series.push(series_l[i]);
		    }
		    for(var i=0;i<series_r.length;i++){
		    	series_r[i].yAxis=series_l.length+i;
		    	series.push(series_r[i]);
		    }
		    
		    for(var i=0;i<color_l.length;i++){
		    	color_all.push(color_l[i]);
		    }
		    for(var i=0;i<color_r.length;i++){
		    	color_all.push(color_r[i]);
		    }
		    initSingleWellDailyReportCurveChartFn(series, tickInterval, 'SingleWellDailyReportCurveDiv_Id', title, '', '', yAxis, color_all,true,timeFormat);
		},
		failure:function(){
			Ext.MessageBox.alert(loginUserLanguageResource.error,loginUserLanguageResource.errorInfo);
		},
		params: {
			orgId: orgId,
			deviceId:deviceId,
			deviceName: deviceName,
			calculateType: calculateType,
			startDate: startDate,
			endDate: endDate,
			reportDate: reportDate,
			reportType: 2,
			interval: interval,
            deviceType: deviceType
        }
	});
};

function initSingleWellDailyReportCurveChartFn(series, tickInterval, divId, title, subtitle, xtitle, yAxis, color,legend,timeFormat) {
	var dafaultMenuItem = Highcharts.getOptions().exporting.buttons.contextButton.menuItems;
	Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });

    var mychart = new Highcharts.Chart({
        chart: {
            renderTo: divId,
            type: 'spline',
            shadow: true,
            borderWidth: 0,
            zoomType: 'xy'
        },
        credits: {
            enabled: false
        },
        title: {
            text: title
        },
        subtitle: {
            text: subtitle
        },
        colors: color,
        xAxis: {
            type: 'datetime',
            title: {
                text: xtitle
            },
//            tickInterval: tickInterval,
            tickPixelInterval:tickInterval,
            labels: {
                formatter: function () {
                    return Highcharts.dateFormat(timeFormat, this.value);
                },
                autoRotation:true,//自动旋转
                rotation: -45 //倾斜度，防止数量过多显示不全  
//                step: 2
            }
        },
        yAxis: yAxis,
        tooltip: {
            crosshairs: true, //十字准线
            shared: true,
            style: {
                color: '#333333',
                fontSize: '12px',
                padding: '8px'
            },
            dateTimeLabelFormats: {
                millisecond: '%Y-%m-%d %H:%M:%S.%L',
                second: '%Y-%m-%d %H:%M:%S',
                minute: '%Y-%m-%d %H:%M',
                hour: '%Y-%m-%d %H',
                day: '%Y-%m-%d',
                week: '%m-%d',
                month: '%Y-%m',
                year: '%Y'
            }
        },
        exporting: {
            enabled: true,
            filename: title,
            sourceWidth: $("#"+divId)[0].offsetWidth,
            sourceHeight: $("#"+divId)[0].offsetHeight,
            buttons: {
            	contextButton: {
            		menuItems:[dafaultMenuItem[0],dafaultMenuItem[1],dafaultMenuItem[2],dafaultMenuItem[3],dafaultMenuItem[4],dafaultMenuItem[5],dafaultMenuItem[6],dafaultMenuItem[7],
            			,dafaultMenuItem[2],{
            				text: loginUserLanguageResource.diagramSet,
            				onclick: function() {
            					var window = Ext.create("AP.view.reportOut.ReportCurveSetWindow", {
                                    title: loginUserLanguageResource.reportDiagramSet
                                });
                                window.show();
            				}
            			}]
            	}
            }
        },
        plotOptions: {
            spline: {
//                lineWidth: 1,
                fillOpacity: 0.3,
                marker: {
                    enabled: true,
                    radius: 3, //曲线点半径，默认是4
                    //                            symbol: 'triangle' ,//曲线点类型："circle", "square", "diamond", "triangle","triangle-down"，默认是"circle"
                    states: {
                        hover: {
                            enabled: true,
                            radius: 6
                        }
                    }
                },
                shadow: true,
                events: {
                	legendItemClick: function(e){
//                		alert("第"+this.index+"个图例被点击，是否可见："+!this.visible);
//                		return true;
                	}
                }
            }
        },
        legend: {
            layout: 'horizontal',//horizontal水平 vertical 垂直
            align: 'center',  //left，center 和 right
            verticalAlign: 'bottom',//top，middle 和 bottom
            enabled: legend,
            borderWidth: 0
        },
        series: series
    });
};

function ExportSingleWellReportData(){
	var SingleWellReportTabPanelActiveId=Ext.getCmp("SingleWellReportTabPanel_Id").getActiveTab().id;
	if(SingleWellReportTabPanelActiveId=='SingleWellDailyReportTabPanel_id'){
		ExportSingleWellDailyReportData();
	}else if(SingleWellReportTabPanelActiveId=='SingleWellRangeReportTabPanel_id'){
		ExportSingleWellRangeReportData();
	}
}

function ExportSingleWellRangeReportData(){
	var timestamp=new Date().getTime();
	var key='ExportSingleWellRangeReportData'+timestamp;
	
	var leftOrg_Id = obtainParams('leftOrg_Id');
	var deviceName = Ext.getCmp('SingleWellDailyReportPanelWellListCombo_Id').getValue();
	var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
	var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;

	var deviceName='';
	var deviceId=0;
	var calculateType=0;
	var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
	var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
	if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
		deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
		deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
		calculateType=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.calculateType;
	}

	var url=context + '/reportDataMamagerController/exportSingleWellRangeReportData?deviceType='+deviceType
	+'&reportType=0'
	+'&calculateType='+calculateType
	+'&deviceName='+URLencode(URLencode(deviceName))
	+'&deviceId='+deviceId
	+'&startDate='+startDate
	+'&endDate='+endDate
	+'&orgId='+leftOrg_Id
	+'&key='+key;
	exportDataMask(key,"SingleWellDailyReportPanel_view",loginUserLanguageResource.loading);
	document.location.href = url;
}

function batchExportSingleWellRangeReportData(){
	var timestamp=new Date().getTime();
	var key='batchExportSingleWellRangeReportData'+timestamp;
	
	var leftOrg_Id = obtainParams('leftOrg_Id');
	var deviceName = Ext.getCmp('SingleWellDailyReportPanelWellListCombo_Id').getValue();
	var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
	var deviceTypeName=getTabPanelActiveName("ProductionReportRootTabPanel");
	
	
	
	var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
	var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
	var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id').rawValue;

	var url=context + '/reportDataMamagerController/batchExportSingleWellRangeReportData?deviceType='+deviceType
	+'&reportType=0'
	+'&deviceName='+URLencode(URLencode(deviceName))
	+'&deviceTypeName='+URLencode(URLencode(deviceTypeName))
	+'&startDate='+startDate
	+'&endDate='+endDate
	+'&reportDate='+reportDate
	+'&orgId='+leftOrg_Id
	+'&key='+key;
	exportDataMask(key,"SingleWellDailyReportPanel_view",loginUserLanguageResource.loading);
	document.location.href = url;
}

function ExportSingleWellDailyReportData(){
	var timestamp=new Date().getTime();
	var key='ExportSingleWellDailyReportData'+timestamp;
	
	var leftOrg_Id = obtainParams('leftOrg_Id');
	var deviceName = Ext.getCmp('SingleWellDailyReportPanelWellListCombo_Id').getValue();
	var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
	var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
	var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id').rawValue;
	var interval = Ext.getCmp('SingleWellDailyReportIntervalComb_Id').getValue();

	var deviceName='';
	var deviceId=0;
	var calculateType=0;
	var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
	var selectRow= Ext.getCmp("SingleWellDailyReportDeviceListSelectRow_Id").getValue();
	if(Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection().length>0){
		deviceName=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceName;
		deviceId=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.id;
		calculateType=Ext.getCmp("SingleWellDailyReportGridPanel_Id").getSelectionModel().getSelection()[0].data.calculateType;
	}

	var url=context + '/reportDataMamagerController/exportSingleWellDailyReportData?deviceType='+deviceType
	+'&reportType=2'
	+'&deviceName='+URLencode(URLencode(deviceName))
	+'&deviceId='+deviceId
	+'&calculateType='+calculateType
	+'&startDate='+startDate
	+'&endDate='+endDate
	+'&reportDate='+reportDate
	+'&interval='+interval
	+'&orgId='+leftOrg_Id
	+'&key='+key;
	exportDataMask(key,"SingleWellDailyReportPanel_view",loginUserLanguageResource.loading);
	document.location.href = url;
}

function batchExportSingleWellDailyReportData(){
	var timestamp=new Date().getTime();
	var key='batchExportSingleWellDailyReportData_'+timestamp;
	var leftOrg_Id = obtainParams('leftOrg_Id');
	var deviceName = Ext.getCmp('SingleWellDailyReportPanelWellListCombo_Id').getValue();
	var deviceType=getDeviceTypeFromTabId("ProductionReportRootTabPanel");
	var deviceTypeName=getTabPanelActiveName("ProductionReportRootTabPanel");
	var startDate = Ext.getCmp('SingleWellDailyReportStartDate_Id').rawValue;
	var endDate = Ext.getCmp('SingleWellDailyReportEndDate_Id').rawValue;
	var reportDate = Ext.getCmp('SingleWellDailyReportDate_Id').rawValue;
	
	var interval = Ext.getCmp('SingleWellDailyReportIntervalComb_Id').getValue();

	var url=context + '/reportDataMamagerController/batchExportSingleWellDailyReportData?deviceType='+deviceType
	+'&deviceTypeName='+URLencode(URLencode(deviceTypeName))
	+'&reportType=2'
	+'&deviceName='+URLencode(URLencode(deviceName))
	+'&startDate='+startDate
	+'&endDate='+endDate
	+'&reportDate='+reportDate
	+'&interval='+interval
	+'&orgId='+leftOrg_Id
	+'&key='+key;
	exportDataMask(key,"SingleWellDailyReportPanel_view",loginUserLanguageResource.loading);
	document.location.href = url;
}