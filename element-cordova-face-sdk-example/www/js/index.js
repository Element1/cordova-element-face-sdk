
function successCallback(message){
    alert(message);
}

function errorCallback(message){
    alert(message);
}

function listAndRender() {
    element.list(function(message) { 
        var env = JSON.parse(message);
        renderUserTable(env.resultMessage); 
    }, errorCallback);
}

function renderUserTable(listJson) {
    //document.getElementById('id').value = "";
    document.getElementById('firstname').value = "";
    document.getElementById('lastname').value = "";

    var userArray = JSON.parse(listJson);
    var html = "<table>";
    for (let user of userArray) {
        html += "<tr><td><div class=\"rowName\">" + user.name + " " + user.name2 + "</div><div class=\"rowId\">" + user.userId + "</div></td><td><button class=\"elementButtonPill\" id=\"auth_" + user.userId + "\">Login</button></td></tr>";
    }
    html += "</table>";
    document.getElementById('table').innerHTML = html;

    // once it's part of the DOM we can hook into it
    for (let user of userArray) {
        document.getElementById('auth_' + user.userId).addEventListener('click', function(){
                element.auth(user.userId, successCallback, errorCallback);
            });
    }
}

var app = {
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
    },
    onDeviceReady: function() {
        console.log('onDeviceReady');
        document.getElementById('create').addEventListener('click', function(){
            var id = ""; //document.getElementById('id').value;
            var firstname = document.getElementById('firstname').value;
            var lastname = document.getElementById('lastname').value;

            if (firstname == "") {
                alert("Please provide a firstname");
                return;
            }

            element.enroll(id, firstname, lastname, function(message) { 
                listAndRender();
            }, errorCallback);
        });

        document.getElementById('sync').addEventListener('click', function(){
            listAndRender();
        });

        listAndRender();
    }
};

app.initialize();