docker build --no-cache -f ./Dockerfile.build -t sb-ext-service-build:eagle .

docker run --name sbext-build sb-ext-service-build:eagle && docker cp sbext-build:/opt/target/bodhi-1.0-SNAPSHOT.jar .
docker rm -f sbext-build
docker rmi -f sb-ext-service-build

docker build --no-cache -t 708570229439.dkr.ecr.us-east-1.amazonaws.com/sb-ext-service:remove_filter_latest_recommendations .
docker push 708570229439.dkr.ecr.us-east-1.amazonaws.com/sb-ext-service:remove_filter_latest_recommendations
