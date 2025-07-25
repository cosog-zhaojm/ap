var auxiliaryDeviceInfoHandsontableHelper = null;
var auxiliaryDeviceDetailsHandsontableHelper=null;
var pumpingUnitPRTFHandsontableHelper=null;
Ext.define('AP.view.well.AuxiliaryDeviceInfoPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.auxiliaryDeviceInfoPanel',
    id: 'AuxiliaryDeviceInfoPanel_Id',
    layout: 'fit',
    border: false,
    initComponent: function () {
        Ext.apply(this, {
            tbar: [{
                id: 'AuxiliaryDeviceSelectRow_Id',
                xtype: 'textfield',
                value: 0,
                hidden: true
            },{
                id: 'AuxiliaryDeviceSelectEndRow_Id',
                xtype: 'textfield',
                value: 0,
                hidden: true
            },{
                xtype: 'button',
                iconCls: 'note-refresh',
                text: loginUserLanguageResource.refresh,
                hidden: false,
                handler: function (v, o) {
                    CreateAndLoadAuxiliaryDeviceInfoTable();
                }
            },'-', {
                id: 'AuxiliaryDeviceTotalCount_Id',
                xtype: 'component',
                hidden: false,
                tpl: loginUserLanguageResource.totalCount + ': {count}',
                style: 'margin-right:15px'
            }, '->',{
    			xtype: 'button',
                text: loginUserLanguageResource.addDevie,
                iconCls: 'add',
                disabled:loginUserAuxiliaryDeviceManagerModuleRight.editFlag!=1,
                handler: function (v, o) {
                	var window = Ext.create("AP.view.well.AuxiliaryDeviceInfoWindow", {
                        title: loginUserLanguageResource.addDevie
                    });
                    window.show();
                    var deviceType=getDeviceTypeFromTabId("AuxiliaryDeviceManagerTabPanel");
                    Ext.getCmp("auxiliaryDeviceType_Id").setValue(deviceType);
                    Ext.getCmp("addFormAuxiliaryDevice_Id").show();
                    Ext.getCmp("updateFormAuxiliaryDevice_Id").hide();
                    return false;
    			}
    		}, '-',{
    			xtype: 'button',
    			text: loginUserLanguageResource.deleteDevice,
    			iconCls: 'delete',
    			disabled:loginUserAuxiliaryDeviceManagerModuleRight.editFlag!=1,
    			handler: function (v, o) {
    				var startRow= Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue();
    				var endRow= Ext.getCmp("AuxiliaryDeviceSelectEndRow_Id").getValue();
    				if(startRow!=''&&endRow!=''){
    					startRow=parseInt(startRow);
    					endRow=parseInt(endRow);
    					var deleteInfo=loginUserLanguageResource.confirmDelete;
    					if(startRow==endRow){
    						deleteInfo=loginUserLanguageResource.confirmDelete;
    					}
    					
    					Ext.Msg.confirm(loginUserLanguageResource.confirmDelete, deleteInfo, function (btn) {
    			            if (btn == "yes") {
    			            	for(var i=startRow;i<=endRow;i++){
    	    						var rowdata = auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRow(i);
    	    						if (rowdata[0] != null && parseInt(rowdata[0])>0) {
    	    		                    auxiliaryDeviceInfoHandsontableHelper.delidslist.push(rowdata[0]);
    	    		                }
    	    					}
    	    					var saveData={};
    	    	            	saveData.updatelist=[];
    	    	            	saveData.insertlist=[];
    	    	            	saveData.delidslist=auxiliaryDeviceInfoHandsontableHelper.delidslist;
    	    	            	Ext.Ajax.request({
    	    	                    method: 'POST',
    	    	                    url: context + '/wellInformationManagerController/saveAuxiliaryDeviceHandsontableData',
    	    	                    success: function (response) {
    	    	                        rdata = Ext.JSON.decode(response.responseText);
    	    	                        if (rdata.success) {
    	    	                        	Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.deleteSuccessfully);
    	    	                        	auxiliaryDeviceInfoHandsontableHelper.clearContainer();
    	    	                            CreateAndLoadAuxiliaryDeviceInfoTable();
    	    	                        } else {
    	    	                            Ext.MessageBox.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.saveFailure+"</font>");
    	    	                        }
    	    	                    },
    	    	                    failure: function () {
    	    	                        Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.requestFailure);
    	    	                        auxiliaryDeviceInfoHandsontableHelper.clearContainer();
    	    	                    },
    	    	                    params: {
    	    	                        data: JSON.stringify(saveData)
    	    	                    }
    	    	                });
    			            }
    			        });
    				}else{
    					Ext.MessageBox.alert(loginUserLanguageResource.message,loginUserLanguageResource.checkOne);
    				}
    			}
    		}, '-', {
                xtype: 'button',
                itemId: 'saveAuxiliaryDeviceDataBtnId',
                id: 'saveAuxiliaryDeviceDataBtn_Id',
                disabled: false,
                hidden: false,
                text: loginUserLanguageResource.save,
                iconCls: 'save',
                disabled:loginUserAuxiliaryDeviceManagerModuleRight.editFlag!=1,
                handler: function (v, o) {
                    auxiliaryDeviceInfoHandsontableHelper.saveData();
                }
            },"-",{
    			xtype: 'button',
                text: loginUserLanguageResource.batchAdd,
                iconCls: 'batchAdd',
                disabled:loginUserAuxiliaryDeviceManagerModuleRight.editFlag!=1,
                hidden: false,
                handler: function (v, o) {
                	var window = Ext.create("AP.view.well.BatchAddAuxiliaryDeviceWindow", {
                        title: loginUserLanguageResource.batchAdd
                    });
                    window.show();
                    return false;
    			}
    		}, '-', {
                xtype: 'button',
                text: loginUserLanguageResource.exportData,
                iconCls: 'export',
                disabled:loginUserAuxiliaryDeviceManagerModuleRight.editFlag!=1,
                hidden: false,
                handler: function (v, o) {
                	exportAuxiliaryDeviceCompleteData();
//                    var fields = "";
//                    var heads = "";
//                    var leftOrg_Id = Ext.getCmp('leftOrg_Id').getValue();
//                    var deviceType=getDeviceTypeFromTabId("AuxiliaryDeviceManagerTabPanel");
//                    var deviceTypeName=getTabPanelActiveName("AuxiliaryDeviceManagerTabPanel");
//                    var url = context + '/wellInformationManagerController/exportAuxiliaryDeviceData';
//                    for (var i = 0; i < auxiliaryDeviceInfoHandsontableHelper.colHeaders.length; i++) {
//                        fields += auxiliaryDeviceInfoHandsontableHelper.columns[i].data + ",";
//                        heads += auxiliaryDeviceInfoHandsontableHelper.colHeaders[i] + ","
//                    }
//                    if (isNotVal(fields)) {
//                        fields = fields.substring(0, fields.length - 1);
//                        heads = heads.substring(0, heads.length - 1);
//                    }
//                    
//                    var fileName=deviceTypeName+loginUserLanguageResource.auxiliaryDevice;
//                    var title=fileName;
//
//                    var param = "&fields=" + fields 
//                    + "&heads=" + URLencode(URLencode(heads)) 
//                    + "&orgId=" + leftOrg_Id 
//                    + "&deviceType=" + deviceType 
//                    + "&recordCount=10000" 
//                    + "&fileName=" + URLencode(URLencode(fileName)) 
//                    + "&title=" + URLencode(URLencode(title));
//                    openExcelWindow(url + '?flag=true' + param);
                }
            }],
    		layout: 'border',
    		items: [{
    			region: 'center',
    			title:loginUserLanguageResource.deviceList,
        		header: true,
        		layout: 'fit',
        		id:'AuxiliaryDeviceListPanel_Id',
        		html: '<div class="AuxiliaryDeviceContainer" style="width:100%;height:100%;"><div class="con" id="AuxiliaryDeviceTableDiv_id"></div></div>',
                listeners: {
                	resize: function (thisPanel, width, height, oldWidth, oldHeight, eOpts) {
                    	if (auxiliaryDeviceInfoHandsontableHelper != null && auxiliaryDeviceInfoHandsontableHelper.hot != null && auxiliaryDeviceInfoHandsontableHelper.hot != undefined) {
                        	var newWidth=width;
                    		var newHeight=height;
                    		var header=thisPanel.getHeader();
                    		if(header){
                    			newHeight=newHeight-header.lastBox.height-2;
                    		}
                    		auxiliaryDeviceInfoHandsontableHelper.hot.updateSettings({
                    			width:newWidth,
                    			height:newHeight
                    		});
                        }
                    }
                }
    		},{
    			region: 'east',
            	width: '50%',
            	split: true,
            	collapsible: true,
            	title:loginUserLanguageResource.detailedInformation,
        		id:'AuxiliaryDeviceDetailsPanel_Id',
        		layout: 'border',
        		items: [{
        			region: 'center',
        			border:false,
        			html: '<div class="AuxiliaryDeviceDetailsContainer" style="width:100%;height:100%;"><div class="con" id="AuxiliaryDeviceDetailsTableDiv_id"></div></div>',
            		tbar:onlyMonitor?null:[{
                        xtype: 'radiogroup',
                        fieldLabel: loginUserLanguageResource.specificType,
                        hidden: onlyMonitor,
                        labelWidth: getLabelWidth(loginUserLanguageResource.specificType,loginUserLanguage),
                        id: 'AuxiliaryDeviceSpecificType_Id',
                        cls: 'x-check-group-alt',
                        items: [
                            {boxLabel: loginUserLanguageResource.pumping,name: 'auxiliaryDeviceSpecificType',width: getLabelWidth(loginUserLanguageResource.pumping,loginUserLanguage)+20, inputValue: 1},
                            {boxLabel: loginUserLanguageResource.nothing,name: 'auxiliaryDeviceSpecificType',width: getLabelWidth(loginUserLanguageResource.nothing,loginUserLanguage)+20, inputValue: 0}
                        ],
                        listeners: {
                        	change: function (radiogroup, newValue, oldValue, eOpts) {
                				var deviceId=0;
                				var specificType=0;
                				var name='';
                				var DeviceSelectRow= Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue();
                				if(isNotVal(DeviceSelectRow)){
                					deviceId=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(DeviceSelectRow,'id');
                	            	specificType=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(DeviceSelectRow,'specificType');
                	            	name=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(DeviceSelectRow,'name');
                				}
                				CreateAuxiliaryDeviceDetailsTable(deviceId,name);
                				
                				
                				Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setValue('');
                    			Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setRawValue('');
                				CreateAndLoadPumpingUnitPTFTable(deviceId,name);
                          	}
                        }
                    }],
                    listeners: {
                        resize: function (thisPanel, width, height, oldWidth, oldHeight, eOpts) {
                        	if (auxiliaryDeviceDetailsHandsontableHelper != null && auxiliaryDeviceDetailsHandsontableHelper.hot != null && auxiliaryDeviceDetailsHandsontableHelper.hot != undefined) {
                        		var newWidth=width;
                        		var newHeight=height-22-1;
                        		var header=thisPanel.getHeader();
                        		if(header){
                        			newHeight=newHeight-header.lastBox.height-2;
                        		}
                        		auxiliaryDeviceDetailsHandsontableHelper.hot.updateSettings({
                        			width:newWidth,
                        			height:newHeight
                        		});
                            }
                        }
                    }
        		},{
        			region: 'south',
        			height: '55%',
        			title: loginUserLanguageResource.pumpingUnitPRTF,
        			split: true,
                	collapsible: true,
                	id:'AuxiliaryDevicePumpingUnitPRTFPanel_Id',
                	hidden: true,
                	html: '<div class="AuxiliaryDevicePumpingUnitPRTFContainer" style="width:100%;height:100%;"><div class="con" id="AuxiliaryDevicePumpingUnitPRTFTableDiv_id"></div></div>',
                	tbar: [{
                        xtype: "combobox",
                        fieldLabel: loginUserLanguageResource.stroke,
                        id: 'AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id',
                        labelWidth: 30,
                        width: 140,
                        labelAlign: 'left',
                        triggerAction: 'all',
                        displayField: "boxval",
                        valueField: "boxkey",
                        selectOnFocus: true,
                        forceSelection: true,
                        value: '',
                        allowBlank: true,
                        editable: false,
                        emptyText: cosog.string.all,
                        blankText: cosog.string.all,
                        store: new Ext.data.SimpleStore({
                            fields: ['boxkey', 'boxval'],
                            data: [['','']]
                        }),
                        queryMode: 'local',
                        listeners: {
                            select: function (v, o) {
                            	var deviceId=0;
                				var name='';
                				var DeviceSelectRow= Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue();
                				if(isNotVal(DeviceSelectRow)){
                					deviceId=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(DeviceSelectRow,'id');
                	            	name=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(DeviceSelectRow,'name');
                				}
                				
                				CreateAndLoadPumpingUnitPTFTable(deviceId,name);
                            }
                        }
                    },'->', {
                        xtype: 'button',
                        id: 'saveAuxiliaryDevicePumpingUnitPRTFDataBtn_Id',
                        disabled: false,
                        hidden: false,
                        text: loginUserLanguageResource.save,
                        iconCls: 'save',
                        handler: function (v, o) {
                        	pumpingUnitPRTFHandsontableHelper.saveData();
                        }
                    }],
                	listeners: {
                        resize: function (thisPanel, width, height, oldWidth, oldHeight, eOpts) {
                        	if (pumpingUnitPRTFHandsontableHelper != null && pumpingUnitPRTFHandsontableHelper.hot != null && pumpingUnitPRTFHandsontableHelper.hot != undefined) {
                        		var newWidth=width;
                        		var newHeight=height-22-1;
                        		var header=thisPanel.getHeader();
                        		if(header){
                        			newHeight=newHeight-header.lastBox.height-2;
                        		}
                        		pumpingUnitPRTFHandsontableHelper.hot.updateSettings({
                        			width:newWidth,
                        			height:newHeight
                        		});
                            }
                        }
                    }
        		}]
    		}],
            listeners: {
                beforeclose: function (panel, eOpts) {
                    
                }
            }
        })
        this.callParent(arguments);
    }
});

