# api_xgboost.py
from fastapi import FastAPI, HTTPException, File, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Tuple
import joblib
import pandas as pd
import numpy as np
import json
import os
from datetime import datetime
import io

# =========================
# 配置参数
# =========================
MODEL_DIR = "saved_models"
MODEL_NAME = "xgboost_survival_model"
LATEST_MODEL_PATH = os.path.join(MODEL_DIR, f"{MODEL_NAME}_latest.pkl")
LATEST_FEATURES_PATH = os.path.join(MODEL_DIR, f"{MODEL_NAME}_latest_features.json")
LATEST_INFO_PATH = os.path.join(MODEL_DIR, f"{MODEL_NAME}_latest_info.json")

# =========================
# 初始化 FastAPI
# =========================
app = FastAPI(
    title="XGBoost Survival Prediction API",
    description="Repository survival prediction service using XGBoost",
    version="1.0.0"
)

# =========================
# 全局变量
# =========================
model = None
feature_names = None
model_info = None


# =========================
# 数据模型定义
# =========================
class PredictionRequest(BaseModel):
    """单个预测请求"""
    features: Dict[str, float] = Field(
        ...,
        description="特征字典，key为特征名，value为特征值（缺失特征将自动填充为0）",
        example={
            "stars": 100,
            "forks": 50,
            "commits": 200,
            "contributors": 10
        }
    )


class BatchPredictionRequest(BaseModel):
    """批量预测请求"""
    data: List[Dict[str, float]] = Field(
        ...,
        description="特征字典列表（缺失特征将自动填充为0）",
        example=[
            {"stars": 100, "forks": 50, "commits": 200},
            {"stars": 200, "forks": 100, "commits": 400}
        ]
    )


class PredictionResponse(BaseModel):
    """预测响应"""
    prediction: int = Field(..., description="预测类别 (0或1)")
    probability: float = Field(..., description="预测为1的概率")
    confidence: float = Field(..., description="预测置信度")
    label: str = Field(..., description="预测标签")


class BatchPredictionResponse(BaseModel):
    """批量预测响应"""
    predictions: List[PredictionResponse]
    total_count: int
    timestamp: str


class ModelInfoResponse(BaseModel):
    """模型信息响应"""
    model_type: str
    timestamp: str
    n_features: int
    feature_names: List[str]
    metrics: Dict
    hyperparameters: Dict


class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str
    model_loaded: bool
    model_path: str
    timestamp: str


# =========================
# 启动事件：加载模型
# =========================
@app.on_event("startup")
async def load_model_on_startup():
    """应用启动时加载模型"""
    global model, feature_names, model_info

    try:
        print("🚀 Loading model on startup...")

        # 检查模型文件是否存在
        if not os.path.exists(LATEST_MODEL_PATH):
            raise FileNotFoundError(f"Model file not found: {LATEST_MODEL_PATH}")

        # 加载模型
        model = joblib.load(LATEST_MODEL_PATH)
        print(f"✅ Model loaded from: {LATEST_MODEL_PATH}")

        # 加载特征名称
        if os.path.exists(LATEST_FEATURES_PATH):
            with open(LATEST_FEATURES_PATH, 'r') as f:
                feature_names = json.load(f)
            print(f"✅ Feature names loaded: {len(feature_names)} features")

        # 加载模型信息
        if os.path.exists(LATEST_INFO_PATH):
            with open(LATEST_INFO_PATH, 'r') as f:
                model_info = json.load(f)
            print(f"✅ Model info loaded")

        print("✅ Model initialization completed!")

    except Exception as e:
        print(f"❌ Error loading model: {str(e)}")
        raise


