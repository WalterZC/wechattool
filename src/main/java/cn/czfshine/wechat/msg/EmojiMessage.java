package cn.czfshine.wechat.msg;

import cn.czfshine.wechat.database.DatabaseDamagedException;
import cn.czfshine.wechat.database.pojo.MessageDO;
import cn.czfshine.wechat.emoji.Emoji;
import cn.czfshine.wechat.emoji.EmojiPool;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 表情
 * @author:czfshine
 * @date:2018/2/20 23:48
 */

public class EmojiMessage  extends BaseMessage implements Serializable {
    private String md5;

    private Emoji emoji;
    public static final MSGTYPE TYPE=MSGTYPE.TYPE_EMOJI;
    public EmojiMessage(MessageDO messageDO) throws SQLException, DatabaseDamagedException {
        super(messageDO);
        init(messageDO);
    }

    private void init(MessageDO messageDO) throws SQLException {
        //TODO:* WTF TALKER
        md5= messageDO.getImgPath();

        String imgPath = messageDO.getImgPath();
        emoji=EmojiPool.getEmoji(imgPath);
        setTalker(messageDO.getContent());
    }

    @Override
    public String toString() {
        return  super.toString()+
                "EmojiMessage{" +
                "md5='" + md5 + '\'' +
                '}';
    }

    public Emoji getEmoji() {
        return emoji;
    }
}
