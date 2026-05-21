"""
OSS 健康报告 API 测试脚本
测试文件上传和文本输入两种方式
"""
import requests
import json
from pathlib import Path


class ReportAPITester:
    """报告生成 API 测试类"""

    def __init__(self, base_url="http://localhost:8092"):
        self.base_url = base_url
        self.session = requests.Session()

    def test_root(self):
        """测试根路径"""
        print("\n" + "="*60)
        print("🧪 测试 1: 根路径")
        print("="*60)

        try:
            response = self.session.get(f"{self.base_url}/")
            print(f"✅ 状态码: {response.status_code}")
            print(f"📄 响应内容:")
            print(json.dumps(response.json(), indent=2, ensure_ascii=False))
            return response.status_code == 200
        except Exception as e:
            print(f"❌ 错误: {str(e)}")
            return False

    def test_generate_report_file(self, file_path):
        """测试文件上传方式生成报告"""
        print("\n" + "="*60)
        print("🧪 测试 2: 文件上传生成报告")
        print("="*60)

        try:
            # 检查文件是否存在
            if not Path(file_path).exists():
                print(f"❌ 文件不存在: {file_path}")
                return False

            print(f"📁 上传文件: {file_path}")

            # 打开文件并上传
            with open(file_path, 'rb') as f:
                files = {'file': (Path(file_path).name, f, 'text/plain')}
                response = self.session.post(
                    f"{self.base_url}/generate-report",
                    files=files,
                    timeout=300  # 5分钟超时
                )

            print(f"✅ 状态码: {response.status_code}")

            if response.status_code == 200:
                result = response.json()
                print(f"\n📊 报告生成结果:")
                print(f"  - 状态: {result.get('status')}")
                print(f"  - 消息: {result.get('message')}")
                print(f"  - 审查轮次: {result.get('review_rounds')}")
                print(f"  - 时间戳: {result.get('timestamp')}")

                # 显示审查历史
                print(f"\n📝 审查历史:")
                for feedback in result.get('feedback_history', []):
                    print(f"  Round {feedback['round']}: {'✅ 通过' if feedback['passed'] else '❌ 需修订'}")
                    if not feedback['passed']:
                        print(f"    反馈: {feedback['feedback'][:100]}...")

                # 保存报告到文件
                report_file = f"report_{Path(file_path).stem}.txt"
                with open(report_file, 'w', encoding='utf-8') as f:
                    f.write(result.get('report', ''))
                print(f"\n💾 报告已保存到: {report_file}")

                # 显示报告前500字符
                print(f"\n📄 报告内容预览:")
                print("-" * 60)
                print(result.get('report', '')[:500])
                print("...")
                print("-" * 60)

                return True
            else:
                print(f"❌ 请求失败: {response.text}")
                return False

        except requests.exceptions.Timeout:
            print("❌ 请求超时（超过5分钟）")
            return False
        except Exception as e:
            print(f"❌ 错误: {str(e)}")
            return False

    def test_generate_report_text(self, file_path):
        """测试文本输入方式生成报告"""
        print("\n" + "="*60)
        print("🧪 测试 3: 文本输入生成报告")
        print("="*60)

        try:
            # 检查文件是否存在
            if not Path(file_path).exists():
                print(f"❌ 文件不存在: {file_path}")
                return False

            # 读取文件内容
            with open(file_path, 'r', encoding='utf-8') as f:
                text_content = f.read()

            print(f"📝 发送文本内容 (长度: {len(text_content)} 字符)")

            # 发送 POST 请求
            response = self.session.post(
                f"{self.base_url}/generate-report-text",
                json={"text": text_content},
                timeout=300  # 5分钟超时
            )

            print(f"✅ 状态码: {response.status_code}")

            if response.status_code == 200:
                result = response.json()
                print(f"\n📊 报告生成结果:")
                print(f"  - 状态: {result.get('status')}")
                print(f"  - 消息: {result.get('message')}")
                print(f"  - 审查轮次: {result.get('review_rounds')}")
                print(f"  - 时间戳: {result.get('timestamp')}")

                # 显示审查历史
                print(f"\n📝 审查历史:")
                for feedback in result.get('feedback_history', []):
                    print(f"  Round {feedback['round']}: {'✅ 通过' if feedback['passed'] else '❌ 需修订'}")

                # 保存报告到文件
                report_file = f"report_text_{Path(file_path).stem}.txt"
                with open(report_file, 'w', encoding='utf-8') as f:
                    f.write(result.get('report', ''))
                print(f"\n💾 报告已保存到: {report_file}")

                # 显示报告前500字符
                print(f"\n📄 报告内容预览:")
                print("-" * 60)
                print(result.get('report', '')[:500])
                print("...")
                print("-" * 60)

                return True
            else:
                print(f"❌ 请求失败: {response.text}")
                return False

        except requests.exceptions.Timeout:
            print("❌ 请求超时（超过5分钟）")
            return False
        except Exception as e:
            print(f"❌ 错误: {str(e)}")
            return False

    def run_all_tests(self, file_path="cann_total.txt"):
        """运行所有测试"""
        print("\n" + "🚀" * 30)
        print("开始测试 OSS 健康报告 API")
        print("🚀" * 30)

        results = {
            "root": self.test_root(),
            "file_upload": self.test_generate_report_file(file_path),
            "text_input": self.test_generate_report_text(file_path)
        }

        # 测试总结
        print("\n" + "="*60)
        print("📊 测试总结")
        print("="*60)
        for test_name, result in results.items():
            status = "✅ 通过" if result else "❌ 失败"
            print(f"{test_name}: {status}")

        total = len(results)
        passed = sum(results.values())
        print(f"\n总计: {passed}/{total} 测试通过")
        print("="*60)

        return all(results.values())


def main():
    """主函数"""
    # 配置
    API_URL = "http://localhost:8092"
    TEST_FILE = "cann_total.txt"  # 你的测试文件路径

    # 创建测试器
    tester = ReportAPITester(base_url=API_URL)

    # 运行所有测试
    success = tester.run_all_tests(file_path=TEST_FILE)

    if success:
        print("\n🎉 所有测试通过！")
    else:
        print("\n⚠️ 部分测试失败，请检查日志")


if __name__ == "__main__":
    main()
