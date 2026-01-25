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

## 部署流程

### 修改代码后重新部署

**⚠️ 重要：** 只重新构建和部署 Java 工程，不要重建 Elasticsearch 容器！

**原因**：
1. **保留索引数据** - ES 容器重建会丢失所有索引数据（1000万+种子）
2. **保留 IK 分词器插件** - 容器重建后插件会丢失，导致集群状态变为 red

**正确命令**：
```bash
# 只重建 Java 应用（推荐）
docker compose -f docker-compose.yml up -d --build bt-search

# 或者使用 docker-compose（旧语法）
docker-compose -f docker-compose.yml up -d --build bt-search
```

### ⚠️ ES 容器重建后的恢复步骤

如果不小心重建了 ES 容器，需要重新安装 IK 分词器：

```bash
# 1. 复制插件到容器
docker cp /mnt/nvme/app/bt-search/elasticsearch-analysis-ik-8.11.0.zip bt-search-es:/tmp/

# 2. 安装插件
docker exec bt-search-es elasticsearch-plugin install file:///tmp/elasticsearch-analysis-ik-8.11.0.zip -b

# 3. 重启 ES
docker restart bt-search-es

# 4. 等待 ES 启动完成（约 10-15 秒）
sleep 10

# 5. 验证集群状态（应该是 green）
curl -s http://localhost:9200/_cluster/health?pretty
```

**症状识别**：
- ES 集群状态为 `red`
- 日志出现：`Custom Analyzer [ik_max_word_analyzer] failed to find tokenizer under name [ik_max_word]`
- 网页搜索功能异常

### 注意事项
- **不需要删除** `bt-search-es` (Elasticsearch) 容器
- **不需要删除** 数据卷 (`es-data`)
- 只重建 `bt-search-app` 容器
- 这样可以保留 Elasticsearch 的索引数据和已安装的插件