function CreateAndLoadAuxiliaryDeviceInfoTable(isNew) {
    if(isNew&&auxiliaryDeviceInfoHandsontableHelper!=null){
    	if(auxiliaryDeviceInfoHandsontableHelper.hot!=undefined){
    		auxiliaryDeviceInfoHandsontableHelper.hot.destroy();
    	}
    	auxiliaryDeviceInfoHandsontableHelper=null;
    }
    var deviceType=getDeviceTypeFromTabId("AuxiliaryDeviceManagerTabPanel");
    Ext.Ajax.request({
        method: 'POST',
        url: context + '/wellInformationManagerController/doAuxiliaryDeviceShow',
        success: function (response) {
            var result = Ext.JSON.decode(response.responseText);
            if (auxiliaryDeviceInfoHandsontableHelper == null || auxiliaryDeviceInfoHandsontableHelper.hot == null || auxiliaryDeviceInfoHandsontableHelper.hot == undefined) {
                auxiliaryDeviceInfoHandsontableHelper = AuxiliaryDeviceInfoHandsontableHelper.createNew("AuxiliaryDeviceTableDiv_id");
                var colHeaders="['"+loginUserLanguageResource.idx+"','类型','"+loginUserLanguageResource.deviceName+"','"+loginUserLanguageResource.manufacturer+"','"+loginUserLanguageResource.model+"','"+loginUserLanguageResource.remark+"','"+loginUserLanguageResource.sortNum+"']";
                var columns="[{data:'id'}," 
                		+"{data:'specificType'}," 
                		+"{data:'name'}," 
                		+"{data:'manufacturer'}," 
                		+"{data:'model'},"
                		+"{data:'remark'}," 
                		+"{data:'sort',type:'text',allowInvalid: true, validator: function(val, callback){return handsontableDataCheck_Num_Nullable(val, callback,this.row, this.col,auxiliaryDeviceInfoHandsontableHelper);}}" 
                		+"]";
                auxiliaryDeviceInfoHandsontableHelper.colHeaders = Ext.JSON.decode(colHeaders);
                auxiliaryDeviceInfoHandsontableHelper.columns = Ext.JSON.decode(columns);
                auxiliaryDeviceInfoHandsontableHelper.createTable(result.totalRoot);
            } else {
                auxiliaryDeviceInfoHandsontableHelper.hot.loadData(result.totalRoot);
            }
            if(result.totalRoot.length==0){
            	Ext.getCmp("AuxiliaryDeviceSelectRow_Id").setValue('');
            	Ext.getCmp("AuxiliaryDeviceSelectEndRow_Id").setValue('');
            	auxiliaryDeviceInfoHandsontableHelper.hot.selectCell(0,'name');
            	CreateAndLoadAuxiliaryDeviceDetailsTable(0,0,'');
            	
            	
            }else{
            	Ext.getCmp("AuxiliaryDeviceSelectRow_Id").setValue(0);
            	Ext.getCmp("AuxiliaryDeviceSelectEndRow_Id").setValue(0);
            	auxiliaryDeviceInfoHandsontableHelper.hot.selectCell(0,'name');
            	var recordId=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(0,'id');
            	var specificType=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(0,'specificType');
            	var name=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(0,'name');
            	CreateAndLoadAuxiliaryDeviceDetailsTable(recordId,specificType,name);
            }
            Ext.getCmp("AuxiliaryDeviceTotalCount_Id").update({
                count: result.totalCount
            });
            
            
        },
        failure: function () {
            Ext.MessageBox.alert(loginUserLanguageResource.error,loginUserLanguageResource.errorInfo);
        },
        params: {
            deviceType: deviceType,
            recordCount: 50,
            page: 1,
            limit: 10000
        }
    });
};

