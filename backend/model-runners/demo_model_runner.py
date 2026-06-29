import hashlib
import json
import sys
import time


def stable_score(seed: str, base: float) -> float:
    digest = hashlib.sha256(seed.encode("utf-8")).hexdigest()
    offset = int(digest[:4], 16) % 1800 / 10000
    return round(min(0.98, base + offset), 4)


def main() -> int:
    request = json.load(sys.stdin)
    model_id = request.get("modelId", "unknown")
    modality = request.get("modality", "unknown")
    inputs = request.get("inputs") or []
    metrics = request.get("requestedMetrics") or ["mAP", "Precision", "Recall"]
    outputs = []
    for item in inputs:
        input_id = item.get("inputId", "input")
        outputs.append({
            "inputId": input_id,
            "label": f"{model_id}.{modality}.demo",
            "confidence": stable_score(model_id + input_id, 0.72),
            "extra": {
                "sourceUri": item.get("sourceUri"),
                "runner": "local-demo-python",
                "attributes": item.get("attributes") or {}
            }
        })
    result = {
        "outputs": outputs,
        "metrics": [
            {
                "name": metric,
                "value": stable_score(model_id + metric, 0.76),
                "unit": "ratio",
                "description": f"{model_id} {metric} demo metric"
            }
            for metric in metrics
        ],
        "durationMs": max(35, len(inputs) * 30),
        "runnerStatus": "COMPLETED",
        "finishedAt": int(time.time() * 1000)
    }
    json.dump(result, sys.stdout, ensure_ascii=False)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

