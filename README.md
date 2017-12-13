* 为了简单，所有请求消息都以 <font color=red>\|</font> 作为分隔符
* ***请求消息***中所有常量字符串都用`register`这样的格式表示，其余字符串是需要前台修改的，如注册两个用户需要用下面这两个请求方式：
* `register`<font color=red>\|</font>zyw<font color=red>\|</font>123和`register`<font color=red>\|</font>xiaoming<font color=red>\|</font>456
* 有些地方做成了服务器自动推送，但是如果将来出现了问题，那么就改成前台主动请求
* 前台需要记录当前的房间号，如果退出房间那么房间号最好设为空
* 下面是麻将牌的种类和名称
```
  dot("饼(筒)"),
  wind("东西南北风"),
  bamboo("条"),
  myriad("万"),
  dragon("中发白");

  oneDot("一饼", MahjongType.dot, 1),
  twoDot("二饼", MahjongType.dot, 2),
  threeDot("三饼", MahjongType.dot, 3),
  fourDot("四饼", MahjongType.dot, 4),
  fiveDot("五饼", MahjongType.dot, 5),
  sixDot("六饼", MahjongType.dot, 6),
  sevenDot("七饼", MahjongType.dot, 7),
  eightDot("八饼", MahjongType.dot, 8),
  nineDot("九饼", MahjongType.dot, 9),
  
  oneMyriad("一万", MahjongType.myriad, 1),
  twoMyriad("二万", MahjongType.myriad, 2),
  threeMyriad("三万", MahjongType.myriad, 3),
  fourMyriad("四万", MahjongType.myriad, 4),
  fiveMyriad("五万", MahjongType.myriad, 5),
  sixMyriad("六万", MahjongType.myriad, 6),
  sevenMyriad("七万", MahjongType.myriad, 7),
  eightMyriad("八万", MahjongType.myriad, 8),
  nineMyriad("九万", MahjongType.myriad, 9),
  
  oneBamboo("一条", MahjongType.bamboo, 1),
  twoBamboo("二条", MahjongType.bamboo, 2),
  threeBamboo("三条", MahjongType.bamboo, 3),
  fourBamboo("四条", MahjongType.bamboo, 4),
  fiveBamboo("五条", MahjongType.bamboo, 5),
  sixBamboo("六条", MahjongType.bamboo, 6),
  sevenBamboo("七条", MahjongType.bamboo, 7),
  eightBamboo("八条", MahjongType.bamboo, 8),
  nineBamboo("九条", MahjongType.bamboo, 9),
  
  
  eastWind("东风", MahjongType.wind, 1),
  westWind("西风", MahjongType.wind, 2),
  southWind("南风", MahjongType.wind, 3),
  northWind("北风", MahjongType.wind, 4),
  
  middle("中", MahjongType.dragon, 1),
  fortune("发", MahjongType.dragon, 2),
  white("白", MahjongType.dragon, 3);

```


