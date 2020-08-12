/*global cordova, module*/
var exec = require('cordova/exec');

var ElementSDK = {
    enroll: function (id, firstname, lastname, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "enroll", [id, firstname, lastname]);
    },
    
    auth: function (id, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "auth", [id]);
    },

    list: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "list", []);
    }
};

module.exports = ElementSDK;