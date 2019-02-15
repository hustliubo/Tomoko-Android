# Tomoko
Tomoko 主要用于远程更新与之绑定的应用（简称目标应用，一般为 IoT 客户端）。  

- Tomoko 以应用的形式安装到 Android 系统中，核心功能通过 Android 服务来提供，图形界面仅用于 Debug 和功能设置。
- Tomoko 运行需要依赖远程服务器。服务器需要提供三个功能：1.发送推送，2.上传与下载应用，3.监控更新状态。  
- Tomoko 需要与目标应用绑定，所有功能的作用对象都是目标应用。   

> 系统架构图：

![image](http://w5e.me/update/tomoko-arch.svg)
## 功能：
- **远程控制**：通过接收并解析后台发送远程推送消息来执行操作；包括应用更新，应用重启与系统重启。
- **本地进程保活**：周期性检测目标应用的运行状态，如果未运行则会启动目标应用。
- **本地系统重启**：通过接收 Android 广播来执行系统重启功能。

***前提条件：Android 系统版本5.0+（API21+）且拥有 ROOT 权限***

## 使用说明：
### 1. 通用配置：
- #### 绑定应用：
    即设置绑定的应用包名，包名的默认值为 common module 中的 Constants 类的 CLIENT_PACKAGE_NAME  变量的值，可根据需要进行修改。  
- #### 配置远程服务器：
    **远程服务器需要提供两个接口：**
    - 绑定推送设备接口：绑定后可以通过后台来发送远程推送；  
    - 监控更新状态接口：在目标应用应用启动后调用，用于判断更新是否成功（判断启动应用的版本号是否提升为最新的版本号）。

    **远程服务器需要提供两个功能：**
    - 上传和下载 APK 功能：由一个 web 页面提供操作界面，上传后能够显示下载链接和应用相关信息。  
    - 发送远程推送功能：提供一个 web 页面执行向指定设备发送远程推送的功能。  
    
    *提醒：*
    > 远程服务接口调用已经由 cloud module 中的 RemoteService 类实现了。可以根据后台服务器具体实现，修改为真实的URL（修改 BindRequestProcessor 类和 NotifyRequestProcessor 类的 getUrl() 方法的返回值）和选择性地修改参数命名即可。 

    > 推送服务可以自行选择，实现 Pushable 接口即可。  
    Tomoko 默认提供了腾讯移动推送实现（由 xinge module 提供，使用前需要修改 gradle 构建脚本中 的 XG_ACCESS_ID 变量和 XG_ACCESS_KEY 变量的值为正确的值）。  
 
- #### 启动服务：
    主服务会在开机时自动运行。第一次安装后需要手动启动应用，应用启动后会开启主服务，退出应用后主服务会继续运行。  
### 2. 远程控制：
通过发送指定格式的远程推送消息来执行相应操作。
- #### 应用更新：
    推送消息格式为 [下载地址]|[版本号]|[应用包名]，| 为分隔符
    > 示例：

    ```
    https://bind.ai/update/ai_camera.apk|5|ai.bind.iot.client
    ```  
    收到推送后会自动完成应用的更新并启动应用。 目标应用启动后如果需要通知后台更新成功（即调用 RemoteService 实例的 notifyAppStartup() 方法）。  

- #### 应用重启：
    推送消息格式为 [指令]|[应用包名]，| 为分隔符，指令值固定为 1
    > 示例：

    ```
    1|ai.bind.iot.client
    ```

    Tomoko 接收到应用重启指令后会发送重启广播，广播的 Action 为 Constants.INTENT_ACTION_RESTART 变量的值，目标应用需要接收广播并自行处理重启逻辑。  
- #### 系统重启：
    推送消息格式为[指令]，指令值固定为 2 。  
    收到推送后会自动执行重启系统操作。
### 3. 本地进程保活：
目标应用需要增加对 common module 的依赖，并使用其中的 ControlProcessAliveChecker 类。  
具体方法为在应用启动后创建 ControlProcessAliveChecker 类的实例（该实例会自动处理保活逻辑）并在结束使用时调用实例的 release() 方法。  
> 进程保活适用于只有一个 Activity 的应用，推荐在 Activity 的 onCreate() 方法中创建实例，并在 Activity 的  onDestory() 方法中调用实例的 release() 方法。  

> 示例代码：
```
 @Override
 protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     processAliveChecker = new ControlProcessAliveChecker(this);
 }
     
 @Override
 protected void onDestroy() {
     super.onDestroy();
     processAliveChecker.release();
     processAliveChecker = null;
 }
 ```
### 4. 本地系统重启：
需要重启系统时目标应用可以通过发送重启广播来实现。广播的 Action 为 Constants.INTENT_ACTION_REBOOT 变量的值。
> 示例代码：
```
 sendBroadcast(new Intent(Constants.INTENT_ACTION_REBOOT));
```