#### 非游戏功能
接口 | 备注 | 页面请求消息格式 | 正确服务器回复消息格式(json) | 错误服务器回复消息格式(json) | 参数说明
---|-|--------|---|---|---
注册 | 注册之后需要登录才能开始游戏 | `register`<font color=red>\|</font>username<font color=red>\|</font>password | {"ok": true, "message": "注册成功", "object": null} | {"ok": false, "message": "错误信息", "object": null}
登录 | | `login`<font color=red>\|</font>username<font color=red>\|</font>password | {"ok": true, "message": "登录成功", "object": null} | {"ok": false, "message": "错误信息", "object": null}
随便加入房间 | 服务器自动挑选一个未满的房间进行游戏，但是不会挑选好友房间 | `join-random`<font color=red>\|</font> | {"ok": true, "message": "join random success", "object": 20171212154942} | {"ok": false, "message": "错误信息", "object": null} | <small>object里面存放的20171212154942是随机房间号；注意，用户加入房间之后，不需要自己请求房间内所有成员的信息，用于画页面，而且房间内的其他成员也不需要自己请求，服务器会自动广播房间内所有成员信息，用户只需要收到消息后重画页面即可
服务器推送房间内所有成员的信息 |  这个接口用于页面的时候，需要展示房间内所有玩家的信息：username、是否准备和积分 | 无需请求自动推送 |</small>{"ok": true, "message": "room information", "object": {"list":[{"username":"zyw","point":100,"ready":true},{"username":"xiaoming","point":50,"ready":false}]}}  | | list里面是一个用户信息的数组；注意游戏开始后就会自动立即为第一个人发牌，所以前端的webSocket要随时准备接收消息 
创建好友房间 | 必须并同时邀请三个好友，暂时设定为通过输入username进行邀请，如果输入错误或者好友不在线，那么创建房间失败；创建成功后，房主进入一个空房间等待其他三位好友 | `invite`<font color=red>\|</font>friend1<font color=red>\|</font>friend2<font color=red>\|</font>friend3 | 对于被邀请玩家收到的信息{"ok": true, "message": "invitation", "object": 20171212154942} 对于房主收到的信息{"ok": true, "message": "invite success", "object": 50}| | friend1、 friend2、 friend3指的是三位好友的用户名；前台通过message的invitation判断是不是一个邀请；object里面存放的是房间号
用户接受邀请 | 暂时不允许拒绝邀请；只有在线才能接受邀请，否则根本收不到邀请消息 | `accept`<font color=red>\|</font>roomId | 无成功回复 | {"ok": false, "message": "错误信息", "object": null} | 成功之后，玩家立即加入房间，同时服务器自动向房间内包括本玩家在内的所有玩家广播房间信息



#### 游戏功能
注意：
1. 每个玩家发牌之后，都会等待5秒钟，等待其他玩家的碰牌和吃牌消息，5秒过后服务器计算出所有请求的优先级，之后再广播有谁得到了这个机会，这个时候前台需要判断一下，如果自己被选中了，那么久把这张牌放到自己的手牌里面；如果自己没被选中，那么就要画出相应某个玩家碰牌的效果

接口 | 备注 | 页面请求消息格式 | 正确服务器回复消息格式(json) | 错误服务器回复消息格式(json) | 参数说明
---|-|--------|---|---|---
准备 | | `ready`<font color=red>\|</font> | 对于所有玩家全部广播房间信息 | | | 
游戏开始 | | 不需要请求 | {"ok": true, "message": "game start", "object": null} | | 
发牌 |***不需要用户请求***，因为用户不知道自己的顺序，这个功能由服务器推送| 无 | 对被发牌玩家{"ok": true, "message": "get tile", "object": sevenDot} 对其他玩家：{"ok": true, "message": "allocate tile", "object": zyw} | {"ok": false, "message": "错误信息", "object": null} | message=get tile是回复类型由前台进行判断；object里面放的是麻将牌的名称；对其他玩家广播给zyw发牌了
出牌 | 前台记得要给玩家出牌进行计时 | `out`<font color=red>\|</font>tile | 对于出牌玩家的回复消息{"ok": true, "message": "out success", "object": sevenDot}  对于其余玩家的广播消息{"ok": true, "message": "other out", "object": {"username":"zyw","tile":sevenDot}} | | 对其他玩家要广播是谁出了什么牌
玩家请求碰牌 | 碰牌比吃牌优先 | `bump`<font color=red>\|</font> | {"ok": true, "message": "bump request success", "object": null} | |
玩家请求吃牌 | | `eat`<font color=red>\|</font> | {"ok": true, "message": "eat request success", "object": null} | | 
推送某玩家碰牌\吃牌成功 | 前台也请记录当前玩家要求吃和碰的是哪一张牌（虽然后台也会记录） | 后台推送不需要请求 | 对获得机会的玩家进行推送{"ok": true, "message": "bump success", "object": sevenDot} 对其余玩家进行推送{"ok": true, "message": "other bump success", "object": {"username":"456","type":bump}}| |
胡牌 |胡牌暂时由客户端负责计算，这样虽然不是很安全，但是客户端有代码可以参考，我暂时先不做了 | `win`<font color=red>\|</font> | 对胡牌玩家的推送{"ok": true, "message": "win", "object": 50} 对其余玩家的推送{"ok": true, "message": "lose", "object": -50}| 游戏结束后房间不散，玩家要重新准备

</small>





