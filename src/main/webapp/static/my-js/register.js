$(function () {
    $("#registerForm").submit(function (e) {
        alert("register!!!");
        var userName = $("#userNameRegister").val();
        var password = $("#passwordRegister").val();
        alert(userName);
        alert(password);
        //连服务器登陆
        //登陆成功后跳转至friendList
        window.location.href = "../../WEB-INF/templates/login.html";
        e.preventDefault();
    });

});