var AuxiliaryDeviceInfoHandsontableHelper = {
    createNew: function (divid) {
        var auxiliaryDeviceInfoHandsontableHelper = {};
        auxiliaryDeviceInfoHandsontableHelper.hot = '';
        auxiliaryDeviceInfoHandsontableHelper.divid = divid;
        auxiliaryDeviceInfoHandsontableHelper.validresult = true; //数据校验
        auxiliaryDeviceInfoHandsontableHelper.colHeaders = [];
        auxiliaryDeviceInfoHandsontableHelper.columns = [];

        auxiliaryDeviceInfoHandsontableHelper.AllData = {};
        auxiliaryDeviceInfoHandsontableHelper.updatelist = [];
        auxiliaryDeviceInfoHandsontableHelper.delidslist = [];
        auxiliaryDeviceInfoHandsontableHelper.insertlist = [];
        auxiliaryDeviceInfoHandsontableHelper.editNameList = [];

        auxiliaryDeviceInfoHandsontableHelper.addCellStyle = function (instance, td, row, col, prop, value, cellProperties) {
            Handsontable.renderers.TextRenderer.apply(this, arguments);
            td.style.whiteSpace='nowrap'; //文本不换行
        	td.style.overflow='hidden';//超出部分隐藏
        	td.style.textOverflow='ellipsis';//使用省略号表示溢出的文本
        }

        auxiliaryDeviceInfoHandsontableHelper.createTable = function (data) {
            $('#' + auxiliaryDeviceInfoHandsontableHelper.divid).empty();
            var hotElement = document.querySelector('#' + auxiliaryDeviceInfoHandsontableHelper.divid);
            auxiliaryDeviceInfoHandsontableHelper.hot = new Handsontable(hotElement, {
            	licenseKey: '96860-f3be6-b4941-2bd32-fd62b',
            	data: data,
                hiddenColumns: {
                    columns: [0,1],
                    indicators: false
                },
                columns: auxiliaryDeviceInfoHandsontableHelper.columns,
                stretchH: 'all', //延伸列的宽度, last:延伸最后一列,all:延伸所有列,none默认不延伸
                autoWrapRow: true,
                rowHeaders: true, //显示行头
                colHeaders: auxiliaryDeviceInfoHandsontableHelper.colHeaders, //显示列头
                columnSorting: true, //允许排序
                allowInsertRow:false,
                sortIndicator: true,
                manualColumnResize: true, //当值为true时，允许拖动，当为false时禁止拖动
                manualRowResize: true, //当值为true时，允许拖动，当为false时禁止拖动
                //	                dropdownMenu: ['filter_by_condition', 'filter_by_value', 'filter_action_bar'],
                filters: true,
                renderAllRows: true,
                search: true,
                outsideClickDeselects:false,
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
                    
                    var AuxiliaryDeviceManagerModuleEditFlag=parseInt(Ext.getCmp("AuxiliaryDeviceManagerModuleEditFlag").getValue());
                    if(AuxiliaryDeviceManagerModuleEditFlag!=1){
                    	cellProperties.readOnly = true;
                    }
                    cellProperties.renderer = auxiliaryDeviceInfoHandsontableHelper.addCellStyle;
                    return cellProperties;
                },
                afterSelectionEnd : function (row,column,row2,column2, preventScrolling,selectionLayerLevel) {
                	if(row<0 && row2<0){//只选中表头
                		Ext.getCmp("AuxiliaryDeviceSelectRow_Id").setValue('');
                    	Ext.getCmp("AuxiliaryDeviceSelectEndRow_Id").setValue('');
                	}else{
                		if(row<0){
                    		row=0;
                    	}
                    	if(row2<0){
                    		row2=0;
                    	}
                    	var startRow=row;
                    	var endRow=row2;
                    	if(row>row2){
                    		startRow=row2;
                        	endRow=row;
                    	}
                    	
                    	var selectedRow=Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue();
                    	if(selectedRow!=startRow){
                    		Ext.getCmp("AuxiliaryDeviceSelectRow_Id").setValue(startRow);
                        	Ext.getCmp("AuxiliaryDeviceSelectEndRow_Id").setValue(endRow);
                        	var recordId=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(startRow,'id');
                        	var specificType=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(startRow,'specificType');
                        	var name=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(startRow,'name');
                        	
                        	CreateAndLoadAuxiliaryDeviceDetailsTable(recordId,specificType,name);
                        	
                        	
                        	
                    	}else{
                    		Ext.getCmp("AuxiliaryDeviceSelectRow_Id").setValue(startRow);
                        	Ext.getCmp("AuxiliaryDeviceSelectEndRow_Id").setValue(endRow);
                    	}
                	}
                },
                afterDestroy: function () {
                },
                beforeRemoveRow: function (index, amount) {
                    var ids = [];
                    //封装id成array传入后台
                    if (amount != 0) {
                        for (var i = index; i < amount + index; i++) {
                            var rowdata = auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRow(i);
                            ids.push(rowdata[0]);
                        }
                        auxiliaryDeviceInfoHandsontableHelper.delExpressCount(ids);
                        auxiliaryDeviceInfoHandsontableHelper.screening();
                    }
                },
                afterChange: function (changes, source) {
                    //params 参数 1.column num , 2,id, 3,oldvalue , 4.newvalue
                    if (changes != null) {
                        for (var i = 0; i < changes.length; i++) {
                            var params = [];
                            var index = changes[i][0]; //行号码
                            var rowdata = auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRow(index);
                            params.push(rowdata[0]);
                            params.push(changes[i][1]);
                            params.push(changes[i][2]);
                            params.push(changes[i][3]);
                            if ("edit" == source && params[1] == "name") { //编辑井名单元格
                                var data = "{\"oldName\":\"" + params[2] + "\",\"newName\":\"" + params[3] + "\"}";
                                auxiliaryDeviceInfoHandsontableHelper.editNameList.push(Ext.JSON.decode(data));
                            }

                            //仅当单元格发生改变的时候,id!=null,说明是更新
                            if (params[2] != params[3] && params[0] != null && params[0] > 0) {
                                var data = "{";
                                for (var j = 0; j < auxiliaryDeviceInfoHandsontableHelper.columns.length; j++) {
                                    data += auxiliaryDeviceInfoHandsontableHelper.columns[j].data + ":'" + rowdata[j] + "'";
                                    if (j < auxiliaryDeviceInfoHandsontableHelper.columns.length - 1) {
                                        data += ","
                                    }
                                }
                                data += "}"
                                auxiliaryDeviceInfoHandsontableHelper.updateExpressCount(Ext.JSON.decode(data));
                            }
                        }
                    }
                },
                afterOnCellMouseOver: function(event, coords, TD){
                	if(coords.col>=0 && coords.row>=0 && auxiliaryDeviceInfoHandsontableHelper!=null&&auxiliaryDeviceInfoHandsontableHelper.hot!=''&&auxiliaryDeviceInfoHandsontableHelper.hot!=undefined && auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtCell!=undefined){
                		var rawValue=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtCell(coords.row,coords.col);
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
            					var height=28;
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
            });
        }
        //插入的数据的获取
        auxiliaryDeviceInfoHandsontableHelper.insertExpressCount = function () {
            var idsdata = auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtCol(0); //所有的id
            for (var i = 0; i < idsdata.length; i++) {
                //id=null时,是插入数据,此时的i正好是行号
                if (idsdata[i] == null || idsdata[i] < 0) {
                    //获得id=null时的所有数据封装进data
                    var rowdata = auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRow(i);
                    //var collength = hot.countCols();
                    if (rowdata != null) {
                        var data = "{";
                        for (var j = 0; j < auxiliaryDeviceInfoHandsontableHelper.columns.length; j++) {
                            data += auxiliaryDeviceInfoHandsontableHelper.columns[j].data + ":'" + rowdata[j] + "'";
                            if (j < auxiliaryDeviceInfoHandsontableHelper.columns.length - 1) {
                                data += ","
                            }
                        }
                        data += "}"
                        auxiliaryDeviceInfoHandsontableHelper.insertlist.push(Ext.JSON.decode(data));
                    }
                }
            }
            if (auxiliaryDeviceInfoHandsontableHelper.insertlist.length != 0) {
                auxiliaryDeviceInfoHandsontableHelper.AllData.insertlist = auxiliaryDeviceInfoHandsontableHelper.insertlist;
            }
        }
        //保存数据
        auxiliaryDeviceInfoHandsontableHelper.saveData = function () {
        	var auxiliaryDeviceInfoHandsontableData=auxiliaryDeviceInfoHandsontableHelper.hot.getData();
        	if(auxiliaryDeviceInfoHandsontableData.length>0){
        		auxiliaryDeviceInfoHandsontableHelper.insertExpressCount();
        		var auxiliaryDeviceSpecificType=0;
        		if(Ext.getCmp("AuxiliaryDeviceSpecificType_Id")!=undefined){
        			auxiliaryDeviceSpecificType=Ext.getCmp("AuxiliaryDeviceSpecificType_Id").getValue().auxiliaryDeviceSpecificType;
        		}
                
                var DeviceSelectRow= Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue();
                var rowdata = auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRow(DeviceSelectRow);
            	var deviceId=rowdata[0];
            	var manufacturer=rowdata[3];
            	var model=rowdata[4];
                
            	var auxiliaryDeviceDetailsSaveData={};
            	auxiliaryDeviceDetailsSaveData.deviceId=deviceId;
            	auxiliaryDeviceDetailsSaveData.auxiliaryDeviceSpecificType=auxiliaryDeviceSpecificType;
            	auxiliaryDeviceDetailsSaveData.auxiliaryDeviceDetailsList=[];
            	

        		if(auxiliaryDeviceDetailsHandsontableHelper!=null && auxiliaryDeviceDetailsHandsontableHelper.hot!=undefined){
            		var auxiliaryDeviceDetailsData=auxiliaryDeviceDetailsHandsontableHelper.hot.getData();
                	Ext.Array.each(auxiliaryDeviceDetailsData, function (name, index, countriesItSelf) {
                        if (isNotVal(auxiliaryDeviceDetailsData[index][1])) {
                        	var auxiliaryDeviceDetails={};
                        	auxiliaryDeviceDetails.deviceId=deviceId;
                        	
                        	var itemName=auxiliaryDeviceDetailsData[index][1];
                        	var itemValue=isNotVal(auxiliaryDeviceDetailsData[index][2])?auxiliaryDeviceDetailsData[index][2]:"";
                        	var itemUnit=isNotVal(auxiliaryDeviceDetailsData[index][3])?auxiliaryDeviceDetailsData[index][3]:"";
                        	var itemCode=isNotVal(auxiliaryDeviceDetailsData[index][4])?auxiliaryDeviceDetailsData[index][4]:"";
                        	
                        	if(auxiliaryDeviceSpecificType==1 && itemCode.toUpperCase()=='structureType'.toUpperCase()){
                        		if(itemValue==loginUserLanguageResource.pumpingUnitStructureType1){
                        			itemValue=1;
                        		}else if(itemValue==loginUserLanguageResource.pumpingUnitStructureType2){
                        			itemValue=2;
                        		}else if(itemValue==loginUserLanguageResource.pumpingUnitStructureType3){
                        			itemValue=3;
                        		}
                        	}else if(auxiliaryDeviceSpecificType==1 && itemCode.toUpperCase()=='crankRotationDirection'.toUpperCase()){
                        		if(itemValue==loginUserLanguageResource.clockwise){
                        			itemValue='Clockwise';
                        		}else if(itemValue==loginUserLanguageResource.anticlockwise){
                        			itemValue='Anticlockwise';
                        		}else{
                        			itemValue='';
                        		}
                        	}
                        	auxiliaryDeviceDetails.itemName=itemName;
                        	auxiliaryDeviceDetails.itemValue=itemValue;
                        	auxiliaryDeviceDetails.itemUnit=itemUnit;
                        	auxiliaryDeviceDetails.itemCode=itemCode;
                        	auxiliaryDeviceDetailsSaveData.auxiliaryDeviceDetailsList.push(auxiliaryDeviceDetails);
                        }
                    });
            	}
                Ext.Ajax.request({
                    method: 'POST',
                    url: context + '/wellInformationManagerController/saveAuxiliaryDeviceHandsontableData',
                    success: function (response) {
                    	rdata = Ext.JSON.decode(response.responseText);
                        if (rdata.success) {
                        	var saveInfo=loginUserLanguageResource.saveSuccessfully;
                        	if(rdata.collisionCount>0){//数据冲突
                        		saveInfo=loginUserLanguageResource.saveSuccessfully+":"+rdata.successCount+','+loginUserLanguageResourceFirstLower.saveFailure+':<font color="red">'+rdata.collisionCount+'</font>';
                        		for(var i=0;i<rdata.list.length;i++){
                        			saveInfo+='<br/><font color="red"> '+rdata.list[i]+'</font>';
                        		}
                        	}
                        	Ext.MessageBox.alert(loginUserLanguageResource.message, saveInfo);
                        	auxiliaryDeviceInfoHandsontableHelper.clearContainer();
                            CreateAndLoadAuxiliaryDeviceInfoTable();
                        } else {
                            Ext.MessageBox.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.saveFailure+"</font>");
                        }
                    },
                    failure: function () {
                        Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.requestFailure);
                        auxiliaryDeviceInfoHandsontableHelper.clearContainer();
                    },
                    params: {
                    	deviceId:deviceId,
                    	auxiliaryDeviceSpecificType:auxiliaryDeviceSpecificType,
                    	data: JSON.stringify(auxiliaryDeviceInfoHandsontableHelper.AllData),
                    	deviceType: getDeviceTypeFromTabId("AuxiliaryDeviceManagerTabPanel"),
                    	auxiliaryDeviceDetailsSaveData: JSON.stringify(auxiliaryDeviceDetailsSaveData)
                    }
                });
        	}else{
        		Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.noDataChange);
            }
        }

        //修改设备名称
        auxiliaryDeviceInfoHandsontableHelper.editWellName = function () {
            if (auxiliaryDeviceInfoHandsontableHelper.editNameList.length > 0 && auxiliaryDeviceInfoHandsontableHelper.validresult) {
                Ext.Ajax.request({
                    method: 'POST',
                    url: context + '/wellInformationManagerController/editAuxiliaryDeviceName',
                    success: function (response) {
                        rdata = Ext.JSON.decode(response.responseText);
                        if (rdata.success) {
                            Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.saveSuccessfully);
                            auxiliaryDeviceInfoHandsontableHelper.clearContainer();
                            CreateAndLoadAuxiliaryDeviceInfoTable();
                        } else {
                            Ext.MessageBox.alert(loginUserLanguageResource.message, "<font color=red>"+loginUserLanguageResource.saveFailure+"</font>");
                        }
                    },
                    failure: function () {
                        Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.requestFailure);
                        auxiliaryDeviceInfoHandsontableHelper.clearContainer();
                    },
                    params: {
                        data: JSON.stringify(auxiliaryDeviceInfoHandsontableHelper.editNameList)
                    }
                });
            } else {
                if (!auxiliaryDeviceInfoHandsontableHelper.validresult) {
                    Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.dataTypeError);
                } else {
                    Ext.MessageBox.alert(loginUserLanguageResource.message, loginUserLanguageResource.noDataChange);
                }
            }
        }


        //删除的优先级最高
        auxiliaryDeviceInfoHandsontableHelper.delExpressCount = function (ids) {
            //传入的ids.length不可能为0
            $.each(ids, function (index, id) {
                if (id != null) {
                    auxiliaryDeviceInfoHandsontableHelper.delidslist.push(id);
                }
            });
            auxiliaryDeviceInfoHandsontableHelper.AllData.delidslist = auxiliaryDeviceInfoHandsontableHelper.delidslist;
        }

        //updatelist数据更新
        auxiliaryDeviceInfoHandsontableHelper.screening = function () {
            if (auxiliaryDeviceInfoHandsontableHelper.updatelist.length != 0 && auxiliaryDeviceInfoHandsontableHelper.delidslist.lentgh != 0) {
                for (var i = 0; i < auxiliaryDeviceInfoHandsontableHelper.delidslist.length; i++) {
                    for (var j = 0; j < auxiliaryDeviceInfoHandsontableHelper.updatelist.length; j++) {
                        if (auxiliaryDeviceInfoHandsontableHelper.updatelist[j].id == auxiliaryDeviceInfoHandsontableHelper.delidslist[i]) {
                            //更新updatelist
                            auxiliaryDeviceInfoHandsontableHelper.updatelist.splice(j, 1);
                        }
                    }
                }
                //把updatelist封装进AllData
                auxiliaryDeviceInfoHandsontableHelper.AllData.updatelist = auxiliaryDeviceInfoHandsontableHelper.updatelist;
            }
        }

        //更新数据
        auxiliaryDeviceInfoHandsontableHelper.updateExpressCount = function (data) {
            if (JSON.stringify(data) != "{}") {
                var flag = true;
                //判断记录是否存在,更新数据     
                $.each(auxiliaryDeviceInfoHandsontableHelper.updatelist, function (index, node) {
                    if (node.id == data.id) {
                        //此记录已经有了
                        flag = false;
                        //用新得到的记录替换原来的,不用新增
                        auxiliaryDeviceInfoHandsontableHelper.updatelist[index] = data;
                    }
                });
                flag && auxiliaryDeviceInfoHandsontableHelper.updatelist.push(data);
                //封装
                auxiliaryDeviceInfoHandsontableHelper.AllData.updatelist = auxiliaryDeviceInfoHandsontableHelper.updatelist;
            }
        }

        auxiliaryDeviceInfoHandsontableHelper.clearContainer = function () {
            auxiliaryDeviceInfoHandsontableHelper.AllData = {};
            auxiliaryDeviceInfoHandsontableHelper.updatelist = [];
            auxiliaryDeviceInfoHandsontableHelper.delidslist = [];
            auxiliaryDeviceInfoHandsontableHelper.insertlist = [];
            auxiliaryDeviceInfoHandsontableHelper.editNameList = [];
        }

        return auxiliaryDeviceInfoHandsontableHelper;
    }
};

