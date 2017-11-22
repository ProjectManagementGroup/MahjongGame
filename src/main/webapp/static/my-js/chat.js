$(function () {
    $("#chatForm").submit(function (e) {
        alert("chat!!!");
        var textValue = $("#replyText").val();
        ////当前发送用户一定是自己，所以一定是li:even,将来如果接收消息，那么就是li:odd
        var userInfo = "<span class=\"user\"><img class=\"img-responsive avatar_\" src=\"images/avatar.png\"><span>灞波儿奔</span></span>";
        $("#chatList").append('<li class=\"even\">' + userInfo + '<br><div class=\"reply-content-box\"><div class=\"reply-content pr transparentbg\"><span class=\"arrow\">&nbsp;</span><span>'
            + textValue + '</span></span></div></div></li>');
        //这里要把消息送到服务器
        $("#replyText").val("");
        e.preventDefault();
    });

    $("#sendFileButton").click(function () {
        window.location.href  = "../../WEB-INF/templates/chooseFile.html";
    })

});

