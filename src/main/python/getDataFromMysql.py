import pandas as pd
import pymysql
from sqlalchemy import create_engine
import warnings

warnings.filterwarnings('ignore')

START_DATE= "2023-6-1"
END_DATE= "2023-12-1"


class MySQLDataExtractor:
    """MySQL数据提取器"""

    def __init__(self, host='localhost', port=3306, user='root',
                 password='123456', database='oss_health'):
        """
        初始化数据库连接

        Args:
            host: 数据库主机地址
            port: 端口号
            user: 用户名
            password: 密码
            database: 数据库名
        """
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database
        self.engine = None
        self.connection = None

    def connect(self):
        """建立数据库连接"""
        try:
            # 方法1: 使用 SQLAlchemy (推荐，支持pandas直接读取)
            connection_string = (
                f"mysql+pymysql://{self.user}:{self.password}@"
                f"{self.host}:{self.port}/{self.database}?charset=utf8mb4"
            )
            self.engine = create_engine(connection_string)

            # 方法2: 使用 PyMySQL (备用)
            self.connection = pymysql.connect(
                host=self.host,
                port=self.port,
                user=self.user,
                password=self.password,
                database=self.database,
                charset='utf8mb4'
            )

            print(f"✅ 成功连接到数据库: {self.database}")
            return True

        except Exception as e:
            print(f"❌ 数据库连接失败: {e}")
            return False

    def list_tables(self):
        """列出数据库中的所有表"""
        try:
            query = "SHOW TABLES"
            tables = pd.read_sql(query, self.engine)
            return tables.iloc[:, 0].tolist()
        except Exception as e:
            print(f"❌ 获取表列表失败: {e}")
            return []

    def get_table_info(self, table_name):
        """获取表的结构信息"""
        try:
            query = f"DESCRIBE `{table_name}`"
            info = pd.read_sql(query, self.engine)
            print(f"\n📊 表 '{table_name}' 的结构:")
            print("=" * 50)
            print(info.to_string(index=False))
            print("=" * 50)

            # 获取行数
            count_query = f"SELECT COUNT(*) as count FROM `{table_name}`"
            count = pd.read_sql(count_query, self.engine)
            print(f"总行数: {count['count'][0]}")

            return info
        except Exception as e:
            print(f"❌ 获取表信息失败: {e}")
            return None

    def extract_table(self, table_name, limit=None, conditions=None):
        """
        提取表数据

        Args:
            table_name: 表名
            limit: 限制行数 (None表示全部)
            conditions: WHERE条件 (例如: "id > 100")

        Returns:
            DataFrame
        """
        try:
            query = f"SELECT * FROM `{table_name}`"

            if conditions:
                query += f" WHERE {conditions}"

            if limit:
                query += f" LIMIT {limit}"

            df = pd.read_sql(query, self.engine)

            return df

        except Exception as e:
            print(f"❌ 数据提取失败: {e}")
            return None

    def save_to_csv(self, df, filename, index=False):
        """
        保存DataFrame为CSV文件

        Args:
            df: DataFrame对象
            filename: 文件名
            index: 是否保存索引
        """
        try:
            df.to_csv(filename, index=index, encoding='utf-8-sig')
            print(f"✅ 数据已保存到: {filename}")
            print(f"   文件大小: {df.shape[0]} 行 × {df.shape[1]} 列")
            return True
        except Exception as e:
            print(f"❌ 保存CSV失败: {e}")
            return False

    def close(self):
        """关闭数据库连接"""
        if self.engine:
            self.engine.dispose()
        if self.connection:
            self.connection.close()
        print("\n✅ 数据库连接已关闭")

    def transform_and_filter_data(self, df, table_name, label_value,
                                  start_date="2023-06-01", end_date="2023-12-01"):
        """
        转换数据结构并按日期过滤 - 宽表格式

        Args:
            df: 原始DataFrame (列: time, id, number)
            table_name: 表名
            label_value: 该表对应的label值
            start_date: 开始日期 (格式: 'YYYY-MM-DD' 或 'YYYY-MM')
            end_date: 结束日期 (格式: 'YYYY-MM-DD' 或 'YYYY-MM')

        Returns:
            转换后的DataFrame，列名为: [full_name, time, 版本1, 版本2, ..., label]
        """
        try:
            # 1. 转换日期格式
            df['time'] = pd.to_datetime(df['time'])

            # 2. 日期过滤
            if start_date:
                start_date = pd.to_datetime(start_date)
                df = df[df['time'] >= start_date]

            if end_date:
                end_date = pd.to_datetime(end_date)
                df = df[df['time'] < end_date]

            # 3. 透视表转换：将id列的值转为列名
            pivot_df = df.pivot_table(
                index='time',
                columns='id',
                values='number',
                aggfunc='sum'  # 如果同一时间有多个相同id，则求和
            ).reset_index()

            # 4. 添加full_name列（使用表名）
            pivot_df.insert(0, 'full_name', table_name)

            # 5. 添加label列
            pivot_df['label'] = label_value

            # 6. 重命名time列
            pivot_df.rename(columns={'time': 'time'}, inplace=True)

            # 7. 按日期排序
            pivot_df = pivot_df.sort_values('time').reset_index(drop=True)

            # 8. 填充缺失值为0（如果某个日期某个版本没有数据）
            version_columns = [col for col in pivot_df.columns
                               if col not in ['full_name', 'time', 'label']]
            pivot_df[version_columns] = pivot_df[version_columns].fillna(0)

            return pivot_df

        except Exception as e:
            print(f"❌ 数据转换失败: {e}")
            import traceback
            traceback.print_exc()
            return None

    def extract_and_save_formatted(self, table_name, label_value,
                                   output_file=None,
                                   start_date="2023-06-01",
                                   end_date="2023-12-01"):
        """
        提取数据并按指定格式保存

        Args:
            table_name: 表名
            label_value: 该表的label值
            output_file: 输出文件名 (默认: {table_name}_formatted.csv)
            start_date: 开始日期 (例如: '2019-01-01' 或 '2019-01')
            end_date: 结束日期 (例如: '2020-12-31' 或 '2020-12')

        Returns:
            转换后的DataFrame
        """

        # 1. 提取原始数据
        df_raw = self.extract_table(table_name)

        if df_raw is None:
            return None

        # 2. 转换数据格式
        df_formatted = self.transform_and_filter_data(
            df_raw,
            table_name,
            label_value,
            start_date,
            end_date
        )

        if df_formatted is None:
            return None

        # 3. 保存为CSV
        if output_file is None:
            output_file = f"{table_name.replace('/', '_')}_formatted.csv"

        self.save_to_csv(df_formatted, output_file)

        return df_formatted

    def batch_extract_from_csv(self, csv_file, output_file='all_tables_combined.csv',
                               start_date="2023-06-01", end_date="2023-12-01"):
        """
        从CSV文件批量提取和转换数据，直接保存到一个文件

        Args:
            csv_file: 包含full_name和label列的CSV文件路径
            output_file: 输出文件名
            start_date: 开始日期
            end_date: 结束日期

        Returns:
            成功处理的表数量
        """
        import os

        try:
            # 1. 读取CSV文件
            config_df = pd.read_csv(csv_file)

            if 'full_name' not in config_df.columns or 'label' not in config_df.columns:
                print("❌ CSV文件必须包含 'full_name' 和 'label' 列")
                return 0

            # 2. 初始化统计
            total_tables = len(config_df)
            success_count = 0
            failed_tables = []
            all_data = []  # 用于合并所有数据

            print(f"\n{'=' * 60}")
            print(f"开始批量处理 {total_tables} 个表")
            print(f"{'=' * 60}\n")

            # 3. 逐个处理表
            for idx, row in config_df.iterrows():
                full_name = row['full_name']
                label_value = row['label']

                # 将 '/' 替换为 '_' 作为表名
                table_name = full_name.replace('/', '_')

                print(f"[{idx + 1}/{total_tables}] 处理表: {full_name}", end=' ... ')

                try:
                    # 提取原始数据
                    df_raw = self.extract_table(table_name)

                    if df_raw is None or df_raw.empty:
                        print(f"⚠️ 无数据")
                        failed_tables.append((full_name, "无数据或表不存在"))
                        continue

                    # 转换数据格式
                    df_formatted = self.transform_and_filter_data(
                        df_raw,
                        full_name,  # 使用原始的full_name（带'/'）
                        label_value,
                        start_date,
                        end_date
                    )

                    if df_formatted is None or df_formatted.empty:
                        print(f"⚠️ 转换失败")
                        failed_tables.append((full_name, "转换失败或过滤后无数据"))
                        continue

                    # 添加到合并列表
                    all_data.append(df_formatted)
                    success_count += 1
                    print(f"✅ 成功 ({df_formatted.shape[0]} 行)")

                except Exception as e:
                    print(f"❌ 失败: {e}")
                    failed_tables.append((full_name, str(e)))

            # 4. 合并并保存所有数据
            if all_data:
                print(f"\n{'=' * 60}")
                print("合并所有表的数据...")
                print('=' * 60)

                combined_df = pd.concat(all_data, ignore_index=True)
                self.save_to_csv(combined_df, output_file)

                print(f"✅ 合并完成: {combined_df.shape[0]} 行 × {combined_df.shape[1]} 列")
            else:
                print("\n⚠️ 没有成功处理的数据")

            # 5. 打印统计信息
            print(f"\n{'=' * 60}")
            print("批量处理完成")
            print('=' * 60)
            print(f"总表数: {total_tables}")
            print(f"成功: {success_count}")
            print(f"失败: {len(failed_tables)}")

            if failed_tables:
                print("\n失败的表:")
                for full_name, reason in failed_tables:
                    print(f"  - {full_name}: {reason}")

            return success_count

        except Exception as e:
            print(f"❌ 批量处理失败: {e}")
            import traceback
            traceback.print_exc()
            return 0


