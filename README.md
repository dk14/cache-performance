#### How to run

Install Docker: https://www.docker.com/docker-toolbox

![Graph1](/docker.png)

So, you can simply run a test cluster:

```bash
#remove previous nodes if necessary
docker kill kamon-grafana-dashboard
docker rm kamon-grafana-dashboard
docker kill hazelseed
docker rm hazelseed
docker kill hazelnode1
docker rm hazelnode1
docker kill hazelnode2
docker rm hazelnode2


#start new cluster
docker run -d -p 80:80 -p 8125:8125/udp -p 8126:8126 --name kamon-grafana-dashboard dk14/docker_grafana_graphite
docker run -d --name hazelseed -h hazelseed --link kamon-grafana-dashboard dk14/docker-hazel
docker run -d --name hazelnode1 -h hazelnode1 --link hazelseed --link kamon-grafana-dashboard dk14/docker-hazel
docker run -d --name hazelnode2 -h hazelnode2 --link hazelseed --link kamon-grafana-dashboard dk14/docker-hazel
```

Now you can connect to 192.168.99.100:80 to see grafana dashboard with metrics:

![Graph1](/dashboard.png)


#Hazelcast-Cassandra comparision

Feature             |   Hazelcast 3.5.3          |    DSE (Cassandra+Slr+Spark)         |
--------------------|:--------------------------:|:------------------------------------:|
AP-mode             |    yes                     |    yes                               |
CP-mode             |    no                      |    yes*                              |
Split-brainOK       |    yes (merge)             |    yes (merge)                       |
ReactiveAPI         |    partially (no queries)  |    yes                               |
CRDT                |    no (manual merge)       |    yes                               |
Queries             |    yes                     |    CQL (restricted), Solr (full) **  |
Write-Through       |    yes                     |    yes (robust write)                |
WriteBehind         |    yes                     |    yes                               |
Locks               |    yes                     |    no                                |
EventListeners      | ContinuousQuery (clent)    |    ServerSide: Triggers, SparkStreams|
EmbeddedPersistence |    no                      |    yes***                            |
Easy Maintanance    |    yes                     |    no                                |
Package Size        |    ~7Mb                    |    ~700Mb                            |

`* you can setup N/R/W to choose between AP/CP

`** Solr is based on Apache Lucene

`*** you can also choose which data are more likely to be written to disk or even turn it off