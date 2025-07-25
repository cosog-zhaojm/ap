Ext.define("AP.view.orgAndUser.ImportUserWindow", {
    extend: 'Ext.window.Window',
    id: 'ImportUserWindow',
    alias: 'widget.ImportUserWindow',
    layout: 'fit',
    title: loginUserLanguageResource.importUser,
    border: false,
    hidden: false,
    collapsible: true,
    constrainHeader: true, //True表示为将window header约束在视图中显示， false表示为允许header在视图之外的地方显示（默认为false）
    //    constrain: true,
    closable: 'sides',
    closeAction: 'destroy',
    maximizable: true,
    minimizable: true,
    width: 1200,
    minWidth: 1200,
    height: 800,
    draggable: true, // 是否可拖曳
    modal: true, // 是否为模态窗口
    initComponent: function () {
        var me = this;
        Ext.apply(me, {
            tbar: [{
                xtype: 'form',
                id: 'UserImportForm_Id',
                width: 300,
                bodyPadding: 0,
                frame: true,
                items: [{
                    xtype: 'filefield',
                    id: 'UserImportFilefield_Id',
                    disabled:loginUserOrgAndUserModuleRight.editFlag!=1,
                    name: 'file',
                    fieldLabel: loginUserLanguageResource.uploadFile,
                    labelWidth: (getLabelWidth(loginUserLanguageResource.uploadFile, loginUserLanguage)),
                    width: '100%',
                    msgTarget: 'side',
                    allowBlank: true,
                    anchor: '100%',
                    draggable: true,
                    buttonText: loginUserLanguageResource.selectUploadFile,
                    accept: '.json',
                    listeners: {
                        change: function (cmp) {
                        	submitImportedUserFile();
                        }
                    }
        	    }, {
                    id: 'ImportUserSelectItemId_Id',
                    xtype: 'textfield',
                    value: '-99',
                    hidden: true
                }]
    		}, {
                xtype: 'label',
                id: 'ImportUserWinTabLabel_Id',
                hidden: true,
                html: ''
            }, '->', {
                xtype: 'button',
                text: loginUserLanguageResource.save,
                iconCls: 'save',
                disabled:loginUserOrgAndUserModuleRight.editFlag!=1,
                handler: function (v, o) {
                    var treeStore = Ext.getCmp("ImportUserContentTreeGridPanel_Id").getStore();
                    var count = treeStore.getCount();
                    var overlayCount = 0;
                    var collisionCount = 0;
                    for (var i = 0; i < count; i++) {
                        if (treeStore.getAt(i).data.saveSign == 1) {
                            overlayCount++;
                        } else if (treeStore.getAt(i).data.saveSign == 2) {
                            collisionCount++;
                        }
                    }
                    if (overlayCount > 0 || collisionCount > 0) {
                        var info = loginUserLanguageResource.collisionInfo4+"?";
                        Ext.Msg.confirm(loginUserLanguageResource.tip, info, function (btn) {
                            if (btn == "yes") {
                                saveAllImportedUser();
                            }
                        });
                    } else {
                        saveAllImportedUser();
                    }
                }
    	    }],
            layout: 'border',
            items: [{
                region: 'center',
                title: loginUserLanguageResource.userList,
                layout: 'fit',
                split: true,
                collapsible: true,
                id: "importUserTreePanel_Id"
            }],
            listeners: {
                beforeclose: function (panel, eOpts) {

                },
                minimize: function (win, opts) {
                    win.collapse();
                }
            }
        });
        me.callParent(arguments);
    }
});


function submitImportedUserFile() {
    var form = Ext.getCmp("UserImportForm_Id");
    if (form.isValid()) {
        form.submit({
            url: context + '/userManagerController/uploadImportedUserFile',
            timeout: 1000 * 60 * 10,
            method: 'post',
            waitMsg: loginUserLanguageResource.uploadingFile+'...',
            success: function (response, action) {
                var result = action.result;
                if (result.flag == true) {
                    Ext.Msg.alert(loginUserLanguageResource.tip, loginUserLanguageResource.loadSuccessfully);
                } else {
                    Ext.Msg.alert(loginUserLanguageResource.tip, loginUserLanguageResource.uploadDataError);
                }

                var importUserContentTreeGridPanel = Ext.getCmp("ImportUserContentTreeGridPanel_Id");
                if (isNotVal(importUserContentTreeGridPanel)) {
                    importUserContentTreeGridPanel.getStore().load();
                } else {
                    Ext.create('AP.store.orgAndUser.ImportUserContentTreeInfoStore');
                }
            },
            failure: function () {
                Ext.Msg.alert(loginUserLanguageResource.tip, "【<font color=red>" + loginUserLanguageResource.uploadFail + "</font>】");
            }
        });
    }
    return false;
};

adviceImportUserCollisionInfoColor = function (val, o, p, e) {
    var saveSign = p.data.saveSign;
    var tipval = val;
    var backgroundColor = '#FFFFFF';
    var color = '#DC2828';
    if (saveSign == 0) {
        color = '#000000';
    }
    var opacity = 0;
    var rgba = color16ToRgba(backgroundColor, opacity);
    o.style = 'background-color:' + rgba + ';color:' + color + ';';
    if (isNotVal(tipval)) {
        return '<span data-qtip="' + tipval + '" data-dismissDelay=10000>' + val + '</span>';
    }
}

function saveAllImportedUser() {
	Ext.Ajax.request({
        url: context + '/userManagerController/saveAllImportedUser',
        method: "POST",
        params: {
        	
        },
        success: function (response) {
            var result = Ext.JSON.decode(response.responseText);
            if (result.success == true) {
                Ext.Msg.alert(loginUserLanguageResource.tip, loginUserLanguageResource.saveSuccessfully);
            } else {
                Ext.Msg.alert(loginUserLanguageResource.tip, "<font color=red>" + loginUserLanguageResource.saveFailure + "</font>");
            }
            Ext.getCmp("ImportUserContentTreeGridPanel_Id").getStore().load();

            var treePanel = Ext.getCmp("UserInfoGridPanel_Id");
            if (isNotVal(treePanel)) {
                treePanel.getStore().load();
            } else {
                Ext.create('AP.store.orgAndUser.UserPanelInfoStore');
            }
        },
        failure: function () {
            Ext.Msg.alert(loginUserLanguageResource.tip, "【<font color=red>" + loginUserLanguageResource.exceptionThrow + "</font>】" + loginUserLanguageResource.contactAdmin);
        }
    });
}

iconImportSingleUserAction = function (value, e, record) {
    var resultstring = '';
    if (record.data.classes == 1 && record.data.saveSign != 2) {
        var instanceName = record.data.text;
        var unitName = record.data.unitName;
        var protocolName = record.data.protocol;

        var saveSign = record.data.saveSign;
        var msg = record.data.msg;

        instanceName = encodeURIComponent(instanceName || '');
        unitName = encodeURIComponent(unitName || '');
        protocolName = encodeURIComponent(protocolName || '');

        saveSign = encodeURIComponent(saveSign || '');
        msg = encodeURIComponent(msg || '');

        resultstring = "<a href=\"javascript:void(0)\" style=\"text-decoration:none;\" " +
            "onclick=saveSingelImportedUser('" + instanceName + "','" + unitName + "','" + protocolName + "','" + saveSign + "','" + msg + "')>" + loginUserLanguageResource.save + "...</a>";
    }
    return resultstring;
}
