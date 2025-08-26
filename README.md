# 微信公众号图片快速下载

## 环境需求：

- [java](https://www.java.com/zh-CN/)

- [chrome driver 驱动](https://github.com/GoogleChromeLabs/chrome-for-testing#json-api-endpoints)

## 配置文件

setting.properties 文件

```properties
# [NEEDED] driverPath 配置本地的driver位置，注意driver要和本地的google浏览器大版本一致
driverPath=D:\\tools\\jars\\chromedriver-win64\\chromedriver.exe

# [OPTION] 替换保存的文件非法字符为下划线_，如果有其他字符也可以添加（如webdav传输限制字符）。
#replacePattern="[\\\\/:*?"<>|｜]"

# [OPTION] 保存的文件夹日期格式
#prefixFormat="yyyy_MM_dd_HHmmss"
```

## 附加说明

图片将会保存在当前目录的img文件夹下，如果不存在则会创建。

以文章为单元创建文件夹，文件夹名称取自文章标题。

图片名称按照顺序编号，以10000起编，方便文件名称对齐计算。

## 推荐

推荐搭配[openlist](https://github.com/OpenListTeam/OpenList) 来管理图片存储

推荐搭配[rclone](https://github.com/rclone/rclone) 来同步图片到webdav服务器

```cmd
rclone copy img WEBDAV:IMG -P
```

