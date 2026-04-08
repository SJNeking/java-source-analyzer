# 1. 停止当前运行的容器
docker-compose down

# 2. 彻底删除旧的数据库数据目录 (这是为了确保 POSTGRES_HOST_AUTH_METHOD 生效)
# 请确保你在 docker-compose.yml 所在的目录下执行
rm -rf ./postgres_data

# 3. 重新启动
docker-compose up -d