## 美团点评万亿级 KV 存储架构演进



以下文章来源于InfoQ Pro ，作者Kitty

演讲嘉宾 | 齐泽斌

演讲整理 | Kitty

KV 存储作为美团点评一项重要的在线存储服务，承载了在线服务每天万亿级的请求量，并且保持着 5 个 9 的服务可用性。美团点评高级技术专家齐泽斌在 QCon 全球软件开发大会（上海站）2019 分享了《美团点评万亿级 KV 存储架构与实践》，本文为演讲整理，主要分为四个部分：第一部分是美团点评 KV 存储发展历程；第二部分是内存 KV Squirrel 架构和实践；第三部分是持久化 KV Cellar 架构和实践；最后是关于发展规划和业界新趋势的介绍。

1美团点评 KV 存储发展历程

下面我们看第一部分。美团点评第一代的分布式 KV 存储就是下图左侧这个架构，应该很多公司都经历过这个阶段。在客户端内做一致性哈希，后端部署上很多 Memcached 实例，这样就实现了最基本的 KV 存储分布式设计。但这样的设计有很明显的问题：比如宕机摘除节点会丢数据；比如缓存空间不够需要扩容，一致性哈希也会丢一些数据，这样会给业务的开发带来很多困扰。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCsPFWAbEh4BLHm2Nib9bnLXZQxsfRlv1AwjfDrf6LXniclt8AzuNN43pbA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

随着 Redis 项目成熟起来，美团也引入了 Redis 来解决我们上面提到的问题，进而演进出来上图右侧这样一个架构。大家可以看到，客户端还是一样，用一致性哈希算法，到服务器端变成 Redis 组成的主从结构，当任何一个节点宕机，我们可以通过 Redis 哨兵完成 Failover，实现高可用。但有个问题还是没有解决，如果扩缩容的话，一致性哈希仍然会丢数据，这个问题如何解决呢？

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCsDwpYrzd4HZOiadwucibgr8Yww45eBb4JTvANngy00zGadb44YlhOseRg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

这个时候我们发现开源界有了一个比较成熟的 KV 存储：阿里 Tair 。2014 年，我们把 Tair 引入到公司，来满足业务 KV 存储方面的需求。Tair 开源版本的架构主要是三部分：上图下边是存储节点，存储节点会上报心跳到它的中心节点，中心节点内部有两个配置管理节点，会监控所有的存储节点，有任何存储节点宕机或者扩容之类，它会做集群拓扑的重新构建。客户端启动的时候，它会直接从中心节点拉来一个路由表，这个路由表简单来说就是一个集群的数据分布图，客户端根据路由表直接去存储节点读写。之前我们 KV 遇到的扩容丢数据问题，它也有数据迁移机制来保证数据的完整性。

但是在使用的过程中，我们还遇到了一些其他问题，比如：它的中心节点虽然是主备高可用的，但实际上它没有类似于分布式仲裁的机制，所以在网络分割的情况下，它是有可能发生脑裂的，这个也给我们的业务造成过比较大的影响。另外，在容灾扩容的时候，也遇到过数据迁移影响到业务可用性的问题。另外，我们之前用过 Redis ，业务会发现 Redis 的数据结构特别丰富， 而 Tair 还不支持这些数据结构。虽然我们用 Tair 解决了一些问题，但是 Tair 同样也无法完全满足我们的业务需求。我们认识到在美团点评这样一个业务规模和业务复杂度的场景下，很难有开源系统能很好满足我们的需求，所以，我们决定在已应用的开源系统之上进行自研。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCsZalLWEIEbjwMI9ZXpt7RemiaSXEthMCHTGtMc0UGLlicrtnr1wpadrQQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

2015 年， Redis 官方正式发布了它的集群版本 Redis Cluster。我们紧跟社区步伐，并结合内部需求做了许多自研功能，演进出我今天要介绍的全内存、高吞吐、低延迟的 KV 存储 Squirrel。另外，我们基于 Tair，加入了许多我们自研的功能，演进出我今天要介绍的持久化、大容量、数据高可靠的 KV 存储 Cellar 。Tair 开源版本已经有四五年没有更新，所以，Cellar 的迭代完全靠自研。而 Redis 社区一直很活跃，所以，Squirrel 的迭代是自研和社区并重，自研功能设计上也会尽量与官方架构兼容。大家后面可以看到，因为这个不同，Cellar 和 Squirrel 在解决同样的问题时选取了不同的设计。

