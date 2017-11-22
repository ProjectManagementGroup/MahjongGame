var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}

function connect() {
    var socket = new SockJS('/gs-guide-websocket');
    socket.onopen = function (event) {
        alert("on open method invoked!!!!");
    };
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        //stompClient.subscribe('/queue/greetings', function (greeting) {
        //    showGreeting(JSON.parse(greeting.body).content);
        //});
        //stompClient.subscribe('/user/' + 520 + '/queue/message', function (greeting) {
        //    alert(JSON.parse(greeting.body).content);
        //    //showGreeting(JSON.parse(greeting.body).content);
        //});
        stompClient.subscribe('/topic/greetings', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });
        //stompClient.subscribe('/queue/greetings', showGreeting);
    })
}

function disconnect() {
    if (stompClient != null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    alert($("#name").val());
    stompClient.send("/app/hello", {}, JSON.stringify({'name': $("#name").val()}));//
}

function showGreeting(message) {
    //alert(JSON.parse(message.body).content);
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
    $("#send").click(function () {
        sendName();
    });
});

