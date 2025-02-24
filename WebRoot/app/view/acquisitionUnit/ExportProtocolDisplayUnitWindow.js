Ext.define("AP.view.acquisitionUnit.ExportProtocolDisplayUnitWindow", {
    extend: 'Ext.window.Window',
    alias: 'widget.ExportProtocolDisplayUnitWindow',
    id: 'ExportProtocolDisplayUnitWindow_Id',
    layout: 'fit',
    title:loginUserLanguageResource.exportDisplayUnit,
    border: false,
    hidden: false,
    collapsible: true,
    constrainHeader:true,//True表示为将window header约束在视图中显示， false表示为允许header在视图之外的地方显示（默认为false）
//    constrain: true,
    closable: 'sides',
    closeAction: 'destroy',
    maximizable: true,
    minimizable: true,
    width: 400,
    minWidth: 400,
    height: 600,
    draggable: true, // 是否可拖曳
    modal: true, // 是否为模态窗口
    initComponent: function () {
        var me = this;
        Ext.create('AP.store.acquisitionUnit.ExportProtocolDisplayUnitTreeInfoStore');
        Ext.apply(me, {
            layout: "border",
            tbar:['->',{
            	xtype: 'button',
            	id:'ExportProtocolDisplayUnitWindowExportBtn_Id',
    			text: loginUserLanguageResource.exportData,
    			iconCls: 'export',
    			handler: function (v, o) {
    				var exportDisplayUnitList = [];
    				
    				var treeGridPanel = Ext.getCmp("ExportProtocolDisplayUnitTreeGridPanel_Id");
    				var selectedRecord = treeGridPanel.getChecked();
    				if(selectedRecord.length>0){
    					Ext.Array.each(selectedRecord, function (name, index, countriesItSelf) {
        			        var unidId = selectedRecord[index].get('id');
        			        exportDisplayUnitList.push(unidId);
        			    });
    					
    					var timestamp=new Date().getTime();
    					var key='exportProtocolDisplayUnitData'+'_'+timestamp;
    					var maskPanelId='ExportProtocolDisplayUnitWindow_Id';
    					
    	        		var url=context + '/acquisitionUnitManagerController/exportProtocolDisplayUnitData?key='+key+'&unitList='+exportDisplayUnitList.join(",");
    	        		
    	        		exportDataMask(key,maskPanelId,loginUserLanguageResource.loading);
    	        	    openExcelWindow(url);
    				}else{
    					Ext.MessageBox.alert(loginUserLanguageResource.message,loginUserLanguageResource.checkOne);
    				}
    			}
            }],
            items: [{
            	region: 'center',
//            	width:'25%',
            	title:loginUserLanguageResource.unitList,
            	layout: 'fit',
            	split: true,
                collapsible: false,
            	id:"ProtocolExportDisplayUnitPanel_Id"
            }],
            listeners: {
                beforeclose: function ( panel, eOpts) {
                	
                },
                minimize: function (win, opts) {
                    win.collapse();
                }
            }
        });
        me.callParent(arguments);
    }
});