这两个存储其实是 KV 存储领域的不同解决方案。实际应用上，如果业务的数据量小，对延迟敏感，建议用 Squirrel ；如果数据量大，对延迟不是特别敏感，我们建议用成本更低的 Cellar 。目前这两套 KV 存储系统在美团点评内部每天的调用量均已突破万亿，它们的请求峰值也都突破了每秒亿级。

2内存 KV Squirrel 架构和实践

在开始之前，我先介绍两个存储系统共通的地方。比如分布式存储的经典问题：数据是如何分布的，这个问题在 KV 存储领域就是 Key 是怎么分布到存储节点上的。这里 Squirrel 跟 Cellar 是一样的。当我们拿到一个 Key 后，用固定的哈希算法拿到一个哈希值，然后将哈希值对 Slot 数目取模得到一个 Slot id，我们两个 KV 现在都是预分片 16384 个 Slot 。得到 Slot id 之后，我们再根据路由表就能查到这个 Slot 存储在哪个存储节点上。这个路由表简单来说就是 Slot 到存储节点的对照表。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCswclctiahibGBJWsiaeiaMOWD0z8jiaxyPYPwAWP3jE7qy9syfaXGHqVho9Q/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

接下来讲一下我对高可用架构的认知。我个人认为高可用可以从宏观和微观两个角度来看。从宏观的角度来看，高可用就是指容灾怎么做。比如说挂一个节点，你该怎么做？挂一个机房或者说某个地域的一批机房挂了，你该怎么做？从微观的角度看，高可用就是怎么能保证端到端的高成功率。在做一些运维升级或者扩缩容数据迁移的时候，你能否做到业务请求的高可用？所以，后面的演讲我也会从宏观和微观两个角度来分享我们做的高可用工作。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCsoVdp2W1ywoDibj9ibs93s2BsJ5eEUwpBoB8UkuEylpEwYK5f937pWVng/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

上图就是我们的 Squirrel 架构。中间部分跟 Redis 官方集群是一样的。它有主从的结构， Redis 实例之间通过 Gossip 协议通信。我们在右边添加了一个集群调度平台，包含调度服务、扩缩容服务和高可用服务等，它会去管理整个集群，把管理结果作为元数据更新到 ZooKeeper。我们的客户端会订阅 ZooKeeper 上的元数据变更，实时获取集群的拓扑状态，直接去 Redis 集群进行读写操作。

Squirrel 节点容灾

接下来我们看一下 Squirrel 容灾是怎么做的。

对于 Redis 集群而言，节点宕机已经有完备的处理机制了。在官方方案下，任何一个节点从宕机到被标记为 FAIL 摘除，一般需要经过 30 秒。主库的摘除可能会影响数据的完整性，所以，我们需要谨慎一些。

但是对于从库呢？我们认为这个过程完全没有必要。另一点，我们都知道内存的 KV 存储数据量一般都比较小。对于业务量很大的公司来说，它往往会有很多的集群。如果发生交换机故障，会影响很多的集群，宕机之后去补副本就会非常麻烦。所以，为了解决这两个问题，我们做了 HA 高可用服务。

它的架构如下图所示，它会实时监控集群的所有节点。不管是网络抖动，还是发生了宕机（比如说 Redis 2 ），它可以实时更新 ZooKeeper ，告诉 ZooKeeper 去摘除 Redis 2 ，客户端收到消息后读流量就直接路由到 Redis 3 上。如果 Redis 2 只是几十秒的网络抖动，过几十秒之后， HA 节点监控到它恢复，会把它重新加回。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCsN1iaDK5s8gmiciaRHCf54RV1A4MWSHYzxpy8HI93nTgD95qx1mkHpL7YQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

