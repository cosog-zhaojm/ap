Ext.define('AP.store.acquisitionUnit.ModbusProtocolSingleWellReportTemplateStore', {
    extend: 'Ext.data.Store',
    alias: 'widget.ModbusProtocolSingleWellReportTemplateStore',
    fields: ['templateName','templateCode','deviceType'],
    autoLoad: true,
    proxy: {
        type: 'ajax',
        url: context + '/acquisitionUnitManagerController/getSingleWellReportDataTemplateList',
        actionMethods: {
            read: 'POST'
        },
        reader: {
            type: 'json',
            rootProperty: 'totalRoot',
            totalProperty: 'totalCount',
            keepRawData: true
        }
    },
    listeners: {
        load: function (store, options, eOpts) {
        	var gridPanel = Ext.getCmp("ReportUnitSingleWellReportTemplateListGridPanel_Id");
            if (!isNotVal(gridPanel)) {
            	gridPanel = Ext.create('Ext.grid.Panel', {
                    id: "ReportUnitSingleWellReportTemplateListGridPanel_Id",
//                    layout: "fit",
                    columnLines: true,
                    forceFit: true,
                    viewConfig: {
                        emptyText: "<div class='con_div_' id='div_lcla_bjgid'><" + cosog.string.nodata + "></div>",
                        forceFit: true
                    },
                    selModel:{
                    	selType: 'checkboxmodel',
                    	mode:'SINGLE',//"SINGLE" / "SIMPLE" / "MULTI" 
                    	checkOnly:false,
                    	allowDeselect:false
                    },
                    store: store,
                    columns: [{
                    	text: '单井日报模板列表',
                        flex: 8,
                        align: 'left',
                        dataIndex: 'templateName',
                        renderer: function (value) {
                            if (isNotVal(value)) {
                                return "<span data-qtip=" + (value == undefined ? "" : value) + ">" + (value == undefined ? "" : value) + "</span>";
                            }
                        }
                    },{
                        header: 'templateCode',
                        hidden: true,
                        dataIndex: 'templateCode'
                    },{
                        header: 'deviceType',
                        hidden: true,
                        dataIndex: 'deviceType'
                    }],
                    listeners: {
                    	checkchange: function (node, checked) {
                    		
                        },
                        selectionchange ( view, selected, eOpts ){
                        	
                        },select( v, record, index, eOpts ){
                        	CreateSingleWellReportTemplateInfoTable(record.data.templateName,record.data.deviceType,record.data.templateCode);
                        }
                    }

                });
                var panel = Ext.getCmp("ReportUnitSingleWellReportTemplateListPanel_Id");
                panel.add(gridPanel);
            }
            
            gridPanel.getSelectionModel().deselectAll(true);
            var selectRow= Ext.getCmp("ModbusProtocolReportUnitConfigSelectRow_Id").getValue();
        	if(selectRow>=0){
        		var selectUnitReportTemplateCode='';
        		var selectUnit = Ext.getCmp("ModbusProtocolReportUnitConfigTreeGridPanel_Id").getSelectionModel().getSelection()[0].data;
            	if(selectUnit.classes==0){
            		if(isNotVal(selectUnit.children) && selectUnit.children.length>0){
            			selectUnitReportTemplateCode=selectUnit.children[0].singlewellReportTemplate;
            		}else{
            			
            		}
            	}else if(selectUnit.classes==1){
            		selectUnitReportTemplateCode=selectUnit.singlewellReportTemplate;
            	}
            	
            	var store = gridPanel.getStore();
            	var selected=false;
            	for(var i=0;i<store.getCount();i++){
					var record=store.getAt(i);
					if(record.data.templateCode==selectUnitReportTemplateCode){
						gridPanel.getSelectionModel().select(i, true);
						selected=true;
						break;
					}
				}
            	if(!selected){
            		if(singleWellReportTemplateHandsontableHelper!=null){
    					if(singleWellReportTemplateHandsontableHelper.hot!=undefined){
    						singleWellReportTemplateHandsontableHelper.hot.destroy();
    					}
    					singleWellReportTemplateHandsontableHelper=null;
    				}
            		Ext.getCmp("ModbusProtocolReportUnitTemplateTableInfoPanel_Id").setTitle('单井报表模板：');
            	}
        	}
//            gridPanel.getSelectionModel().deselectAll(true);
//            gridPanel.getSelectionModel().select(0, true);
        },
        beforeload: function (store, options) {
        	var deviceType=0;
        	var reportType=0;
        	var selectRow= Ext.getCmp("ModbusProtocolReportUnitConfigSelectRow_Id").getValue();
        	if(selectRow>=0){
        		deviceType = Ext.getCmp("ModbusProtocolReportUnitConfigTreeGridPanel_Id").getSelectionModel().getSelection()[0].data.deviceType;
        	}
        	var new_params = {
        			deviceType: deviceType,
        			reportType: reportType
                };
            Ext.apply(store.proxy.extraParams, new_params);
        }
    }
});