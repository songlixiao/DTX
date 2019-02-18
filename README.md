# DTX
Distributed transaction manager. Implement database transaction control in microservice scenarios such as SpringCloud or Dubbo.

分布式事务管理器。用于SpringCloud或Dubbo之类的微服务框架场景下的数据库事务控制。

在微服务场景下使用。当事务提交时，并非真正提交数据库，而是等待顶级事务（简称：根事务）的提交或回滚通知。收到通知后所有子事务，统一提交或者回滚事务。从而达到微服务架构跨JVM场景下的数据库事务控制的目的。

当前只实现了Dubbo版本的事务控制，以及使用Redis消息队列作为事务通知的功能。后续会拓展对SpringCloud事务的支持，以及使用Zookeeper或者ActiveMQ等作为事务提交通知的实现。
