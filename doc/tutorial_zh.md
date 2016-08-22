#Stream Platform使用文档
***
##部署
###生成安装包
执行`build.sh`进行编译打包,如果不加任何参数,则生成的安装包里面只会包含最基本依赖的jar包。

如果要加入某个项目特有的功能,需要再执行脚本的时候加上参数,例如要包含ShaanxiyidongFeature项目的私有特性执行`build.sh ShaanxiyidongFeature`,生成的安装包OCDP_Stream.tar.gz保存在build文件夹里面。

###启动运行
将生成的OCDP_Stream.tar.gz解压到集群的`$SPARK_HOME`目录下,修改conf下的common.xml文件,根据实际情况修改MySQL的相关配置。

运行`$SPARK_HOME/OCDP_Stream/bin/stream_start.sh`启动流服务,执行`jps`命令查看MainFrameManager进程是否启动。