如果过了一段时间，HA 判断它是永久性宕机，HA 节点会直接从 Kubernetes 集群申请一个新的 Redis 4 容器实例，把它加到集群里。此时，拓扑结构又变成了一主两从的标准结构，HA 节点更新完集群拓扑之后，就会去写 ZooKeeper 通知客户端去更新路由，客户端就能到 Redis 4 这个新从库上读了。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCsqbrnUYtCfE6ZibJR6We62PPezbZjr1sjoERjxOZ0oT5PeGj09eggZ6Q/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

通过上述方案，我们把从库的摘除时间从 30 秒降低到了 5 秒。另外，我们通过 HA 自动申请容器实例加入集群的方式，把宕机补副本变成了一个分钟级的自动操作，不需要任何人工介入。

Squirrel 跨地域容灾

我们解决了单节点宕机的问题，跨地域问题如何解决呢？我们首先来看下跨地域有什么不同。第一，相对于同地域机房间的网络而言，跨地域专线很不稳定；第二，跨地域专线的带宽是非常有限且昂贵的。而集群内的复制是没有考虑这种极端网络环境的。假如我把主库部署到北京，两个从库部署在上海，同样一份数据要在北上专线传输两次，这样会造成巨大的专线带宽浪费。另外，随着公司业务的发展和演进，我们也在做单元化部署和异地多活架构。用官方的主从同步，是满足不了我们这些需求的。基于此，我们做了集群间的复制方案。

