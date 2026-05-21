"""
FastAPI 服务器 - OSS 健康报告生成
"""
from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from typing import Dict, Tuple
from datetime import datetime
import os
from dotenv import load_dotenv
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_openai import ChatOpenAI

load_dotenv()

# ==================== FastAPI 应用初始化 ====================
app = FastAPI(
    title="OSS Health Report API",
    description="开源软件健康报告生成 API",
    version="1.0.0"
)

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==================== LLM 初始化 ====================
llm = ChatOpenAI(
    model="openai/gpt-5",
    temperature=1,
    top_p=1,
    streaming=False,
    openai_api_key=os.getenv("MY_API_KEY"),
    openai_api_base=os.getenv("MY_URL"),
)

MAX_ROUND = 3


# ==================== Reporter 类 ====================
class Reporter:
    """负责根据输入生成报告"""
    systemPrompt = """
Role:
You are an expert in analyzing the health of open-source software projects.

Task:
I will provide you with historical metric data of a given open-source project. Based on this data, please perform the following tasks:
1. Determine the current health status of the project (Healthy / Sub-healthy / Unhealthy) and explain the reasoning.
2. Identify which metrics support your conclusion (e.g., number of contributors, commit frequency, issue closing rate, PR merging rate, etc.).
3. Analyze each key metric group (Software, Community, Market) using the What–Why–How analytical framework:
What: Summarize the metric's key changes, such as growth/decline, trend direction, or anomalies. Describe possible situations or events based on the metric's category (e.g., code quality, activity, engagement).
Why: Explain the potential reasons for these changes or events. Identify possible risks or underlying issues.
How: For negative or deteriorating indicators, propose actionable solutions or optimization measures. For positive or stable trends, suggest how to maintain or further improve performance.
4. Structure your output into the following sections:
    - Health Status Summary
    - Key Metric Interpretation
    - Conclusions and Recommendations
5. The table's metric IDs correspond to the following names:
   1.1.1 → technicaldebt
   1.1.2 → bugs
   1.1.3 → codesmells
   1.1.4 → duplicatedlinesdensity
   1.2.1 → complexity
   1.2.2 → cognitivecomplexity
   1.2.3 → vulnerabilities
   1.2.4 → commentlinesdensity
   1.3.1 → monthchangedcodes
   1.3.2 → monthissue
   1.3.3 → monthcommit
   1.3.4 → monthpr
   2.1.1 → monthorgcommits
   2.1.2 → monthorgentropy
   2.1.3 → monthvolunteerentropy
   2.1.4 → monthvolunteercommits
   2.2.1 → reviewratio
   2.2.2 → prmergedratio
   2.2.3 → prlinkedissue
   2.3.1 → codecontributorcount/total
   2.3.1.1 → codecommiter
   2.3.1.2 → prsubmitter
   2.3.1.3 → reviewer
   2.3.2 → longtermcontributors
   3.1.1 → [define as needed]
   3.2.1 → monthstar
   3.2.2 → monthfork
6. Replace metric IDs with their corresponding names in your reply.
7. Ignore the most recent month of data when making your evaluation.

Output Format (strictly follow):
Your response should strictly follow this structure:
- Health Status Summary
Overall classification (Healthy / Sub-healthy / Unhealthy) Key evidence and brief rationale
- Key Metric Interpretation (What–Why–How Analysis)
Example Format:
Metric: monthcommit (Community Activity)
What: Monthly commits increased by 25%, showing an upward trend since Q2.
Why: Possibly due to onboarding of new contributors and active PR reviews.
How: Maintain engagement by rewarding contributors and monitoring review backlog.
(Repeat for each major metric)
- Conclusions and Recommendations
Summarize the OSS project's overall health trajectory, highlight strengths, risks, and next-step actions.

Input:
The historical metric data of the open-source project will be provided.
    """

    def __init__(self, llm):
        self.llm = llm

    def generate_report(self, input_text: str) -> str:
        messages = [
            SystemMessage(content=self.systemPrompt),
            HumanMessage(content=input_text)
        ]
        response = self.llm.invoke(messages)
        return response.content.strip()


