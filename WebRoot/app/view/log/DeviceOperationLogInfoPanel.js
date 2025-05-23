Ext.define('AP.view.log.DeviceOperationLogInfoPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.deviceOperationLogInfoPanel',
    layout: "fit",
    id: "DeviceOperationLogInfoPanel",
    border: false,
    initComponent: function () {
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
                    var deviceName = Ext.getCmp('DeviceOperationLogDeviceListComb_Id').getValue();
                    var deviceType = getDeviceTypeFromTabId("DeviceOperationLogRootTabPanel");
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
                    id: "DeviceOperationLogDeviceListComb_Id",
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
                        }
                    }
                });
        
        var operationTypeCombStore = new Ext.data.JsonStore({
        	pageSize:defaultWellComboxSize,
            fields: [{
                name: "boxkey",
                type: "string"
            }, {
                name: "boxval",
                type: "string"
            }],
            proxy: {
            	url: context + '/wellInformationManagerController/loadCodeComboxList',
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
                    var new_params = {
                    	itemCode: 'action'
                    };
                    Ext.apply(store.proxy.extraParams,new_params);
                }
            }
        });
    	
        var operationTypeCombo = Ext.create(
                'Ext.form.field.ComboBox', {
                    fieldLabel: loginUserLanguageResource.operation,
                    labelWidth: getLabelWidth(loginUserLanguageResource.operation,loginUserLanguage),
                    width: (getLabelWidth(loginUserLanguageResource.operation,loginUserLanguage)+110),
                    id: "DeviceOperationLogOperationTypeListComb_Id",
                    labelAlign: 'left',
                    queryMode: 'remote',
                    typeAhead: true,
                    store: operationTypeCombStore,
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
                        	operationTypeCombo.getStore().loadPage(1); // 加载井下拉框的store
                        },
                        select: function (combo, record, index) {
                        }
                    }
                });
        
    	Ext.apply(this, {
    		layout: "border",
    		items:[{
    			region: 'center',
    			layout: 'fit',
    			id:"DeviceOperationLogPanel_Id"
    		}],
    		tbar: [{
                id: 'DeviceOperationLogColumnStr_Id',
                xtype: 'textfield',
                value: '',
                hidden: true
            },{
                xtype: 'button',
                text: loginUserLanguageResource.refresh,
                iconCls: 'note-refresh',
                hidden:false,
                handler: function (v, o) {
                	Ext.getCmp('DeviceOperationLogDeviceListComb_Id').setValue('');
                	Ext.getCmp('DeviceOperationLogDeviceListComb_Id').setRawValue('');
                	Ext.getCmp('DeviceOperationLogOperationTypeListComb_Id').setValue('');
                	Ext.getCmp('DeviceOperationLogOperationTypeListComb_Id').setRawValue('');
                	
                	Ext.getCmp('DeviceOperationLogQueryStartDate_Id').setValue('');
                	Ext.getCmp('DeviceOperationLogQueryStartDate_Id').setRawValue('');
                	Ext.getCmp('DeviceOperationLogQueryStartTime_Hour_Id').setValue('');
                	Ext.getCmp('DeviceOperationLogQueryStartTime_Minute_Id').setValue('');
//                	Ext.getCmp('DeviceOperationLogQueryStartTime_Second_Id').setValue('');
                    Ext.getCmp('DeviceOperationLogQueryEndDate_Id').setValue('');
                    Ext.getCmp('DeviceOperationLogQueryEndDate_Id').setRawValue('');
                    Ext.getCmp('DeviceOperationLogQueryEndTime_Hour_Id').setValue('');
                	Ext.getCmp('DeviceOperationLogQueryEndTime_Minute_Id').setValue('');
//                	Ext.getCmp('DeviceOperationLogQueryEndTime_Second_Id').setValue('');
                	
                	var gridPanel = Ext.getCmp("DeviceOperationLogGridPanel_Id");
                	if (isNotVal(gridPanel)) {
                		gridPanel.getStore().loadPage(1);
                	}
                }
    		},'-',deviceCombo,'-',operationTypeCombo,'-',{
                xtype: 'datefield',
                anchor: '100%',
                fieldLabel: loginUserLanguageResource.range,
                labelWidth: getLabelWidth(loginUserLanguageResource.range,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.range,loginUserLanguage)+100,
                format: 'Y-m-d ',
                value: '',
                id: 'DeviceOperationLogQueryStartDate_Id',
                listeners: {
                	select: function (combo, record, index) {
                    }
                }
            },{
            	xtype: 'numberfield',
            	id: 'DeviceOperationLogQueryStartTime_Hour_Id',
            	fieldLabel: loginUserLanguageResource.hour,
                labelWidth: getLabelWidth(loginUserLanguageResource.hour,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.hour,loginUserLanguage)+45,
                minValue: 0,
                maxValue: 23,
                value:'',
                msgTarget: 'none',
                regex:/^(2[0-3]|[0-1]?\d|\*|-|\/)$/,
                listeners: {
                	blur: function (field, event, eOpts) {
                		var r = /^(2[0-3]|[0-1]?\d|\*|-|\/)$/;
                		var flag=r.test(field.value);
                		if(!flag){
                			Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.hourlyValidData);
                			field.focus(true, 100);
                		}
                    }
                }
            },{
            	xtype: 'numberfield',
            	id: 'DeviceOperationLogQueryStartTime_Minute_Id',
            	fieldLabel: loginUserLanguageResource.minute,
                labelWidth: getLabelWidth(loginUserLanguageResource.minute,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.minute,loginUserLanguage)+45,
                minValue: 0,
                maxValue: 59,
                value:'',
                msgTarget: 'none',
                regex:/^[1-5]?\d([\/-][1-5]?\d)?$/,
                listeners: {
                	blur: function (field, event, eOpts) {
                		var r = /^[1-5]?\d([\/-][1-5]?\d)?$/;
                		var flag=r.test(field.value);
                		if(!flag){
                			Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.minuteValidData);
                			field.focus(true, 100);
                		}
                    }
                }
            },{
            	xtype: 'numberfield',
            	id: 'DeviceOperationLogQueryStartTime_Second_Id',
            	hidden: true,
            	fieldLabel: loginUserLanguageResource.second,
                labelWidth: getLabelWidth(loginUserLanguageResource.second,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.second,loginUserLanguage)+45,
                minValue: 0,
                maxValue: 59,
                value:'',
                msgTarget: 'none',
                regex:/^[1-5]?\d([\/-][1-5]?\d)?$/,
                listeners: {
                	blur: function (field, event, eOpts) {
                		var r = /^[1-5]?\d([\/-][1-5]?\d)?$/;
                		var flag=r.test(field.value);
                		if(!flag){
                			Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.secondValidData);
                			field.focus(true, 100);
                		}
                    }
                }
            },{
                xtype: 'datefield',
                anchor: '100%',
                fieldLabel: loginUserLanguageResource.timeTo,
                labelWidth: getLabelWidth(loginUserLanguageResource.timeTo,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.timeTo,loginUserLanguage)+95,
                format: 'Y-m-d ',
                value: '',
                id: 'DeviceOperationLogQueryEndDate_Id',
                listeners: {
                	select: function (combo, record, index) {
                    }
                }
            },{
            	xtype: 'numberfield',
            	id: 'DeviceOperationLogQueryEndTime_Hour_Id',
            	fieldLabel: loginUserLanguageResource.hour,
                labelWidth: getLabelWidth(loginUserLanguageResource.hour,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.hour,loginUserLanguage)+45,
                minValue: 0,
                maxValue: 23,
                value:'',
                msgTarget: 'none',
                regex:/^(2[0-3]|[0-1]?\d|\*|-|\/)$/,
                listeners: {
                	blur: function (field, event, eOpts) {
                		var r = /^(2[0-3]|[0-1]?\d|\*|-|\/)$/;
                		var flag=r.test(field.value);
                		if(!flag){
                			Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.hourlyValidData);
                			field.focus(true, 100);
                		}
                    }
                }
            },{
            	xtype: 'numberfield',
            	id: 'DeviceOperationLogQueryEndTime_Minute_Id',
            	fieldLabel: loginUserLanguageResource.minute,
                labelWidth: getLabelWidth(loginUserLanguageResource.minute,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.minute,loginUserLanguage)+45,
                minValue: 0,
                maxValue: 59,
                value:'',
                msgTarget: 'none',
                regex:/^[1-5]?\d([\/-][1-5]?\d)?$/,
                listeners: {
                	blur: function (field, event, eOpts) {
                		var r = /^[1-5]?\d([\/-][1-5]?\d)?$/;
                		var flag=r.test(field.value);
                		if(!flag){
                			Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.minuteValidData);
                			field.focus(true, 100);
                		}
                    }
                }
            },{
            	xtype: 'numberfield',
            	id: 'DeviceOperationLogQueryEndTime_Second_Id',
            	hidden: true,
            	fieldLabel: loginUserLanguageResource.second,
                labelWidth: getLabelWidth(loginUserLanguageResource.second,loginUserLanguage),
                width: getLabelWidth(loginUserLanguageResource.second,loginUserLanguage)+45,
                minValue: 0,
                maxValue: 59,
                value:'',
                msgTarget: 'none',
                regex:/^[1-5]?\d([\/-][1-5]?\d)?$/,
                listeners: {
                	blur: function (field, event, eOpts) {
                		var r = /^[1-5]?\d([\/-][1-5]?\d)?$/;
                		var flag=r.test(field.value);
                		if(!flag){
                			Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.secondValidData);
                			field.focus(true, 100);
                		}
                    }
                }
            },'-',{
                xtype: 'button',
                text: loginUserLanguageResource.search,
                iconCls: 'search',
                handler: function () {
                	var r = /^(2[0-3]|[0-1]?\d|\*|-|\/)$/;
                	var r2 = /^[1-5]?\d([\/-][1-5]?\d)?$/;
                	var startTime_Hour=Ext.getCmp('DeviceOperationLogQueryStartTime_Hour_Id').getValue();
                	if(!r.test(startTime_Hour)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.hourlyValidData);
                		Ext.getCmp('DeviceOperationLogQueryStartTime_Hour_Id').focus(true, 100);
                		return;
                	}
                	var startTime_Minute=Ext.getCmp('DeviceOperationLogQueryStartTime_Minute_Id').getValue();
                	if(!r2.test(startTime_Minute)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.minuteValidData);
                		Ext.getCmp('DeviceOperationLogQueryStartTime_Minute_Id').focus(true, 100);
                		return;
                	}
                	var startTime_Second=0;
                	
                	var endTime_Hour=Ext.getCmp('DeviceOperationLogQueryEndTime_Hour_Id').getValue();
                	if(!r.test(endTime_Hour)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.hourlyValidData);
                		Ext.getCmp('DeviceOperationLogQueryEndTime_Hour_Id').focus(true, 100);
                		return;
                	}
                	var endTime_Minute=Ext.getCmp('DeviceOperationLogQueryEndTime_Minute_Id').getValue();
                	if(!r2.test(endTime_Minute)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.minuteValidData);
                		Ext.getCmp('DeviceOperationLogQueryEndTime_Minute_Id').focus(true, 100);
                		return;
                	}
                	var endTime_Second=0;
                	
                	var gridPanel = Ext.getCmp("DeviceOperationLogGridPanel_Id");
                	if (isNotVal(gridPanel)) {
                		gridPanel.getStore().loadPage(1);
                	}
                }
            },'-', {
                xtype: 'button',
                text: loginUserLanguageResource.exportData,
                iconCls: 'export',
                hidden:false,
                handler: function (v, o) {
                	var r = /^(2[0-3]|[0-1]?\d|\*|-|\/)$/;
                	var r2 = /^[1-5]?\d([\/-][1-5]?\d)?$/;
                	var startTime_Hour=Ext.getCmp('DeviceOperationLogQueryStartTime_Hour_Id').getValue();
                	if(!r.test(startTime_Hour)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.hourlyValidData);
                		Ext.getCmp('DeviceOperationLogQueryStartTime_Hour_Id').focus(true, 100);
                		return;
                	}
                	var startTime_Minute=Ext.getCmp('DeviceOperationLogQueryStartTime_Minute_Id').getValue();
                	if(!r2.test(startTime_Minute)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.minuteValidData);
                		Ext.getCmp('DeviceOperationLogQueryStartTime_Minute_Id').focus(true, 100);
                		return;
                	}
                	var startTime_Second=0;
                	
                	var endTime_Hour=Ext.getCmp('DeviceOperationLogQueryEndTime_Hour_Id').getValue();
                	if(!r.test(endTime_Hour)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.hourlyValidData);
                		Ext.getCmp('DeviceOperationLogQueryEndTime_Hour_Id').focus(true, 100);
                		return;
                	}
                	var endTime_Minute=Ext.getCmp('DeviceOperationLogQueryEndTime_Minute_Id').getValue();
                	if(!r2.test(endTime_Minute)){
                		Ext.Msg.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.invalidData+"</font>"+loginUserLanguageResource.minuteValidData);
                		Ext.getCmp('DeviceOperationLogQueryEndTime_Minute_Id').focus(true, 100);
                		return;
                	}
                	var endTime_Second=0;
                	
                	var orgId = Ext.getCmp('leftOrg_Id').getValue();
                	var deviceType=getDeviceTypeFromTabId("DeviceOperationLogRootTabPanel");
                	var deviceName=Ext.getCmp('DeviceOperationLogDeviceListComb_Id').getValue();
                	var dictDeviceType=deviceType;
                	if(dictDeviceType.includes(",")){
                		dictDeviceType=getDeviceTypeFromTabId_first("DeviceOperationLogRootTabPanel");
                	}
                	var operationType=Ext.getCmp('DeviceOperationLogOperationTypeListComb_Id').getValue();
                	var startDate=Ext.getCmp('DeviceOperationLogQueryStartDate_Id').rawValue;
                    var endDate=Ext.getCmp('DeviceOperationLogQueryEndDate_Id').rawValue;
               	 	
               	 	var fileName=loginUserLanguageResource.deviceOperationLog;
               	 	var title=loginUserLanguageResource.deviceOperationLog;
               	 	var columnStr=Ext.getCmp("DeviceOperationLogColumnStr_Id").getValue();
               	 	exportDeviceOperationLogExcel(orgId,deviceType,dictDeviceType,deviceName,operationType,getDateAndTime(startDate,startTime_Hour,startTime_Minute,startTime_Second),getDateAndTime(endDate,endTime_Hour,endTime_Minute,endTime_Second),fileName,title,columnStr);
                }
            }]
        });
        this.callParent(arguments);
    }
});