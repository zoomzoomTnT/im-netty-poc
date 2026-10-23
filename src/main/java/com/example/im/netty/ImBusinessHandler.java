package com.example.im.netty;

import com.example.im.service.OnlineUserRegistry;
import com.example.im.util.HmacUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Business handler for IM messages.
 * Commands (simple text protocol for POC):
 * LOGIN:userId:token
 * HEARTBEAT
 * MSG:toUserId:content
 * LOGOUT
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ImBusinessHandler extends SimpleChannelInboundHandler<String> {

    private final OnlineUserRegistry onlineRegistry;
    private final ConcurrentHashMap<String, ChannelHandlerContext> userChannels = new ConcurrentHashMap<>();

    public ImBusinessHandler(OnlineUserRegistry onlineRegistry) {
        this.onlineRegistry = onlineRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        String[] parts = msg.split(":", 3);
        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "LOGIN" -> handleLogin(ctx, parts);
            case "HEARTBEAT" -> ctx.writeAndFlush("HEARTBEAT_ACK");
            case "MSG" -> handleMsg(ctx, parts);
            case "LOGOUT" -> handleLogout(ctx);
            default -> ctx.writeAndFlush("ERROR:unknown command");
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, String[] parts) {
        if (parts.length < 3) {
            ctx.writeAndFlush("ERROR:LOGIN format userId:token");
            return;
        }
        String userId = parts[1];
        String token = parts[2];
        // Simple HMAC demo (in real use proper key + timestamp)
        if (!HmacUtil.verifyDemo(token, userId)) {
            ctx.writeAndFlush("ERROR:invalid token");
            ctx.close();
            return;
        }
        userChannels.put(userId, ctx);
        onlineRegistry.userOnline(userId);
        ctx.writeAndFlush("LOGIN_OK:" + userId);
        log.info("User {} logged in", userId);
    }

    private void handleMsg(ChannelHandlerContext ctx, String[] parts) {
        if (parts.length < 3) return;
        String toUser = parts[1];
        String content = parts[2];
        ChannelHandlerContext target = userChannels.get(toUser);
        if (target != null && target.channel().isActive()) {
            target.writeAndFlush("MSG:" + content);
            ctx.writeAndFlush("MSG_ACK");
        } else {
            // Offline - in real system push to RocketMQ / store in Redis/ES
            log.info("User {} offline, message queued (POC)", toUser);
            ctx.writeAndFlush("MSG_OFFLINE");
        }
    }

    private void handleLogout(ChannelHandlerContext ctx) {
        userChannels.entrySet().removeIf(e -> e.getValue() == ctx);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.warn("Idle connection closed: {}", ctx.channel());
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        userChannels.entrySet().removeIf(e -> e.getValue() == ctx);
        log.info("Channel inactive");
    }
}