# ==================== Reviewer 类 ====================
class Reviewer:
    """负责审查 Reporter 生成的报告，判断是否合格"""
    systemPrompt = """
Role:
You are a strict and detail-oriented reviewer responsible for evaluating whether the generated OSS Health Report meets all specified requirements.

Evaluation Criteria:
You must thoroughly check whether the report strictly follows the required structure, logic, and content completeness defined below.
1. Overall Structure
The report must include three main sections, each clearly titled:
- Health Status Summary    
- Key Metric Interpretation (What–Why–How Analysis)
- Conclusions and Recommendations

2. Health Status Summary
- Must explicitly state the project's overall health status (Healthy / Sub-healthy / Unhealthy).
- Must include brief but clear reasoning and supporting evidence.
- The reasoning must refer to key metric categories.

3. Key Metric Interpretation (What–Why–How Analysis)
Each key metric discussed must have all three subcomponents:
- What: Describes metric change (increase/decrease/trend/anomaly) and relevant context.
- Why: Explains causes, potential risks, or underlying reasons.
- How: Suggests solutions for negative trends or maintenance measures for positive trends.

4. Conclusions and Recommendations
- Must summarize overall findings clearly (e.g., stability, improvement, or decline).
- Must give at least one actionable recommendation related to improving or maintaining OSS health.
- Should link back to earlier observations (coherence check).

5. Quality Requirements
- Language must be formal, analytical, and concise.
- Explanations must be data-driven and logically consistent.
- The output must not include extra sections or unrelated commentary.
- All data references must accurately correspond to the source data; avoid any mismatches or inconsistencies between analysis results and original data.

Your Judgment Rule:
If all the above conditions are met, reply only with: "__PASS__"
If any condition is not met, reply with specific and constructive revision suggestions, following this format:
The report does not meet the requirements.
Issues:
- [Describe what is missing or incorrect]
- [Describe another issue if applicable]
Suggested Revisions:
- [Concrete fix or instruction]
- [Concrete fix or instruction]
    """

    def __init__(self, llm, max_rounds=3):
        self.llm = llm
        self.max_rounds = max_rounds

    def review_report(self, report: str, round_idx: int, user_input: str) -> Tuple[bool, str]:
        messages = [
            SystemMessage(content=self.systemPrompt),
            HumanMessage(
                content=f"Report content of round {round_idx}:\n{report}\nThe initial data for report:\n{user_input}")
        ]
        response = self.llm.invoke(messages)
        review_text = response.content.strip()

        if "__PASS__" in review_text.upper():
            return True, "Report approved"
        else:
            return False, review_text


# ==================== API 接口 ====================

@app.get("/", tags=["Root"])
async def root():
    """根路径 - API 信息"""
    return {
        "message": "OSS Health Report API",
        "version": "1.0.0",
        "endpoints": {
            "report_generation_file": "/generate-report",
            "report_generation_text": "/generate-report-text",
            "docs": "/docs"
        }
    }