# =========================
# 辅助函数
# =========================
def validate_features(features: Dict[str, float]) -> Tuple[pd.DataFrame, List[str]]:
    """
    验证并转换特征为DataFrame，缺失特征自动填充为0

    Args:
        features: 特征字典

    Returns:
        (DataFrame格式的特征, 缺失特征列表)
    """
    if feature_names is None:
        raise HTTPException(status_code=500, detail="Feature names not loaded")

    # 检查多余特征（仍然报错，因为这可能是用户输入错误）
    extra_features = set(features.keys()) - set(feature_names)
    if extra_features:
        raise HTTPException(
            status_code=400,
            detail=f"Unknown features: {list(extra_features)}"
        )

    # 创建完整的特征字典，缺失的特征填充为0
    ordered_features = {}
    missing_features = []

    for name in feature_names:
        if name in features:
            ordered_features[name] = features[name]
        else:
            ordered_features[name] = 0.0  # 缺失特征填充为0
            missing_features.append(name)

    # 记录缺失特征（用于日志）
    if missing_features:
        print(f"⚠️  Missing features filled with 0: {missing_features}")

    # 转换为DataFrame
    df = pd.DataFrame([ordered_features])

    return df, missing_features


def make_prediction(features_df: pd.DataFrame) -> Dict:
    """
    执行预测

    Args:
        features_df: 特征DataFrame

    Returns:
        预测结果字典
    """
    if model is None:
        raise HTTPException(status_code=500, detail="Model not loaded")

    # 预测
    prediction = int(model.predict(features_df)[0])
    probability = float(model.predict_proba(features_df)[0, 1])

    # 计算置信度（概率距离0.5的距离）
    confidence = abs(probability - 0.5) * 2

    # 标签
    label = "Survived" if prediction == 1 else "Not Survived"

    return {
        "prediction": prediction,
        "probability": round(probability, 4),
        "confidence": round(confidence, 4),
        "label": label
    }


# =========================
# API 端点
# =========================

@app.get("/", tags=["Root"])
async def root():
    """根路径"""
    return {
        "message": "XGBoost Survival Prediction API",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
        "test": "/test"
    }


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """健康检查"""
    return {
        "status": "healthy" if model is not None else "unhealthy",
        "model_loaded": model is not None,
        "model_path": LATEST_MODEL_PATH,
        "timestamp": datetime.now().isoformat()
    }


@app.get("/model/info", response_model=ModelInfoResponse, tags=["Model"])
async def get_model_info():
    """获取模型信息"""
    if model_info is None:
        raise HTTPException(status_code=500, detail="Model info not loaded")

    return model_info


@app.get("/model/features", tags=["Model"])
async def get_features():
    """获取特征列表"""
    if feature_names is None:
        raise HTTPException(status_code=500, detail="Feature names not loaded")

    return {
        "features": feature_names,
        "count": len(feature_names)
    }


@app.post("/predict", tags=["Prediction"])
async def predict(request: PredictionRequest):
    """
    单个样本预测

    - **features**: 特征字典，包含所有必需的特征（缺失特征将自动填充为0）
    """
    try:
        # 验证并转换特征
        features_df, missing_features = validate_features(request.features)

        # 执行预测
        result = make_prediction(features_df)

        # 添加缺失特征信息
        if missing_features:
            result["warning"] = f"Missing features filled with 0: {missing_features}"
            result["missing_features"] = missing_features

        return result

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")


@app.post("/predict/batch", tags=["Prediction"])
async def predict_batch(request: BatchPredictionRequest):
    """
    批量预测

    - **data**: 特征字典列表（缺失特征将自动填充为0）
    """
    try:
        predictions = []
        all_missing_features = []

        for idx, features in enumerate(request.data):
            try:
                # 验证并转换特征
                features_df, missing_features = validate_features(features)

                # 执行预测
                result = make_prediction(features_df)

                # 添加样本索引和缺失特征信息
                result["sample_index"] = idx
                if missing_features:
                    result["missing_features"] = missing_features
                    all_missing_features.append({
                        "sample_index": idx,
                        "missing_features": missing_features
                    })

                predictions.append(result)

            except Exception as e:
                raise HTTPException(
                    status_code=400,
                    detail=f"Error in sample {idx}: {str(e)}"
                )

        response = {
            "predictions": predictions,
            "total_count": len(predictions),
            "timestamp": datetime.now().isoformat()
        }

        # 如果有缺失特征，添加汇总信息
        if all_missing_features:
            response["warning"] = "Some samples had missing features filled with 0"
            response["samples_with_missing_features"] = all_missing_features

        return response

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch prediction error: {str(e)}")