function CreateAndLoadAuxiliaryDeviceDetailsTable(deviceId,specificType,name){
	var auxiliaryDeviceSpecificType=0;
	if(Ext.getCmp("AuxiliaryDeviceSpecificType_Id")!=undefined){
		auxiliaryDeviceSpecificType=Ext.getCmp("AuxiliaryDeviceSpecificType_Id").getValue().auxiliaryDeviceSpecificType;
	}
	if(specificType!=auxiliaryDeviceSpecificType){
		if(Ext.getCmp("AuxiliaryDeviceSpecificType_Id")!=undefined){
			Ext.getCmp('AuxiliaryDeviceSpecificType_Id').setValue({auxiliaryDeviceSpecificType:specificType});
		}
	}else{
		CreateAuxiliaryDeviceDetailsTable(deviceId,name);
		
		Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setValue('');
		Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setRawValue('');
		CreateAndLoadPumpingUnitPTFTable(deviceId,name);
	}
}

function CreateAuxiliaryDeviceDetailsTable(deviceId,name){
	if(auxiliaryDeviceDetailsHandsontableHelper!=null){
		if(auxiliaryDeviceDetailsHandsontableHelper.hot!=undefined){
			auxiliaryDeviceDetailsHandsontableHelper.hot.destroy();
		}
		auxiliaryDeviceDetailsHandsontableHelper=null;
	}
	var showInfo=loginUserLanguageResource.detailedInformation;
	if(isNotVal(name)){
		showInfo="【<font color=red>"+name+"</font>】"+showInfo;
	}
	Ext.getCmp("AuxiliaryDeviceDetailsPanel_Id").setTitle(showInfo);
	var auxiliaryDeviceSpecificType=0;
	if(Ext.getCmp("AuxiliaryDeviceSpecificType_Id")!=undefined){
		auxiliaryDeviceSpecificType=Ext.getCmp("AuxiliaryDeviceSpecificType_Id").getValue().auxiliaryDeviceSpecificType;
	}

	Ext.Ajax.request({
		method:'POST',
		url:context + '/wellInformationManagerController/getAuxiliaryDeviceDetailsInfo',
		success:function(response) {
			var result =  Ext.JSON.decode(response.responseText);
			if(auxiliaryDeviceDetailsHandsontableHelper==null || auxiliaryDeviceDetailsHandsontableHelper.hot==undefined){
				auxiliaryDeviceDetailsHandsontableHelper = AuxiliaryDeviceDetailsHandsontableHelper.createNew("AuxiliaryDeviceDetailsTableDiv_id");
				var colHeaders="['"+loginUserLanguageResource.idx+"','"+loginUserLanguageResource.name+"','"+loginUserLanguageResource.variable+"','"+loginUserLanguageResource.unit+"','itemCode']";
				var columns="[{data:'id'},{data:'itemName'},{data:'itemValue'},{data:'itemUnit'},{data:'itemCode'}]";
				
				auxiliaryDeviceDetailsHandsontableHelper.colHeaders=Ext.JSON.decode(colHeaders);
				auxiliaryDeviceDetailsHandsontableHelper.columns=Ext.JSON.decode(columns);
				if(result.totalRoot.length==0){
					auxiliaryDeviceDetailsHandsontableHelper.createTable([{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}]);
				}else{
					auxiliaryDeviceDetailsHandsontableHelper.createTable(result.totalRoot);
				}
			}else{
				if(result.totalRoot.length==0){
					auxiliaryDeviceDetailsHandsontableHelper.hot.loadData([{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}]);
				}else{
					auxiliaryDeviceDetailsHandsontableHelper.hot.loadData(result.totalRoot);
				}
			}
		},
		failure:function(){
			Ext.MessageBox.alert(loginUserLanguageResource.error,loginUserLanguageResource.errorInfo);
		},
		params: {
			deviceId:deviceId,
			auxiliaryDeviceSpecificType: auxiliaryDeviceSpecificType
        }
	});
};

