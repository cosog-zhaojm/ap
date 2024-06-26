var loginUserDeviceManagerModuleRight=getRoleModuleRight('WellInformation');
Ext.define("AP.view.well.DeviceManagerInfoView", {
    extend: 'Ext.panel.Panel',
    alias: 'widget.deviceManagerInfoView', // 定义别名
    layout: 'fit',
    border: false,
    initComponent: function () {
        var me = this;
        var DeviceInfoPanel = Ext.create('AP.view.well.DeviceInfoPanel');
        var items=[];
        if(tabInfo.children!=undefined && tabInfo.children!=null && tabInfo.children.length>0){
        	for(var i=0;i<tabInfo.children.length;i++){
        		var panelItem={};
        		if(tabInfo.children[i].children!=undefined && tabInfo.children[i].children!=null && tabInfo.children[i].children.length>0){
        			panelItem={
        					title: tabInfo.children[i].text,
        					tpl: tabInfo.children[i].text,
        					xtype: 'tabpanel',
        	        		id: 'DeviceManagerPanel_'+tabInfo.children[i].deviceTypeId,
        	        		activeTab: 0,
        	        		border: false,
        	        		tabPosition: 'left',
        	        		items:[],
        	        		listeners: {
        	        			beforetabchange ( tabPanel, newCard, oldCard, eOpts ) {
        	        				oldCard.removeAll();
        	        			},
        	        			tabchange: function (tabPanel, newCard,oldCard, obj) {
        	        				var DeviceInfoPanel = Ext.create('AP.view.well.DeviceInfoPanel');
        	        				newCard.add(DeviceInfoPanel);
        	        				Ext.getCmp("selectedDeviceType_global").setValue(newCard.id.split('_')[1]); 
        	        				
        	        				Ext.getCmp("DeviceSelectRow_Id").setValue(0);
        	        		    	Ext.getCmp("DeviceSelectEndRow_Id").setValue(0);
        	        				CreateAndLoadDeviceInfoTable(true);
        	        			},
        	        			afterrender: function (panel, eOpts) {
        	        				
        	        			}
        	        		}
        			}
        			
        			for(var j=0;j<tabInfo.children[i].children.length;j++){
        				var secondTabPanel={
//        						title: '<div style="color:#000000;font-size:11px;font-family:SimSun">'+tabInfo.children[i].children[j].text+'</div>',
        						title:tabInfo.children[i].children[j].text,
        						tpl:tabInfo.children[i].children[j].text,
        						layout: 'fit',
        						id: 'DeviceManagerPanel_'+tabInfo.children[i].children[j].deviceTypeId,
        						border: false
        				};
            			if(j==0){
            				if(i==0){
            					secondTabPanel.items=[];
                				secondTabPanel.items.push(DeviceInfoPanel);
            				}
                		}
            			panelItem.items.push(secondTabPanel);
        			}
        		}else{
        			panelItem={
        					title: tabInfo.children[i].text,
        					tpl: tabInfo.children[i].text,
        					layout: 'fit',
    						id: 'DeviceManagerPanel_'+tabInfo.children[i].deviceTypeId,
    						border: false
        			};
        			if(i==0){
            			panelItem.items=[];
            			panelItem.items.push(DeviceInfoPanel);
            		}
        		}
        		items.push(panelItem);
        	}
        }
        
        
        Ext.apply(me, {
        	tbar:[{
            	id: 'DeviceManagerModuleViewFlag',
            	xtype: 'textfield',
                value: loginUserDeviceManagerModuleRight.viewFlag,
                hidden: true
             },{
            	id: 'DeviceManagerModuleEditFlag',
            	xtype: 'textfield',
                value: loginUserDeviceManagerModuleRight.editFlag,
                hidden: true
             },{
            	id: 'DeviceManagerModuleControlFlag',
            	xtype: 'textfield',
                value: loginUserDeviceManagerModuleRight.controlFlag,
                hidden: true
            }],
            items: [{
        		xtype: 'tabpanel',
        		id:"DeviceManagerTabPanel",
        		activeTab: 0,
        		border: false,
        		tabPosition: 'bottom',
        		items:items,
        		listeners: {
        			beforetabchange ( tabPanel, newCard, oldCard, eOpts ) {
        				if(oldCard.xtype=='tabpanel'){
        					oldCard.activeTab.removeAll();
        				}else{
        					oldCard.removeAll();
        				}
        			},
        			tabchange: function (tabPanel, newCard,oldCard, obj) {
        				Ext.getCmp("bottomTab_Id").setValue(newCard.id); 
        				var DeviceInfoPanel = Ext.create('AP.view.well.DeviceInfoPanel');
        				var deviceTypeId=0;
        				if(newCard.xtype=='tabpanel'){
        					newCard.activeTab.add(DeviceInfoPanel);
        					deviceTypeId=newCard.activeTab.id.split('_')[1];
        				}else{
	        				newCard.add(DeviceInfoPanel);
	        				deviceTypeId=newCard.id.split('_')[1];
        				}
        				Ext.getCmp("selectedDeviceType_global").setValue(deviceTypeId); 
        				
        				Ext.getCmp("DeviceSelectRow_Id").setValue(0);
        		    	Ext.getCmp("DeviceSelectEndRow_Id").setValue(0);
						CreateAndLoadDeviceInfoTable(true);
        			}
        		}
            	}],
            	listeners: {
            		afterrender: function (panel, eOpts) {
            			
            		},
        			beforeclose: function ( panel, eOpts) {
        				if(deviceInfoHandsontableHelper!=null){
        					if(deviceInfoHandsontableHelper.hot!=undefined){
        						deviceInfoHandsontableHelper.hot.destroy();
        					}
        					deviceInfoHandsontableHelper=null;
        				}
        				if(productionHandsontableHelper!=null){
        					if(productionHandsontableHelper.hot!=undefined){
        						productionHandsontableHelper.hot.destroy();
        					}
        					productionHandsontableHelper=null;
        				}
        				if(pumpingInfoHandsontableHelper!=null){
        					if(pumpingInfoHandsontableHelper.hot!=undefined){
        						pumpingInfoHandsontableHelper.hot.destroy();
        					}
        					pumpingInfoHandsontableHelper=null;
        				}
        				if (videoInfoHandsontableHelper != null) {
                            if (videoInfoHandsontableHelper.hot != undefined) {
                            	videoInfoHandsontableHelper.hot.destroy();
                            }
                            videoInfoHandsontableHelper = null;
                        }
        				if (deviceAuxiliaryDeviceInfoHandsontableHelper != null) {
                            if (deviceAuxiliaryDeviceInfoHandsontableHelper.hot != undefined) {
                            	deviceAuxiliaryDeviceInfoHandsontableHelper.hot.destroy();
                            }
                            deviceAuxiliaryDeviceInfoHandsontableHelper = null;
                        }
        				if (deviceAdditionalInfoHandsontableHelper != null) {
                            if (deviceAdditionalInfoHandsontableHelper.hot != undefined) {
                            	deviceAdditionalInfoHandsontableHelper.hot.destroy();
                            }
                            deviceAdditionalInfoHandsontableHelper = null;
                        }
        			}
        		}
        });
        me.callParent(arguments);
    }

});