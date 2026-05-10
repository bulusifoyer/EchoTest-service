# EchoTest 本地工具链加载脚本（仅本仓库使用）
# 用法：source ./.echotest-env.sh
# 作用：把 ~/.echotest-toolchain 下的 JDK 8 + Maven + Node.js 临时注入当前 shell
# 退出当前 shell 后环境变量自动失效，不污染全局配置

export JAVA_HOME="$HOME/.echotest-toolchain/jdk8u412-b08/Contents/Home"
export M2_HOME="$HOME/.echotest-toolchain/apache-maven-3.9.15"
export NODE_HOME="$HOME/.echotest-toolchain/node-v20.18.1-darwin-x64"
export PATH="$JAVA_HOME/bin:$M2_HOME/bin:$NODE_HOME/bin:$PATH"

echo "[echotest] JAVA_HOME=$JAVA_HOME"
echo "[echotest] MAVEN_HOME=$M2_HOME"
echo "[echotest] NODE_HOME=$NODE_HOME"
java -version 2>&1 | head -1
mvn -v | head -1
echo "node $(node -v) / npm $(npm -v)"
