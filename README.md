Cornelius Wiehl c.wiehl@campus.fct.unl.pt 72009

Jonas Dimitirow j.dimitrow@campus.fct.unl.pt 71867

To use azure adjust the property files in `src/main/resources`

There are some properties files expected with the following keys:
- `db.properties` for the cosmosdb or postgresql connection
  - `dbtype` should be set to `postgresql`
  - `connectionString` the postgresql connection string
  - `username` the postgresql user
  - `password` the password for the postgresql user
- `redis.properties` for the redis cache connection
  - `redisHostName` for the redis host
  -  `redisKey` for the redis key
  -  `redisPort` for the redis port (default 6380)
  -  `redisTimeout` for the redis timeout (default 1000) 
  -  `redisUseTls` to use a tls connection (default true)


## Kubernetes

1. Build docker 

`docker build -t <dockerhub-username>/tukano-app:v1 .`

2. Push docker image

`docker login`
`docker push <dockerhub-username>/tukano-app:v1`


3. Start minikube

   `minikube start`
   
4. create deployments, services and volumes
   `kubectl apply -f <yaml file>`

5. Get your IP with

`minikube service tukano-service`