# ==================== 主程序 ====================

def main():
    extractor = MySQLDataExtractor(
        host='localhost',
        port=3306,
        user='root',
        password='123456',
        database='oss_health'
    )

    if not extractor.connect():
        return

    # 批量处理，直接保存到一个文件
    success_count = extractor.batch_extract_from_csv(
        csv_file='predictions.csv',           # 你的配置文件
        output_file='predictions_input.csv',  # 输出文件名
        start_date=START_DATE,
        end_date=END_DATE
    )

    print(f"\n✅ 批量处理完成，成功处理 {success_count} 个表")

    extractor.close()

# ==================== 批量提取多个表 ====================

def batch_extract_tables(table_list):
    """批量提取多个表"""

    extractor = MySQLDataExtractor(
        host='localhost',
        port=3306,
        user='root',
        password='123456',
        database='oss_health'
    )

    if not extractor.connect():
        return

    for table_name in table_list:
        print(f"\n{'=' * 60}")
        print(f"正在处理表: {table_name}")
        print('=' * 60)

        df = extractor.extract_table(table_name)

        if df is not None:
            output_file = f"{table_name.replace('/', '_')}_data.csv"
            extractor.save_to_csv(df, output_file)

    extractor.close()


# ==================== 高级查询示例 ====================

def advanced_query_example():
    """高级查询示例"""

    extractor = MySQLDataExtractor(
        host='localhost',
        port=3306,
        user='root',
        password='123456',
        database='oss_health'
    )

    if not extractor.connect():
        return

    # 自定义SQL查询
    custom_query = """
    SELECT 
        column1,
        column2,
        COUNT(*) as count
    FROM `10up_wp-local-docker-v2`
    WHERE column1 IS NOT NULL
    GROUP BY column1, column2
    ORDER BY count DESC
    LIMIT 100
    """

    try:
        df = pd.read_sql(custom_query, extractor.engine)
        print("✅ 自定义查询结果:")
        print(df.head())

        extractor.save_to_csv(df, "custom_query_result.csv")
    except Exception as e:
        print(f"❌ 查询失败: {e}")

    extractor.close()


if __name__ == "__main__":
    # 运行主程序
    main()

    # 如果需要批量提取多个表，取消下面的注释
    # batch_extract_tables([
    #     '10up_wp-local-docker-v2',
    #     'another_table_name',
    #     'yet_another_table'
    # ])

    # 如果需要执行自定义查询，取消下面的注释
    # advanced_query_example()
