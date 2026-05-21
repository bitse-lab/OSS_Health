import re
from pathlib import Path
import random
from datetime import datetime
from git import Repo
from collections import defaultdict
import statistics
import json
import math
import csv

START_DATE= "2023-6-01"
END_DATE= "2023-12-01"


class RepoInfo:
    """仓库信息类 - 简化版"""

    def __init__(self, owner, name, label=0):
        """
        初始化仓库信息

        Args:
            owner: 仓库所有者
            name: 仓库名称
            label: 活跃度标签 (0或1)
        """
        self.owner = owner
        self.name = name
        self.label = label
        self.metrics = []  # 保存其他指标的列表

    def __repr__(self):
        return f"RepoInfo(owner={self.owner}, name={self.name}, label={self.label})"

    def add_metric(self, metric):
        """添加指标"""
        self.metrics.append(metric)

    def get_full_name(self):
        """获取完整仓库名"""
        return f"{self.owner}/{self.name}" if self.owner else self.name


def extract_success_repos(log_dir="F://github_api_repos", prefix="github_api_"):
    """提取所有成功处理的仓库（去重）"""
    repos = set()
    pattern = re.compile(r'成功处理仓库:\s*([\w\-\.]+/[\w\-\.]+)')
    log_files = sorted(Path(log_dir).glob(f"{prefix}*.log"))

    for log_file in log_files:
        with open(log_file, 'r', encoding='utf-8') as f:
            for line in f:
                if '成功处理仓库' in line:
                    match = pattern.search(line)
                    if match:
                        repos.add(match.group(1))

    return sorted(list(repos))


def filter_repos_by_file_count(repos, base_path="F:/github_api_repos", min_files=5):
    """
    过滤掉文件夹中文件数量少于指定数量的仓库

    Args:
        repos: 仓库名称列表 (owner/repo 格式)
        base_path: 本地仓库根目录
        min_files: 最小文件数量阈值

    Returns:
        list: 过滤后的仓库名称列表
    """
    filtered_repos = []
    removed_count = 0

    print(f"\n开始过滤仓库（文件夹中至少需要 {min_files} 个文件）...")

    for i, full_name in enumerate(repos, 1):
        # 提取仓库名
        if '/' in full_name:
            _, name = full_name.split('/', 1)
        else:
            name = full_name

        repo_path = Path(base_path) / name

        # 检查路径是否存在
        if not repo_path.exists():
            print(f"[{i}/{len(repos)}] ⚠ 跳过（路径不存在）: {name}")
            removed_count += 1
            continue

        # 检查是否是目录
        if not repo_path.is_dir():
            print(f"[{i}/{len(repos)}] ⚠ 跳过（不是目录）: {name}")
            removed_count += 1
            continue

        try:
            # 统计文件夹中的文件数量（不包括子目录）
            file_count = sum(1 for item in repo_path.iterdir() if item.is_file())

            if file_count >= min_files:
                filtered_repos.append(full_name)
                # print(f"[{i}/{len(repos)}] ✓ 保留: {name} (文件数: {file_count})")
            else:
                removed_count += 1
                # print(f"[{i}/{len(repos)}] ✗ 移除: {name} (文件数: {file_count})")

        except Exception as e:
            print(f"[{i}/{len(repos)}] ✗ 处理出错: {name} - {e}")
            removed_count += 1
            continue

    print(f"\n{'=' * 60}")
    print(f"过滤完成:")
    print(f"  原始仓库数: {len(repos)}")
    print(f"  保留仓库数: {len(filtered_repos)}")
    print(f"  移除仓库数: {removed_count}")
    print(f"  保留率: {len(filtered_repos) / len(repos) * 100:.1f}%")
    print(f"{'=' * 60}\n")

    return filtered_repos


def get_random_repos(repos, count=1000, seed=None):
    """从仓库列表中随机获取指定数量的仓库"""
    if seed is not None:
        random.seed(seed)

    if len(repos) <= count:
        print(f"⚠ 总仓库数({len(repos)})少于请求数量({count})，返回全部仓库")
        return repos

    selected = random.sample(repos, count)
    return sorted(selected)


def create_repo_objects(full_repo_names):
    """
    创建RepoInfo对象列表

    Args:
        full_repo_names: owner/repo 格式的仓库列表

    Returns:
        list[RepoInfo]: RepoInfo对象列表
    """
    repo_objects = []

    for full_name in full_repo_names:
        if '/' in full_name:
            owner, name = full_name.split('/', 1)
        else:
            owner = None
            name = full_name

        repo_obj = RepoInfo(owner, name)
        repo_objects.append(repo_obj)

    return repo_objects


