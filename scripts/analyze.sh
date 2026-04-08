#!/usr/bin/env bash
#
# analyze.sh - Java Source Analyzer 快捷分析脚本
#
# 用法:
#   ./scripts/analyze.sh /path/to/java/project
#   ./scripts/analyze.sh /path/to/java/project /custom/output/dir
#   ./scripts/analyze.sh /path/to/java/project -v 3.2 -n "My Project"
#

set -euo pipefail

# 项目根目录（脚本所在目录的上级目录）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_FILE="$PROJECT_ROOT/target/glossary-java-source-analyzer-1.0.jar"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印信息函数
info()    { echo -e "${BLUE}[INFO]${NC}    $*"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}    $*"; }
error()   { echo -e "${RED}[ERROR]${NC}   $*"; }

# 打印使用说明
usage() {
    echo -e "${BLUE}Java Source Analyzer - 快捷分析脚本${NC}"
    echo ""
    echo -e "用法:"
    echo -e "  $0 <项目路径> [输出路径] [选项]"
    echo ""
    echo -e "参数:"
    echo -e "  项目路径        要分析的 Java 项目根目录（必填）"
    echo -e "  输出路径        分析结果输出目录（可选，默认: ./analysis-output）"
    echo -e ""
    echo -e "选项:"
    echo -e "  -v, --version <ver>     版本号（默认: 从 pom.xml 自动检测）"
    echo -e "  -n, --name <name>       项目名称（默认: 从 pom.xml 自动检测）"
    echo -e "  -p, --prefix <prefix>   内部包前缀（默认: java）"
    echo -e "  -h, --help              显示此帮助信息"
    echo ""
    echo -e "示例:"
    echo -e "  $0 ~/projects/spring-framework"
    echo -e "  $0 ~/projects/my-app /tmp/analysis"
    echo -e "  $0 ~/projects/my-app -v 2.0 -n 'My App'"
    exit 0
}

# 检查 JAR 文件是否存在
check_jar() {
    if [[ ! -f "$JAR_FILE" ]]; then
        error "JAR 文件不存在: $JAR_FILE"
        info "请先运行构建: mvn clean package -Dmaven.test.skip=true"
        info "或在项目根目录运行: ./scripts/build.sh"
        exit 1
    fi
}

# 检查项目路径
check_project() {
    local path="$1"
    if [[ ! -d "$path" ]]; then
        error "项目路径不存在: $path"
        exit 1
    fi

    # 检查是否是 Java 项目
    if [[ ! -f "$path/pom.xml" ]] && [[ ! -f "$path/build.gradle" ]] && [[ ! -d "$path/src" ]]; then
        warn "警告: 该目录看起来不像一个 Java 项目（没有 pom.xml / build.gradle / src 目录）"
        read -rp "继续分析？(y/N) " confirm
        if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
            exit 0
        fi
    fi
}

# 主函数
main() {
    # 解析参数
    local project_path=""
    local output_dir=""
    local version=""
    local artifact_name=""
    local pkg_prefix="java"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                usage
                ;;
            -v|--version)
                version="$2"
                shift 2
                ;;
            -n|--name)
                artifact_name="$2"
                shift 2
                ;;
            -p|--prefix)
                pkg_prefix="$2"
                shift 2
                ;;
            *)
                if [[ -z "$project_path" ]]; then
                    project_path="$1"
                elif [[ -z "$output_dir" ]]; then
                    output_dir="$1"
                else
                    error "未知参数: $1"
                    usage
                fi
                shift
                ;;
        esac
    done

    # 验证必填参数
    if [[ -z "$project_path" ]]; then
        error "请提供项目路径"
        usage
    fi

    # 默认输出目录
    if [[ -z "$output_dir" ]]; then
        output_dir="$PROJECT_ROOT/analysis-output"
    fi

    # 前置检查
    check_jar
    check_project "$project_path"

    # 创建输出目录
    mkdir -p "$output_dir"

    # 构建命令
    local cmd=(java -jar "$JAR_FILE" --sourceRoot "$project_path" --outputDir "$output_dir" --internalPkgPrefix "$pkg_prefix")

    if [[ -n "$version" ]]; then
        cmd+=(--version "$version")
    fi

    if [[ -n "$artifact_name" ]]; then
        cmd+=(--artifactName "$artifact_name")
    fi

    # 打印开始信息
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Java Source Analyzer - 分析引擎启动${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    info "项目路径:    $project_path"
    info "输出目录:    $output_dir"
    [[ -n "$version" ]]      && info "版本号:      $version"
    [[ -n "$artifact_name" ]] && info "项目名称:    $artifact_name"
    echo ""
    info "开始分析..."
    echo ""

    # 执行分析
    local start_time=$(date +%s)
    if "${cmd[@]}"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo ""
        success "分析完成！耗时: ${duration}s"
        echo ""
        info "输出文件:"
        find "$output_dir" -name "*.json" -type f | sort | while read -r file; do
            local size
            size=$(ls -lh "$file" | awk '{print $5}')
            echo -e "  ${GREEN}✓${NC} $file ($size)"
        done
        echo ""
        info "提示: 项目专属词汇表已保存至 $project_path/.universe/tech-glossary.json"
    else
        error "分析失败，请检查项目路径和参数"
        exit 1
    fi
}

main "$@"