var AuxiliaryDeviceDetailsHandsontableHelper = {
	    createNew: function (divid) {
	        var auxiliaryDeviceDetailsHandsontableHelper = {};
	        auxiliaryDeviceDetailsHandsontableHelper.hot = '';
	        auxiliaryDeviceDetailsHandsontableHelper.divid = divid;
	        auxiliaryDeviceDetailsHandsontableHelper.colHeaders = [];
	        auxiliaryDeviceDetailsHandsontableHelper.columns = [];
	        auxiliaryDeviceDetailsHandsontableHelper.addColBg = function (instance, td, row, col, prop, value, cellProperties) {
	            Handsontable.renderers.TextRenderer.apply(this, arguments);
	            td.style.backgroundColor = 'rgb(242, 242, 242)';
	        }

	        auxiliaryDeviceDetailsHandsontableHelper.addBoldBg = function (instance, td, row, col, prop, value, cellProperties) {
	            Handsontable.renderers.TextRenderer.apply(this, arguments);
	            td.style.backgroundColor = 'rgb(245, 245, 245)';
	        }
	        
	        auxiliaryDeviceDetailsHandsontableHelper.createTable = function (data) {
	            $('#' + auxiliaryDeviceDetailsHandsontableHelper.divid).empty();
	            var hotElement = document.querySelector('#' + auxiliaryDeviceDetailsHandsontableHelper.divid);
	            auxiliaryDeviceDetailsHandsontableHelper.hot = new Handsontable(hotElement, {
	            	licenseKey: '96860-f3be6-b4941-2bd32-fd62b',
	            	data: data,
	                hiddenColumns: {
	                    columns: [0,4],
	                    indicators: false
	                },
	                columns: auxiliaryDeviceDetailsHandsontableHelper.columns,
	                stretchH: 'all', //延伸列的宽度, last:延伸最后一列,all:延伸所有列,none默认不延伸
	                autoWrapRow: true,
	                rowHeaders: true, //显示行头
	                colHeaders: auxiliaryDeviceDetailsHandsontableHelper.colHeaders, //显示列头
	                columnSorting: true, //允许排序
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
	                contextMenu: {
	                    items: {
	                        "row_above": {
	                            name: loginUserLanguageResource.contextMenu_insertRowAbove,
	                        },
	                        "row_below": {
	                            name: loginUserLanguageResource.contextMenu_insertRowBelow,
	                        },
	                        "col_left": {
	                            name: loginUserLanguageResource.contextMenu_insertColumnLeft,
	                        },
	                        "col_right": {
	                            name: loginUserLanguageResource.contextMenu_insertColumnRight,
	                        },
	                        "remove_row": {
	                            name: loginUserLanguageResource.contextMenu_removeRow,
	                        },
	                        "remove_col": {
	                            name: loginUserLanguageResource.contextMenu_removeColumn,
	                        },
	                        "merge_cell": {
	                            name: loginUserLanguageResource.contextMenu_mergeCell,
	                        },
	                        "copy": {
	                            name: loginUserLanguageResource.contextMenu_copy,
	                        },
	                        "cut": {
	                            name: loginUserLanguageResource.contextMenu_cut,
	                        }
//	                        ,
//	                        "paste": {
//	                            name: loginUserLanguageResource.contextMenu_paste,
//	                            disabled: function () {
//	                            },
//	                            callback: function () {
//	                            }
//	                        }
	                    }
	                }, 
	                sortIndicator: true,
	                manualColumnResize: true, //当值为true时，允许拖动，当为false时禁止拖动
	                manualRowResize: true, //当值为true时，允许拖动，当为false时禁止拖动
	                filters: true,
	                renderAllRows: true,
	                search: true,
	                cells: function (row, col, prop) {
	                    var cellProperties = {};
	                    var visualRowIndex = this.instance.toVisualRow(row);
	                    var visualColIndex = this.instance.toVisualColumn(col);
	                    var AuxiliaryDeviceManagerModuleEditFlag=parseInt(Ext.getCmp("AuxiliaryDeviceManagerModuleEditFlag").getValue());
	                    if(AuxiliaryDeviceManagerModuleEditFlag!=1){
	                    	cellProperties.readOnly = true;
	                    }else{
	                    	var auxiliaryDeviceSpecificType=0;
	                    	if(Ext.getCmp("AuxiliaryDeviceSpecificType_Id")!=undefined){
	                    		auxiliaryDeviceSpecificType=Ext.getCmp("AuxiliaryDeviceSpecificType_Id").getValue().auxiliaryDeviceSpecificType;
	                    	}
	                    	if(auxiliaryDeviceSpecificType==1){
	                    		if(visualColIndex!=2){
	                    			cellProperties.readOnly = true;
	                    			cellProperties.renderer = auxiliaryDeviceDetailsHandsontableHelper.addBoldBg;
	                    		}
	                    		
	                    		if (visualColIndex === 2 && visualRowIndex===0) {
			                    	this.type = 'dropdown';
			                    	this.source = [loginUserLanguageResource.pumpingUnitStructureType1,loginUserLanguageResource.pumpingUnitStructureType2,loginUserLanguageResource.pumpingUnitStructureType3];
			                    	this.strict = true;
			                    	this.allowInvalid = false;
			                    }
	                    		if (visualColIndex === 2 && visualRowIndex===2) {
			                    	this.type = 'dropdown';
			                    	this.source = [loginUserLanguageResource.clockwise,loginUserLanguageResource.anticlockwise];
			                    	this.strict = true;
			                    	this.allowInvalid = false;
			                    }
	                    	}
	                    }
	                    return cellProperties;
	                }
	            });
	        }
	        return auxiliaryDeviceDetailsHandsontableHelper;
	    }
	};

