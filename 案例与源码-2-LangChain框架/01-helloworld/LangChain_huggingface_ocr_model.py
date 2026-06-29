"""
【案例】HuggingFace 非聊天模型的使用方式（图像分类 & 多行 OCR）

核心概念：聊天模型 vs 非聊天模型

┌─────────────────────┬────────────────────────────┬────────────────────────────────┐
│                     │ 聊天模型（Chat Model）        │ 非聊天模型（Non-Chat Model）    │
├─────────────────────┼────────────────────────────┼────────────────────────────────┤
│ 输入                │ 文字对话                     │ 图像、音频、文档等               │
│ 输出                │ 文字回复                     │ 分类标签、提取文字、坐标等         │
│ LangChain 支持      │ ✅ init_chat_model 直接用    │ ❌ 不支持，需绕过 LangChain       │
│ 例子                │ Qwen、GPT-4、Claude          │ google/vit、trocr、GOT-OCR2     │
└─────────────────────┴────────────────────────────┴────────────────────────────────┘

【两种调用方式的区别】

  方式 A：pipeline()    ← transformers 封装好的便利接口
  ─────────────────────────────────────────────────────
  适合任务类型明确的"标准"模型，如：
    image-classification（图像分类）
    text-generation（文字生成）
    automatic-speech-recognition（语音识别）

  用法：pipeline("任务类型", model="模型ID")
  优点：一行代码，自动处理预处理/后处理

  ─────────────────────────────────────────────────────
  方式 B：AutoProcessor + AutoModel    ← 手动加载处理器和模型
  ─────────────────────────────────────────────────────
  适合"非标准"或结构复杂的模型，如：
    GOT-OCR-2.0-hf（图像→多行文字，不是单一任务类型）
    TrOCR（图像→文字，有自己专属的 TrOCRProcessor）
    VQA 视觉问答等模型

  为什么不能用 pipeline()？
    pipeline() 依赖固定的"任务类型"入口。
    GOT-OCR-2.0-hf 使用自定义 chat template，
    必须手动调用 processor 和 model.generate() 才能正确驱动。

How to run?
$ python3 案例与源码-2-LangChain框架/01-helloworld/LangChain_huggingface_ocr_model.py
"""

import os
import torch
from dotenv import load_dotenv
from PIL import Image, ImageDraw

load_dotenv(encoding="utf-8")

# ==============================================================
# 示例 A：pipeline() 方式 — 图像分类（google/vit-base-patch16-224）
# ==============================================================
# pipeline("image-classification") 是 transformers 提供的便利接口。
# 只需指定任务类型 + 模型名，它会自动：
#   1. 下载并缓存模型权重
#   2. 处理图像（resize、normalize 等）
#   3. 返回分类标签 + 置信度分数
#
# google/vit-base-patch16-224：~330MB，识别 1000 种 ImageNet 类别
# ==============================================================

print("=" * 60)
print("示例 A：pipeline() 图像分类")
print("=" * 60)

from transformers import pipeline

# 创建图像分类 pipeline
# 等价于：自动加载 ViTFeatureExtractor + ViTForImageClassification
classifier = pipeline(
    "image-classification",
    model="google/vit-base-patch16-224",
)

# 生成一张测试图片（橙色背景，代表"纯色"）
test_img = Image.new("RGB", (224, 224), color=(255, 165, 0))

# 推理：返回 top-5 分类结果
results = classifier(test_img, top_k=3)
print("分类结果（置信度从高到低）：")
for r in results:
    print(f"  {r['label']:30s}  {r['score']:.4f}")

# ==============================================================
# 示例 B：AutoProcessor + AutoModel 方式 — 多行 OCR（GOT-OCR-2.0-hf）
# ==============================================================
# 为什么不用 pipeline()？
#   GOT-OCR-2.0-hf 使用了自定义的 chat template 和特殊推理逻辑，
#   transformers 的标准 pipeline 无法正确驱动它（会报 chat template 错误）。
#   只能手动：
#     1. processor = AutoProcessor（负责把图像转成模型输入格式）
#     2. model     = AutoModelForImageTextToText（图像 → 文字生成）
#     3. 手动调用 model.generate() + processor.decode()
#
# AutoProcessor / AutoModel 的"Auto"：
#   这是 transformers 的"自动工厂"类，会根据模型 ID 自动选择对应的
#   具体 Processor / Model 类，不需要你手动查文档去选哪个类。
#
# stepfun-ai/GOT-OCR-2.0-hf：约 1 GB
#   支持多行文字、整页文档 OCR，比 TrOCR 功能强得多
# ==============================================================

print("\n" + "=" * 60)
print("示例 B：AutoProcessor + AutoModel 多行 OCR")
print("=" * 60)

from transformers import AutoProcessor, AutoModelForImageTextToText

print("正在加载 GOT-OCR2 模型（stepfun-ai/GOT-OCR-2.0-hf，约 1 GB）...")
processor = AutoProcessor.from_pretrained("stepfun-ai/GOT-OCR-2.0-hf")
model = AutoModelForImageTextToText.from_pretrained(
    "stepfun-ai/GOT-OCR-2.0-hf",
    torch_dtype=torch.float32,  # 用 float32 兼容 CPU 和 MPS；CUDA 可改 float16
)

# ---- 准备图像 ----
# 方式 A：你自己的本地图片
IMAGE_PATH = "/Users/rading/Desktop/Screenshot 2026-06-25 at 2.44.47 PM.png"
image = Image.open(IMAGE_PATH).convert("RGB")

# 方式 B：动态生成多行文字图片（验证多行 OCR 是否正常工作）
# image = Image.new("RGB", (400, 120), color="white")
# draw = ImageDraw.Draw(image)
# draw.text((10, 10), "LangChain 2025", fill="black")
# draw.text((10, 40), "HuggingFace OCR", fill="black")
# draw.text((10, 70), "Multi-line test", fill="black")

# ---- 运行 OCR ----
print("识别图中所有文字（多行）...\n")
inputs = processor(image, return_tensors="pt")
generated_ids = model.generate(**inputs, max_new_tokens=200)
raw_output = processor.decode(generated_ids[0], skip_special_tokens=True)

# 模型输出包含 chat 模板头部，提取 "assistant\n" 之后的实际内容
extracted_text = raw_output.split("assistant\n")[-1].strip()

print("【OCR 提取结果（多行）】")
print(extracted_text)

"""
【输出示例】
示例 A：pipeline() 图像分类
分类结果（置信度从高到低）：
  web site                         0.0623
  orange                           0.0412
  apricot                          0.0398

示例 B：AutoProcessor + AutoModel 多行 OCR
【OCR 提取结果（多行）】
LangChain 2025
HuggingFace OCR
Multi-line test
"""
