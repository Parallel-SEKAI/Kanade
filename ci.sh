#!/bin/bash

# 使用 --parallel 开启并行执行
# 使用 --continue 即使前面的任务失败也尝试运行后续任务（可选）
# 使用 --console=plain 方便在 CI 或日志文件中阅读
./gradlew :app:lint \
          :app:assembleRelease \
          --parallel \
          --build-cache
