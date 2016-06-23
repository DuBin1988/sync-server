# sync-server

- 数据库元数据同步服务，
  * com.aote.rs.DBService  
   ```
  	请求方式： POST 
  	url：/db/meta 
  	请求体：多个实体，用英文,号隔开。如：实体实体名1,实体名2
  	``` 
- 目录文件同步服务。
  * com.aote.rs.DirService  
  * 服务1：指定目录，递归得到子目录及文件列表，每个列表项包括文件名和文件最后修改时间  
  	```
  	请求方式： GET 
  	url：/dir?path={目录名}
  	```
  * 服务2：提取指定文件，返回文件内容  
  	```
  	请求方式： POST 
  	url：/dir 
  	请求体：带绝对路径的文件名
  	```

## Maven仓库

```
http://125.76.225.203:3002/nexus/content/repositories/thirdparty/
```

以如下方式引入

```
"af:sync-server:1.0.0",
```

## 目录结构

- 源程序：`src/main/java/com/aote`
  * rs：所有服务均放在该目录下。
  * [rs/mapper](docs/mapper.md)：把异常映射成错误内容。


- 资源内容: src/test/resources
  * hibernate.cfg.xml：hibernate的主配置文件
  * hibernate目录：用于测试的数据库表

  
- 测试环境
  * 单元测试用例在 `src/test/java/com/aote` 下。
  * 单元测试对每个类进行测试，重点测试服务。

## 版本发布

发布前，修改build.gradle中版本号，用下列命令发布版本：
```
gradle release
```
"# sync-server" 
