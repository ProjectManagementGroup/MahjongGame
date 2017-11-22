$(function () {
    $("#applyFriend").submit(function (e) {
        alert("applyFriend!!!");
        var textValue = $("#friendName").val();
        alert(textValue);
        $("#passBy").empty();
        $("#passBy").append("<span class=\"glyphicon glyphicon-list\"> </span> &nbsp;搜索结果");
        $("#passBy").append("<br>");
        $("#passBy").append("<br>");
        $("#passBy").append("<div data-role=\"content\" class=\"container\" role=\"main\">");
        $("#passBy").append("<ul><li><span class=\"user\"><img class=\"avatar_\" src=\"images/avatar.png\"><span>&nbsp;&nbsp;&nbsp;用户名:</span><span>&nbsp;灞波儿奔&nbsp;&nbsp;</span></span> <button id=\"userName0ApplyButton\" type=\"button\" class=\"btn btn-success btn-sm\">发送好友请求</button></li>");
        $("#passBy").append("<br/>");
        $("#passBy").append("<li><span class=\"user\"><img class=\"avatar_\" src=\"images/avatar.png\"><span>&nbsp;&nbsp;&nbsp;用户名:</span><span>&nbsp;灞波儿奔&nbsp;&nbsp;</span></span> <button id=\"userName1ApplyButton\" type=\"button\" class=\"btn btn-success btn-sm\">发送好友请求</button></li>");
        $("#passBy").append("<br/>");
        $("#passBy").append("<br/>");
        $("#passBy").append("</ul>");
        $("#passBy").append("</div>");
        $("#userName0ApplyButton").on("click",function(){
            $("#userName0ApplyButton").attr({"disabled":"disabled"});
            $("#userName0ApplyButton").text("成功发送请求");

            //同时还要发送请求
        });
        $("#userName1ApplyButton").on("click",function(){
            $("#userName1ApplyButton").attr({"disabled":"disabled"});
            $("#userName1ApplyButton").text("成功发送请求");
            //同时还要发送请求
        });
        e.preventDefault();
    });

});

