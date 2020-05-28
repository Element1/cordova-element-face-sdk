/*global cordova, module*/
var exec = require('cordova/exec');

var ElementSDK = {
    create: function (id, firstname, lastname, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "create", [id, firstname, lastname]);
    },
    
    train: function (id, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "train", [id]);
    },
    
    authentication: function (id, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "authentication", [id]);
    },

    list: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ElementCordovaFaceSDK", "list", []);
    }
};

module.exports = ElementSDK;