function CreateAndLoadPumpingUnitPTFTable(deviceId,deviceName){
	if(pumpingUnitPRTFHandsontableHelper!=null){
		if(pumpingUnitPRTFHandsontableHelper.hot!=undefined){
			pumpingUnitPRTFHandsontableHelper.hot.destroy();
		}
		pumpingUnitPRTFHandsontableHelper=null;
	}
	
	var auxiliaryDeviceSpecificType=0;
	if(Ext.getCmp("AuxiliaryDeviceSpecificType_Id")!=undefined){
		auxiliaryDeviceSpecificType=Ext.getCmp("AuxiliaryDeviceSpecificType_Id").getValue().auxiliaryDeviceSpecificType;
	}
	
	if(auxiliaryDeviceSpecificType==0){
		Ext.getCmp('AuxiliaryDevicePumpingUnitPRTFPanel_Id').hide();
		return;
	}else if(auxiliaryDeviceSpecificType==1){
		Ext.getCmp('AuxiliaryDevicePumpingUnitPRTFPanel_Id').show();
	}
	
	var showInfo=loginUserLanguageResource.pumpingUnitPRTF;
	if(isNotVal(deviceName)){
		showInfo="【<font color=red>"+deviceName+"</font>】"+showInfo;
	}
	Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFPanel_Id").setTitle(showInfo);
	
	var stroke=Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").rawValue;
	
	Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFPanel_Id").el.mask(cosog.string.loading).show();
	Ext.Ajax.request({
        method: 'POST',
        url: context + '/wellInformationManagerController/getPumpingPRTFData',
        success: function (response) {
        	Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFPanel_Id").getEl().unmask();
        	var result = Ext.JSON.decode(response.responseText);
        	Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").getStore().loadData(result.strokeList);
			if(result.strokeList.length>0){
				var pumpingModelPRTFStrokeCombValeu=Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").getValue();
				if(!isNotVal(pumpingModelPRTFStrokeCombValeu)){
					Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setValue(result.strokeList[0][0]);
					Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setRawValue(result.strokeList[0][0]);
				}
			}else{
				Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setValue('');
				Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").setRawValue('');
			}
            
            if (pumpingUnitPRTFHandsontableHelper == null || pumpingUnitPRTFHandsontableHelper.hot == null || pumpingUnitPRTFHandsontableHelper.hot == undefined) {
            	pumpingUnitPRTFHandsontableHelper = PumpingUnitPRTFHandsontableHelper.createNew("AuxiliaryDevicePumpingUnitPRTFTableDiv_id");
            	var colHeaders="['"+loginUserLanguageResource.crankAngle+"(°)','"+loginUserLanguageResource.pumpingUnitPR+"(%)','"+loginUserLanguageResource.pumpingUnitTF+"(m)']";
        		var columns="[" 
        			+"{data:'CrankAngle',type:'text',allowInvalid: true, validator: function(val, callback){return handsontableDataCheck_Num_Nullable(val, callback,this.row, this.col,pumpingUnitPRTFHandsontableHelper);}}," 
        			+"{data:'PR',type:'text',allowInvalid: true, validator: function(val, callback){return handsontableDataCheck_Num_Nullable(val, callback,this.row, this.col,pumpingUnitPRTFHandsontableHelper);}}," 
        			+"{data:'TF',type:'text',allowInvalid: true, validator: function(val, callback){return handsontableDataCheck_Num_Nullable(val, callback,this.row, this.col,pumpingUnitPRTFHandsontableHelper);}}" 
        			+"]";
        		pumpingUnitPRTFHandsontableHelper.colHeaders = Ext.JSON.decode(colHeaders);
        		pumpingUnitPRTFHandsontableHelper.columns = Ext.JSON.decode(columns);
        		if(result.totalRoot==0){
        			pumpingUnitPRTFHandsontableHelper.createTable([{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}]);
        		}else{
        			pumpingUnitPRTFHandsontableHelper.createTable(result.totalRoot);
        		}
        		
            }else {
                if(result.totalRoot==0){
                	pumpingUnitPRTFHandsontableHelper.hot.loadData([{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}]);
        		}else{
        			pumpingUnitPRTFHandsontableHelper.hot.loadData(result.totalRoot);
        		}
            }
        },
        failure: function () {
        	Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFPanel_Id").getEl().unmask();
        	Ext.MessageBox.alert("错误", "与后台联系的时候出了问题");
        },
        params: {
        	deviceId: deviceId,
        	stroke:stroke
        }
    });
};

