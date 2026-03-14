# 项目运行配置指引
## 1. 环境准备
- JDK 1.8+
- MySQL 8.0+
- 阿里云OSS账号（可选，若不需要图片上传功能可注释OSS相关代码）

## 2. 配置步骤
1. 复制 `src/main/resources/application-dev.yml.template` 为 `application-dev.yml`；
2. 修改 `application-dev.yml` 中的配置项：
   - 数据库配置：替换 `username`/`password` 为你的本地MySQL信息；
   - OSS配置：
     - 登录阿里云控制台，创建AccessKey（https://ram.console.aliyun.com/manage/ak）；
     - 创建OSS Bucket，复制地域节点（endpoint）和桶名（bucket-name）；
     - 将上述信息填入 `alioss` 相关配置；
3. 启动项目即可。

## 3. 注意事项
- `application-dev.yml` 已被.gitignore忽略，不会提交到Git，无需担心敏感信息泄露；
- 若没有阿里云OSS账号，可注释代码中OSS相关功能，或使用本地文件存储替代。
