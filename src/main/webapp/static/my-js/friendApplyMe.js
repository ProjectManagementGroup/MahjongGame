$(function () {
     //这里的div和按钮，都是被username命名的，只要找到名字就可以定位是哪一条数据
    $("#userName0_accept").click(function () {
        alert("accept!!");
        //TODO:申请通过
        $("#userName0_div").remove();
    })
    $("#userName0_refuse").click(function () {
        //TODO:拒绝申请
        alert("refuse!!");
        $("#userName0_div").remove();
    })


    $("#userName1_accept").click(function () {
        alert("accept!!");
        //TODO:申请通过
        $("#userName1_div").remove();
    })
    $("#userName1_refuse").click(function () {
        //TODO:拒绝申请
        alert("refuse!!");
        $("#userName1_div").remove();
    })


    $("#userName2_accept").click(function () {
        alert("accept!!");
        //TODO:申请通过
        $("#userName2_div").remove();
    })
    $("#userName2_refuse").click(function () {
        //TODO:拒绝申请
        alert("refuse!!");
        $("#userName2_div").remove();
    })


    $("#userName3_accept").click(function () {
        alert("accept!!");
        //TODO:申请通过
        $("#userName3_div").remove();
    })
    $("#userName3_refuse").click(function () {
        //TODO:拒绝申请
        alert("refuse!!");
        $("#userName3_div").remove();
    })

});

function change(){
    alert("onclick");
}