![img](https://mmbiz.qpic.cn/mmbiz_png/YriaiaJPb26VMkhVgXiasJEhu40tn4atQCs5ADaHBr4AH3VvoibHTBECOXzM73m2icc16OThRqS3ZEic61tayoEEMTVw/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

如上图，我画了北京的主集群以及上海的从集群，我们要做的是通过集群同步服务，把北京主集群的数据同步到上海从集群上。按照流程，首先要向我们的同步调度模块下发在两个集群间建立同步链路的任务，同步调度模块会根据主从集群的拓扑结构，把主从集群间的同步任务下发到同步集群，同步集群收到同步任务后会扮成 Redis 的 Slave，通过 Redis 的复制协议，从主集群上的从库拉取数据，包括 RDB 以及后续的增量变更。同步机收到数据后会把它转成客户端的写命令，写到上海从集群的主节点里。通过这样的方式，我们把北京主集群的数据同步到了上海的从集群。同样的，我们要做异地多活也很简单，再加一个反向的同步链路，就可以实现集群间的双向同步。

接下来我们讲一下如何做好微观角度的高可用，也就是保持端到端的高成功率。对于 Squirrel ，这里会主要介绍一下我们遇到的三个影响成功率的问题：

\1. 数据迁移造成超时抖动

\2. 持久化造成超时抖动

\3. 热点 Key 请求导致单节点过载

Squirrel 智能迁移

对于数据迁移，我们主要遇到三个问题：

Redis Cluster 虽然提供了数据迁移能力，但是对于要迁哪些 Slot，Slot 从哪迁到哪它并不管；做数据迁移的时候，大家都想越快越好，但是迁移速度过快又可能影响业务正常请求；Redis 的 migrate 命令会阻塞工作线程，尤其在迁移大 Value 的时候会阻塞特别久。

为了解决这些问题，我们做了全新的迁移服务。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

下面我们按照工作流讲一下它是如何运行的。首先生成迁移任务，这一步的核心是就近原则，比如说同机房的两个节点做迁移肯定比跨机房的两个节点快。迁移任务生成之后，我会把任务下发到一批迁移机上。迁移机迁移的时候，有这样几个特点：

第一，我们会在集群内迁出节点间做并发，比如同时给 Redis 1、Redis 3 下发迁移命令；第二，每个 Migrate 命令会迁移一批 Key；第三，我们会用监控服务去实时采集客户端的成功率、耗时，服务端的负载、QPS 等，之后把这个状态反馈到迁移机上。迁移数据的过程就类似 TCP 慢启动的过程，它会把速度一直往上加，若出现请求成功率下降等情况，它的速度就会降低，最终迁移速度会在动态平衡中稳定下来，这样我们就达到了最快速的迁移，同时又尽可能小地影响业务的正常请求。

接着我们看一下大 Value 的迁移，我们实现了一个异步 Migrate 命令，该命令执行时，Redis 的主线程会继续处理其他的正常请求。如果此时有对正在迁移 Key 的写请求过来，Redis 会直接返回错误。这样最大限度保证了业务请求的正常处理，同时又不会阻塞主线程。

Squirrel 持久化重构

Redis 主从同步时会生成 RDB。生成 RDB 的过程会调用 Fork 产生一个子进程去写数据到硬盘，Fork 虽然有操作系统的 COW 机制，但是当内存用量达到 10 G 或 20 G 时，这依然会造成整个进程接近秒级的阻塞。这对在线业务来说几乎是无法接受的。我们也会为数据可靠性要求高的业务去开启 AOF，而开 AOF 就可能因 IO 抖动造成进程阻塞，这也会影响请求成功率。对官方持久化机制的这两个问题，我们的解决方案是重构持久化机制。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

上图是我们最新版的 Redis 持久化机制，写请求会先写到 DB 里，然后写到内存 Backlog，这跟官方是一样的。同时它会把请求发给异步线程，异步线程负责把变更刷到硬盘的 Backlog 里。当硬盘 Backlog 过多时，我们会主动在业务低峰期做一次 RDB ，然后把 RDB 之前生成的 Backlog 删除。

如果这时候我们要做主从同步，去寻找同步点的时候，该怎么办？第一步还是跟官方一样，我们会从 内存 Backlog 里找有没有要求的同步点，如果没有，我们会去硬盘 Backlog 找同步点。由于硬盘空间很大，硬盘 Backlog 可以存储特别多的数据，所以很少会出现找不到同步点的情况。如果硬盘 Backlog 也没有，我们就会触发一次类似于全量重传的操作，但这里的全量重传是不需要当场生成 RDB 的，它可以直接用硬盘已存的 RDB 及其之后的硬盘 Backlog 完成全量重传。通过这个设计，我们减少了很多的全量重传。另外，我们通过控制在低峰区生成 RDB ，减少了很多 RDB 造成的抖动。同时，我们也避免了写 AOF 造成的抖动。不过，这个方案因为写 AOF 是完全异步的，所以会比官方的数据可靠性差一些，但我们认为这个代价换来的可用性提升是值得的。

Squirrel 热点 Key

下面看一下 Squirrel 的热点 Key 解决方案。如下图，普通主、从是一个正常集群中的节点，热点主、从是游离于正常集群之外的节点。我们看一下它们之间怎么发生联系。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

当有请求进来读写普通节点时，节点内会同时做请求 Key 的统计。如果某个 Key 达到了一定的访问量或者带宽的占用量，会自动触发流控以限制热点 Key 访问，防止节点被热点请求打满。同时，监控服务会周期性地去所有 Redis 实例上查询统计到的热点 Key。如果有热点，监控服务会把热点 Key 所在 Slot 上报到我们的迁移服务。迁移服务这时会把热点主从节点加入到这个集群中，然后把热点 Slot 迁移到这个热点主从上。因为热点主从上只有热点 Slot 的请求，所以热点 Key 的处理能力得到了大幅提升。通过这样的设计，我们可以做到实时的热点监控，并及时通过流控去止损；通过热点迁移，我们能做到自动的热点隔离和快速的容量扩充。

3持久化 KV Cellar 架构和实践

下面看一下持久化 KV Cellar 的架构和实践。下图是我们最新的 Cellar 架构图。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

跟阿里开源的 Tair 主要有两个架构上的不同。第一个是 OB，第二个是 ZooKeeper。我们的 OB 跟 ZooKeeper 的 Observer 是类似的作用，提供 Cellar 中心节点元数据的查询服务。它可以实时与中心节点的 Master 同步最新的路由表，客户端的路由表都是从 OB 去拿。这样做的好处主要有两点，第一，把大量的业务客户端跟集群的大脑 Master 做了天然的隔离，防止路由表请求影响集群的管理。第二，因为 OB 只供路由表查询，不参与集群的管理，所以它可以水平扩展，极大地提升了我们路由表的查询能力。另外，我们引入了 ZooKeeper 做分布式仲裁，解决我刚才提到的 Master、Slave 在网络分割情况下的脑裂问题。并且通过把集群的元数据存储到 ZooKeeper，我们保证了元数据的高可靠。

Cellar 节点容灾

介绍完整体架构，我们看一下 Cellar 怎么做节点容灾的。一个集群节点的宕机一般是临时的，一个节点的网络抖动也是临时的，它们会很快的恢复，并重新加入集群。因为节点的临时离开就把它彻底摘除，并做数据副本补全操作，会消耗大量资源，进而影响到业务请求。所以，我们实现了 Handoff 机制来解决这种节点短时故障带来的影响。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

如上图 ，如果 A 节点宕机了，会触发 Handoff 机制，这时候中心节点会通知客户端 A 节点发生了故障，让客户端把分片 1 的请求也打到 B 上。B 节点正常处理完客户端的读写请求之后，还会把本应该写入 A 节点的分片 1&2 数据写入到本地的 Log 中。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

如果 A 节点宕机后 3~5 分钟，或者网络抖动 30~50 秒之后恢复了，A 节点就会上报心跳到中心节点，中心节点就会通知 B 节点，“ A 节点恢复了，你去把它不在期间的数据传给它。”这时候 B 节点就会把本地存储的 Log 回写到 A 节点。等到 A 节点拥有了故障期间的全量数据之后，中心节点就会告诉客户端，A 节点已经彻底恢复了，客户端就可以重新把分片 1 的请求打回 A 节点。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

通过这样的操作，我们可以做到秒级的快速节点摘除，而且节点恢复后加回，只需补齐少量的增量数据。另外如果 A 节点要做升级，中心节点先通过主动 Handoff 把 A 节点流量切到 B 节点，A 升级后再回写增量 Log，然后切回流量加入集群。这样通过主动触发 Handoff 机制我们就实现了静默升级功能。

Cellar 跨地域容灾

下面我介绍一下 Cellar 跨地域容灾是怎么做的。Cellar 跟 Squirrel 面对的跨地域容灾问题是一样的，解决方案同样也是集群间复制。以下图一个北京主集群、上海从集群的跨地域场景为例，比如说客户端的写操作到了北京的主集群 A 节点，A 节点会像正常集群内复制一样，把它复制到 B 和 D 节点上。同时 A 节点还会把数据复制一份到从集群的 H 节点。H 节点处理完集群间复制写入之后，它也会做从集群内的复制，把这个写操作复制到从集群的 I 、K 节点上。通过在主从集群的节点间建立这样一个复制链路，我们完成了集群间的数据复制，并且这个复制保证了最低的跨地域带宽占用。同样的，集群间的两个节点通过配置两个双向复制的链路，就可以达到双向同步异地多活的效果。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

Cellar 强一致

我们做好了节点容灾以及跨地域容灾后，业务又对我们提出了更高要求：强一致存储。我们之前的数据复制是异步的，在做故障摘除时，可能因为故障节点数据还没复制出来，导致数据丢失。但是对于金融支付等场景来说，它们是不容许数据丢失的。面对这个难题我们该怎么解决？目前业界主流的解决方案是基于 Paxos 或 Raft 协议的强一致复制。我们最终选择了 Raft 协议。主要是因为 Raft 论文是非常详实的，是一篇工程化程度很高的论文。业界也有不少比较成熟的 Raft 开源实现，可以作为我们研发的基础，缩短研发周期。

下图是现在 Cellar 集群 Raft 复制模式下的架构图，中心节点会做 Raft 组的调度，它会决定每一个 Slot 的三副本存在哪些节点上。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

大家可以看到 Slot 1 在存储节点 1、2、4 上，Slot 2 在存储节点 2、3、4 上。每个 Slot 组成一个 Raft 组，客户端会去 Raft Leader 上进行读写。由于我们是预分配了 16384 个 Slot，所以，在集群规模很小的时候，我们的存储节点上可能会有数百甚至上千个 Slot 。这时候如果每个 Raft 复制组都有自己的复制线程、 复制请求和 Log 等，那么资源消耗会非常大，写入性能会很差。

所以我们做了 Multi Raft 实现， Cellar 会把同一个节点上所有的 Raft 复制组写一份 Log，用同一组线程去做复制，不同 Raft 组间的复制包也会按照目标节点做整合，以保证写入性能不会因 Raft 组过多而变差。

Raft 内部其实是有自己的选主机制，它可以自己控制自己的主，如果有任何节点宕机，它可以通过选举机制选出新的主。那么，中心节点是不是就不需要管理 Raft 组了吗？不是的。

这里讲一个典型的场景，如果一个集群的部分节点经过几轮宕机恢复的过程， Raft Leader 在存储节点之间会变得极其不均。而为了保证数据的强一致，客户端的读写流量又必须发到 Raft Leader，这时候集群的节点流量会很不均衡。所以我们的中心节点还会做 Raft 组的 Leader 调度。比如说 Slot 1 存储在节点 1、2、4，并且节点 1 是 Leader。如果节点 1 挂了，Raft 把节点 2 选成了 Leader。然后节点 1 恢复了并重新加入集群，中心节点这时会让节点 2 把 Leader 还给节点 1 。这样，即便经过一系列宕机和恢复，我们存储节点之间的 Leader 数目仍然能保证是均衡的。

接下来我们看一下 Cellar 如何保证它的端到端高成功率。这里我也讲三个影响成功率的问题。Cellar 遇到的数据迁移和热点 Key 问题与 Squirrel 是一样的，但解决方案是不一样的，这是因为 Cellar 走的自研路径，不用考虑与官方版本的兼容性，对架构改动更大些。另一个问题是慢请求阻塞服务队列导致大面积超时，这是 Cellar 网络、工作多线程模型设计下会遇到的不同问题。

Cellar 智能迁移

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

上图是 Cellar 智能迁移架构图。我把桶的迁移分成了三个状态。第一个状态就是正常的状态，没有任何迁移。如果这时候要把 Slot 2 从 A 节点迁移到 B 节点，A 会给 Slot 2 打一个快照，然后把这个快照全量发到 B 节点上。在迁移数据的时候， B 节点的回包会带回 B 节点的状态。B 的状态包括什么？引擎的压力、网卡流量、队列长度等。A 节点会根据 B 节点的状态调整自己的迁移速度。像 Squirrel 一样，它经过一段时间调整后，迁移速度会达到一个动态平衡，达到最快速的迁移，同时又尽可能小的影响业务的正常请求。

当 Slot 2 迁移完后， 会进入图中 Slot 3 的状态。客户端这时可能还没更新路由表，当它请求到了 A 节点，A 节点会发现客户端请求错了节点，但它不会返回错误，它会把请求代理到 B 节点上，然后把 B 的响应包再返回客户端。同时它会告诉客户端，你需要更新一下路由表了，此后客户端就能直接访问到 B 节点。这样就解决了客户端路由更新延迟造成的请求错误。

Cellar 快慢列队

下图上方是一个标准的线程队列模型。网络线程池接收网络流量解析出请求包，然后把请求放到工作队列里，工作线程池会从工作队列取请求来处理，然后把响应包放回网络线程池发出。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

我们分析线上发生的超时案例时发现，一批超时请求当中往往只有一两个请求是引擎处理慢导致的，大部分请求只是因为在队列等待过久导致整体响应时间过长而超时了。从线上分析来看，真正的慢请求占超时请求的比例只有 1/20。

我们的解法是什么样？很简单，拆线程池、拆队列。我们的网络线程在收到包之后，会根据它的请求特点，是读还是写，快还是慢，分到四个队列里。读写请求比较好区分，但快慢怎么分开？我们会根据请求的 Key 个数、Value 大小、数据结构元素数等对请求进行快慢区分。然后用对应的四个工作线程池处理对应队列的请求，就实现了快慢读写请求的隔离。这样如果我有一个读的慢请求，不会影响另外三种请求的正常处理。不过这样也会带来一个问题，我们的线程池从一个变成四个，那线程数是不是变成原来的四倍？其实并不是的，我们某个线程池空闲的时候会去帮助其它的线程池处理请求，所以，我们线程池变成了四个，但是线程总数并没有变。我们线上验证中这样的设计能把服务 TP999 的延迟降低 86%，可大幅降低超时率。

Cellar 热点 Key

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

上图是 Cellar 热点 Key 解决方案的架构图。

我们可以看到中心节点加了一个职责，多了热点区域管理，它现在不只负责正常的数据副本分布，还要管理热点数据的分布，图示这个集群在节点 C、D 放了热点区域。

我们通过读写流程看一下这个方案是怎么运转的。如果客户端有一个写操作到了 A 节点，A 节点处理完成后，会根据实时的热点统计结果判断写入的 Key 是否为热点。如果这个 Key 是一个热点，那么它会在做集群内复制的同时，还会把这个数据复制有热点区域的节点，也就是图中的 C、D 节点。同时，存储节点在返回结果给客户端时，会告诉客户端，这个 Key 是热点，这时客户端内会缓存这个热点 Key。当客户端有这个 Key 的读请求时，它就会直接去热点区域做数据的读取。通过这样的方式，我们可以做到只对热点数据做扩容，不像 Squirrel ，要把整个 Slot 迁出来做扩容。有必要的话，中心节点也可以把热点区域放到集群的所有节点上，所有的热点读请求就能均衡的分到所有节点上。另外，通过这种实时的热点数据复制，我们很好地解决了类似客户端缓存热点 KV 方案造成的一致性问题。

4发展规划和业界趋势

最后，一起来看看我们项目的规划和业界的技术趋势。这块我会按照服务、系统、硬件三层来阐述。

![img](data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg==)

服务层，主要有三点：

第一，Redis Gossip 协议优化。大家都知道 Gossip 协议在集群的规模变大之后，消息量会剧增，它的 Failover 时间也会变得越来越长。所以当集群规模达到 TB 级后，集群的可用性会受到很大影响，所以我们后面会重点在这方面做一些优化；

第二，我们已经在 Cellar 存储节点的数据副本间做了 Raft 复制，可以保证数据强一致，后面我们会在 Cellar 的中心点内部也做一个 Raft 复制，这样就不用依赖于 ZooKeeper 做分布式仲裁、元数据存储了，我们的架构也会变得更加简单、可靠；

第三，Squirrel 和 Cellar 虽然都是 KV 存储，但是因为他们是基于不同的开源项目研发的，所以他们的 API 和访问协议是不同的，我们之后会考虑将 Squirrel 和 Cellar 在 SDK 层做整合，虽然后端会有不同的存储集群，但业务侧是用一套 SDK 访问。

系统层面，我们正在调研并去落地一些 Kernel Bypass 技术，像 DPDK、SPDK 这种网络和硬盘的用户态 IO 技术。它可以绕过内核，通过轮询机制访问这些设备，可以极大提升系统的 IO 能力。存储作为 IO 密集型服务，性能会获得大幅提升。

硬件层面，像支持 RDMA 的智能网卡能大幅降低网络延迟和提升吞吐；还有像 3D XPoint 的闪存技术，比如英特尔新发布的 AEP 存储，其访问延迟已经比较接近内存了，以后闪存跟内存之间的界限也会越来越模糊；最后看一下计算型硬件，比如通过在闪存上加 FPGA 卡，把原本应该 CPU 做的工作，像数据压缩、解压等，下沉到卡上执行，这种硬件能在解放 CPU 的同时，还可以降低服务的响应延迟。

 嘉宾简介：

齐泽斌，美团点评高级技术专家，KV 存储团队负责人，8 年以上分布式存储研发经验。2011 年天津大学毕业后加入百度，负责过分布式文件系统 MFS 和分布式 KV BDRP 系统研发及运营。2014 年加入美团，负责过分布式 KV 存储 Cellar、分布式缓存 Squirrel、数据传输 Databus 等系统研发及运营，主要关注于分布式存储技术领域。