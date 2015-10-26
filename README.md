You can simply run a test cluster:

```bash
docker run -d -p 80:80 -p 8125:8125/udp -p 8126:8126 --name kamon-grafana-dashboard dk14/docker_grafana_graphite
docker run -d --name hazelseed -h hazelseed --link kamon-grafana-dashboard dk14/docker-hazel
docker run -d --name hazelnode1 -h hazelnode1 --link hazelseed --link kamon-grafana-dashboard dk14/docker-hazel
docker run -d --name hazelnode2 -h hazelnode2 --link hazelseed --link kamon-grafana-dashboard dk14/docker-hazel
```

Now you can connect to port 80 to see graphana dashboard with metrics

