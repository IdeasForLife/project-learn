### 前言
随着产品业务的发展，产品和用户量不断提升，由于之前每个产品对应一套接口，使得接口数目迅速膨胀，维护的成本也日渐变高。好的接口设计，不管从代码复用还是代码健壮性来看都尤为必要，从复杂的产品中抽象出一套公用接口也变得势在必行。

### 设计之初
首先，研究了大话设计模式这本书，统缆设计模式之后，从产品设计角度去分析，采用简单工厂尤为简便，但是具体的实现细节会被类`case when`进行判断，故此采用工厂方法实现，让子类决定去实例化哪个类，在`spring`容器中直接取相应对象。本次设计
用到的设计模式有工厂模式+命令模式。

### 开始实现
1. 定义接口基类`ProductInterface`，此类主要包含大部分产品所需要的接口服务，用作基本实现
2. 定义抽象类`AbstractProductInterface`实现`ProductInterface`，在抽象类中，可以提供短信发送等类似的基础服务
3. 定义默认实现`DefaultProduct`和扩展实现`AppProduct`，根据不同的产品代码和渠道来源，从容器中选择对应的产品实现
4. 采用命令模式实现服务调用与实际服务实现接口的隔离，实现解耦的目的，通过定义接口`LoanCommand`和具体的服务实现类`AppCreditApplyCommand`等实现

### 扩展实现
1. 定义扩展接口`ExtendProductInterface`，此类主要继承接口基类`ProductInterface`，并新增特殊接口服务，用作扩展实现
2. 定义抽象类`AbstractExtendProductInterface`实现`ExtendProductInterface`，在抽象类中，可以提供短信发送等类似的基础服务
3. 定义默认实现`DefaultExtendProduct`，根据不同的产品代码和渠道来源，从容器中选择对应的产品实现
4. 采用命令模式实现服务调用与实际服务实现接口的隔离，实现解耦的目的，通过定义接口`LoanCommand`和具体的服务实现类`ExtendOpenPersonAccountCommand`等实现

### 设计思考
通过本次设计，达到了新增服务，只需要新增产品即可实现功能的目的，但是在函数式设计高速发展的今天，这种每次新增产品服务就新增产品实现类的做法还是不优雅，其中还是会通过`DefaultProduct`或者`DefaultExtendProduct`产生依赖关系,有一定的耦合性，会有许多`xxxProduct.java`的创建，而且提供的好多是相同的功能，比如`授信申请`、`放款申请`等，随着产品数量的急剧增长，由最初的两三个膨胀到十几个甚至有超过二十的趋势，思考再三，决定再次优化接口设计。

### 函数式接口设计
从名称上来看很高大上，但实际没有什么高大上的东西，主要实现是在`Java8+`的基础执行的，函数式编程的实现，是对`Lambda`表达式的深入理解和使用，让功能的实现比较易读和简洁，让调用与被调用方和的耦合性降低

### 开始设计
1. 定义函数式接口服务类`FunctionProduct`,主要方法就是`ProductInterface`接口中服务的代理实现，是实现函数式方法调用具体实现类，通过委派模式，实现直接达到服务调用的目的，从而实现松耦合
2. 定义具体的服务实现，授信申请`AppCreditApplyFunction`和`AppCreditLimitSelectFunction`即为具体的服务实现类
3. 服务扩展，从贷款业务的发展来看，新接口新服务的出现必不可少，但是由于在此我们实现了松耦合，在基础接口`ProductInterface`中新增公共服务`doServicexxx`描述，即可在不同的产品中服务中，动态的进行具体的`doServicexxx`服务实现。如果是一批全新的服务可以通过扩展服务接口
比如`ExtendProductInterface`实现，在不同的产品对应的服务进行实现
4. 注意点：由于使用函数式松耦合的实现，所以产品划分，产品对应服务名的划分，做到统一规划，就显得尤为重要，规划方式：
     - 以`产品名+product`为产品包的命名
     - 以`产品名+接口中对应的服务名+Command`，作为具体的服务实现类命名
     - 以`LoanCommand`接口的`execute`方法作为具体服务的实现，方便以后对服务调用做埋点和统一拦截处理
5. 接口存在的目的是为了与具体产品的具体服务实现做呼应，利于代码阅读和扩展，方便问题的定位 

### 设计优化
由于上述设计在功能上已经实现了从接口抽象化到函数式合计的转变，但是由于对最终服务实现的`beanName`的选取写死，导致了硬编码问题的存在，为了使代码可读性、灵活性增强，设计了从接口参数中获取对应`beanName`的功能，具体实现思路是什么，
具体有哪些实现，大概有以下几种：
#### 实现思路:
 ```text
    App 产品相关服务 BeanName 维护
    维护方式： 交易码 + productCode + channelId = 对应产品对应的服务名
    实现意义： 为了实现不同产品不同渠道来源对于不同服务的精确定位
    当前产品 1000 -> App  01-> 内部
    交易码描述，不同产品，交易码含义一样，可扩展交易码
    例如： ln1001 -> appCreditApply (授信申请)
    例如： ln1002 -> appCreditLimitSelect (额度查询)
```
#### 实现方式
- 第一种：存储在数据库中
    - 方案一：直接从数据库中读
    - 方案二：存在数据库中，在`spring`容器启动时加载到内存当中
- 第二种：存在内存中
    - 方案一：在`java`文件中类似于`Map<key,value>`存储
    - 方案二：在`properties`配置文件中`key=value`方式存储
    - 方案三：如果项目自身集成`携程 Apollo`，可以实现线上动态路由开关、相关服务的选取

### `beanName`的获取
定义属性加载类`BeanNameUtils`，在容器启动时从对应的位置加载配置信息，在具体的交易到达后，根据请求参数信息，动态的
获取对应的`beanName`，可以实现具体服务选取的功能。
- 优点是：
    - 1.直接在内存中操作，效率较高
    - 2.在配置文件中描述`beanName`无冗余代码，可读性强，统一维护，不容易出错
    - 3.如果产品过多，可以按产品性质，拆分、组合、新增或者是删除配置文件，扩展性强
### 代码实现
```tex
由于本文代码量比较大，请关注博主GitHub，获取完整代码
```
### 代码目录总览
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190513103028223.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3NoYW5nX3hz,size_16,color_FFFFFF,t_70)
### 完整代码和相关依赖请见GitHub

https://github.com/dwyanewede/project-learn/tree/master/src/main/java/com/learn/demo/interfaces/loan/product

### 博客推荐
https://blog.csdn.net/shang_xs 

### 公众号推荐
![在这里插入图片描述](https://img-blog.csdnimg.cn/20190429165130430.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3NoYW5nX3hz,size_16,color_FFFFFF,t_70)