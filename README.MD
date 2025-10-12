# Leaves Plugin Template

[![许可证](https://img.shields.io/github/license/LeavesMC/leaves-plugin-template)](LICENSE)

一个帮助你快速开始 Leaves 插件开发的项目模板

**当前仅支持 [Gradle](https://gradle.org/)**

## 特性

- 在构建脚本中声明插件元数据
- 开箱即用的 [NMS](https://docs.papermc.io/paper/dev/internals/), [Mixin](https://github.com/FabricMC/Mixin)，[Mixin Extras](https://github.com/LlamaLad7/MixinExtras)，[AccessWidener](https://github.com/FabricMC/access-widener)
- 快速启动开发服务器

**此模板为全功能模板，包含了上述说到的所有功能，如果你不需要某（几）个功能，请参考：[其他分支](BRANCHES.MD)**

## 依赖

此模板需要 JDK 21 或更高版本

## 快速开始

1. **克隆本模板：**

   ```bash
   git clone https://github.com/LeavesMC/leaves-plugin-template.git
   ```

2. **配置项目**

   按照项目中的 `// TODO` 注释完成对应内容，并根据你的插件需求修改 `src/mixin/resources` 下的 `leaves-template-plugin.accesswidener` 与 `leaves-template-plugin.mixins.json` 文件

3. **编写插件 ：**

   在 `src/main` 模块下编写你的插件代码
   在 `src/mixin` 模块下编写你的 Mixin 代码

4. **构建插件：**

   ```bash
   ./gradlew build
   ```

   构建成功后，插件 JAR 文件会生成在 `build/libs` 目录下。

5. **部署插件：**

   将生成的 `.jar` 文件放入支持 Leaves 插件的服务器的 `plugins` 文件夹，并重启服务器。

## 常见问题

**用此模板开发的插件可以用于哪些服务端？**

目前只有 [Leaves](https://github.com/LeavesMC/Leaves)

**什么是 Mixin？**

参考 [Fabric 教程：Mixin 介绍](https://wiki.fabricmc.net/zh_cn:tutorial:mixin_introduction)

**什么是 Mixin Extras？**

参考 [GitHub：Mixin Extras](https://github.com/LlamaLad7/MixinExtras)

（本模板已配置，无需额外操作）

**什么是 AccessWidener？**

参考 [Fabric 教程：访问加宽器](https://wiki.fabricmc.net/zh_cn:tutorial:accesswideners)

**为什么 Mixin 不起作用？**

Leavesclip 默认禁用了 Mixin 功能。你需要在服务器启动脚本中加入 `-Dleavesclip.enable.mixin=true`

模板中附带的开发服务器已默认启用 Mixin，无需额外设置。

## 贡献

欢迎通过 Issue 或 Pull Request 提交你的建议或改进！

## 许可证

本项目遵循 [MIT License](LICENSE) 开源协议。