@app.post("/predict/csv", tags=["Prediction"])
async def predict_csv(file: UploadFile = File(...)):
    """
    CSV文件批量预测

    - **file**: CSV文件，包含所有必需的特征列（缺失列将自动填充为0）
    """
    try:
        # 读取CSV文件
        contents = await file.read()
        df = pd.read_csv(io.StringIO(contents.decode('utf-8')))

        # 检查特征列
        if feature_names is None:
            raise HTTPException(status_code=500, detail="Feature names not loaded")

        # 找出缺失的列
        missing_cols = set(feature_names) - set(df.columns)

        # 为缺失的列添加默认值0
        for col in missing_cols:
            df[col] = 0.0
            print(f"⚠️  Missing column '{col}' filled with 0")

        # 选择并排序特征列
        X = df[feature_names]

        # 批量预测
        predictions = model.predict(X)
        probabilities = model.predict_proba(X)[:, 1]

        # 构建结果
        results = []
        for i in range(len(predictions)):
            pred = int(predictions[i])
            prob = float(probabilities[i])
            conf = abs(prob - 0.5) * 2

            results.append({
                "row_index": i,
                "prediction": pred,
                "probability": round(prob, 4),
                "confidence": round(conf, 4),
                "label": "Survived" if pred == 1 else "Not Survived"
            })

        response = {
            "predictions": results,
            "total_count": len(results),
            "timestamp": datetime.now().isoformat()
        }

        # 如果有缺失列，添加警告信息
        if missing_cols:
            response["warning"] = f"Missing columns filled with 0: {list(missing_cols)}"
            response["missing_columns"] = list(missing_cols)

        return response

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"CSV prediction error: {str(e)}")


@app.get("/test", tags=["Test"])
async def test_endpoint():
    """
    测试接口 - 返回固定的测试数据和预测结果

    用于快速验证API功能是否正常
    """
    try:
        # 固定的测试数据
        test_payload = {
            "features": {
                "code_changes": 131571,
                "issue_count": 7,
                "commit_count": 153,
                "pr_count": 16,
                "org_commits": 52,
                "org_entropy": 0.139232999,
                "volunteer_entropy": 2.67517743,
                "volunteer_commits": 101,
                "review_ratio": 0.75,
                "pr_merged_ratio": 0.625,
                "pr_linked_ratio": 0,
                "contributor_count": 12,
                "star_count": 32,
                "fork_count": 14,
                "long_term_contributors_active": 0
            }
        }

        # 验证并转换特征
        features_df, missing_features = validate_features(test_payload["features"])

        # 执行预测
        prediction_result = make_prediction(features_df)

        # 返回完整的测试结果
        response = {
            "status": "success",
            "message": "Test endpoint - Fixed test data",
            "test_data": test_payload,
            "prediction_result": prediction_result,
            "timestamp": datetime.now().isoformat(),
            "model_info": {
                "model_loaded": model is not None,
                "feature_count": len(feature_names) if feature_names else 0
            }
        }

        if missing_features:
            response["warning"] = f"Missing features filled with 0: {missing_features}"

        return response

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Test endpoint error: {str(e)}"
        )


@app.post("/model/reload", tags=["Model"])
async def reload_model():
    """重新加载模型"""
    global model, feature_names, model_info

    try:
        # 重新加载模型
        model = joblib.load(LATEST_MODEL_PATH)

        # 重新加载特征名称
        if os.path.exists(LATEST_FEATURES_PATH):
            with open(LATEST_FEATURES_PATH, 'r') as f:
                feature_names = json.load(f)

        # 重新加载模型信息
        if os.path.exists(LATEST_INFO_PATH):
            with open(LATEST_INFO_PATH, 'r') as f:
                model_info = json.load(f)

        return {
            "status": "success",
            "message": "Model reloaded successfully",
            "timestamp": datetime.now().isoformat()
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Reload error: {str(e)}")


# =========================
# 运行服务器
# =========================
if __name__ == "__main__":
    import uvicorn

    print("=" * 60)
    print("🚀 Starting XGBoost Survival Prediction API Server")
    print("=" * 60)
    print(f"📁 Model directory: {MODEL_DIR}")
    print(f"📦 Model file: {LATEST_MODEL_PATH}")
    print("=" * 60)

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8091,
        log_level="info"
    )
