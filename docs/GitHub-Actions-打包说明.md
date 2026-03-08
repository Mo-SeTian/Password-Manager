# GitHub Actions 打包说明

当前仓库已加入自动打包工作流：

- 工作流文件：`.github/workflows/release-apk.yml`
- 触发方式：
  1. 推送 `v*` 格式的标签，例如 `v1.0.0`
  2. 手动在 GitHub Actions 页面点击运行

## 当前策略

当前使用的是：

- **Debug APK 自动构建**
- 自动创建 GitHub Release
- 自动把 APK 挂到 Release 附件

这样做的优点是：

- 不需要先处理 keystore 签名
- 能最快验证 GitHub Actions 打包链路是否打通
- 适合当前阶段性 V1 节点做交付验证

## 后续可升级

后续如果要正式发布，可以再继续补：

- Release 签名 APK / AAB
- GitHub Secrets 中配置 keystore
- versionCode / versionName 自动化
- changelog 模板
- 多渠道构建
