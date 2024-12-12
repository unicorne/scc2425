Cornelius Wiehl c.wiehl@campus.fct.unl.pt 72009

Jonas Dimitirow j.dimitrow@campus.fct.unl.pt 71867

To use azure adjust the property files in `src/main/resources`

There are 3 properties files expected with the following keys:
- `azureblob.properties` for the azure blobstorage connection
  - `storageConnectionString` the connection string
  - `blobContainerName` the name of the blob container
- `db.properties` for the cosmosdb or postgresql connection
  - `dbtype` to switch between cosmosdb and postgresql (possible values: `cosmosdb`, `postgresql`)
  - `connectionUrl` the connection url when using cosmosdb
  - `dbKey` the database key for cosmosdb
  - `dbName` the name of the cosmodb
  - `userContainerName` the name of the users container in cosmosdb
  - `shortContainerName` the name of the shorts container in cosmosdb
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

`docker build -t tukano-app:v1`

2. Push docker image

`docker login`
`docker push <dockerhub-username>/tukano-app:v1`


3. Start minikube

   `minikube start`
   `kubectl apply -f deployment.yaml`
   `kubectl apply -f service.yaml`

4. Get your IP with

minikube service tukano-service


