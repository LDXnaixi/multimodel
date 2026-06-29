export interface MetricOption {
  id: string
  label: string
  description: string
  defaultEnabled?: boolean
}

const performance: MetricOption[] = [
  { id: 'Latency', label: '推理延迟', description: '完成一次或一批推理所需时间。' },
  { id: 'Throughput', label: '吞吐量', description: '单位时间内完成的样本数量。' }
]

const detectionBase: MetricOption[] = [
  { id: 'mAP50-95', label: 'mAP50-95', description: 'IoU 0.50 至 0.95 范围的平均精度均值。', defaultEnabled: true },
  { id: 'mAP50', label: 'mAP50', description: 'IoU 阈值为 0.50 时的平均精度均值。', defaultEnabled: true },
  { id: 'mAP75', label: 'mAP75', description: 'IoU 阈值为 0.75 时的平均精度均值。' },
  { id: 'Precision', label: 'Precision', description: '预测为正的结果中真正例的比例。', defaultEnabled: true },
  { id: 'Recall', label: 'Recall', description: '全部真实正例中被正确检出的比例。', defaultEnabled: true },
  { id: 'F1-Score', label: 'F1', description: 'Precision 与 Recall 的调和平均。' },
  { id: 'IoU', label: 'IoU', description: '预测区域与真实区域的交并比。' },
  { id: 'TP', label: 'TP', description: '正确检出的目标数量。' },
  { id: 'FP', label: 'FP', description: '错误检出的目标数量。' },
  { id: 'FN', label: 'FN', description: '未被检出的真实目标数量。' },
  { id: 'DetectionCount', label: '检出数量', description: '模型输出的有效目标总数。' },
  { id: 'FPS', label: 'FPS', description: '每秒可处理的图像帧数。' },
  ...performance
]

const classification: MetricOption[] = [
  { id: 'Top1-Accuracy', label: 'Top-1 Accuracy', description: '最高分预测与真实类别一致的比例。', defaultEnabled: true },
  { id: 'Top5-Accuracy', label: 'Top-5 Accuracy', description: '真实类别位于前五个预测中的比例。', defaultEnabled: true },
  { id: 'Accuracy', label: 'Accuracy', description: '全部样本中分类正确的比例。' },
  { id: 'Balanced-Accuracy', label: 'Balanced Accuracy', description: '各类别召回率的平均值。' },
  { id: 'Precision-Macro', label: 'Macro Precision', description: '逐类别 Precision 的宏平均。' },
  { id: 'Recall-Macro', label: 'Macro Recall', description: '逐类别 Recall 的宏平均。' },
  { id: 'F1-Macro', label: 'Macro F1', description: '逐类别 F1 的宏平均。', defaultEnabled: true },
  { id: 'F1-Micro', label: 'Micro F1', description: '汇总所有类别计数后计算的 F1。' },
  { id: 'F1-Weighted', label: 'Weighted F1', description: '按各类别样本数量加权的 F1。' },
  { id: 'AUROC', label: 'AUROC', description: 'ROC 曲线下面积，衡量跨阈值区分能力。' },
  { id: 'Average-Precision', label: 'Average Precision', description: 'Precision-Recall 曲线的汇总指标。' },
  { id: 'MCC', label: 'MCC', description: 'Matthews 相关系数，适合类别不平衡场景。' },
  { id: 'Cohen-Kappa', label: 'Cohen Kappa', description: '扣除随机一致性后的分类一致程度。' },
  { id: 'Specificity', label: 'Specificity', description: '真实负例被正确识别的比例。' },
  { id: 'ECE', label: '校准误差 ECE', description: '预测置信度与实际正确率之间的偏差。' },
  { id: 'Log-Loss', label: 'Log Loss', description: '基于预测概率的对数损失，越低越好。' },
  ...performance
]

