# IM Netty POC - One-Day Intensive Course

> **Reality check**: You cannot *master* Java concurrency + Netty long-connection + Protobuf + Redis + RocketMQ + Disruptor + Arthas + HMAC/RC4/LZO/LZ4 in one day.  
> This POC + plan lets you **touch the critical path** of an App-embedded IM system and have something runnable to put on GitHub and talk about.

## What this POC demonstrates

- Spring Boot + embedded **Netty** long-connection server (port 9000)
- Length-field framing (solves sticky/unpack packets)
- IdleStateHandler for heartbeat / connection anomaly detection
- Simple text protocol (easy to replace with **Protobuf**)
- **HMAC** token verification on login
- Online user registry via **Redisson** (falls back to in-memory)
- **Arthas** starter included for JVM troubleshooting practice
- LZ4 dependency present for compression experiments
- Disruptor dependency ready for high-throughput message processing

## Quick Start

```bash
# Optional: start Redis
docker run -d -p 6379:6379 redis

mvn spring-boot:run
```

### Test with netcat / telnet

```bash
# Terminal 1 - login as alice
nc localhost 9000
LOGIN:alice:<hmac-token>   # generate token with HmacUtil.main

# Terminal 2 - login as bob and send message
nc localhost 9000
LOGIN:bob:<hmac-token>
MSG:alice:hello from bob
```

Generate token:

```bash
# Run HmacUtil.main or temporarily print in code
# For alice the token is the HMAC-SHA256 of "alice" with the demo secret
```

## One-Day Learning Plan

### Morning (3-4h): Core Long Connection
1. Read Netty Bootstrap + ChannelPipeline (this project's `NettyImServer`)
2. Understand LengthFieldBasedFrameDecoder (why we need it)
3. IdleStateHandler + heartbeat
4. Replace the string protocol with **Protobuf** (see below)

### Afternoon (3h): Reliability & Scale Patterns
1. Online status with Redisson (`OnlineUserRegistry`)
2. Message backlog / offline: sketch RocketMQ producer or Redis List
3. High-throughput: introduce Disruptor for business processing
4. Compression: LZ4 on payload before writing to channel

### Evening (2h): Production Mindset
1. Attach **Arthas** to the running process
   - `dashboard`, `thread -n 10`, `memory`, `jad` on your handler
2. Practice finding a deliberate leak or deadlock (add a demo later)
3. Review common IM pitfalls: message loss, duplicate, out-of-order, long-connection reconnect

## Next Steps to Make It Production-like

1. **Protobuf**: Add `message.proto`, use `protobuf-maven-plugin`, replace StringDecoder/Encoder with ProtobufDecoder/Encoder.
2. **RocketMQ**: On offline user, send to topic; consumer writes to Elasticsearch or DB.
3. **Reconnect & ACK**: Client-side exponential backoff + server-side message ID + ACK.
4. **Security**: Replace demo HMAC with proper JWT or custom binary token + RC4 if the JD requires it (note: RC4 is weak; prefer AES-GCM).
5. **Observability**: Metrics for connection count, message lag, GC.

## Key Resources

- Netty + Protobuf examples: https://github.com/xuwujing/Netty-study
- Distributed IM reference: https://github.com/crossoverJie/cim
- Arthas quick start: https://arthas.aliyun.com/doc/quick-start.html
- Disruptor: https://lmax-exchange.github.io/disruptor/

## JD Mapping

| JD Requirement              | Where in this POC                  |
|----------------------------|------------------------------------|
| Netty long connection      | `NettyImServer`                    |
| Protobuf / Protostuff      | Dependency + extension guide       |
| HMAC / RC4 / LZO / LZ4     | `HmacUtil` + lz4 dependency        |
| Spring Boot + Redis/Redisson | `OnlineUserRegistry`            |
| RocketMQ / Disruptor       | Dependencies ready                 |
| Arthas JVM troubleshooting | `arthas-spring-boot-starter`       |
| Message backlog / loss     | Offline path sketched              |

---

**This is a learning skeleton, not a production IM.** Use it to drive your study, then expand the parts the interviewer will dig into.
