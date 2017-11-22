package org.mahjong.game.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 本类用于注册Spring WebSocket服务
 * ref: http://kuku940.github.io/2015/12/27/spring4%E9%85%8D%E7%BD%AEwebsocket%E7%9A%84%E7%AE%80%E5%8D%95Demo/
 * ref: http://www.cfei.net/archives/88770  This is for STOMP client using，especially for point to point message process.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    private final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    public WebSocketConfig() {

    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("注册spring websocket开始");
        // 用来注册websocket server实现类，第二个参数是访问websocket的地址
        registry.addHandler(systemWebSocketHandler(), "/ws").setAllowedOrigins("*").addInterceptors(new HandshakeInterceptor());
        // 使用Sockjs的注册方法
        registry.addHandler(systemWebSocketHandler(), "/sockjs/ws").setAllowedOrigins("*").addInterceptors(new HandshakeInterceptor()).withSockJS();
        log.info("注册spring websocket成功");
    }

    @Bean
    public WebSocketHandler systemWebSocketHandler() {
        return new SystemWebSocketHandler();
    }

}