const catalogs: Record<string, MetricOption[]> = {
  YOLO_DETECT: detectionBase,
  YOLO_POSE: [
    { id: 'Box-mAP50-95', label: 'Box mAP50-95', description: '目标框在 IoU 0.50 至 0.95 下的 mAP。' },
    { id: 'Pose-mAP50-95', label: 'Pose mAP50-95', description: '关键点姿态在 OKS 阈值范围内的 mAP。', defaultEnabled: true },
    { id: 'Pose-mAP50', label: 'Pose mAP50', description: '关键点姿态在 OKS 0.50 下的 mAP。' },
    { id: 'OKS', label: 'OKS', description: '考虑目标尺度和关键点可见性的关键点相似度。' },
    { id: 'PCK', label: 'PCK', description: '落在归一化距离阈值内的正确关键点比例。' },
    ...detectionBase
  ],
  YOLO_SEGMENT: [
    { id: 'Box-mAP50-95', label: 'Box mAP50-95', description: '实例外接框的 COCO 风格 mAP。' },
    { id: 'Mask-mAP50-95', label: 'Mask mAP50-95', description: '实例掩膜在 IoU 0.50 至 0.95 下的 mAP。', defaultEnabled: true },
    { id: 'Mask-mAP50', label: 'Mask mAP50', description: '实例掩膜在 IoU 0.50 下的 mAP。' },
    { id: 'Mask-IoU', label: 'Mask IoU', description: '预测掩膜与真实掩膜的交并比。' },
    { id: 'Dice', label: 'Dice', description: '预测掩膜与真实掩膜的 Dice 重叠系数。' },
    ...detectionBase
  ],
  YOLO_OBB: [
    { id: 'Rotated-mAP50-95', label: '旋转框 mAP50-95', description: '旋转框在多个 IoU 阈值下的 mAP。', defaultEnabled: true },
    { id: 'Rotated-IoU', label: '旋转框 IoU', description: '预测旋转框与真实旋转框的交并比。' },
    ...detectionBase
  ],
  YOLO_CLASSIFY: classification,
  TORCHVISION_CLASSIFY: classification,
  PADDLEOCR_DB_DET: [
    { id: 'Detection-Hmean', label: 'Detection Hmean', description: '文本检测 Precision 与 Recall 的调和平均。', defaultEnabled: true },
    { id: 'Precision', label: 'Precision', description: '检出文本区域中正确区域的比例。', defaultEnabled: true },
    { id: 'Recall', label: 'Recall', description: '真实文本区域中被正确检出的比例。', defaultEnabled: true },
    { id: 'Polygon-IoU', label: '多边形 IoU', description: '检测多边形与真实多边形的交并比。' },
    { id: 'FPS', label: 'FPS', description: '每秒处理图像数量。' },
    ...performance
  ],
  PADDLEOCR_CRNN_REC: [
    { id: 'RecognitionRate', label: '识别准确率', description: '文本行被完整正确识别的比例。', defaultEnabled: true },
    { id: 'RejectionRate', label: '拒识率', description: '置信度低于拒识阈值的样本比例。', defaultEnabled: true },
    { id: 'Exact-Match', label: 'Exact Match', description: '预测文本与参考文本完全一致的比例。' },
    { id: 'CER', label: 'CER', description: '字符级替换、删除和插入错误率。', defaultEnabled: true },
    { id: 'WER', label: 'WER', description: '词级替换、删除和插入错误率。' },
    { id: 'Normalized-Edit-Distance', label: '归一化编辑距离', description: '按文本长度归一化的编辑距离。' },
    { id: 'Average-Confidence', label: '平均置信度', description: '全部识别结果置信度的平均值。' },
    ...performance
  ],
  TRANSFORMERS_NLP: [
    { id: 'Accuracy', label: 'Accuracy', description: '语义分类正确样本比例。', defaultEnabled: true },
    { id: 'Precision-Macro', label: 'Macro Precision', description: '各语义类别 Precision 的宏平均。', defaultEnabled: true },
    { id: 'Recall-Macro', label: 'Macro Recall', description: '各语义类别 Recall 的宏平均。', defaultEnabled: true },
    { id: 'F1-Macro', label: 'Macro F1', description: '各语义类别 F1 的宏平均。', defaultEnabled: true },
    { id: 'F1-Micro', label: 'Micro F1', description: '汇总全部类别后计算的 F1。' },
    { id: 'F1-Weighted', label: 'Weighted F1', description: '按类别样本数加权的 F1。' },
    { id: 'AUROC', label: 'AUROC', description: '跨分类阈值的区分能力。' },
    { id: 'Average-Precision', label: 'Average Precision', description: 'Precision-Recall 曲线汇总值。' },
    { id: 'MCC', label: 'MCC', description: '适用于不平衡分类的 Matthews 相关系数。' },
    { id: 'Cohen-Kappa', label: 'Cohen Kappa', description: '预测与标注扣除随机一致性后的吻合度。' },
    { id: 'Exact-Match', label: 'Exact Match', description: '预测标签或答案与参考结果完全一致的比例。' },
    { id: 'Perplexity', label: 'Perplexity', description: '语言模型对样本的不确定程度，越低越好。' },
    ...performance
  ],
  ASR_SPEECH_TO_TEXT: [
    { id: 'WER', label: 'WER', description: '词错误率，统计替换、删除和插入错误。', defaultEnabled: true },
    { id: 'CER', label: 'CER', description: '字符错误率，中文转录尤其常用。', defaultEnabled: true },
    { id: 'MER', label: 'MER', description: '匹配错误率 Match Error Rate。' },
    { id: 'WIL', label: 'WIL', description: '词信息丢失率 Word Information Lost。' },
    { id: 'WIP', label: 'WIP', description: '词信息保留率 Word Information Preserved。' },
    { id: 'Sentence-Accuracy', label: '句准确率', description: '整句转录完全正确的样本比例。' },
    { id: 'Exact-Match', label: 'Exact Match', description: '归一化后转录文本与参考文本完全一致的比例。' },
    { id: 'RTF', label: '实时率 RTF', description: '处理时间与音频时长之比，越低越快。', defaultEnabled: true },
    ...performance
  ],
  VLM_CHAT: [
    { id: 'Exact-Match', label: 'Exact Match', description: '生成答案与参考答案完全一致的比例。' },
    { id: 'BLEU', label: 'BLEU', description: '生成文本与参考文本的 n-gram 精确匹配程度。' },
    { id: 'ROUGE-1', label: 'ROUGE-1', description: '生成文本与参考文本的一元词重叠。' },
    { id: 'ROUGE-2', label: 'ROUGE-2', description: '生成文本与参考文本的二元词重叠。' },
    { id: 'ROUGE-L', label: 'ROUGE-L', description: '基于最长公共子序列的文本重叠度。', defaultEnabled: true },
    { id: 'METEOR', label: 'METEOR', description: '考虑词形和语义对齐的生成质量指标。' },
    { id: 'BERTScore', label: 'BERTScore', description: '基于上下文向量的生成文本语义相似度。', defaultEnabled: true },
    { id: 'Relevance', label: '回答相关性', description: '回答与图像及问题的相关程度。', defaultEnabled: true },
    { id: 'Faithfulness', label: '忠实度', description: '回答是否忠实于输入图像内容。' },
    { id: 'Hallucination-Rate', label: '幻觉率', description: '输出中缺少输入依据的内容比例，越低越好。' },
    { id: 'CLIPScore', label: 'CLIPScore', description: '图像与生成文本在跨模态空间中的一致程度。' },
    ...performance
  ],
  CUSTOM_JSON_PROCESS: [
    { id: 'Accuracy', label: 'Accuracy', description: '自定义任务整体准确率。', defaultEnabled: true },
    { id: 'Precision', label: 'Precision', description: '自定义任务精确率。' },
    { id: 'Recall', label: 'Recall', description: '自定义任务召回率。' },
    { id: 'F1-Score', label: 'F1', description: '自定义任务 Precision 与 Recall 的调和平均。' },
    { id: 'CustomMetric', label: '自定义指标', description: '由甲方自研 Adapter 返回的业务指标。', defaultEnabled: true },
    ...performance
  ]
}

const categoryFallbacks: Record<string, MetricOption[]> = {
  OBJECT_DETECTION: detectionBase,
  OCR: catalogs.PADDLEOCR_CRNN_REC,
  IMAGE_CLASSIFICATION: classification,
  SEMANTIC_ANALYSIS: catalogs.TRANSFORMERS_NLP,
  SPEECH_RECOGNITION: catalogs.ASR_SPEECH_TO_TEXT,
  VISION_LANGUAGE: catalogs.VLM_CHAT,
  CUSTOM: catalogs.CUSTOM_JSON_PROCESS
}

export function metricsForModel(model: any): MetricOption[] {
  return catalogs[model?.adapterType] || categoryFallbacks[model?.modelCategory] || catalogs.CUSTOM_JSON_PROCESS
}
