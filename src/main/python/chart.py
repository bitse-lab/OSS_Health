import mysql.connector
import pandas as pd
import matplotlib

matplotlib.use('TkAgg')  # 设置后端
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from datetime import datetime

# 配置中文字体（根据系统选择）
plt.rcParams['font.sans-serif'] = ['SimHei', 'Arial Unicode MS', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False


def fetch_data_from_db(db_config, versions):
    """从MySQL数据库获取指定版本的数据"""
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    all_data = {}
    for version in versions:
        query = """
        SELECT time, number 
        FROM openharmony_kernel_liteos 
        WHERE s1 = %s AND number IS NOT NULL 
        ORDER BY time ASC
        """
        cursor.execute(query, (version,))
        results = cursor.fetchall()

        if results:
            # 转换为DataFrame
            df = pd.DataFrame(results, columns=['time', 'number'])
            # 转换时间格式
            df['time'] = pd.to_datetime(df['time'])
            all_data[version] = df
            print(f"   ✓ Version {version}: {len(df)} 条记录")
        else:
            print(f"   ✗ Version {version}: 无数据")

    cursor.close()
    conn.close()
    return all_data


def plot_line_chart(data_dict, title="OpenHarmony Kernel LiteOS Versions Comparison"):
    """绘制折线图"""
    fig, ax = plt.subplots(figsize=(14, 7))

    # 设置淡蓝色背景
    fig.patch.set_facecolor('#E8F4F8')
    ax.set_facecolor('#F0F8FF')

    # 颜色方案
    colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']

    # 绘制每个版本的折线
    for idx, (version, df) in enumerate(data_dict.items()):
        color = colors[idx % len(colors)]
        ax.plot(df['time'], df['number'],
                marker='o',
                linewidth=2.5,
                markersize=6,
                label=f'Version {version}',
                color=color,
                alpha=0.8)

    # 设置标题和标签
    ax.set_title(title, fontsize=18, fontweight='bold', pad=20)
    ax.set_xlabel('Time (Year-Month)', fontsize=13, fontweight='bold')
    ax.set_ylabel('Metric Value', fontsize=13, fontweight='bold')

    # 格式化 X 轴日期
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m'))
    ax.xaxis.set_major_locator(mdates.MonthLocator(interval=2))
    plt.xticks(rotation=45, ha='right')

    # 添加网格
    ax.grid(True, linestyle='--', alpha=0.4, color='gray')

    # 添加图例
    ax.legend(loc='best', fontsize=11, framealpha=0.9, shadow=True)

    # 自动调整布局
    plt.tight_layout()

    return fig


def get_available_versions(db_config):
    """获取数据库中所有可用的版本号"""
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    query = "SELECT DISTINCT s1 FROM openharmony_kernel_liteos WHERE s1 IS NOT NULL ORDER BY s1"
    cursor.execute(query)
    versions = [row[0] for row in cursor.fetchall()]

    cursor.close()
    conn.close()
    return versions


def main():
    """主函数"""
    # ========== 数据库配置区域 ==========
    DB_CONFIG = {
        'host': 'localhost',
        'user': 'root',
        'password': '123456',
        'database': 'oss_health'
    }

    # 要对比的版本（可以修改这里）
    VERSIONS = ['2.3.2', '2.1.3']

    # 图片保存路径
    SAVE_PATH = 'kernel_liteos_chart.png'

    # 图表标题
    CHART_TITLE = "OpenHarmony Kernel LiteOS Versions Comparison"
    # ====================================

    print("=" * 60)
    print("📊 OpenHarmony Kernel LiteOS 数据可视化工具")
    print("=" * 60)

    # 显示所有可用版本
    print("\n🔍 正在查询数据库中的可用版本...")
    try:
        available_versions = get_available_versions(DB_CONFIG)
        if available_versions:
            print(f"✅ 找到 {len(available_versions)} 个版本:")
            for v in available_versions:
                print(f"   - {v}")
        else:
            print("❌ 数据库中没有找到任何版本数据")
            return
    except Exception as e:
        print(f"❌ 连接数据库失败: {e}")
        return

    # 获取指定版本的数据
    print(f"\n🔍 正在获取版本 {VERSIONS} 的数据...")
    try:
        data = fetch_data_from_db(DB_CONFIG, VERSIONS)
    except Exception as e:
        print(f"❌ 获取数据失败: {e}")
        return

    if not data:
        print("❌ 未找到任何数据，请检查版本号是否正确")
        print(f"💡 提示: 可用版本为 {available_versions}")
        return

    print(f"\n✅ 成功获取 {len(data)} 个版本的数据")

    # 显示数据统计
    print("\n📈 数据统计:")
    for version, df in data.items():
        print(f"   Version {version}:")
        print(f"      - 记录数: {len(df)}")
        print(f"      - 时间范围: {df['time'].min().date()} 至 {df['time'].max().date()}")
        print(f"      - 数值范围: {df['number'].min():.2f} 至 {df['number'].max():.2f}")

    # 生成图表
    print("\n📊 正在生成图表...")
    try:
        fig = plot_line_chart(data, title=CHART_TITLE)

        # 保存图片
        fig.savefig(SAVE_PATH, dpi=300, bbox_inches='tight', facecolor='#E8F4F8')
        print(f"💾 图表已保存到: {SAVE_PATH}")

        # 显示图表
        print("🖼️  正在显示图表窗口...")
        plt.show()

        print("\n✅ 完成！")

    except Exception as e:
        print(f"❌ 生成图表失败: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
