### 1. 概述：
Tomoko主要用于远程更新与之绑定的应用（简称目标应用，一般为IoT客户端）。Tomoko运行需要依赖远程服务器，远程服务器需要提供三个核心功能：发送推送，上传应用和监控更新。
Tomoko以应用的形式安装到Android系统中，核心功能通过Android服务来提供，图形界面仅用于Debug和功能设置。Tomoko需要与目标应用绑定，所有功能的作用对象都是目标应用。下图为系统架构图。
![image](http://w5e.me/update/tomoko-arch.svg)
### 2. 功能：
- **远程控制**：通过接收并解析后台发送远程推送消息来执行操作；包括应用更新，应用重启与系统重启。
- **本地进程保活**：周期性检测目标应用的运行状态，如果未运行则会启动目标应用。
- **本地系统重启**：通过接收Android广播来执行系统重启功能。

***前提条件：Android系统版本5.0+（API21+）且拥有ROOT权限***

### 3. 使用说明：
> #### 通用配置：
> > **绑定应用**：即设置绑定的应用包名，包名的默认值为common module中的Constants类的CLIENT_PACKAGE_NAME变量的值，可根据需要进行修改。  
> > 
> > **配置远程服务器**：远程服务器需要提供两个接口和一个上传APK的功能。一个接口是绑定推送设备（绑定后可以通过后台来发送远程推送），另一个接口是监控更新接口（在目标应用应用启动后调用，用于判断更新是否成功，判断方法是判断启动应用的版本号是否提升为最新的版本号）。远程服务本地调用由cloud module中的RemoteService实现了，根据后台服务器具体实现，修改为真实的URL（修改BindRequestProcessor和NotifyRequestProcessor类的getUrl方法的返回值）和选择性的修改参数命名即可。推送服务可以自行选择，实现Pushable接口即可。默认提供了腾讯移动推送实现，使用前需要修改gradle构建脚本中的XG_ACCESS_ID变量和XG_ACCESS_KEY变量的值为正确有值。  
> > 
> > **启动服务**：主服务会在开机时自动运行。第一次安装后需要手动启动应用，应用启动后会开启主服务，退出应用后主服务会继续运行。  
> #### 远程控制：
> > **应用更新**：推送消息格式为[下载地址]|[版本号]|[应用包名]，|为分隔符，比如： “https://bind.ai/update/ai_camera.apk|5|ai.bind.iot.client”。收到推送后会自动完成应用的更新并启动应用。 目标应用启动后如果需要通知后台更新成功，调用RemoteService实例的notifyAppStartup()方法。  
> > 
> > **应用重启**：推送消息格式为[指令ID]|[应用包名],|为分隔符，指令ID固定为1，比如：“1|ai.bind.iot.client”，Control接收到应用重启指令后会发送重启广播，广播的Action为Constants.INTENT_ACTION_RESTART变量的值，目标应用需要接收广播并自行处理重启逻辑。  
> > 
> > **系统重启**：推送消息格式为[指令ID]，指令ID固定为2。收到推送后会自动执行重启系统操作。
> #### 本地进程保活：
> > 目标应用需要增加对common module的依赖，并使用其中的ControlProcessAliveChecker类；具体方法为在应用启动后创建ControlProcessAliveChecker类的实例（该实例会自动处理保活逻辑）并在结束使用时调用实例的release()方法。进程保活适用于只有一个Activity的应用，推荐在Activity的onCreate()方法中创建实例，并在页面的onDestory()方法中调用实例的release()方法。
> > ```
> > @Override
> > protected void onCreate(Bundle savedInstanceState) {
> >     super.onCreate(savedInstanceState);
> >     processAliveChecker = new ControlProcessAliveChecker(this);
> > }
> >     
> > @Override
> > protected void onDestroy() {
> >     super.onDestroy();
> >     processAliveChecker.release();
> >     processAliveChecker = null;
> > }
> > ```
> #### 本地系统重启：
> > 需要重启系统时目标应用可以通过发送重启广播来实现。广播的Action为Constants.INTENT_ACTION_REBOOT变量的值。
> > ```
> > sendBroadcast(new Intent(Constants.INTENT_ACTION_REBOOT));
> > ```