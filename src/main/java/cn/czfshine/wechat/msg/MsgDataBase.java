package cn.czfshine.wechat.msg;
import cn.czfshine.wechat.contant.Contact;
import cn.czfshine.wechat.contant.Group;
import cn.czfshine.wechat.contant.Person;
import cn.czfshine.wechat.contant.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * 用来操作微信聊天数据库，要使用解密后的数据库文件
 *
 * @author:czfshine
 * @date:2018/1/15 17:52
 */

public class MsgDataBase {
    private String datapath;
    private String selfid="self";
    Message[] allmsgs;
    Map<String,Contact> contacts;
    private Logger logger = LoggerFactory.getLogger("DBofmsg");
    private void getSelf(){
        //TODO
    }
    private Connection connection;
    public MsgDataBase(String path) throws SQLException {
        datapath=path;
        init();
    }

    private void init() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:"+datapath);
        allmsgs=getAllMsgssage();
        contacts=getAllConTact();
        popAllMessageToContact();

    }

    /* 会话信息 */
    private Map<String,Contact> getAllConTact() throws SQLException {
        Map<String,Contact> contacts=new HashMap<>();

        Statement statement = connection.createStatement();
        Logger logger = LoggerFactory.getLogger("DBofmsg");

        ResultSet rs = statement.executeQuery("SELECT username,alias,conRemark,nickname,type,verifyFlag,contactLabelIds FROM rcontact");
        int count=0;
        while (rs.next()) {
            count++;
            String username=rs.getString("username");
            String nickname=rs.getString("nickname");

            if(username.endsWith("@chatroom")){
                contacts.put(username,new Group(username,nickname));
            }else if(username.startsWith("gh_")){
                contacts.put(username,new Service(username,nickname));
            }else{
                String alias=rs.getString("alias");
                String conRemark=rs.getString("conRemark");
                String contactLabelIds=rs.getString("contactLabelIds");
                contacts.put(username,new Person(username,nickname,alias,conRemark,contactLabelIds));
            }
        }

        logger.info("一共有{}个用户信息",contacts.size());
        return contacts;
    }


    public List <Contact> getAllChatRoom(){
        List <Contact> chatrooms = new ArrayList<>(contacts.values() );
        chatrooms.sort(Comparator.comparingInt((Contact a) -> -a.getMessages().size()));
        for(Contact chatroom:chatrooms){
            chatroom.sortMessage();
            logger.info("username{}-count{}",chatroom.getNickname(),chatroom.getMessages().size());
        }

        return chatrooms;
    }

    private Message[] getAllMsgssage() throws SQLException {
        logger.info("开始读取聊天");
        long startTime=System.nanoTime();
        int count=0;
        ArrayList<Message> msgs=new ArrayList<>();

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(
                "SELECT  msgSvrId,type,isSend,createTime,talker,content,imgPath  FROM message ");

        while (rs.next()) {
            count++;
            try {
                Message msg=parseMsgRow(rs);
                if(msg!=null) {
                    logger.debug(msg.toString());
                    msgs.add(msg);
                }
            }catch (DatabaseDamagedException e) {
                logger.warn("在数据库{}第{}条消息损坏",datapath,""+count);
            }catch (UnknowMassageTypeException w){
                logger.warn("类型{}未知，内容为：{}\n\t",datapath,""+count,""+w.getId(),rs.getString("content"));
            }catch (Exception e){
                logger.error("在数据库{}第{}条消息出错",datapath,""+count);
                throw e;
            }

        }

        logger.info("共读取{}条记录，解析成功{}条记录",count,msgs.size());

        long endTime=System.nanoTime();
        logger.info("读取解析耗时{}纳秒",endTime-startTime);

        Message[] res=new Message[msgs.size()];

        statement.close();
        return msgs.toArray(res);


    }



    private Message parseMsgRow(ResultSet rs) throws SQLException, DatabaseDamagedException, UnknowMassageTypeException {
        return MessageFactory.getMessage(rs);
    }
    private void popAllMessageToContact() throws SQLException {

        Logger logger=LoggerFactory.getLogger("DBmsg");
        for(Message msg:allmsgs){
            String chatroom=msg.getChatroom();
            if(contacts.containsKey(chatroom)){
                contacts.get(chatroom).addMessage(msg);
            }else{
                logger.warn("聊天室id:{}有聊天记录，但是在rcontact里面没有",chatroom);
            }
        }
    }

}