def calculate_repo_label(repo_info, base_path="F:/github_repos",
                         start_date="2023-12-01", end_date="2025-11-30"):
    """
    计算单个仓库的活跃度标签

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 1表示活跃，0表示不活跃
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        repo_info.label = 0
        return 0

    try:
        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        monthly_count = defaultdict(int)

        # 遍历所有分支的commits
        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    if start <= commit_date <= end:
                        year_month = commit_date.strftime("%Y-%m")
                        monthly_count[year_month] += 1
            except Exception as e:
                print(f"  ⚠ 处理分支 {branch.name} 时出错: {e}")
                continue

        # 计算中位数
        if monthly_count:
            commit_counts = list(monthly_count.values())
            median_commits = statistics.median(commit_counts)

            # 设置标签：中位数 >= 1 为活跃
            repo_info.label = 1 if median_commits >= 1 else 0

            # print(f"✓ {repo_info.name}: 月度中位数={median_commits:.1f}, 标签={repo_info.label}")
        else:
            repo_info.label = 0

        return repo_info.label

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        repo_info.label = 0
        return 0


def batch_calculate_labels(repo_objects, base_path="F:/github_repos",
                           start_date="2023-12-01", end_date="2025-11-30"):
    """
    批量计算所有仓库的活跃度标签

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        tuple: (活跃仓库数, 不活跃仓库数)
    """
    print(f"\n开始分析 {len(repo_objects)} 个仓库的标签...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    active_count = 0
    inactive_count = 0

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        label = calculate_repo_label(repo, base_path, start_date, end_date)

        if label == 1:
            active_count += 1
        else:
            inactive_count += 1

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  活跃仓库 (标签=1): {active_count}")
    print(f"  不活跃仓库 (标签=0): {inactive_count}")
    print(f"  活跃率: {active_count / len(repo_objects) * 100:.1f}%")
    print(f"{'=' * 60}\n")

    return active_count, inactive_count


def get_labels(repo_objects):
    """
    获取所有仓库的标签列表

    Args:
        repo_objects: RepoInfo对象列表

    Returns:
        list: 标签列表
    """
    return [repo.label for repo in repo_objects]


def save_results(repo_objects, output_file="repo_labels.txt"):
    """
    保存仓库名和标签

    Args:
        repo_objects: RepoInfo对象列表
        output_file: 输出文件名
    """
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("仓库名称\t标签\n")
        for repo in repo_objects:
            f.write(f"{repo.name}\t{repo.label}\n")

    print(f"✓ 结果已保存到: {output_file}")


def save_detailed_results(repo_objects, output_file="repo_analysis_results.txt"):
    """
    保存详细分析结果

    Args:
        repo_objects: RepoInfo对象列表
        output_file: 输出文件名
    """
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("仓库名称\t标签\t指标\n")
        f.write("=" * 80 + "\n")

        for repo in repo_objects:
            metrics_str = ','.join(str(m) for m in repo.metrics)
            f.write(f"{repo.name}\t{repo.label}\t{metrics_str}\n")

        active = sum(1 for r in repo_objects if r.label == 1)
        f.write("\n" + "=" * 80 + "\n")
        f.write(f"总计: {len(repo_objects)} 个仓库\n")
        f.write(f"活跃: {active} 个 ({active / len(repo_objects) * 100:.1f}%)\n")
        f.write(f"不活跃: {len(repo_objects) - active} 个\n")

    print(f"✓ 详细结果已保存到: {output_file}")


def filter_repos_by_first_commit(repo_names, base_path="F:/github_repos",
                                 cutoff_date="2023-09-01"):
    """
    过滤掉第一个commit在指定日期之后的仓库

    Args:
        repo_names: 仓库名称列表 (owner/repo 格式)
        base_path: 本地仓库根目录
        cutoff_date: 截止日期 (YYYY-MM-DD)，第一个commit必须在此日期之前

    Returns:
        list: 过滤后的仓库名称列表
    """
    cutoff = datetime.strptime(cutoff_date, "%Y-%m-%d")
    filtered_repos = []
    removed_count = 0

    print(f"\n开始过滤仓库（第一个commit必须在 {cutoff_date} 之前）...")

    for i, full_name in enumerate(repo_names, 1):
        # 提取仓库名
        if '/' in full_name:
            _, name = full_name.split('/', 1)
        else:
            name = full_name

        local_path = Path(base_path) / name

        # 检查路径是否存在
        if not local_path.exists():
            print(f"[{i}/{len(repo_names)}] ⚠ 跳过（路径不存在）: {name}")
            removed_count += 1
            continue

        try:
            repo = Repo(str(local_path))

            # 获取最早的commit（第一个commit）
            first_commit = None
            first_commit_date = None

            # 遍历所有分支找到最早的commit
            for branch in repo.branches:
                try:
                    commits = list(repo.iter_commits(branch.name))
                    if commits:
                        # 最后一个commit是最早的
                        branch_first_commit = commits[-1]
                        branch_first_date = datetime.fromtimestamp(branch_first_commit.committed_date)

                        if first_commit_date is None or branch_first_date < first_commit_date:
                            first_commit = branch_first_commit
                            first_commit_date = branch_first_date
                except Exception as e:
                    continue

            if first_commit_date is None:
                print(f"[{i}/{len(repo_names)}] ⚠ 跳过（无法获取commit）: {name}")
                removed_count += 1
                continue

            # 判断第一个commit是否在截止日期之前
            if first_commit_date < cutoff:
                filtered_repos.append(full_name)
                # print(f"[{i}/{len(repo_names)}] ✓ 保留: {name} (首次commit: {first_commit_date.strftime('%Y-%m-%d')})")
            else:
                removed_count += 1
                # print(f"[{i}/{len(repo_names)}] ✗ 移除: {name} (首次commit: {first_commit_date.strftime('%Y-%m-%d')})")

        except Exception as e:
            print(f"[{i}/{len(repo_names)}] ✗ 处理出错: {name} - {e}")
            removed_count += 1
            continue

    print(f"\n{'=' * 60}")
    print(f"过滤完成:")
    print(f"  原始仓库数: {len(repo_names)}")
    print(f"  保留仓库数: {len(filtered_repos)}")
    print(f"  移除仓库数: {removed_count}")
    print(f"  保留率: {len(filtered_repos) / len(repo_names) * 100:.1f}%")
    print(f"{'=' * 60}\n")

    return filtered_repos


def calculate_code_changes(repo_info, base_path="F:/github_repos",
                           start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的代码更改行数（增加+删除）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 总代码更改行数
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0

    try:
        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        total_changes = 0
        processed_commits = set()  # 避免重复计算

        # 遍历所有分支
        for branch in repo.branches:
            try:
                commits = list(repo.iter_commits(branch.name))

                # 遍历相邻的commit对
                for i in range(len(commits) - 1):
                    current_commit = commits[i]
                    parent_commit = commits[i + 1]

                    # 避免重复计算同一个commit
                    if current_commit.hexsha in processed_commits:
                        continue

                    commit_date = datetime.fromtimestamp(current_commit.committed_date)

                    # 检查是否在时间范围内
                    if start <= commit_date <= end:
                        # 计算这次commit的代码变更行数
                        try:
                            diffs = parent_commit.diff(current_commit, create_patch=True)

                            for diff in diffs:
                                if diff.diff:
                                    diff_text = diff.diff.decode('utf-8', errors='ignore')
                                    # 统计增加和删除的行数
                                    insertions = diff_text.count('\n+') - diff_text.count('\n+++')
                                    deletions = diff_text.count('\n-') - diff_text.count('\n---')
                                    total_changes += (insertions + deletions)

                        except Exception as e:
                            # 某些commit可能无法diff，跳过
                            continue

                        processed_commits.add(current_commit.hexsha)

            except Exception as e:
                print(f"  ⚠ 处理分支 {branch.name} 时出错: {e}")
                continue

        return total_changes

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0


def batch_calculate_code_changes(repo_objects, base_path="F:/github_repos",
                                 start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的代码更改行数

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: code_changes}
    """
    print(f"\n开始计算代码更改行数...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        changes = calculate_code_changes(repo, base_path, start_date, end_date)
        results[repo.name] = changes
        # print(f"  代码更改行数: {changes:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(changes)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均代码更改行数: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大代码更改行数: {max(results.values()):,}")
    print(f"  最小代码更改行数: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_issue_count(repo_info, base_path="F:/github_api_repos",
                          start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的issue数量

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 总issue数量
    """
    # 构建IssueData.json文件路径
    issue_file = Path(base_path) / repo_info.name / "IssueData.json"

    if not issue_file.exists():
        print(f"⚠ Issue文件不存在: {issue_file}")
        return 0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取JSON文件
        with open(issue_file, 'r', encoding='utf-8') as f:
            issues = json.load(f)

        issue_count = 0

        # 遍历所有issue
        for issue in issues:
            # 尝试获取created_at字段（兼容两种命名方式）
            created_at_str = issue.get('created_at') or issue.get('createdAt')

            if not created_at_str:
                continue

            # 解析时间（支持ISO 8601格式）
            try:
                # 处理带时区的ISO 8601格式
                if 'T' in created_at_str:
                    # 移除时区信息进行简单解析
                    created_at_str = created_at_str.split('T')[0]

                created_date = datetime.strptime(created_at_str, "%Y-%m-%d")

                # 检查是否在时间范围内
                if start <= created_date <= end:
                    issue_count += 1

            except ValueError as e:
                # 如果日期格式不匹配，尝试完整的ISO格式
                try:
                    from dateutil import parser
                    created_date = parser.parse(created_at_str).replace(tzinfo=None)
                    if start <= created_date <= end:
                        issue_count += 1
                except:
                    continue

        return issue_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的Issue数据时出错: {e}")
        return 0


def batch_calculate_issue_count(repo_objects, base_path="F:/github_api_repos",
                                start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的issue数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: issue_count}
    """
    print(f"\n开始计算Issue数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        issue_count = calculate_issue_count(repo, base_path, start_date, end_date)
        results[repo.name] = issue_count
        # print(f"  Issue数量: {issue_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(issue_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"{'=' * 60}\n")

    return results


def calculate_commit_count(repo_info, base_path="F:/github_repos",
                           start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的commit数量

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 总commit数量
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0

    try:
        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        commit_count = 0
        processed_commits = set()  # 避免重复计算

        # 遍历所有分支
        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    # 避免重复计算同一个commit
                    if commit.hexsha in processed_commits:
                        continue

                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    # 检查是否在时间范围内
                    if start <= commit_date <= end:
                        commit_count += 1
                        processed_commits.add(commit.hexsha)

            except Exception as e:
                print(f"  ⚠ 处理分支 {branch.name} 时出错: {e}")
                continue

        return commit_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0


def batch_calculate_commit_count(repo_objects, base_path="F:/github_repos",
                                 start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的commit数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: commit_count}
    """
    print(f"\n开始计算Commit数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        commit_count = calculate_commit_count(repo, base_path, start_date, end_date)
        results[repo.name] = commit_count
        # print(f"  Commit数量: {commit_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(commit_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均Commit数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大Commit数量: {max(results.values()):,}")
    print(f"  最小Commit数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_pr_count(repo_info, base_path="F:/github_api_repos",
                       start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的PR数量

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 总PR数量
    """
    # 构建PRData.json文件路径
    pr_file = Path(base_path) / repo_info.name / "PRData.json"

    if not pr_file.exists():
        print(f"⚠ PR文件不存在: {pr_file}")
        return 0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取JSON文件
        with open(pr_file, 'r', encoding='utf-8') as f:
            prs = json.load(f)

        pr_count = 0

        # 遍历所有PR
        for pr in prs:
            # 获取created_at字段
            created_at_str = pr.get('created_at')

            if not created_at_str:
                continue

            # 解析时间（支持ISO 8601格式）
            try:
                # 处理带时区的ISO 8601格式
                if 'T' in created_at_str:
                    # 移除时区信息进行简单解析
                    created_at_str = created_at_str.split('T')[0]

                created_date = datetime.strptime(created_at_str, "%Y-%m-%d")

                # 检查是否在时间范围内
                if start <= created_date <= end:
                    pr_count += 1

            except ValueError:
                # 如果日期格式不匹配，尝试完整的ISO格式
                try:
                    from dateutil import parser
                    created_date = parser.parse(created_at_str).replace(tzinfo=None)
                    if start <= created_date <= end:
                        pr_count += 1
                except:
                    continue

        return pr_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的PR数据时出错: {e}")
        return 0


def batch_calculate_pr_count(repo_objects, base_path="F:/github_api_repos",
                             start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的PR数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: pr_count}
    """
    print(f"\n开始计算PR数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        pr_count = calculate_pr_count(repo, base_path, start_date, end_date)
        results[repo.name] = pr_count
        # print(f"  PR数量: {pr_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(pr_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均PR数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大PR数量: {max(results.values()):,}")
    print(f"  最小PR数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_org_commits(repo_info, base_path="F:/github_repos",
                          start_date="2023-09-01", end_date="2023-12-31",
                          common_domains_file="free_email_provider_domains.txt"):
    """
    计算指定时间范围内的组织commits数量

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        int: 组织commits数量
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0

    try:
        # 加载公共邮箱域名
        common_domains = load_common_email_domains(common_domains_file)

        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 用于存储committer和其所有邮箱的映射
        user_email = defaultdict(list)
        # 用于存储邮箱和对应committer的映射
        email_user = defaultdict(list)
        # 存储所有commit信息
        all_commits = []

        processed_commits = set()

        # 遍历所有分支收集commit信息
        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    if commit.hexsha in processed_commits:
                        continue

                    processed_commits.add(commit.hexsha)

                    committer_name = commit.author.name
                    email = commit.author.email.lower()
                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    # 保存commit信息
                    all_commits.append({
                        'id': commit.hexsha,
                        'date': commit_date,
                        'committer': committer_name,
                        'email': email
                    })

                    # 建立映射关系
                    if email and '@' in email:
                        user_email[committer_name].append(email)
                        email_user[email].append(committer_name)

            except Exception as e:
                continue

        # 找出所有拥有非公共邮箱的人员（属于组织的用户）
        org_users = set()
        for committer, emails in user_email.items():
            # 检查是否有非公共邮箱
            has_private_email = any(
                not is_common_email(email, common_domains)
                for email in emails
            )
            if has_private_email:
                org_users.add(committer)

        # 找出属于组织的邮箱
        org_emails = set()
        for email, committers in email_user.items():
            # 如果邮箱的某个用户在org_users中，该邮箱属于组织
            if any(committer in org_users for committer in committers):
                org_emails.add(email)

        # 统计时间范围内的组织commits数量
        org_commit_count = 0
        for commit_info in all_commits:
            commit_date = commit_info['date']

            # 检查是否在时间范围内
            if start <= commit_date <= end:
                # 判断是否属于组织
                is_org_commit = (
                        commit_info['committer'] in org_users or
                        commit_info['email'] in org_emails
                )

                if is_org_commit:
                    org_commit_count += 1

        return org_commit_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0


def load_common_email_domains(file_path="free_email_provider_domains.txt"):
    """
    从文件加载公共邮箱域名

    Args:
        file_path: 域名文件路径

    Returns:
        set: 公共邮箱域名集合
    """
    common_domains = set()

    if not Path(file_path).exists():
        print(f"⚠ 公共邮箱域名文件不存在: {file_path}")
        return common_domains

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                domain = line.strip()
                if domain:
                    common_domains.add(domain.lower())
    except Exception as e:
        print(f"✗ 读取域名文件出错: {e}")

    return common_domains


def is_common_email(email, common_domains):
    """
    检查邮箱是否是公共邮箱

    Args:
        email: 邮箱地址
        common_domains: 公共邮箱域名集合

    Returns:
        bool: 是否为公共邮箱
    """
    if '@' not in email:
        return False

    domain = email.split('@')[1].lower()
    return domain in common_domains


def batch_calculate_org_commits(repo_objects, base_path="F:/github_repos",
                                start_date="2023-09-01", end_date="2023-12-31",
                                common_domains_file="free_email_provider_domains.txt"):
    """
    批量计算所有仓库的组织commits数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        dict: {repo_name: org_commit_count}
    """
    print(f"\n开始计算组织Commits数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        org_commit_count = calculate_org_commits(repo, base_path, start_date,
                                                 end_date, common_domains_file)
        results[repo.name] = org_commit_count
        # print(f"  组织Commit数量: {org_commit_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(org_commit_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均组织Commit数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大组织Commit数量: {max(results.values()):,}")
    print(f"  最小组织Commit数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_org_entropy(repo_info, base_path="F:/github_repos",
                          start_date="2023-09-01", end_date="2023-12-31",
                          common_domains_file="free_email_provider_domains.txt"):
    """
    计算指定时间范围内的组织熵（归一化）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        float: 归一化的组织熵值
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0.0

    try:
        # 加载公共邮箱域名
        common_domains = load_common_email_domains(common_domains_file)

        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 用于存储committer和其所有邮箱的映射
        user_email = defaultdict(list)
        # 用于存储组织域名和对应committer的映射
        domain_user = defaultdict(list)
        # 存储所有commit信息
        commit_data_list = []

        # 存储属于组织的人员和域名
        users = set()
        domains = set()

        processed_commits = set()

        # 遍历所有分支收集commit信息
        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    if commit.hexsha in processed_commits:
                        continue

                    processed_commits.add(commit.hexsha)

                    committer_name = commit.author.name
                    email = commit.author.email.lower()
                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    # 保存commit信息（组织暂时设为unknown）
                    commit_data_list.append({
                        'id': commit.hexsha,
                        'date': commit_date,
                        'committer': committer_name,
                        'email': email,
                        'organization': 'unknown'
                    })

                    # 建立映射关系
                    if email and '@' in email:
                        user_email[committer_name].append(email)

            except Exception as e:
                continue

        # 按日期排序
        commit_data_list.sort(key=lambda x: x['date'])

        # 找出所有拥有非公共邮箱的人员和组织域名
        for committer, emails in user_email.items():
            has_private_email = False

            for email in emails:
                if '@' not in email:
                    continue

                domain = email.split('@')[1].lower()

                if not is_common_email(email, common_domains):
                    domains.add(domain)
                    has_private_email = True
                    domain_user[domain].append(committer)

            if has_private_email:
                users.add(committer)

        # 补全commit的organization字段
        latest_user_org = {}  # 每个用户的最新组织记录

        for commit_info in commit_data_list:
            committer = commit_info['committer']
            email = commit_info['email']

            if committer not in users:
                continue

            if email and '@' in email:
                domain = email.split('@')[1].lower()

                if domain in domains:
                    # 使用非公共域名，更新最新组织记录
                    latest_user_org[committer] = domain
                    commit_info['organization'] = domain
                else:
                    # 使用公共邮箱，尝试从历史记录获取组织
                    if committer in latest_user_org:
                        commit_info['organization'] = latest_user_org[committer]

        # 统计时间范围内各组织的提交数
        org_commit_counts = defaultdict(int)

        for commit_info in commit_data_list:
            commit_date = commit_info['date']
            organization = commit_info['organization']

            # 检查是否在时间范围内
            if start <= commit_date <= end:
                if organization != 'unknown':
                    org_commit_counts[organization] += 1

        # 计算归一化的组织熵
        if not org_commit_counts or len(org_commit_counts) <= 1:
            return 0.0

        total_commits = sum(org_commit_counts.values())
        org_count = len(org_commit_counts)

        if total_commits == 0:
            return 0.0

        # 计算信息熵
        entropy = 0.0
        for count in org_commit_counts.values():
            p = count / total_commits
            if p > 0:
                entropy -= p * math.log2(p)

        # 归一化：除以最大熵 log2(组织数)
        max_entropy = math.log2(org_count)
        normalized_entropy = entropy / max_entropy if max_entropy > 0 else 0.0

        return normalized_entropy

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0.0


def batch_calculate_org_entropy(repo_objects, base_path="F:/github_repos",
                                start_date="2023-09-01", end_date="2023-12-31",
                                common_domains_file="free_email_provider_domains.txt"):
    """
    批量计算所有仓库的组织熵

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        dict: {repo_name: org_entropy}
    """
    print(f"\n开始计算组织熵...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        org_entropy = calculate_org_entropy(repo, base_path, start_date,
                                            end_date, common_domains_file)
        results[repo.name] = org_entropy
        # print(f"  组织熵: {org_entropy:.4f}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(org_entropy)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均组织熵: {sum(results.values()) / len(results):.4f}")
    print(f"  最大组织熵: {max(results.values()):.4f}")
    print(f"  最小组织熵: {min(results.values()):.4f}")
    print(f"{'=' * 60}\n")

    return results


def calculate_volunteer_entropy(repo_info, base_path="F:/github_repos",
                                start_date="2023-09-01", end_date="2023-12-31",
                                common_domains_file="free_email_provider_domains.txt"):
    """
    计算指定时间范围内的志愿者熵（非归一化）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        float: 志愿者熵值
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0.0

    try:
        # 加载公共邮箱域名
        common_domains = load_common_email_domains(common_domains_file)

        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 用于存储committer和其所有邮箱的映射
        user_email = defaultdict(list)
        # 存储所有commit信息
        commit_data_list = []

        # 存储属于组织的人员
        users = set()

        processed_commits = set()

        # 遍历所有分支收集commit信息
        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    if commit.hexsha in processed_commits:
                        continue

                    processed_commits.add(commit.hexsha)

                    committer_name = commit.author.name
                    email = commit.author.email.lower()
                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    # 保存commit信息
                    commit_data_list.append({
                        'id': commit.hexsha,
                        'date': commit_date,
                        'committer': committer_name,
                        'email': email
                    })

                    # 建立映射关系
                    if email and '@' in email:
                        user_email[committer_name].append(email)

            except Exception as e:
                continue

        # 按日期排序
        commit_data_list.sort(key=lambda x: x['date'])

        # 找出所有拥有非公共邮箱的人员（组织成员）
        for committer, emails in user_email.items():
            has_private_email = False

            for email in emails:
                if '@' not in email:
                    continue

                if not is_common_email(email, common_domains):
                    has_private_email = True
                    break

            if has_private_email:
                users.add(committer)

        # 过滤掉组织成员的提交，只保留志愿者的提交
        volunteer_commits = [
            commit for commit in commit_data_list
            if commit['committer'] not in users
        ]

        # 统计时间范围内各志愿者的提交数
        volunteer_commit_counts = defaultdict(int)

        for commit_info in volunteer_commits:
            commit_date = commit_info['date']
            committer = commit_info['committer']

            # 检查是否在时间范围内
            if start <= commit_date <= end:
                volunteer_commit_counts[committer] += 1

        # 计算志愿者熵（不归一化）
        if not volunteer_commit_counts or len(volunteer_commit_counts) <= 1:
            return 0.0

        total_commits = sum(volunteer_commit_counts.values())

        if total_commits == 0:
            return 0.0

        # 计算信息熵（不归一化）
        entropy = 0.0
        for count in volunteer_commit_counts.values():
            p = count / total_commits
            if p > 0:
                entropy -= p * math.log2(p)

        return entropy

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0.0


def batch_calculate_volunteer_entropy(repo_objects, base_path="F:/github_repos",
                                      start_date="2023-09-01", end_date="2023-12-31",
                                      common_domains_file="free_email_provider_domains.txt"):
    """
    批量计算所有仓库的志愿者熵

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        dict: {repo_name: volunteer_entropy}
    """
    print(f"\n开始计算志愿者熵...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        volunteer_entropy = calculate_volunteer_entropy(repo, base_path, start_date,
                                                        end_date, common_domains_file)
        results[repo.name] = volunteer_entropy
        # print(f"  志愿者熵: {volunteer_entropy:.4f}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(volunteer_entropy)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均志愿者熵: {sum(results.values()) / len(results):.4f}")
    print(f"  最大志愿者熵: {max(results.values()):.4f}")
    print(f"  最小志愿者熵: {min(results.values()):.4f}")
    print(f"{'=' * 60}\n")

    return results


def calculate_volunteer_commits(repo_info, base_path="F:/github_repos",
                                start_date="2023-09-01", end_date="2023-12-31",
                                common_domains_file="free_email_provider_domains.txt"):
    """
    计算指定时间范围内的志愿者commits数量（非组织成员的提交）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        int: 志愿者commits数量
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0

    try:
        # 加载公共邮箱域名
        common_domains = load_common_email_domains(common_domains_file)

        repo = Repo(str(local_path))
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 用于存储committer和其所有邮箱的映射
        user_email = defaultdict(list)
        # 用于存储邮箱和对应committer的映射
        email_user = defaultdict(list)
        # 存储所有commit信息
        commit_data_list = []

        # 存储属于组织的人员和邮箱
        users = set()
        emails = set()

        processed_commits = set()

        # 遍历所有分支收集commit信息
        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    if commit.hexsha in processed_commits:
                        continue

                    processed_commits.add(commit.hexsha)

                    committer_name = commit.author.name
                    email = commit.author.email.lower()
                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    # 保存commit信息
                    commit_data_list.append({
                        'id': commit.hexsha,
                        'date': commit_date,
                        'committer': committer_name,
                        'email': email
                    })

                    # 建立映射关系
                    if email and '@' in email:
                        user_email[committer_name].append(email)
                        email_user[email].append(committer_name)

            except Exception as e:
                continue

        # 找出所有拥有非公共邮箱的人员（组织成员）
        for committer, email_list in user_email.items():
            has_private_email = False

            for email in email_list:
                if '@' not in email:
                    continue

                if not is_common_email(email, common_domains):
                    has_private_email = True
                    break

            if has_private_email:
                users.add(committer)

        # 找出属于组织的邮箱
        for email, committers in email_user.items():
            # 如果邮箱的某个用户在users中，该邮箱属于组织
            if any(committer in users for committer in committers):
                emails.add(email)

        # 统计时间范围内的志愿者commits数量
        volunteer_commit_count = 0

        for commit_info in commit_data_list:
            commit_date = commit_info['date']
            committer = commit_info['committer']
            email = commit_info['email']

            # 检查是否在时间范围内
            if start <= commit_date <= end:
                # 判断是否不属于组织（即为志愿者）
                is_volunteer_commit = (
                        committer not in users and
                        email not in emails
                )

                if is_volunteer_commit:
                    volunteer_commit_count += 1

        return volunteer_commit_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0


def batch_calculate_volunteer_commits(repo_objects, base_path="F:/github_repos",
                                      start_date="2023-09-01", end_date="2023-12-31",
                                      common_domains_file="free_email_provider_domains.txt"):
    """
    批量计算所有仓库的志愿者commits数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        dict: {repo_name: volunteer_commit_count}
    """
    print(f"\n开始计算志愿者Commits数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        volunteer_commit_count = calculate_volunteer_commits(repo, base_path, start_date,
                                                             end_date, common_domains_file)
        results[repo.name] = volunteer_commit_count
        # print(f"  志愿者Commit数量: {volunteer_commit_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(volunteer_commit_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均志愿者Commit数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大志愿者Commit数量: {max(results.values()):,}")
    print(f"  最小志愿者Commit数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_review_ratio(repo_info, base_path="F:/github_api_repos",
                           start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的PR review比率（有review的PR数量 / 总PR数量）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        float: review比率 (0.0-1.0)
    """
    # 构建文件路径
    pr_file = Path(base_path) / repo_info.name / "PRData.json"
    review_file = Path(base_path) / repo_info.name / "PRReviewData.json"

    if not pr_file.exists() or not review_file.exists():
        print(f"⚠ PR或Review文件不存在: {repo_info.name}")
        return 0.0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取PR数据
        with open(pr_file, 'r', encoding='utf-8') as f:
            pr_array = json.load(f)

        # 读取Review数据
        with open(review_file, 'r', encoding='utf-8') as f:
            review_map = json.load(f)

        # 构建review数量映射：key是PR编号，value是review数量
        review_count_map = {}
        for pr_number, reviews in review_map.items():
            review_count_map[pr_number] = len(reviews) if isinstance(reviews, list) else 0

        # 统计时间范围内的PR
        pr_count = 0
        reviewed_pr_count = 0

        for pr in pr_array:
            # 获取PR编号和创建时间
            pr_number = str(pr.get('number', ''))
            created_at_str = pr.get('created_at', '')

            if not created_at_str:
                continue

            # 解析时间
            try:
                if 'T' in created_at_str:
                    created_at_str = created_at_str.split('T')[0]

                created_date = datetime.strptime(created_at_str, "%Y-%m-%d")

                # 检查是否在时间范围内
                if start <= created_date <= end:
                    pr_count += 1

                    # 检查是否有review
                    review_num = review_count_map.get(pr_number, 0)
                    if review_num > 0:
                        reviewed_pr_count += 1

            except ValueError:
                try:
                    from dateutil import parser
                    created_date = parser.parse(created_at_str).replace(tzinfo=None)
                    if start <= created_date <= end:
                        pr_count += 1
                        review_num = review_count_map.get(pr_number, 0)
                        if review_num > 0:
                            reviewed_pr_count += 1
                except:
                    continue

        # 计算比率
        if pr_count > 0:
            return reviewed_pr_count / pr_count
        else:
            return 0.0

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的Review Ratio时出错: {e}")
        return 0.0


def batch_calculate_review_ratio(repo_objects, base_path="F:/github_api_repos",
                                 start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的review ratio

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: review_ratio}
    """
    print(f"\n开始计算Review Ratio...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        review_ratio = calculate_review_ratio(repo, base_path, start_date, end_date)
        results[repo.name] = review_ratio
        # print(f"  Review Ratio: {review_ratio:.4f}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(review_ratio)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均Review Ratio: {sum(results.values()) / len(results):.4f}")
    print(f"  最大Review Ratio: {max(results.values()):.4f}")
    print(f"  最小Review Ratio: {min(results.values()):.4f}")
    print(f"{'=' * 60}\n")

    return results


def calculate_pr_merged_ratio(repo_info, base_path="F:/github_api_repos",
                              start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的PR merged比率（merged的PR数量 / 总PR数量）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        float: merged比率 (0.0-1.0)
    """
    # 构建PRData.json文件路径
    pr_file = Path(base_path) / repo_info.name / "PRData.json"

    if not pr_file.exists():
        print(f"⚠ PR文件不存在: {pr_file}")
        return 0.0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取PR数据
        with open(pr_file, 'r', encoding='utf-8') as f:
            pr_array = json.load(f)

        # 统计时间范围内的PR
        pr_count = 0
        merged_pr_count = 0

        for pr in pr_array:
            # 获取PR创建时间
            created_at_str = pr.get('created_at', '')

            if not created_at_str:
                continue

            # 解析时间
            try:
                if 'T' in created_at_str:
                    created_at_str = created_at_str.split('T')[0]

                created_date = datetime.strptime(created_at_str, "%Y-%m-%d")

                # 检查是否在时间范围内
                if start <= created_date <= end:
                    pr_count += 1

                    # 检查是否merged（merged_at字段存在且不为null）
                    merged_at = pr.get('merged_at')
                    if merged_at is not None and merged_at:
                        merged_pr_count += 1

            except ValueError:
                try:
                    from dateutil import parser
                    created_date = parser.parse(created_at_str).replace(tzinfo=None)
                    if start <= created_date <= end:
                        pr_count += 1
                        merged_at = pr.get('merged_at')
                        if merged_at is not None and merged_at:
                            merged_pr_count += 1
                except:
                    continue

        # 计算比率
        if pr_count > 0:
            return merged_pr_count / pr_count
        else:
            return 0.0

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的PR Merged Ratio时出错: {e}")
        return 0.0


def batch_calculate_pr_merged_ratio(repo_objects, base_path="F:/github_api_repos",
                                    start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的PR merged ratio

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: pr_merged_ratio}
    """
    print(f"\n开始计算PR Merged Ratio...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        pr_merged_ratio = calculate_pr_merged_ratio(repo, base_path, start_date, end_date)
        results[repo.name] = pr_merged_ratio
        # print(f"  PR Merged Ratio: {pr_merged_ratio:.4f}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(pr_merged_ratio)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均PR Merged Ratio: {sum(results.values()) / len(results):.4f}")
    print(f"  最大PR Merged Ratio: {max(results.values()):.4f}")
    print(f"  最小PR Merged Ratio: {min(results.values()):.4f}")
    print(f"{'=' * 60}\n")

    return results


def calculate_pr_linked_ratio(repo_info, base_path="F:/github_api_repos",
                              start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的PR linked比率（linked到issue的PR数量 / 总PR数量）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        float: linked比率 (0.0-1.0)
    """
    # 构建PRData.json文件路径
    pr_file = Path(base_path) / repo_info.name / "PRData.json"

    if not pr_file.exists():
        print(f"⚠ PR文件不存在: {pr_file}")
        return 0.0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取PR数据
        with open(pr_file, 'r', encoding='utf-8') as f:
            pr_array = json.load(f)

        # 定义关键词（与Java代码一致）
        keywords = ["close", "closes", "closed", "fix", "fixes", "fixed",
                    "resolve", "resolves", "resolved"]

        # 构建正则表达式（不区分大小写）
        keyword_pattern = r'(?i)\b(' + '|'.join(keywords) + r')\b\s*(\S+)?(\s*#\d+)'
        pattern = re.compile(keyword_pattern)

        # 统计时间范围内的PR
        pr_count = 0
        linked_pr_count = 0

        for pr in pr_array:
            # 获取PR创建时间
            created_at_str = pr.get('created_at', '')

            if not created_at_str:
                continue

            # 解析时间
            try:
                if 'T' in created_at_str:
                    created_at_str = created_at_str.split('T')[0]

                created_date = datetime.strptime(created_at_str, "%Y-%m-%d")

                # 检查是否在时间范围内
                if start <= created_date <= end:
                    pr_count += 1

                    # 检查PR的title和body是否包含关键词和issue编号
                    title = pr.get('title', '')
                    body = pr.get('body', '')

                    # 合并title和body进行搜索
                    combined_text = f"{title} {body}"

                    # 使用正则表达式匹配
                    if pattern.search(combined_text):
                        linked_pr_count += 1

            except ValueError:
                try:
                    from dateutil import parser
                    created_date = parser.parse(created_at_str).replace(tzinfo=None)
                    if start <= created_date <= end:
                        pr_count += 1

                        title = pr.get('title', '')
                        body = pr.get('body', '')
                        combined_text = f"{title} {body}"

                        if pattern.search(combined_text):
                            linked_pr_count += 1
                except:
                    continue

        # 计算比率
        if pr_count > 0:
            return linked_pr_count / pr_count
        else:
            return 0.0

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的PR Linked Ratio时出错: {e}")
        return 0.0


def batch_calculate_pr_linked_ratio(repo_objects, base_path="F:/github_api_repos",
                                    start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的PR linked ratio

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: pr_linked_ratio}
    """
    print(f"\n开始计算PR Linked Ratio...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        pr_linked_ratio = calculate_pr_linked_ratio(repo, base_path, start_date, end_date)
        results[repo.name] = pr_linked_ratio
        # print(f"  PR Linked Ratio: {pr_linked_ratio:.4f}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(pr_linked_ratio)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均PR Linked Ratio: {sum(results.values()) / len(results):.4f}")
    print(f"  最大PR Linked Ratio: {max(results.values()):.4f}")
    print(f"  最小PR Linked Ratio: {min(results.values()):.4f}")
    print(f"{'=' * 60}\n")

    return results


def calculate_contributor_count(repo_info, base_path_git="F:/github_repos",
                                base_path_api="F:/github_api_repos",
                                start_date="2023-09-01", end_date="2023-12-31",
                                common_domains_file="free_email_provider_domains.txt"):
    """
    计算指定时间范围内的contributor数量（去重的代码提交者、PR提交者、审核者）

    Args:
        repo_info: RepoInfo对象
        base_path_git: 本地Git仓库根目录
        base_path_api: API数据根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        int: contributor总数
    """
    local_path = Path(base_path_git) / repo_info.name
    pr_file = Path(base_path_api) / repo_info.name / "PRData.json"
    review_file = Path(base_path_api) / repo_info.name / "PRReviewData.json"

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 用于存储所有活跃的contributor
        active_contributors = set()

        # 1. 获取代码提交者（Code Committers）
        repo = Repo(str(local_path))
        processed_commits = set()

        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    if commit.hexsha in processed_commits:
                        continue

                    processed_commits.add(commit.hexsha)
                    commit_date = datetime.fromtimestamp(commit.committed_date)

                    # 检查是否在时间范围内
                    if start <= commit_date <= end:
                        # 判断是否为代码提交
                        if is_code_commit(repo, commit):
                            committer_name = commit.author.name
                            active_contributors.add(committer_name)
            except Exception as e:
                continue

        # 2. 获取PR提交者（PR Submitters）
        if pr_file.exists():
            try:
                with open(pr_file, 'r', encoding='utf-8') as f:
                    pr_array = json.load(f)

                for pr in pr_array:
                    created_at_str = pr.get('created_at', '')
                    if not created_at_str:
                        continue

                    try:
                        if 'T' in created_at_str:
                            created_at_str = created_at_str.split('T')[0]
                        created_date = datetime.strptime(created_at_str, "%Y-%m-%d")

                        if start <= created_date <= end:
                            user = pr.get('user', {})
                            if user and 'login' in user:
                                active_contributors.add(user['login'])
                    except:
                        continue
            except Exception as e:
                print(f"  ⚠ 读取PR数据出错: {e}")

        # 3. 获取代码审核者（Reviewers）
        if review_file.exists():
            try:
                with open(review_file, 'r', encoding='utf-8') as f:
                    review_map = json.load(f)

                for pr_number, reviews in review_map.items():
                    if not isinstance(reviews, list):
                        continue

                    for review in reviews:
                        submitted_at_str = review.get('submitted_at', '')
                        if not submitted_at_str:
                            continue

                        try:
                            if 'T' in submitted_at_str:
                                submitted_at_str = submitted_at_str.split('T')[0]
                            submitted_date = datetime.strptime(submitted_at_str, "%Y-%m-%d")

                            if start <= submitted_date <= end:
                                user = review.get('user', {})
                                if user and 'login' in user:
                                    active_contributors.add(user['login'])
                        except:
                            continue
            except Exception as e:
                print(f"  ⚠ 读取Review数据出错: {e}")

        return len(active_contributors)

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0


def is_code_commit(repo, commit):
    """
    判断commit是否包含代码更改

    Args:
        repo: Git仓库对象
        commit: RevCommit对象

    Returns:
        bool: 是否为代码提交
    """
    try:
        # 定义代码文件扩展名
        code_extensions = {
            '.java', '.py', '.cpp', '.h', '.hpp', '.c', '.js', '.ts',
            '.html', '.css', '.scss', '.go', '.rb', '.php', '.swift',
            '.kt', '.scala', '.rust', '.sh', '.bat', '.sql', '.r',
            '.lua', '.clojure', '.jl', '.v', '.vhdl'
        }

        # 如果没有父提交（初始提交）
        if len(commit.parents) == 0:
            # 检查初始提交的文件
            for item in commit.tree.traverse():
                if item.type == 'blob':  # 文件
                    file_path = item.path
                    if any(file_path.endswith(ext) for ext in code_extensions):
                        return True
            return False

        # 有父提交，比较差异
        parent = commit.parents[0]
        diffs = parent.diff(commit)

        for diff in diffs:
            # 检查新文件路径
            if diff.b_path:
                if any(diff.b_path.endswith(ext) for ext in code_extensions):
                    # 排除删除操作
                    if diff.change_type != 'D':
                        return True

        return False

    except Exception as e:
        return False


def batch_calculate_contributor_count(repo_objects, base_path_git="F:/github_repos",
                                      base_path_api="F:/github_api_repos",
                                      start_date="2023-09-01", end_date="2023-12-31",
                                      common_domains_file="free_email_provider_domains.txt"):
    """
    批量计算所有仓库的contributor数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path_git: 本地Git仓库根目录
        base_path_api: API数据根目录
        start_date: 开始日期
        end_date: 结束日期
        common_domains_file: 公共邮箱域名文件路径

    Returns:
        dict: {repo_name: contributor_count}
    """
    print(f"\n开始计算Contributor数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        contributor_count = calculate_contributor_count(repo, base_path_git, base_path_api,
                                                        start_date, end_date, common_domains_file)
        results[repo.name] = contributor_count
        # print(f"  Contributor数量: {contributor_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(contributor_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均Contributor数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大Contributor数量: {max(results.values()):,}")
    print(f"  最小Contributor数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_long_term_contributors(repo_info, base_path="F:/github_repos",
                                     start_date="2023-09-01",
                                     check_date="2023-12-31"):
    """
    计算指定日期的长期贡献者数量（贡献时长超过3年且在前80%提交量中的贡献者）

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        check_date: 检查日期 (YYYY-MM-DD)

    Returns:
        int: 长期贡献者数量
    """
    local_path = Path(base_path) / repo_info.name

    if not local_path.exists():
        print(f"⚠ 仓库路径不存在: {local_path}")
        return 0

    try:
        repo = Repo(str(local_path))
        start_date_obj = datetime.strptime(start_date, "%Y-%m-%d")
        check_date_obj = datetime.strptime(check_date, "%Y-%m-%d")

        # 获取所有提交记录
        all_commits = []
        processed_commits = set()

        for branch in repo.branches:
            try:
                for commit in repo.iter_commits(branch.name):
                    if commit.hexsha in processed_commits:
                        continue
                    processed_commits.add(commit.hexsha)
                    all_commits.append(commit)
            except Exception as e:
                continue

        if not all_commits:
            return 0

        # 获取第一个提交的时间
        first_commit_date = min(
            datetime.fromtimestamp(commit.committed_date)
            for commit in all_commits
        )

        # 计算第一个时间节点（第一个提交后3年）
        start_date1 = first_commit_date.replace(year=first_commit_date.year + 3)
        # 调整到下个月的第一天
        if start_date1.day != 1:
            if start_date1.month == 12:
                start_date1 = start_date1.replace(year=start_date1.year + 1, month=1, day=1)
            else:
                start_date1 = start_date1.replace(month=start_date1.month + 1, day=1)

        # 如果检查日期早于第一个时间节点，返回0
        if check_date_obj < start_date1:
            return 0

        # 存储贡献者的提交时间记录
        contributor_commits = defaultdict(list)

        # 遍历提交记录，按作者存储提交时间
        for commit in all_commits:
            author = commit.author.name
            commit_date = datetime.fromtimestamp(commit.committed_date)
            contributor_commits[author].append(commit_date)

        # 对每个贡献者的提交时间排序
        for author in contributor_commits:
            contributor_commits[author].sort()

        # 筛选在start_date和check_date之间有过commit的贡献者
        active_contributors = set()
        for author, commits in contributor_commits.items():
            has_commit_in_range = any(
                start_date_obj <= commit_date <= check_date_obj
                for commit_date in commits
            )
            if has_commit_in_range:
                active_contributors.add(author)

        # 如果没有活跃贡献者，返回0
        if not active_contributors:
            return 0

        # 计算截至check_date的总提交数量
        total_commits_before_date = sum(
            1 for commits in contributor_commits.values()
            for commit_date in commits
            if commit_date <= check_date_obj
        )

        if total_commits_before_date == 0:
            return 0

        # 统计每个贡献者在check_date之前的提交数量
        contributor_commit_counts = []
        for author, commits in contributor_commits.items():
            commit_count = sum(1 for commit_date in commits if commit_date <= check_date_obj)
            if commit_count > 0:
                contributor_commit_counts.append((author, commit_count))

        # 计算check_date之前的贡献者总数
        total_contributors_before_date = len(contributor_commit_counts)

        # 按提交数量从高到低排序
        contributor_commit_counts.sort(key=lambda x: x[1], reverse=True)

        # 计算总提交数的80%
        commit_threshold = total_commits_before_date * 0.8
        cumulative_commits = 0
        long_term_contributors = 0

        # 累计贡献者直到提交数达到80%
        three_years_before_check = check_date_obj.replace(year=check_date_obj.year - 3)
        for author, commit_count in contributor_commit_counts:
            cumulative_commits += commit_count

            # 如果累计提交数已达到总提交数的80%，退出循环
            if cumulative_commits >= commit_threshold:
                break

            if author not in active_contributors:
                continue

            # 获取该贡献者的首次提交时间
            first_contribution_date = contributor_commits[author][0]

            # 判断该贡献者是否贡献超过三年
            if first_contribution_date < three_years_before_check:
                long_term_contributors += 1

        return long_term_contributors

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 时出错: {e}")
        return 0


def batch_calculate_long_term_contributors(repo_objects, base_path="F:/github_repos",
                                           start_date="2023-09-01",
                                           check_date="2023-12-31"):
    """
    批量计算所有仓库的长期贡献者数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        check_date: 检查日期

    Returns:
        dict: {repo_name: long_term_contributor_count}
    """
    print(f"\n开始计算长期贡献者数量...")
    print(f"检查日期: {check_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        ltc_count = calculate_long_term_contributors(repo, base_path, check_date)
        results[repo.name] = ltc_count
        # print(f"  长期贡献者数量: {ltc_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(ltc_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均长期贡献者数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大长期贡献者数量: {max(results.values()):,}")
    print(f"  最小长期贡献者数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_star_count(repo_info, base_path="F:/github_api_repos",
                         start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的star数量

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 总star数量
    """
    # 构建StarData.json文件路径
    star_file = Path(base_path) / repo_info.name / "StarData.json"

    if not star_file.exists():
        print(f"⚠ Star文件不存在: {star_file}")
        return 0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取JSON文件
        with open(star_file, 'r', encoding='utf-8') as f:
            stars = json.load(f)

        star_count = 0

        # 遍历所有star记录
        for star in stars:
            # 获取starredAt字段
            starred_at_str = star.get('starredAt')

            if not starred_at_str:
                continue

            # 解析时间（支持ISO 8601格式）
            try:
                # 提取日期部分 YYYY-MM-DD
                if 'T' in starred_at_str:
                    starred_at_str = starred_at_str.split('T')[0]

                starred_date = datetime.strptime(starred_at_str[:10], "%Y-%m-%d")

                # 检查是否在时间范围内（包含起始日期，包含结束日期）
                if start <= starred_date <= end:
                    star_count += 1

            except ValueError:
                # 如果日期格式不匹配，尝试完整的ISO格式
                try:
                    from dateutil import parser
                    starred_date = parser.parse(starred_at_str).replace(tzinfo=None)
                    if start <= starred_date <= end:
                        star_count += 1
                except:
                    continue

        return star_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的Star数据时出错: {e}")
        return 0


def batch_calculate_star_count(repo_objects, base_path="F:/github_api_repos",
                               start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的star数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: star_count}
    """
    print(f"\n开始计算Star数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        star_count = calculate_star_count(repo, base_path, start_date, end_date)
        results[repo.name] = star_count
        # print(f"  Star数量: {star_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(star_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均Star数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大Star数量: {max(results.values()):,}")
    print(f"  最小Star数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def calculate_fork_count(repo_info, base_path="F:/github_api_repos",
                         start_date="2023-09-01", end_date="2023-12-31"):
    """
    计算指定时间范围内的fork数量

    Args:
        repo_info: RepoInfo对象
        base_path: 本地仓库根目录
        start_date: 开始日期 (YYYY-MM-DD)
        end_date: 结束日期 (YYYY-MM-DD)

    Returns:
        int: 总fork数量
    """
    # 构建ForkData.json文件路径
    fork_file = Path(base_path) / repo_info.name / "ForkData.json"

    if not fork_file.exists():
        print(f"⚠ Fork文件不存在: {fork_file}")
        return 0

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        # 读取JSON文件
        with open(fork_file, 'r', encoding='utf-8') as f:
            forks = json.load(f)

        fork_count = 0

        # 遍历所有fork记录
        for fork in forks:
            # 获取createdAt字段（从node节点中）
            created_at_str = None

            # 尝试从node节点获取createdAt
            if 'node' in fork and 'createdAt' in fork['node']:
                created_at_str = fork['node']['createdAt']
            # 兼容直接在根节点的情况
            elif 'createdAt' in fork:
                created_at_str = fork['createdAt']

            if not created_at_str:
                continue

            # 解析时间（支持ISO 8601格式）
            try:
                # 提取日期部分 YYYY-MM-DD
                if 'T' in created_at_str:
                    created_at_str = created_at_str.split('T')[0]

                created_date = datetime.strptime(created_at_str[:10], "%Y-%m-%d")

                # 检查是否在时间范围内（包含起始日期，包含结束日期）
                if start <= created_date <= end:
                    fork_count += 1

            except ValueError:
                # 如果日期格式不匹配，尝试完整的ISO格式
                try:
                    from dateutil import parser
                    created_date = parser.parse(created_at_str).replace(tzinfo=None)
                    if start <= created_date <= end:
                        fork_count += 1
                except:
                    continue

        return fork_count

    except Exception as e:
        print(f"✗ 处理仓库 {repo_info.name} 的Fork数据时出错: {e}")
        return 0


def batch_calculate_fork_count(repo_objects, base_path="F:/github_api_repos",
                               start_date="2023-09-01", end_date="2023-12-31"):
    """
    批量计算所有仓库的fork数量

    Args:
        repo_objects: RepoInfo对象列表
        base_path: 本地仓库根目录
        start_date: 开始日期
        end_date: 结束日期

    Returns:
        dict: {repo_name: fork_count}
    """
    print(f"\n开始计算Fork数量...")
    print(f"时间范围: {start_date} 到 {end_date}\n")

    results = {}

    for i, repo in enumerate(repo_objects, 1):
        # print(f"[{i}/{len(repo_objects)}] 处理: {repo.name}")
        fork_count = calculate_fork_count(repo, base_path, start_date, end_date)
        results[repo.name] = fork_count
        # print(f"  Fork数量: {fork_count:,}")

        # 将结果添加到repo对象的metrics中
        repo.add_metric(fork_count)

    print(f"\n{'=' * 60}")
    print(f"统计完成:")
    print(f"  总仓库数: {len(repo_objects)}")
    print(f"  平均Fork数量: {sum(results.values()) / len(results):,.0f}")
    print(f"  最大Fork数量: {max(results.values()):,}")
    print(f"  最小Fork数量: {min(results.values()):,}")
    print(f"{'=' * 60}\n")

    return results


def save_results_to_csv(repo_objects, output_file="repo_metrics.csv"):
    """
    将仓库指标和标签保存到CSV文件

    Args:
        repo_objects: RepoInfo对象列表
        output_file: 输出CSV文件名
    """
    # 定义CSV列名
    headers = [
        'owner',
        'name',
        'full_name',
        'label',
        'code_changes',
        'issue_count',
        'commit_count',
        'pr_count',
        'org_commits',
        'org_entropy',
        'volunteer_entropy',
        'volunteer_commits',
        'review_ratio',
        'pr_merged_ratio',
        'pr_linked_ratio',
        'contributor_count',
        'long_term_contributors',
        'star_count',
        'fork_count'
    ]

    try:
        with open(output_file, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)

            # 写入表头
            writer.writerow(headers)

            # 写入数据
            for repo in repo_objects:
                row = [
                    repo.owner,
                    repo.name,
                    repo.get_full_name(),
                    repo.label
                ]

                # 添加所有指标（按照计算顺序）
                row.extend(repo.metrics)

                writer.writerow(row)

        print(f"\n{'=' * 60}")
        print(f"✓ 数据已成功保存到: {output_file}")
        print(f"  总记录数: {len(repo_objects)}")
        print(f"  列数: {len(headers)}")
        print(f"{'=' * 60}\n")

    except Exception as e:
        print(f"✗ 保存CSV文件时出错: {e}")


def read_repos_from_csv(csv_file="repo_metrics6.csv"):
    """
    从CSV文件中读取仓库信息并创建RepoInfo对象列表

    Args:
        csv_file: CSV文件路径

    Returns:
        list[RepoInfo]: RepoInfo对象列表
    """
    repo_objects = []

    try:
        with open(csv_file, 'r', encoding='utf-8-sig') as f:
            reader = csv.DictReader(f, delimiter=',')

            for row in reader:
                owner = row.get('owner', '').strip()
                name = row.get('name', '').strip()
                label = int(row.get('label', 0))
                if not name:
                    continue

                # 创建RepoInfo对象
                repo_obj = RepoInfo(owner, name, label)

                repo_objects.append(repo_obj)

        print(f"\n{'=' * 60}")
        print(f"✓ 从CSV文件读取仓库信息:")
        print(f"  文件: {csv_file}")
        print(f"  读取仓库数: {len(repo_objects)}")
        print(f"{'=' * 60}\n")

        return repo_objects

    except Exception as e:
        print(f"✗ 读取CSV文件时出错: {e}")
        return []

# 使用示例
if __name__ == "__main__":
    # # 1. 提取所有仓库
    # repos = extract_success_repos()
    # print(f"成功提取 {len(repos)} 个去重后的仓库")
    #
    # repos = filter_repos_by_file_count(repos,
    #                                    base_path="F:/github_api_repos",
    #                                    min_files=5)
    # print(f"过滤后剩余 {len(repos)} 个仓库")
    #
    # # 2. 过滤掉第一个commit在2023年9月1日之后的仓库
    # repos = filter_repos_by_first_commit(repos,
    #                                      base_path="F:/github_repos",
    #                                      cutoff_date=START_DATE)
    # print(f"过滤后剩余 {len(repos)} 个仓库")

    # # 3. 随机选择1000个
    # repos1000 = get_random_repos(repos, count=1000, seed=42)
    # print(f"随机选择了 {len(repos1000)} 个仓库\n")
    #
    # # 4. 创建RepoInfo对象列表
    # # repo_objects = create_repo_objects(repos)
    repo_objects = read_repos_from_csv("repo_metrics6_1.csv")
    print(f"创建了 {len(repo_objects)} 个RepoInfo对象\n")

    # 5. 批量计算活跃度标签
    batch_calculate_labels(repo_objects,
                           base_path="F:/github_repos",
                           start_date=END_DATE,
                           end_date="2025-11-30")
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_1.csv")

    # 6. 批量计算代码更改行数（2023-09-01 到 2023-12-31）
    code_changes = batch_calculate_code_changes(repo_objects,
                                                base_path="F:/github_repos",
                                                start_date=START_DATE,
                                                end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_2.csv")

    # 7. 批量计算Issue数量（2023-09-01 到 2023-12-31）
    issue_counts = batch_calculate_issue_count(repo_objects,
                                               base_path="F:/github_api_repos",
                                               start_date=START_DATE,
                                               end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_3.csv")

    # 8. 批量计算Commit数量（2023-09-01 到 2023-12-31）
    commit_counts = batch_calculate_commit_count(repo_objects,
                                                 base_path="F:/github_repos",
                                                 start_date=START_DATE,
                                                 end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_4.csv")

    # 9. 批量计算PR数量（2023-09-01 到 2023-12-31）
    pr_counts = batch_calculate_pr_count(repo_objects,
                                         base_path="F:/github_api_repos",
                                         start_date=START_DATE,
                                         end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_5.csv")

    # 10. 批量计算组织Commits数量（2023-09-01 到 2023-12-31）
    org_commit_counts = batch_calculate_org_commits(repo_objects,
                                                    base_path="F:/github_repos",
                                                    start_date=START_DATE,
                                                    end_date=END_DATE,
                                                    common_domains_file="free_email_provider_domains.txt")
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_6.csv")

    # 11. 批量计算组织熵（2023-09-01 到 2023-12-31）
    org_entropy_values = batch_calculate_org_entropy(repo_objects,
                                                     base_path="F:/github_repos",
                                                     start_date=START_DATE,
                                                     end_date=END_DATE,
                                                     common_domains_file="free_email_provider_domains.txt")
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_7.csv")

    # 12. 批量计算志愿者熵（2023-09-01 到 2023-12-31）
    volunteer_entropy_values = batch_calculate_volunteer_entropy(repo_objects,
                                                                 base_path="F:/github_repos",
                                                                 start_date=START_DATE,
                                                                 end_date=END_DATE,
                                                                 common_domains_file="free_email_provider_domains.txt")
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_8.csv")

    # 13. 批量计算志愿者Commits数量（2023-09-01 到 2023-12-31）
    volunteer_commit_counts = batch_calculate_volunteer_commits(repo_objects,
                                                                base_path="F:/github_repos",
                                                                start_date=START_DATE,
                                                                end_date=END_DATE,
                                                                common_domains_file="free_email_provider_domains.txt")
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_9.csv")

    # 14. 批量计算Review Ratio（2023-09-01 到 2023-12-31）
    review_ratios = batch_calculate_review_ratio(repo_objects,
                                                 base_path="F:/github_api_repos",
                                                 start_date=START_DATE,
                                                 end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_10.csv")

    # 15. 批量计算PR Merged Ratio（2023-09-01 到 2023-12-31）
    pr_merged_ratios = batch_calculate_pr_merged_ratio(repo_objects,
                                                       base_path="F:/github_api_repos",
                                                       start_date=START_DATE,
                                                       end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_11.csv")

    # 16. 批量计算PR Linked Ratio（2023-09-01 到 2023-12-31）
    pr_linked_ratios = batch_calculate_pr_linked_ratio(repo_objects,
                                                       base_path="F:/github_api_repos",
                                                       start_date=START_DATE,
                                                       end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_12.csv")

    # 17. 批量计算Contributor数量（2023-09-01 到 2023-12-31）
    contributor_counts = batch_calculate_contributor_count(repo_objects,
                                                           base_path_git="F:/github_repos",
                                                           base_path_api="F:/github_api_repos",
                                                           start_date=START_DATE,
                                                           end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_13.csv")

    # 18. 批量计算长期贡献者数量（截至2023-12-31）
    long_term_contributor_ratio = batch_calculate_long_term_contributors(repo_objects,
                                                                          base_path="F:/github_repos",
                                                                          start_date=START_DATE,
                                                                          check_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_14.csv")

    # 19. 批量计算Star数量（2023-09-01 到 2023-12-31）
    star_counts = batch_calculate_star_count(repo_objects,
                                             base_path="F:/github_api_repos",
                                             start_date=START_DATE,
                                             end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_15.csv")

    # 20. 批量计算Fork数量（2023-09-01 到 2023-12-31）
    fork_counts = batch_calculate_fork_count(repo_objects,
                                             base_path="F:/github_api_repos",
                                             start_date=START_DATE,
                                             end_date=END_DATE)
    # save_results_to_csv(repo_objects, output_file="repo_metrics_10000_16.csv")

    # 21. 保存详细结果
    save_results_to_csv(repo_objects, output_file="predictions_input.csv")
    print("✓ 所有处理完成！")