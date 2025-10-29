import os
from dotenv import load_dotenv
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_openai import ChatOpenAI
from typing import Tuple

load_dotenv()

# 初始化模型
# llm = ChatOpenAI(
#     model="deepseek/deepseek-r1-0528",
#     temperature=0.2,
#     top_p=0.4,
#     streaming=False,
#     openai_api_key=os.getenv("MY_API_KEY"),
#     openai_api_base=os.getenv("MY_URL"),
# )
llm = ChatOpenAI(
    model="openai/gpt-5",
    temperature=1,
    top_p=1,
    streaming=False,
    openai_api_key=os.getenv("MY_API_KEY"),
    openai_api_base=os.getenv("MY_URL"),
)
MAX_ROUND = 3

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
What: Summarize the metric’s key changes, such as growth/decline, trend direction, or anomalies. Describe possible situations or events based on the metric’s category (e.g., code quality, activity, engagement).
Why: Explain the potential reasons for these changes or events. Identify possible risks or underlying issues.
How: For negative or deteriorating indicators, propose actionable solutions or optimization measures. For positive or stable trends, suggest how to maintain or further improve performance.
4. Structure your output into the following sections:
    - Health Status Summary
    - Key Metric Interpretation
    - Conclusions and Recommendations
5. The table’s metric IDs correspond to the following names:
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
Summarize the OSS project’s overall health trajectory, highlight strengths, risks, and next-step actions.

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
- Must explicitly state the project’s overall health status (Healthy / Sub-healthy / Unhealthy).
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
            HumanMessage(content=f"Report content of round {round_idx}:\n{report}\nThe initial da   ta for report:\n{user_input}")
        ]
        response = self.llm.invoke(messages)
        review_text = response.content.strip()

        if "__PASS__" in review_text.upper():
            return True, "Report approved"
        else:
            return False, review_text


def main():
    reporter = Reporter(llm)
    reviewer = Reviewer(llm, max_rounds=MAX_ROUND)

    with open("paddle-lite.txt", "r", encoding="utf-8") as f:
        user_input = f.read().strip()

    report = reporter.generate_report(user_input)
    round_idx = 1
    feedbackTotal= ""

    while True:
        is_pass, feedback = reviewer.review_report(report, round_idx, user_input)
        print(f"\n=== Reviewer Round {round_idx} Feedback ===")
        print(feedback)
        feedbackTotal+= feedback+'/n'

        if is_pass:
            print("\nFinal report output:")
            print(report)
            break
        elif round_idx >= reviewer.max_rounds:
            print("\nMaximum revision attempts reached. Outputting the current report:")
            print(report)
            break
        else:
            # 将反馈返回给 Reporter，生成新版本报告
            report = reporter.generate_report(user_input + "\nPlease revise the report according to the following reviewer feedback:\n" + feedbackTotal)
            round_idx += 1


if __name__ == "__main__":
    main()
