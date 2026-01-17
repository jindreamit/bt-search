# 项目配置说明

## Elasticsearch IK 中文分词器

### 下载地址
- 官方镜像源（推荐）：https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.11.0.zip
- GitHub备用：https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

### 本地文件
- 位置：`/mnt/nvme/app/bt-search/elasticsearch-analysis-ik-8.11.0.zip`
- MD5: `4.4M` - elasticsearch 8.11.0 对应版本

### 安装方法
```bash
# 复制到容器
docker cp elasticsearch-analysis-ik-8.11.0.zip bt-search-es:/tmp/

# 安装插件
docker exec bt-search-es elasticsearch-plugin install file:///tmp/elasticsearch-analysis-ik-8.11.0.zip -b

# 重启ES
docker restart bt-search-es
```

## 同步服务配置

### 修复记录
1. **数据库连接池泄漏**：将 JPA `@Transactional` 改为直接使用 JdbcTemplate
2. **ES嵌套文档限制**：`index.mapping.nested_objects.limit` 提升到 500000
3. **HTTP请求大小限制**：`http.max_content_length` 提升到 500MB

### 启动同步服务
```bash
# 构建镜像
docker build -t bt-sync-app:latest -f docker/Dockerfile.sync .

# 运行同步服务
docker run -d --name bt-sync-app --network host \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/forge?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=Shijin1221.nb \
  -e SPRING_ELASTICSEARCH_URIS=http://localhost:9200 \
  bt-sync-app:latest

# 查看日志
docker logs -f bt-sync-app
```
