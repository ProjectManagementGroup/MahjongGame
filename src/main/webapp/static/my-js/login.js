$(document).ready(function () {
        var websocket;
        if ('WebSocket' in window) {
            websocket = new WebSocket("wss://127.0.0.1:9000/game/ws");
            console.log("建立WebSocket连接");
        } else if ('MozWebSocket' in window) {
            websocket = new MozWebSocket("wss://127.0.0.1:9000/game/ws");
            console.log("建立MozWebSocket连接");
        } else {
            websocket = new SockJS("https://127.0.0.1:9000/game/sockjs/ws");
            console.log("建立SockJS连接");
        }
        // var websocket = new WebSocket("wss://127.0.0.1:9000/game/sockjs/ws/websocket");
        var userNameGlobal;
        var publicKeyEncoded;
        var privateKeyEncoded;
        var friendPublicKeyEncoded;
        var symkey = CryptoJS.enc.Utf8.parse("2017051820170518");
        var IV = CryptoJS.enc.Utf8.parse("0807060504030201");
        websocket.onopen = function (payload) {
            $("#msgcount").append("WebSocket链接开始！<br/>");
        };
        websocket.onmessage = function (evnt) {
            //先解密
            //console.log(evnt.data);
            var payload = symDecrypt(evnt.data);
            console.log("payload:::" + payload);
            //console.log(evnt.data);//登陆成功/失败
            //如果是登录消息
            if (payload.indexOf("login") >= 0 || payload.indexOf("register") >= 0) {
                console.log("jump to friendList");
                loginMessage(payload);
            } else if (payload.indexOf("chating") >= 0) {//如果是接收消息，即有人向我发送消息
                console.log("receive message from others");
                //重画页面
                //console.log("收到消息重画页面:::" + payload);
                goToChatForChatHtml(payload);
            } else if (payload.indexOf("search") >= 0) {//如果是搜索朋友
                console.log("display search result");
                searchMessage(payload);
            } else if (payload.indexOf("apply") >= 0) {
                console.log("receive apply reply");
            } else if (payload.indexOf("applyPublicKey") >= 0) {
                console.log("receive public key");
                friendPublicKeyEncoded = payload.split("|")[1];
            } else if (payload.indexOf("close") >= 0) {
                alert("socket 出错啦！！ 请强制刷新后重新登录！！");
            }

        };
        websocket.onerror = function (evnt) {
            $("#msgcount").append("WebSocket链接出错！<br/>");
        };
        websocket.onclose = function (evnt) {
            $("#msgcount").append("WebSocket链接关闭！<br/>");
        };

        //看到朋友列表后点击开始聊天按钮
        $("body").delegate("button[name='startChatButton']", "click", function (e) {
            var friendUserName = $(this).attr("value");
            console.log("friendUserName:::" + friendUserName);
            goToChat(friendUserName);
        });

        //导航栏"我的好友列表"点击事件
        $('body').delegate("#myFriendListNav", "click", function (e) {
            console.log("jump to friendList")
            //$("#changedDiv").load("friendList.html #changedDiv");
            var resultDiv = $("#changedDiv");
            var action = $("#myFriendListNav").attr("action");
            $.ajax({
                type: "post",
                url: action,
                cache: false,
                data: "userName=" + userNameGlobal,
                dataType: 'html',
                success: function (result) {
                    resultDiv.html(result);
                }
            });
        });

        //导航栏"加好友"点击事件
        $('body').delegate("#addFriendNav", "click", function (e) {
            console.log("jump to addFriend")
            var resultDiv = $("#changedDiv");
            var action = $("#addFriendNav").attr("action");
            $.ajax({
                type: "post",
                url: action,
                cache: false,
                data: "userName=" + userNameGlobal,
                dataType: 'html',
                success: function (result) {
                    resultDiv.html(result);
                }
            });
        });

        //导航栏"我收到的好友请求"点击事件
        $('body').delegate("#friendApplyNav", "click", function (e) {
            console.log("jump to friendApplyMe")
            var resultDiv = $("#changedDiv");
            var action = $("#friendApplyNav").attr("action");
            $.ajax({
                type: "post",
                url: action,
                cache: false,
                data: "userName=" + userNameGlobal,
                dataType: 'html',
                success: function (result) {
                    resultDiv.html(result);
                    //resultDiv.replaceWith(result);
                }
            });
        });

        //接受或者拒绝好友请求
        $("body").delegate("li[name='acceptButton']", "click", function () {
            console.log("accept!!");
            var applyId = $(this).attr("value");
            websocket.send(symEncrypt("accept|" + applyId));
            $(this).parent().parent().remove();
        })
        $("body").delegate("li[name='refuseButton']", "click", function () {
            console.log("refuse!!");
            var applyId = $(this).attr("value");
            websocket.send(symEncrypt("refuse|" + applyId));
            $(this).parent().parent().remove();
        })


        //用户登录
        $("#loginSubmitButton").on("click", function (e) {
            console.log("login!!!");
            var userName = $("#userNameLogin").val();
            userNameGlobal = userName;
            var password = $("#passwordLogin").val();
            console.log(userName);
            console.log(password);
            send();
            //连服务器登陆
            //登陆成功后跳转至friendList
        });


        function send() {
            websocket.send(symEncrypt("login|" + $("#userNameLogin").val() + "|" + $("#passwordLogin").val()));
        }

        //登陆成功后跳转到friendList
        function loginMessage(payload) {
            if (payload.indexOf("success") > 0) {
                var loginArray = payload.split("|");
                alert(loginArray[0]);
                publicKeyEncoded = loginArray[1];//得到了自己的公钥和私钥
                privateKeyEncoded = loginArray[2];
                console.log("jump to friendList")
                var resultDiv = $("#changedDiv");
                var form = $("#loginForm");
                var action = form.attr("action");
                //向服务器请求好友列表
                $.ajax({
                    type: "post",
                    url: action,
                    cache: false,
                    data: "userName=" + userNameGlobal,
                    dataType: 'html',
                    success: function (result) {
                        resultDiv.html(result);
                        //向UserController
                        //resultDiv.replaceWith(result);
                    }
                });
            } else {
                alert("密码错误啦！！");
            }
        }

        //寻找朋友并添加
        $("body").delegate("#searchSubmitButton", "click", function (e) {
            console.log("start search friend");
            var searchFriendName = $("#searchFriendName").val();
            console.log("searchFriendName:::" + searchFriendName);
            //不能搜自己
            if (searchFriendName == userNameGlobal) {
                alert("您搜自己干嘛");
            } else {
                websocket.send(symEncrypt("search|" + searchFriendName));
            }
        });

        //收到服务器的寻找朋友消息并处理
        function searchMessage(payload) {
            if (payload.indexOf("success") >= 0) {
                var mode = payload.split("|")[1];
                var buttonText = mode == "true" ? "已经是好友了" : "发送好友请求";
                console.log("search friend result");
                var textValue = $("#searchFriendName").val();
                var buttonName = "startChatWithHimButton";
                console.log(textValue);
                //讲道理最多一个用户被搜索到
                $("#passBy").empty();
                $("#passBy").append("<span class=\"glyphicon glyphicon-list\"> </span> &nbsp;搜索结果");
                $("#passBy").append("<br>");
                $("#passBy").append("<br>");
                $("#passBy").append("<div data-role=\"content\" class=\"container\" role=\"main\">");
                $("#passBy").append("<ul><li><span class=\"user\"><img style=\"background: beige\" class=\"avatar_\" src=\"/game/static/images/avatar.png\" th:src=\"@{/game/static/images/avatar.png}\"><span>&nbsp;&nbsp;&nbsp;用户名:</span><span style=\"background: beige\">&nbsp;" + textValue + "&nbsp;&nbsp;</span></span> <button id=\"" + buttonName + "\" type=\"button\" class=\"btn btn-success btn-sm\">" + buttonText + "</button></li>");
                $("#passBy").append("<br/>");
                $("#passBy").append("</ul>");
                $("#passBy").append("</div>");
                if (mode == "true") {
                    $("#" + buttonName).attr({"disabled": "disabled"});
                }
                $("body").delegate("#" + buttonName, "click", function () {
                    //向服务器发送交友信息
                    console.log("向服务器发送交友信息");
                    websocket.send(symEncrypt("applyFriend|" + textValue));
                    $("#" + buttonName).attr({"disabled": "disabled"});
                    $("#" + buttonName).text("成功发送请求");//TODO:apply返回消息的处理
                });
            } else {
                $("#passBy").empty();
                $("#passBy").append("没找到这个人耶");
            }
        }


        //这是我自己发送消息
        //先用我自己的私钥加密，再用对方的公钥加密，实现保密性，身份验证和抗抵赖
        $("body").delegate("#replyChatButton", "click", function (e) {
            console.log("chat!!!");
            var textValue = $("#replyText").val();
            sendMyMessage(textValue, "1");
        });

        function sendMyMessage(textValue, flag) {
            if ($("#chatList").length < 0 || $("#chatList").length == 0) {//ul不存在，先创建一个
                var ulHtml = "<ul class=\"content-reply-box mg10\" id=\"chatList\" th:each=\"chatMessage: ${chatMessages}\">";
                console.log(ulHtml);
                $("#ulAppendToIt").after(ulHtml);
            }
            var userInfo = "<span class=\"user\"><img class=\"img-responsive avatar_\" src=\"/game/static/images/avatar.png\" th:src =\"@{/game/static/images/avatar.png}\"/><span>" + userNameGlobal + "</span></span>";
            console.log(userInfo);
            var textvalue;
            if (flag == "1") {//发文本消息
                textvalue = encryptMessage(textValue);
                $("#chatList").append('<li class=\"odd\">' + userInfo + '<br><div class=\"reply-content-box\"><div class=\"reply-content pr transparentbg\"><span class=\"arrow\">&nbsp;</span><span>'
                    + textValue + '</span></span></div></div></li>');
            } else {//textValue是个base64String,flag文件名
                //要用mac码来保护完整性
                var macCode = CryptoJS.MD5(textValue);
                textvalue = textValue + "|" + macCode;
                textvalue = encryptMessage(textvalue);
                $("#chatList").append('<li class=\"odd\">' + userInfo + '<br><div class=\"reply-content-box\"><div class=\"reply-content pr transparentbg\"><span class=\"arrow\">&nbsp;</span><span>'
                    + flag + '</span></span></div></div></li>');
            }
            //这里要把消息送到服务器
            var friendUserName = $("#friendUserName").text();
            websocket.send(symEncrypt("charMessage|" + textValue + "|" + friendUserName + "|" + flag + "|" + textvalue));
            $("#replyText").val("");
        }

        function encryptMessage(textValue) {
            var encrypt = new JSEncrypt();
            var encrypt1 = new JSEncrypt();
            encrypt.setPrivateKey(privateKeyEncoded);
            encrypt1.setPublicKey(friendPublicKeyEncoded);
            var data = encrypt.encrypt(textValue);
            data = encrypt1.encrypt(data);
            return data;
        }

        //发送文件
        $("body").delegate("#sendFileButton", "click", function (e) {
            var selectedFile = document.getElementById("files").files[0];//获取读取的File对象
            var name = selectedFile.name;//读取选中文件的文件名
            var size = selectedFile.size;//读取选中文件的大小
            console.log("文件名:" + name + "大小：" + size);

            //var data = new FormData();
            var reader = new FileReader();
            reader.readAsDataURL(selectedFile);
            reader.onload = function (e) {
                sendMyMessage(e.target.result, selectedFile.name);
            };

        });


        $("#changedDiv").delegate('.list-group-item,.menu a', "click", function () {
            $.mobile.changePage($(this).attr('href'), {
                transition: 'flip', //转场效果
                reverse: true       //默认为false,设置为true时将导致一个反方向的转场
            });
        });

        //用于friendList页面跳到聊天页面
        function goToChat(friendUserName) {
            var resultDiv = $("#changedDiv");
            var action = $("#friendListDiv").attr("action");
            var data = "friendUserName=" + friendUserName + "&userName=" + userNameGlobal;
            $.ajax({
                type: "post",
                url: action,
                cache: false,
                data: data,
                dataType: 'html',
                success: function (result) {
                    resultDiv.html(result);
                    //这个controller返回model，不用二次请求
                    //loadChatHistory(payload);
                }
            });
            //再向服务器请求好友的公钥
            websocket.send(symEncrypt("applyPublicKey|" + friendUserName));
        }

        //chat.html收到消息后自己刷新
        //这里要判断，是文件还是图片还是普通消息
        function goToChatForChatHtml(payload) {
            var textValue = payload.split("|");
            var message = textValue[1];
            var decryptM = decryptMessage(message);
            var userInfo = "<span class=\"user\"><img class=\"img-responsive avatar_\" src=\"/game/static/images/avatar.png\" th:src=\"@{/game/static/images/avatar.png}\"/><span>" + textValue[2] + "</span></span>";
            console.log("message:::" + message);
            //如果没有ul元素则要先创建一个
            if ($("#chatList").length < 0 || $("#chatList").length == 0) {//ul不存在，先创建一个
                console.log("ul不存在，创建一个")
                var ulHtml = "<ul class=\"content-reply-box mg10\" id=\"chatList\" th:each=\"chatMessage: ${chatMessages}\">";
                console.log(ulHtml);
                $("#ulAppendToIt").after(ulHtml);
            }
            if (textValue[3] == "1") {//普通消息
                $("#chatList").append('<li class=\"odd\">' + userInfo + '<br><div class=\"reply-content-box\"><div class=\"reply-content pr transparentbg\"><span class=\"arrow\">&nbsp;</span><span>'
                    + message + '</span></span></div></div></li>');
            } else if (textValue[3] == "2") {//图片
                $("#chatList").append('<li class=\"odd\">' + userInfo + '<br><div class=\"reply-content-box\"><div class=\"reply-content pr transparentbg\"><span class=\"arrow\">&nbsp;</span><img src=\"' + message + '\" th:src=\"@{' + message + '}\"/></span></div></div></li>');
            } else {//文本文件
                //比对mac码
                //if(CryptoJS.MD5(decryptM)!=decryptM.split("|")[0]){
                //    websocket.send("file wrong!");
                //}
                var deText = $.base64.decode(message, "utf8");
                $("#chatList").append('<li class=\"odd\">' + userInfo + '<br><div class=\"reply-content-box\"><div class=\"reply-content pr transparentbg\"><span class=\"arrow\">&nbsp;</span><span>'
                    + deText + '</span></span></div></div></li>');
            }
            websocket.send(symEncrypt("receive|" + decryptM));
        }

        function decryptMessage(textValue) {
            var dncrypt = new JSEncrypt();
            var dncrypt1 = new JSEncrypt();
            dncrypt.setPrivateKey(privateKeyEncoded);
            dncrypt1.setPublicKey(friendPublicKeyEncoded);
            var data = dncrypt.decrypt(textValue);
            data = dncrypt1.decrypt(data);
            return data;
        }

        //和服务器通信的对称密钥加密解密
        function symEncrypt(payload) {
            return CryptoJS.AES.encrypt(CryptoJS.enc.Utf8.parse(payload), symkey, {iv: IV, mode: CryptoJS.mode.CBC}) + "";
        }

        function symDecrypt(payload) {
            console.log("payload:::" + payload);
            return CryptoJS.AES.decrypt(payload, symkey, {iv: IV, mode: CryptoJS.mode.CBC}).toString(CryptoJS.enc.Utf8);
        }


    }
);