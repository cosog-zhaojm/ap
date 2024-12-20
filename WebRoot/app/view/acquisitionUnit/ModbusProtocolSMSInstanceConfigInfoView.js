Ext.define('AP.view.acquisitionUnit.ModbusProtocolSMSInstanceConfigInfoView', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.modbusProtocolSMSInstanceConfigInfoView',
    layout: "fit",
    id:'modbusProtocolSMSInstanceConfigInfoViewId',
    border: false,
    initComponent: function () {
    	var me = this;
    	Ext.apply(me, {
    		items: [{
            	tbar: [{
                    id: 'ModbusProtocolSMSInstanceTreeSelectRow_Id',
                    xtype: 'textfield',
                    value: 0,
                    hidden: true
                },{
                    xtype: 'button',
                    text: loginUserLanguageResource.refresh,
                    iconCls: 'note-refresh',
                    hidden:false,
                    handler: function (v, o) {
                    	var gridPanel = Ext.getCmp("ModbusProtocolSMSInstanceGridPanel_Id");
                        if (isNotVal(gridPanel)) {
                        	gridPanel.getStore().load();
                        }else{
                        	Ext.create('AP.store.acquisitionUnit.ModbusProtocolSMSInstanceStore');
                        }
                    }
        		},'->',{
        			xtype: 'button',
                    text: loginUserLanguageResource.add,
                    disabled:loginUserProtocolConfigModuleRight.editFlag!=1,
                    iconCls: 'add',
                    handler: function (v, o) {
        				addModbusProtocolSMSInstanceConfigData();
        			}
        		},"-",{
        			xtype: 'button',
                    text: loginUserLanguageResource.update,
                    disabled:loginUserProtocolConfigModuleRight.editFlag!=1,
                    iconCls: 'edit',
                    handler: function (v, o) {
                    	modifyModbusProtocolSMSInstanceConfigData();
        			}
        		}, "-", {
                    xtype: 'button',
                    itemId: 'delModbusProtocolSMSInstanceBtnId',
                    id: 'delModbusProtocolSMSInstanceBtn_Id',
                    disabled:loginUserProtocolConfigModuleRight.editFlag!=1,
                    text: loginUserLanguageResource.deleteData,
                    iconCls: 'delete',
                    handler: function (v, o) {
                    	delModbusProtocolSMSInstanceInfo();
        			}
        		}],
                layout: "fit",
                id:'modbusProtocolSMSInstanceConfigPanelId'
            }]
    	});
        this.callParent(arguments);
    }
});

function createProtocolSMSInstanceColumn(columnInfo) {
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
        myColumns += "{text:'" + attr.header + "',lockable:false,align:'center' "+width_;
        if (attr.dataIndex.toUpperCase() == 'id'.toUpperCase()) {
            myColumns += ",xtype: 'rownumberer',sortable : false,locked:false";
        }
        else if (attr.dataIndex.toUpperCase()=='name'.toUpperCase()) {
            myColumns += ",sortable : false,locked:false,dataIndex:'" + attr.dataIndex + "',renderer:function(value){if(isNotVal(value)){return \"<span data-qtip=\"+(value==undefined?\"\":value)+\">\"+(value==undefined?\"\":value)+\"</span>\";}}";
        }
        else {
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
