# cs457-557-fall2020-pa3-mparlaw1

1. compile the project - make 
2. To run replica server - ./server.sh {port} {replica server name } { config value}.  example:  ./server.sh 9011 s1 true 
        Note:  value: false -> Read repair. true -> Hinted Hand-off
3. To run client - ./client.sh 
