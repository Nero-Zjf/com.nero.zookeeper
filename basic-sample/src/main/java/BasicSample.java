import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * 分布式配置中心demo
 *
 * @author
 */
public class BasicSample {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private static ZooKeeper zk = null;
    private static Stat stat = new Stat();

    public static void main(String[] args) throws Exception {
        //连接zookeeper并且注册一个默认的监听器
        //对于集群connectString为"192.168.1.126:2181,192.168.1.126:2182"
        zk = new ZooKeeper("172.17.17.20:2181", 5000,
                new Watcher() {// 事件监控
                    public void process(WatchedEvent event) {
                        if (Event.KeeperState.SyncConnected == event.getState()) {  //zk连接成功通知事件
                            if (Event.EventType.None == event.getType() && null == event.getPath()) {
                                connectedSemaphore.countDown();
                            } else if (event.getType() == Event.EventType.NodeDataChanged) {  //zk目录节点数据变化通知事件
                                try {
                                    System.out.println(event.getPath() + "配置已修改，新值为：" + new String(zk.getData(event.getPath(), true, stat)));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
        //等待zk连接成功的通知
        System.out.println("await");
        connectedSemaphore.await();


        // 查询指定node是否存在
        Stat java_stat = zk.exists("/java", true);
        // 创建Znode
        if (java_stat == null) {
            zk.create("/java", "api".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        //获取path目录节点的配置数据，并注册默认的监听器
        System.out.println(new String(zk.getData("/java", true, java_stat)));
        System.out.println(new String(zk.getData("/redis/user", true, stat)));

        System.out.println(zk.getChildren("/redis", true));
        //zk.delete("/java", java_stat.getVersion());
        zk.delete("/java", 5);
        System.out.println("start sleep");
        Thread.sleep(Integer.MAX_VALUE);
    }
}