@app.post("/generate-report", tags=["Report"])
async def generate_report(file: UploadFile = File(...)):
    """
    生成 OSS 健康报告（文件上传方式）

    - **file**: 上传包含项目指标数据的文本文件（如 cann_total.txt）

    返回生成的健康报告及审查过程信息
    """
    try:
        # 1. 读取上传的文件内容
        content = await file.read()
        user_input = content.decode('utf-8').strip()

        if not user_input:
            raise HTTPException(status_code=400, detail="上传的文件内容为空")

        # 2. 初始化 Reporter 和 Reviewer
        reporter = Reporter(llm)
        reviewer = Reviewer(llm, max_rounds=MAX_ROUND)

        # 3. 生成初始报告
        report = reporter.generate_report(user_input)
        round_idx = 1
        feedback_history = []

        # 4. 迭代审查和修订
        while True:
            is_pass, feedback = reviewer.review_report(report, round_idx, user_input)

            feedback_history.append({
                "round": round_idx,
                "feedback": feedback,
                "passed": is_pass
            })

            if is_pass:
                # 审查通过
                return {
                    "status": "success",
                    "message": "报告生成成功",
                    "report": report,
                    "review_rounds": round_idx,
                    "feedback_history": feedback_history,
                    "timestamp": datetime.now().isoformat()
                }
            elif round_idx >= reviewer.max_rounds:
                # 达到最大修订次数
                return {
                    "status": "partial_success",
                    "message": f"已达到最大修订次数 ({MAX_ROUND})，返回当前版本报告",
                    "report": report,
                    "review_rounds": round_idx,
                    "feedback_history": feedback_history,
                    "warning": "报告可能未完全符合所有要求",
                    "timestamp": datetime.now().isoformat()
                }
            else:
                # 继续修订
                feedback_total = "\n".join([f"Round {fb['round']}: {fb['feedback']}"
                                            for fb in feedback_history])
                report = reporter.generate_report(
                    user_input + "\nPlease revise the report according to the following reviewer feedback:\n" + feedback_total
                )
                round_idx += 1

    except UnicodeDecodeError:
        raise HTTPException(status_code=400, detail="文件编码错误，请确保文件为 UTF-8 编码")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"报告生成失败: {str(e)}")


@app.post("/generate-report-text", tags=["Report"])
async def generate_report_from_text(data: Dict[str, str]):
    """
    从文本内容生成 OSS 健康报告（不需要上传文件）

    - **text**: 项目指标数据的文本内容

    返回生成的健康报告及审查过程信息
    """
    try:
        user_input = data.get("text", "").strip()

        if not user_input:
            raise HTTPException(status_code=400, detail="文本内容为空")

        # 初始化 Reporter 和 Reviewer
        reporter = Reporter(llm)
        reviewer = Reviewer(llm, max_rounds=MAX_ROUND)

        # 生成初始报告
        report = reporter.generate_report(user_input)
        round_idx = 1
        feedback_history = []

        # 迭代审查和修订
        while True:
            is_pass, feedback = reviewer.review_report(report, round_idx, user_input)

            feedback_history.append({
                "round": round_idx,
                "feedback": feedback,
                "passed": is_pass
            })

            if is_pass:
                return {
                    "status": "success",
                    "message": "报告生成成功",
                    "report": report,
                    "review_rounds": round_idx,
                    "feedback_history": feedback_history,
                    "timestamp": datetime.now().isoformat()
                }
            elif round_idx >= reviewer.max_rounds:
                return {
                    "status": "partial_success",
                    "message": f"已达到最大修订次数 ({MAX_ROUND})，返回当前版本报告",
                    "report": report,
                    "review_rounds": round_idx,
                    "feedback_history": feedback_history,
                    "warning": "报告可能未完全符合所有要求",
                    "timestamp": datetime.now().isoformat()
                }
            else:
                feedback_total = "\n".join([f"Round {fb['round']}: {fb['feedback']}"
                                            for fb in feedback_history])
                report = reporter.generate_report(
                    user_input + "\nPlease revise the report according to the following reviewer feedback:\n" + feedback_total
                )
                round_idx += 1

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"报告生成失败: {str(e)}")


# ==================== 启动服务器 ====================

if __name__ == "__main__":
    import uvicorn

    print("=" * 60)
    print("🚀 启动 OSS 健康报告生成 API 服务器")
    print("=" * 60)
    print(f"📍 服务地址: http://localhost:8092")
    print(f"📚 API 文档: http://localhost:8092/docs")
    print("=" * 60)

    uvicorn.run(app, host="0.0.0.0", port=8092)