var PumpingUnitPRTFHandsontableHelper = {
	    createNew: function (divid) {
	        var pumpingUnitPRTFHandsontableHelper = {};
	        pumpingUnitPRTFHandsontableHelper.hot1 = '';
	        pumpingUnitPRTFHandsontableHelper.divid = divid;
	        pumpingUnitPRTFHandsontableHelper.validresult=true;//数据校验
	        pumpingUnitPRTFHandsontableHelper.colHeaders=[];
	        pumpingUnitPRTFHandsontableHelper.columns=[];
	        pumpingUnitPRTFHandsontableHelper.AllData=[];
	        
	        pumpingUnitPRTFHandsontableHelper.createTable = function (data) {
	        	$('#'+pumpingUnitPRTFHandsontableHelper.divid).empty();
	        	var hotElement = document.querySelector('#'+pumpingUnitPRTFHandsontableHelper.divid);
	        	pumpingUnitPRTFHandsontableHelper.hot = new Handsontable(hotElement, {
	        		data: data,
	        		licenseKey: '96860-f3be6-b4941-2bd32-fd62b',
//	                hiddenColumns: {
//	                    columns: [0],
//	                    indicators: false,
//                    	copyPasteEnabled: false
//	                },
	                columns:pumpingUnitPRTFHandsontableHelper.columns,
	                stretchH: 'all',//延伸列的宽度, last:延伸最后一列,all:延伸所有列,none默认不延伸
	                autoWrapRow: true,
	                rowHeaders: true,//显示行头
	                colHeaders:pumpingUnitPRTFHandsontableHelper.colHeaders,//显示列头
	                columnSorting: true,//允许排序
	                sortIndicator: true,
	                manualColumnResize:true,//当值为true时，允许拖动，当为false时禁止拖动
	                manualRowResize:true,//当值为true时，允许拖动，当为false时禁止拖动
	                filters: true,
	                renderAllRows: true,
	                search: true,
	                contextMenu: {
	                    items: {
	                        "row_above": {
	                            name: loginUserLanguageResource.contextMenu_insertRowAbove
	                        },
	                        "row_below": {
	                            name: loginUserLanguageResource.contextMenu_insertRowBelow
	                        },
	                        "col_left": {
	                            name: loginUserLanguageResource.contextMenu_insertColumnLeft
	                        },
	                        "col_right": {
	                            name: loginUserLanguageResource.contextMenu_insertColumnRight
	                        },
	                        "remove_row": {
	                            name: loginUserLanguageResource.contextMenu_removeRow
	                        },
	                        "remove_col": {
	                            name: loginUserLanguageResource.contextMenu_removeColumn
	                        },
	                        "merge_cell": {
	                            name: loginUserLanguageResource.contextMenu_mergeCell
	                        },
	                        "copy": {
	                            name: loginUserLanguageResource.contextMenu_copy
	                        },
	                        "cut": {
	                            name: loginUserLanguageResource.contextMenu_cut
	                        }
//	                        ,
//	                        "paste": {
//	                            name: loginUserLanguageResource.contextMenu_paste
//	                        }
	                    }
	                }
	        	});
	        }
	        //保存数据
	        pumpingUnitPRTFHandsontableHelper.saveData = function () {
	            var selectedDeviceId=0;
	            var stroke=Ext.getCmp("AuxiliaryDevicePumpingUnitPRTFStrokeComb_Id").rawValue;
	            var row=parseInt(Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue());
	            if(Ext.getCmp("AuxiliaryDeviceSelectRow_Id").getValue()!=''){
	            	selectedDeviceId=auxiliaryDeviceInfoHandsontableHelper.hot.getDataAtRowProp(row,'id');
	            }
	            var strokePRTFData={};
	            strokePRTFData.Stroke=stroke;
	            strokePRTFData.PRTF=[];
	            var PRTFData=pumpingUnitPRTFHandsontableHelper.hot.getData();
	            for(var i=0;i<PRTFData.length;i++){
	            	var CrankAngle=pumpingUnitPRTFHandsontableHelper.hot.getDataAtRowProp(i,'CrankAngle');
	            	var PR=pumpingUnitPRTFHandsontableHelper.hot.getDataAtRowProp(i,'PR');
	            	var TF=pumpingUnitPRTFHandsontableHelper.hot.getDataAtRowProp(i,'TF');
	            	if(isNumber(CrankAngle) && isNumber(PR) && isNumber(TF)){
	            		var PRTF={};
		            	PRTF.CrankAngle=parseFloat(CrankAngle);
		            	PRTF.PR=parseFloat(PR);
		            	PRTF.TF=parseFloat(TF);
		            	strokePRTFData.PRTF.push(PRTF);
	            	}
	            }
	            
                Ext.Ajax.request({
                    method: 'POST',
                    url: context + '/wellInformationManagerController/savePumpingPRTFData',
                    success: function (response) {
                    	rdata = Ext.JSON.decode(response.responseText);
                        if (rdata.success) {
                        	Ext.MessageBox.alert(loginUserLanguageResource.message,loginUserLanguageResource.saveSuccessfully);
                        } else {
                        	Ext.MessageBox.alert(loginUserLanguageResource.message,loginUserLanguageResource.saveFailure);
                        }
                    },
                    failure: function () {
                    	Ext.MessageBox.alert(loginUserLanguageResource.message,loginUserLanguageResource.requestFailure);
                        pumpingUnitPRTFHandsontableHelper.clearContainer();
                    },
                    params: {
                        data: JSON.stringify(strokePRTFData),
                        deviceId:selectedDeviceId
                    }
                });
	        }
	        pumpingUnitPRTFHandsontableHelper.clearContainer = function () {
	        	pumpingUnitPRTFHandsontableHelper.AllData = [];
	        }
	        return pumpingUnitPRTFHandsontableHelper;
	    }
};