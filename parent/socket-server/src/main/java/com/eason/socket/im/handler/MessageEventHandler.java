package com.eason.socket.im.handler;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.eason.socket.im.dao.ClientInfoRepository;
import com.eason.socket.im.po.ClientInfo;
import com.eason.socket.im.po.Room;
import com.eason.socket.im.protocol.MessageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;

@Component
public class MessageEventHandler {
    private final SocketIOServer server;

    @Autowired
    private ClientInfoRepository clientInfoRepository;

    @Autowired
    public MessageEventHandler(SocketIOServer server) {
        this.server = server;
    }

    //添加connect事件，当客户端发起连接时调用，本文中将clientid与sessionid存入数据库
    //方便后面发送消息时查找到对应的目标client,
    @OnConnect
    public void onConnect(SocketIOClient client) {
        String clientId = client.getHandshakeData().getSingleUrlParam("clientid");
        ClientInfo clientInfo = clientInfoRepository.findClientByclientid(clientId);
        if (clientInfo != null) {
            Date nowTime = new Date(System.currentTimeMillis());
            clientInfo.setConnected((short) 1);
            clientInfo.setMostsignbits(client.getSessionId().getMostSignificantBits());
            clientInfo.setLeastsignbits(client.getSessionId().getLeastSignificantBits());
            clientInfo.setLastconnecteddate(nowTime);
            clientInfoRepository.save(clientInfo);
        }
        Collection<SocketIONamespace> collection= server.getAllNamespaces();
        for (SocketIONamespace socketIONamespace: collection){
            System.out.println("s="+socketIONamespace.getName());
        }
        Collection<SocketIOClient> clients= server.getAllClients();
        for (SocketIOClient client1: clients){
            System.out.println("client1="+client1.getAllRooms());
        }
        System.out.println("client="+client.getNamespace().getName());
    }

    //添加@OnDisconnect事件，客户端断开连接时调用，刷新客户端信息
    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String clientId = client.getHandshakeData().getSingleUrlParam("clientid");
        ClientInfo clientInfo = clientInfoRepository.findClientByclientid(clientId);
        if (clientInfo != null) {
            clientInfo.setConnected((short) 0);
            clientInfo.setMostsignbits(null);
            clientInfo.setLeastsignbits(null);
            clientInfoRepository.save(clientInfo);
        }
    }

    @OnEvent(value = "createRoom")
    public void createRoom(SocketIOClient client, AckRequest request, Room room) {
        SocketIONamespace chatNamespace=server.addNamespace("/"+room.getRoomName());
        MessageInfo sendData = new MessageInfo();
        sendData.setMsgContent("聊天室创建成功");
        chatNamespace.getBroadcastOperations().sendEvent("sendMsg",sendData);
    }

    @OnEvent(value = "destoryRoom")
    public void destoryRoom(SocketIOClient client, AckRequest request, Room room) {
        MessageInfo sendData = new MessageInfo();
        sendData.setMsgContent("聊天室已经关闭");
        client.getNamespace().getBroadcastOperations().sendEvent("sendMsg",sendData);
        server.removeNamespace("/"+room.getRoomName());
    }

    //消息接收入口，当接收到消息后，查找发送目标客户端，并且向该客户端发送消息，且给自己发送消息
    @OnEvent(value = "sendMsg")
    public void onEvent(SocketIOClient client, AckRequest request, MessageInfo data) {
//        String targetClientId = data.getTargetClientId();
//        ClientInfo clientInfo = clientInfoRepository.findClientByclientid(targetClientId);
//        List<ClientInfo> clientInfoList=clientInfoRepository.findAll();
//        clientInfoList.forEach(clientInfo -> {
//            if (clientInfo != null && clientInfo.getConnected() != 0) {
//                UUID uuid = new UUID(clientInfo.getMostsignbits(), clientInfo.getLeastsignbits());
//                System.out.println(uuid.toString());
//                MessageInfo sendData = new MessageInfo();
//                sendData.setSourceClientId(data.getSourceClientId());
//                sendData.setTargetClientId(clientInfo.getClientid());
//                sendData.setMsgType("chat");
//                sendData.setMsgContent(data.getMsgContent());
////                client.sendEvent("messageevent", sendData);
//                if (server.getClient(uuid)!=null){
////                    server.getClient(uuid).sendEvent("sendMsg", sendData);
//                }
//                server.getBroadcastOperations().sendEvent("sendMsg", sendData);
//            }
//        });
        MessageInfo sendData = new MessageInfo();
        sendData.setMsgContent(data.getMsgContent());
        server.getBroadcastOperations().sendEvent("sendMsg", sendData);
        System.out.println("namespace==="+client.getNamespace().getName());
    }
}