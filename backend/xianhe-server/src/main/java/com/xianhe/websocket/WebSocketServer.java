package com.xianhe.websocket;

import com.xianhe.properties.JwtProperties;
import com.xianhe.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务：管理端网页连上来后，服务端向全体会话广播来单 / 催单提醒（契约第 10 节）
 *
 * 鉴权：浏览器建连不能带自定义请求头，所以管理端令牌走 query 参数 `?token=`，
 * onOpen 用与管理端接口同一把密钥验签，验不过直接关闭——否则任何人只要知道地址就能收到全部订单号。
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    //在线会话，key 是前端生成并持久化的 sid
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    // 端点实例的生命周期由 WebSocket 容器管理，字段注入不可靠（实测 @Autowired 不生效）。
    // 改由 WebSocketConfiguration 在启动时把配置塞进静态字段。
    private static JwtProperties jwtProperties;

    /**
     * 启动时由配置类注入（只调用一次）
     * @param properties
     */
    public static void setJwtProperties(JwtProperties properties) {
        WebSocketServer.jwtProperties = properties;
    }

    /**
     * 连接建立：先验令牌再登记；同一 sid 重连时关掉旧连接，避免残留
     * @param session
     * @param sid
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        if (!authorized(session)) {
            closeQuietly(session, CloseReason.CloseCodes.VIOLATED_POLICY, "unauthorized");
            log.warn("WebSocket 连接被拒：sid={}，管理端令牌缺失或无效", sid);
            return;
        }

        Session old = SESSIONS.put(sid, session);
        if (old != null && old.isOpen()) {
            closeQuietly(old, CloseReason.CloseCodes.NORMAL_CLOSURE, "replaced");
        }
        log.info("WebSocket 连接建立：sid={}，当前在线 {}", sid, SESSIONS.size());
    }

    /**
     * 收到客户端消息；本项目推送是单向的，这里只留痕
     * @param message
     * @param sid
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到 WebSocket 消息：sid={}，message={}", sid, message);
    }

    /**
     * 连接关闭
     * @param session
     * @param sid
     */
    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid) {
        // 只有「当前登记的就是这个会话」才移除：重连时新会话已覆盖同名 sid，
        // 旧连接随后关闭若直接 remove(sid)，会把新连接从表里删掉，之后再也收不到推送
        SESSIONS.remove(sid, session);
        log.info("WebSocket 连接断开：sid={}，当前在线 {}", sid, SESSIONS.size());
    }

    /**
     * 向所有在线客户端广播一条消息
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = SESSIONS.values();
        if (sessions.isEmpty()) {
            // 没有店员在线时不算异常：消息本就只在有人看的时候才有意义
            log.info("当前没有管理端在线，提醒未推送：{}", message);
            return;
        }

        for (Session session : sessions) {
            try {
                // 用异步发送：同步 basicRemote 会阻塞调用线程，而支付接口就是在业务线程里调到这里的，
                // 一个网络差的店员足以拖慢所有人的支付响应
                session.getAsyncRemote().sendText(message);
            } catch (Exception ex) {
                // 单个连接发失败不影响其它连接
                log.warn("WebSocket 推送失败：sessionId={}", session.getId(), ex);
            }
        }
    }

    // 握手参数里取令牌验签：浏览器无法给 WebSocket 加自定义请求头，只能走 query
    private boolean authorized(Session session) {
        List<String> values = session.getRequestParameterMap().get("token");
        if (values == null || values.isEmpty()) {
            return false;
        }
        try {
            JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), values.get(0));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void closeQuietly(Session session, CloseReason.CloseCodes code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException ignored) {
            // 关闭失败无非是连接已经断了
        }
    }
}
