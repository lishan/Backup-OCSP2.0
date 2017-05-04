# OCSP

### 中文介绍

* [中文介绍](https://github.com/OCSP/OCSP_mainline/wiki/OCSP%E4%B8%AD%E6%96%87%E4%BB%8B%E7%BB%8D)

### Introduction

OCSP is a high performance stream processing system which is based on Spark. It supports multiple types of data input, such as Socket, files, Kafka etc. Besides, it can output the result as different types. Currently, OCSP supports output type of Kafka, Redis, HDFS etc. User does not need to know the detail of RDDs. Instead, a api function needs to be overwrittern to implement the logic.

User can use OCSP to build his streaming platform easily and quickly.

### Features

 
| Feature        |   Detailed feature       |
| ------------- |:-------------:|
| Event processing       | Data reading      |
|               | Data pre-processing      |
|               | Label enhancement     |
|               | Cache        |
|               | Event subscription        |
|               | Event output      |
| Configuration | Input configuration  |
|               | Pre-processing configuration  |
|               | label configuration   |
|               | Processing  configuration |
|               | Subscription configuration |
|               | System  configuration  |
| Monitor       | Metrics view  |
|               | Status view  |
|               | Service Start/Stop  |

     

* Framework [Details](https://github.com/OCSP/OCSP_mainline/wiki/OCSP-Architecture)

   OCSP consists of four components: Data input, Core processing, Configuration, Basic components as below:

   ![OCSP架构图](http://ohpsj3ec3.bkt.clouddn.com/overview.png?imageView/2/w/500/q/100)
   
* UI
 
     * Stream status view
 
         ![前台页面展示1](http://ohpsj3ec3.bkt.clouddn.com/web1.png?imageView/2/w/500/q/100)

     * Stream configuration view
     
         ![前台页面展示2](http://ohpsj3ec3.bkt.clouddn.com/web2.png?imageView/2/w/500/q/100)

     * Stream creation view
  
         ![前台页面展示3](http://ohpsj3ec3.bkt.clouddn.com/web3.png?imageView/2/w/500/q/100)


### Build & Installation

* [Build](https://github.com/OCSP/OCSP_mainline/wiki/Compile-Source-Code)

* [Installation](https://github.com/OCSP/OCSP_mainline/wiki/OCSP-Setup)

* [Codis installation](https://github.com/CodisLabs/codis/blob/release3.2/doc/tutorial_en.md)

* [YARN label configuration](https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.4.2/bk_yarn_resource_mgt/content/configuring_node_labels.html)

### How to use

* [Guideline](https://github.com/OCSP/OCSP_mainline/wiki/2.1-\(Medivh\)-OCSP-User-Guide)
* [Label development](https://github.com/OCSP/OCSP_mainline/wiki/Customize-Label)
* [Performance optimization](https://github.com/OCSP/OCSP_mainline/wiki/Tuning-OCSP)

### Milestone

*   2016 Nov. [V2.0.0](https://github.com/OCSP/OCSP_mainline/releases/tag/2.0.0) 
*   2017 Jan 13. [V2.0.1](https://github.com/OCSP/OCSP_mainline/releases/tag/2.0.1)
*   2017 May 4.  [V2.1](https://github.com/OCSP/OCSP_mainline/releases/tag/2.1.0)

### Downloads
*   V2.0.1 RPM builds based on Spark1.6.x [v2.0.1](https://pan.baidu.com/s/1gfn6hzX)
*   V2.1 RPM builds based on Spark1.6.x [v2.1](https://pan.baidu.com/s/1pKH5qA3)

### FAQ
* [FAQ](https://github.com/OCSP/OCSP_mainline/wiki/FAQ)
